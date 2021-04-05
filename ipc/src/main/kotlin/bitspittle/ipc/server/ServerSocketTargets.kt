package bitspittle.ipc.server

import bitspittle.ipc.common.Port
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import java.net.ServerSocket
import java.net.SocketException

// TODO: Replace implementation with async server socket
/**
 * @param onPortConnected Fired when an actual port is connected; may be different from the passed in port if it is
 *   set to a dynamic value that allows the system to choose.
 */
class ServerSocketTargets(
    port: Port,
    private val backgroundScope: CoroutineScope,
    onPortConnected: (Port) -> Unit,
    onClientConnected: (ServerSocketTarget) -> Unit,
): AutoCloseable {
    private val serverSocket = ServerSocket(port.value).also {
        // Don't let the callback potentially block this class's progress
        backgroundScope.launch { onPortConnected(Port(it.localPort)) }
    }

    private val targets = mutableListOf<ServerSocketTarget>()
    val numClientsConnected get() = targets.size

    init {
        backgroundScope.launch {
            try {
                while (true) {
                    val clientTarget = ServerSocketTarget(serverSocket.accept())
                    synchronized(targets) { targets.add(clientTarget) }
                    onClientConnected(clientTarget)
                }
            }
            catch (ignored: SocketException) {}
        }
    }

    fun sendAll(byteArray: ByteArray) {
        synchronized(targets) {
            targets.forEach { it.send(byteArray) }
        }
    }

    internal fun handleDisconnect(serverTarget: ServerSocketTarget) {
        synchronized(targets) {
            targets.remove(serverTarget)
            serverTarget.close()
        }
    }

    override fun close() {
        synchronized(targets) {
            targets.forEach { it.close() }
            targets.clear()
        }
        serverSocket.close()
    }
}

