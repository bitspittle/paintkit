package bitspittle.ipc.client

import bitspittle.ipc.common.ConnectionTarget
import bitspittle.ipc.common.SocketAddress
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import net.jcip.annotations.ThreadSafe
import java.io.DataInputStream
import java.io.DataOutputStream
import java.io.EOFException
import java.net.Socket
import java.net.SocketException

// TODO: Replace implementation with async socket
@ThreadSafe
class ClientSocketTarget(private val socket: Socket): ConnectionTarget {
    constructor(address: SocketAddress): this(Socket(address.host, address.port.value))

    private val incoming = DataInputStream(socket.getInputStream())
    private val outgoing = DataOutputStream(socket.getOutputStream())

    override val isConnected get() = !socket.isClosed

    override fun send(bytes: ByteArray) {
        try {
            synchronized(outgoing) {
                outgoing.writeInt(bytes.size) // Let server know how many bytes are coming in
                outgoing.write(bytes)
                outgoing.flush()
            }
        }
        catch (ignored: SocketException) {
            // Socket was closed, that means we're probably in the process of shutting down
        }
    }

    override val received: Flow<ByteArray> = flow {
        while (true) {
            try {
                if (!isConnected) return@flow
                val bytes = synchronized(incoming) {
                    val size = incoming.readInt()
                    incoming.readNBytes(size)!!
                }

                emit(bytes)
            } catch (ex: SocketException) {
                // Socket was closed, abort
                return@flow
            } catch (ex: EOFException) {
                // Socket was closed, abort
                return@flow
            }
        }
    }

    override fun close() {
        incoming.close()
        outgoing.close()
        socket.close()
    }

}