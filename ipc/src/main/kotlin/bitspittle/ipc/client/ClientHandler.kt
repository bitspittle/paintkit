package bitspittle.ipc.client

import kotlinx.coroutines.CoroutineDispatcher
import java.time.Duration

interface ClientConnection {
    /** Send a command to the client and suspend until we receive a response. */
    suspend fun sendCommand(command: ByteArray): ByteArray

    /** See how long the round trip is from client to server and back. */
    suspend fun ping(): Duration

    /** Disconnect this client immediately. */
    fun disconnect()
}

class ClientEnvironment(
    /** The dispatcher used by this handler, in case the user needs to trampoline to and back from another thread. */
    val dispatcher: CoroutineDispatcher,
)

class ClientContext(
    val environment: ClientEnvironment,
    val connection: ClientConnection,
)

/**
 * A class that lives on the client responsible for handling messages between itself and a server.
 *
 * All handle methods are run on a single thread (that executes in order), so implementors should take care not to block
 * with expensive work. See also: [ClientEnvironment.dispatcher]
 */
interface ClientHandler {
    /** Handle an incoming event from the server. */
    fun handleEvent(event: ByteArray)

    fun handleServerShutdown(message: String) {}
    fun handleDispose() {}
}