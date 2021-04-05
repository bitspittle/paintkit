package bitspittle.ipc.common

import kotlinx.coroutines.flow.Flow

interface ConnectionTarget : AutoCloseable {
    fun send(bytes: ByteArray)
    val received: Flow<ByteArray>
    val isConnected: Boolean
}
