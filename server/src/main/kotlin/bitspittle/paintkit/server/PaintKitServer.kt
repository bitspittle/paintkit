package bitspittle.paintkit.server

import Settings
import bitspittle.ipc.common.Port
import bitspittle.ipc.server.CommandResponder
import bitspittle.ipc.server.IpcServer
import bitspittle.ipc.server.ServerEnvironment
import bitspittle.ipc.server.ServerHandler
import kotlinx.coroutines.delay

class PaintKitServer {
    private class Handler(private val environment: ServerEnvironment) : ServerHandler {
        override fun handleCommand(command: ByteArray, responder: CommandResponder) {
            // TODO: Handle command
        }
    }

    private val server = IpcServer({ environment -> Handler(environment) })
    suspend fun run(port: Port, onPortConnected: (Port) -> Unit) {
        server.start(port, onPortConnected)

        // Semi-regularly check if our server is running but with no clients connected to it for a while; at some
        // point, shut down if too much time passes like that, so we don't waste resources
        var shouldQuit = false
        var quitTimeRemainingMs = Settings.waitFor.toMillis()
        var lastNumClientsConnected = 0
        val delayMs = 1000L
        while (server.isConnected && !shouldQuit) {
            delay(delayMs)
            server.numClientsConnected.let { numClientsConnected ->
                if (numClientsConnected == 0) {
                    if (lastNumClientsConnected > 0) {
                        quitTimeRemainingMs = Settings.waitFor.toMillis()
                    }
                    else {
                        quitTimeRemainingMs -= delayMs
                        if (quitTimeRemainingMs < 0) {
                            shouldQuit = true
                        }
                    }
                }
                lastNumClientsConnected = numClientsConnected
            }
        }
    }
}