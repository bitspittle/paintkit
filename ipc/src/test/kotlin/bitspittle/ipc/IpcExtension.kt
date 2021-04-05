package bitspittle.ipc

import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.ClientMessenger
import bitspittle.ipc.client.IpcClient
import bitspittle.ipc.server.CommandResponder
import bitspittle.ipc.server.IpcServer
import bitspittle.ipc.server.ServerHandler
import bitspittle.ipc.server.ServerMessenger
import kotlinx.coroutines.CompletableDeferred
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.Duration
import java.util.concurrent.CountDownLatch

class IpcExtension : BeforeEachCallback, AfterEachCallback {
    var serverHandler: ServerHandler? = null
    var clientHandler: ClientHandler? = null

    val clientMessenger = CompletableDeferred<ClientMessenger>()
    val serverMessenger = CompletableDeferred<ServerMessenger>()

    val server = IpcServer(createServerHandler = {
        object : ServerHandler {
            override fun onInitialized(messenger: ServerMessenger) {
                serverMessenger.complete(messenger)
            }

            override fun handleCommand(command: ByteArray, responder: CommandResponder, messenger: ServerMessenger) {
                serverHandler?.handleCommand(command, responder, messenger)
            }
        }
    })

    // "Disable" ping during tests
    val client = IpcClient(pingFrequency = Duration.ofNanos(Long.MAX_VALUE), createClientHandler = {
        object : ClientHandler {
            override fun onInitialized(messenger: ClientMessenger) {
                clientMessenger.complete(messenger)
            }

            override fun handleEvent(event: ByteArray, messenger: ClientMessenger) {
                clientHandler?.handleEvent(event, messenger)
            }

            override fun handleServerShutdown(message: String) {
                clientHandler?.handleServerShutdown(message)
            }
        }
    })

    override fun beforeEach(context: ExtensionContext) {
        val clientConnected = CountDownLatch(1)
        server.start { port ->
            client.start(port.toLocalAddress())
            clientConnected.countDown()
        }
        clientConnected.await()
    }

    override fun afterEach(context: ExtensionContext) {
        client.disconnect()
        server.shutdown()
    }
}