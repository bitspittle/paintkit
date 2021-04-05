package bitspittle.ipc.client

import bitspittle.ipc.proto.*
import bitspittle.ipc.proto.IpcProto.ClientMessage
import bitspittle.ipc.proto.IpcProto.ServerMessage
import bitspittle.ipc.common.SocketAddress
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.future.await
import java.time.Duration
import java.util.*
import java.util.concurrent.CompletableFuture

class IpcClient(
    private val createClientHandler: () -> ClientHandler,
    private val backgroundDispatcher: CoroutineDispatcher = Dispatchers.IO,
    private val pingFrequency: Duration = Duration.ofSeconds(30)
) {
    private class State(
        val clientTarget: ClientSocketTarget,
        val backgroundScope: CoroutineScope,
        val handler: ClientHandler
    ) {
        val responseFutures = mutableMapOf<IpcProto.Id, CompletableFuture<ByteArray>>()

        fun dispose() {
            responseFutures.clear()
            backgroundScope.cancel()
            clientTarget.close()
            handler.onDisposed()
        }
    }

    private var state: State? = null

    val isConnected get() = state != null

    fun start(address: SocketAddress): ClientMessenger {
        disconnect()

        val state = State(ClientSocketTarget(address), CoroutineScope(backgroundDispatcher), createClientHandler())

        val self = this
        val messenger = object : ClientMessenger {
            override suspend fun sendCommand(command: ByteArray): ByteArray {
                val id = UUID.randomUUID().toId()
                val responseFuture = CompletableFuture<ByteArray>()
                state.responseFutures[id] = responseFuture

                state.clientTarget.send(buildClientMessage {
                    commandBuilder.apply {
                        this.id = id
                        this.payload = command.toByteString()
                    }
                })

                return responseFuture.await()
            }

            override fun disconnect() {
                self.disconnect()
            }
        }
        state.handler.onInitialized(messenger)

        state.backgroundScope.launch {
            state.clientTarget.received
                .map { bytes -> ServerMessage.parseFrom(bytes) }
                .collect { serverMessage ->
                    when (serverMessage.specializedCase) {
                        ServerMessage.SpecializedCase.RESPONSE ->
                            state.responseFutures.remove(serverMessage.response.id)!!
                                .complete(serverMessage.response.payload.toByteArray())
                        ServerMessage.SpecializedCase.ERROR -> {
                            state.responseFutures.remove(serverMessage.error.id)!!
                                .completeExceptionally(IpcException(serverMessage.error.message))
                        }

                        ServerMessage.SpecializedCase.EVENT ->
                            state.handler.handleEvent(serverMessage.event.payload.toByteArray(), messenger)

                        ServerMessage.SpecializedCase.PONG -> {
                            // No-op for now, ping/pong just used as a keep alive
                        }

                        ServerMessage.SpecializedCase.SHUTDOWN -> {
                            handleDisconnect()
                            state.handler.handleServerShutdown(serverMessage.shutdown.message)
                        }

                        else -> throw Exception("Unexpected ServerMessage case: ${serverMessage.specializedCase}")
                    }

                }
        }

        state.backgroundScope.launch {
            while (true) {
                delay(pingFrequency.toMillis())
                state.clientTarget.send(buildClientMessage {
                    ping = ClientMessage.Ping.getDefaultInstance()
                })
            }
        }

        this.state = state
        return messenger
    }

    fun disconnect() {
        state?.clientTarget?.send(buildClientMessage {
            disconnect = ClientMessage.Disconnect.getDefaultInstance()
        })

        handleDisconnect()
    }

    private fun handleDisconnect() {
        state?.dispose()
        state = null
    }
}