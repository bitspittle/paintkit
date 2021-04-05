package bitspittle.ipc.common

inline class Port(val value: Int) {
    companion object {
        /**
         * Special port value used to tell the system to automatically choose any available port.
         */
        val DYNAMIC = Port(0)
    }

    fun toLocalAddress() = SocketAddress.createLocal(this)
    override fun toString() = value.toString()
}
class SocketAddress(val host: String, val port: Port) {
    companion object {
        @Suppress("MemberVisibilityCanBePrivate") // Not used at this time but it's a useful constant
        const val LOCAL_HOST = "127.0.0.1"
        fun createLocal(port: Port) = SocketAddress(LOCAL_HOST, port)
    }
}
