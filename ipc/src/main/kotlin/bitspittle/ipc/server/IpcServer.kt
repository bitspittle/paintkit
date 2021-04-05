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

class IpcServer(
    private val createServerHandler: () -> ServerHandler,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO
) {

    private class State(
        val serverTargets: ServerSocketTargets,
        val backgroundScope: CoroutineScope,
        val handler: ServerHandler,
    ) {
        fun dispose() {
            backgroundScope.cancel()
            serverTargets.close()
            handler.onDisposed()
        }
    }

    private var state: State? = null

    val isConnected get() = state != null
    val numClientsConnected get() = state?.serverTargets?.numClientsConnected ?: 0

    fun start(onPortConnected: (Port) -> Unit) =
        start(Port.DYNAMIC, onPortConnected)

    fun start(port: Port, onPortConnected: (Port) -> Unit) {
        shutdown()

        val handler = createServerHandler()
        val backgroundScope = CoroutineScope(backgroundDispatcher)
        val serverTargets = ServerSocketTargets(port, backgroundScope, onPortConnected) { serverTarget ->
            val messenger = object : ServerMessenger {
                override fun sendEvent(event: ByteArray) {
                    serverTarget.send(buildServerMessage {
                        eventBuilder.payload = event.toByteString()
                    })
                }
            }
            handler.onInitialized(messenger)

            backgroundScope.launch {
                serverTarget.received
                    .map { bytes -> ClientMessage.parseFrom(bytes) }
                    .collect { clientMessage ->
                        when (clientMessage.specializedCase) {
                            ClientMessage.SpecializedCase.COMMAND -> handleCommand(
                                serverTarget,
                                handler,
                                clientMessage.command,
                                messenger
                            )
                            ClientMessage.SpecializedCase.PING -> {
                                serverTarget.send(buildServerMessage {
                                    pong = ServerMessage.Pong.getDefaultInstance()
                                })
                            }
                            ClientMessage.SpecializedCase.DISCONNECT -> {
                                state?.serverTargets?.handleDisconnect(serverTarget)
                            }
                            else -> throw Exception("Unexpected clientMessage case: ${clientMessage.specializedCase}")
                        }
                    }
            }
        }

        this.state = State(serverTargets, backgroundScope, handler)
    }

    fun shutdown(message: String = "Server stopped") {
        state?.serverTargets?.sendAll(buildServerMessage {
            shutdownBuilder.apply {
                this.message = message
            }
        })

        handleDisconnect()
    }

    private fun handleDisconnect() {
        state?.dispose()
        state = null
    }

    private fun handleCommand(
        serverTarget: ServerSocketTarget,
        handler: ServerHandler,
        command: ClientMessage.Command,
        messenger: ServerMessenger
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

        handler.handleCommand(command.payload.toByteArray(), responder, messenger)
    }
}
