package bitspittle.ipc.client

interface ClientMessenger {
    suspend fun sendCommand(command: ByteArray): ByteArray

    fun disconnect()
}

interface ClientHandler {
    fun onInitialized(messenger: ClientMessenger) {}
    fun onDisposed() {}

    fun handleEvent(event: ByteArray, messenger: ClientMessenger)

    fun handleServerShutdown(message: String) {}
}