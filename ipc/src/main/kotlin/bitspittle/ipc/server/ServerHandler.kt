package bitspittle.ipc.server

interface ServerMessenger {
    fun sendEvent(event: ByteArray)
}

interface CommandResponder {
    fun respond(response: ByteArray)
    fun fail(message: String)
}

interface ServerHandler {
    fun onInitialized(messenger: ServerMessenger) {}
    fun onDisposed() {}

    fun handleCommand(command: ByteArray, responder: CommandResponder, messenger: ServerMessenger)
}