package bitspittle.ipc.client

import bitspittle.ipc.common.SocketAddress
import bitspittle.ipc.proto.*
import bitspittle.ipc.proto.IpcProto.ClientMessage
import bitspittle.ipc.proto.IpcProto.ServerMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

@ThreadSafe
class IpcClient(
    private val createClientHandler: (ClientEnvironment) -> ClientHandler,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pingFrequency: Duration = Duration.ofSeconds(30)
) {
    private inner class State(address: SocketAddress) {
        val clientTarget = ClientSocketTarget(address)
        val backgroundScope = CoroutineScope(backgroundDispatcher)
        val handlerDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()
        val handlerScope = CoroutineScope(Executors.newSingleThreadExecutor().asCoroutineDispatcher())

        val responseFutures = mutableMapOf<IpcProto.Id, CompletableFuture<ByteArray>>()
        val handler: ClientHandler

        init {
            val messenger = object : ClientMessenger {
                override suspend fun sendCommand(command: ByteArray): ByteArray {
                    val id = UUID.randomUUID().toId()
                    val responseFuture = CompletableFuture<ByteArray>()
                    responseFutures[id] = responseFuture

                    clientTarget.send(buildClientMessage {
                        commandBuilder.apply {
                            this.id = id
                            this.payload = command.toByteString()
                        }
                    })

                    return responseFuture.await()
                }

                override fun disconnect() {
                    this@IpcClient.disconnect()
                }
            }

            handler = createClientHandler(ClientEnvironment(messenger, handlerDispatcher))

            backgroundScope.launch {
                clientTarget.received
                    .map { bytes -> ServerMessage.parseFrom(bytes) }
                    .collect { serverMessage ->
                        when (serverMessage.specializedCase) {
                            ServerMessage.SpecializedCase.RESPONSE ->
                                responseFutures.remove(serverMessage.response.id)!!
                                    .complete(serverMessage.response.payload.toByteArray())
                            ServerMessage.SpecializedCase.ERROR -> {
                                responseFutures.remove(serverMessage.error.id)!!
                                    .completeExceptionally(IpcException(serverMessage.error.message))
                            }

                            ServerMessage.SpecializedCase.EVENT ->
                                handlerScope.launch {
                                    handler.handleEvent(serverMessage.event.payload.toByteArray())
                                }

                            ServerMessage.SpecializedCase.PONG -> {
                                // No-op for now, ping/pong just used as a keep alive
                            }

                            ServerMessage.SpecializedCase.SHUTDOWN -> {
                                handlerScope.launch {
                                    handler.handleServerShutdown(serverMessage.shutdown.message)
                                }.join() // Wait before calling disposeState, as that cancels the scope
                                disposeState()
                            }

                            else -> throw Exception("Unexpected ServerMessage case: ${serverMessage.specializedCase}")
                        }

                    }
            }

            backgroundScope.launch {
                while (true) {
                    delay(pingFrequency.toMillis())
                    clientTarget.send(buildClientMessage {
                        ping = ClientMessage.Ping.getDefaultInstance()
                    })
                }
            }
        }

        fun disconnect() {
            clientTarget.send(buildClientMessage {
                disconnect = ClientMessage.Disconnect.getDefaultInstance()
            })
        }

        fun dispose() {
            responseFutures.clear()
            backgroundScope.cancel()
            runBlocking {
                // Block on disposal, as right after this the scope gets cancelled
                handlerScope.launch { handler.handleDispose() }.join()
            }
            handlerScope.cancel()
            handlerDispatcher.close()
            clientTarget.close()
        }
    }

    private val stateLock = Any()
    @GuardedBy("stateLock")
    private var state: State? = null

    val isConnected get() = synchronized(stateLock) { state != null }

    fun start(address: SocketAddress) {
        disconnect()
        synchronized(stateLock) {
            state = State(address)
        }
    }

    fun disconnect() {
        synchronized(stateLock) {
            state?.disconnect()
            disposeState()
        }
    }

    private fun disposeState() {
        synchronized(stateLock) {
            state?.dispose()
            state = null
        }
    }

}