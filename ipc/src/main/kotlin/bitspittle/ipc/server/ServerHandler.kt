package bitspittle.ipc.server

import kotlinx.coroutines.CoroutineDispatcher

interface ServerMessenger {
    /** Fire and forget an event to the client. */
    fun sendEvent(event: ByteArray)
}

/**
 * Callback class that must be triggered in response to a command from the client (or else the client will be left
 * hanging)
 */
interface CommandResponder {
    fun respond(response: ByteArray)
    fun fail(message: String)
}

class ServerEnvironment(
    /** Sends messages to the single client associated with this handler */
    val messenger: ServerMessenger,
    /** Sends messages to all other clients connected to the server (not ourselves). */
    val broadcastMessenger: ServerMessenger,
    /** The dispatcher used by this handler, in case the user needs to trampoline to and back from another thread. */
    val dispatcher: CoroutineDispatcher,
)

/**
 * A class that lives on the server responsible for handling messages between itself and a client.
 *
 * Note if there are many clients, there will be one handler allocated per client. However, they
 * will all share the same dispatcher, so if any of the handling logic is expensive, it should be
 * delegated to another thread.
 *
 * All handle methods are run on a single thread (that executes in order), so implementors should take care not to block
 * with expensive work. See also: [ServerEnvironment.dispatcher]
 */
interface ServerHandler {
    fun handleCommand(command: ByteArray, responder: CommandResponder)
    fun handleDispose() {}
}