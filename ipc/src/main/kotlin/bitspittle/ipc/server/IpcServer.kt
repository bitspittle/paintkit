package bitspittle.ipc.server

import bitspittle.ipc.proto.IpcProto.ClientMessage
import bitspittle.ipc.proto.IpcProto.ServerMessage
import bitspittle.ipc.proto.buildServerMessage
import bitspittle.ipc.proto.send
import bitspittle.ipc.proto.sendAll
import bitspittle.ipc.proto.toByteString
import bitspittle.ipc.common.Port
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import java.util.concurrent.Executors

@ThreadSafe
class IpcServer(
    private val createServerHandler: (ServerEnvironment) -> ServerHandler,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) {
    private inner class State(port: Port, onPortConnected: (Port) -> Unit) {
        val backgroundScope = CoroutineScope(backgroundDispatcher)
        val handlerDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val handlerScope = CoroutineScope(handlerDispatcher)
        @GuardedBy("itself")
        val handlers = mutableSetOf<ServerHandler>()

        var serverTargets: ServerSocketTargets =
            ServerSocketTargets(port, backgroundScope, onPortConnected) { serverTarget ->
                val messenger = object : ServerMessenger {
                    override fun sendEvent(event: ByteArray) {
                        val message = buildServerMessage {
                            eventBuilder.payload = event.toByteString()
                        }
                        serverTarget.send(message)
                    }
                }
                val broadcastMessenger = object : ServerMessenger {
                    override fun sendEvent(event: ByteArray) {
                        val message = buildServerMessage {
                            eventBuilder.payload = event.toByteString()
                        }
                        sendAll(message) { it !== serverTarget }
                    }
                }

                val handler = createServerHandler(ServerEnvironment(messenger, broadcastMessenger, handlerDispatcher))
                synchronized(handlers) { handlers.add(handler) }

                backgroundScope.launch {
                    serverTarget.received
                        .map { bytes -> ClientMessage.parseFrom(bytes) }
                        .collect { clientMessage ->
                            when (clientMessage.specializedCase) {
                                ClientMessage.SpecializedCase.COMMAND ->
                                    handleCommand(serverTarget, handlerScope, handler, clientMessage.command)

                                ClientMessage.SpecializedCase.PING -> {
                                    serverTarget.send(buildServerMessage {
                                        pong = ServerMessage.Pong.getDefaultInstance()
                                    })
                                }
                                ClientMessage.SpecializedCase.DISCONNECT -> {
                                    handleDisconnect(serverTarget)
                                    synchronized(handlers) { handlers.remove(handler) }
                                    handlerScope.launch { handler.handleDispose() }
                                }
                                else -> throw Exception("Unexpected clientMessage case: ${clientMessage.specializedCase}")
                            }
                        }
                }

            }

        fun dispose() {
            backgroundScope.cancel()
            synchronized(handlers) {
                // Block on disposal, as right after this the relevant scope gets cancelled
                runBlocking {
                    handlers.forEach {
                        handlerScope.launch { it.handleDispose() }.join()
                    }
                }
                handlers.clear()
            }
            handlerScope.cancel()
            handlerDispatcher.close()
            serverTargets.close()
        }

        private fun handleCommand(
            serverTarget: ServerSocketTarget,
            handlerScope: CoroutineScope,
            handler: ServerHandler,
            command: ClientMessage.Command,
        ) {
            val responder = object : CommandResponder {
                override fun respond(response: ByteArray) {
                    serverTarget.send(buildServerMessage {
                        responseBuilder.apply {
                            this.id = command.id
                            this.payload = response.toByteString()
                        }
                    })
                }

                override fun fail(message: String) {
                    serverTarget.send(buildServerMessage {
                        errorBuilder.apply {
                            this.id = command.id
                            this.message = message
                        }
                    })
                }
            }

            handlerScope.launch {
                handler.handleCommand(command.payload.toByteArray(), responder)
            }
        }
    }

    private val stateLock = Any()
    @GuardedBy("stateLock")
    private var state: State? = null

    val isConnected get() = synchronized(stateLock) { state != null }
    val numClientsConnected get() = synchronized(stateLock) { state?.serverTargets?.numClientsConnected ?: 0 }

    fun start(onPortConnected: (Port) -> Unit) =
        start(Port.DYNAMIC, onPortConnected)

    fun start(port: Port, onPortConnected: (Port) -> Unit) {
        synchronized(stateLock) {
            shutdown()
            state = State(port, onPortConnected)
        }
    }

    fun shutdown(message: String = "Server stopped") {
        synchronized(stateLock) {
            state?.serverTargets?.sendAll(buildServerMessage {
                shutdownBuilder.apply {
                    this.message = message
                }
            })
        }

        disposeState()
    }

    private fun disposeState() {
        synchronized(stateLock) {
            state?.dispose()
            state = null
        }
    }
}
