package bitspittle.ipc.client

import kotlinx.coroutines.CoroutineDispatcher

interface ClientMessenger {
    /** Send a command to the client and suspend until we receive a response. */
    suspend fun sendCommand(command: ByteArray): ByteArray

    /** Disconnect this client immediately. */
    fun disconnect()
}

class ClientEnvironment(
    /** A messenger used for communicating to the server. */
    val messenger: ClientMessenger,
    /** The dispatcher used by this handler, in case the user needs to trampoline to and back from another thread. */
    val dispatcher: CoroutineDispatcher,
)

/**
 * A class that lives on the client responsible for handling messages between itself and a server.
 *
 * All handle methods are run on a single thread (that executes in order), so implementors should take care not to block
 * with expensive work. See also: [ClientEnvironment.dispatcher]
 */
interface ClientHandler {
    fun handleEvent(event: ByteArray)
    fun handleServerShutdown(message: String) {}
    fun handleDispose() {}
}