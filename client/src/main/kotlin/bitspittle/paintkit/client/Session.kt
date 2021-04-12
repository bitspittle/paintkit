package bitspittle.paintkit.client

import androidx.compose.ui.unit.IntOffset
import bitspittle.ipc.client.ClientEnvironment
import bitspittle.ipc.client.ClientHandler

/**
 * Represents the entire state of an active session (canvas, user configs, history, layers, etc., plus a connection to
 * a backing server.)
 */
abstract class Session {
    fun withRecording(block: () -> Unit) {
        beginRecording()
        try {
            block()
        }
        finally {
            endRecording()
        }
    }

    abstract fun disconnect()

    abstract fun beginRecording()
    abstract fun endRecording()

    abstract fun drawLine(p1: IntOffset, p2: IntOffset)
}

internal class ClientHandlerImpl(private val environment: ClientEnvironment) : ClientHandler, Session() {
    override fun disconnect() {
        environment.messenger.disconnect()
    }

    override fun handleEvent(event: ByteArray) {
        // TODO: handle events
    }

    override fun beginRecording() {
        println("Begin recording")
    }
    override fun endRecording() {
        println("End recording")
    }

    override fun drawLine(p1: IntOffset, p2: IntOffset) {
        println("Drawing line from $p1 -> $p2")
    }
}