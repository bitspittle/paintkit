package bitspittle.ipc

import bitspittle.ipc.client.ClientEnvironment
import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.IpcClient
import bitspittle.ipc.server.*
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.extension.AfterEachCallback
import org.junit.jupiter.api.extension.BeforeEachCallback
import org.junit.jupiter.api.extension.ExtensionContext
import java.time.Duration
import java.util.concurrent.CountDownLatch

class IpcExtension : BeforeEachCallback, AfterEachCallback {
    /** If set, will receive ServerHandler messages */
    var serverHandler: ServerHandler? = null
    /** If set, will receive ClientHandler messages */
    var clientHandler: ClientHandler? = null

    private val _clientEnvironment = CompletableDeferred<ClientEnvironment>()
    private val _serverEnvironment = CompletableDeferred<ServerEnvironment>()
    val clientEnvironment get() = runBlocking { _clientEnvironment.await() }
    val serverEnvironment get() = runBlocking { _serverEnvironment.await() }

    val server = IpcServer(createServerHandler = { environment ->
        _serverEnvironment.complete(environment)
        object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder) {
                serverHandler?.handleCommand(command, responder)
            }
            override fun handleDispose() {
                serverHandler?.handleDispose()
            }
        }
    })

    // "Disable" ping during tests
    val client = IpcClient(pingFrequency = Duration.ofNanos(Long.MAX_VALUE), createClientHandler = { environment ->
        _clientEnvironment.complete(environment)
        object : ClientHandler {
            override fun handleEvent(event: ByteArray) {
                clientHandler?.handleEvent(event)
            }

            override fun handleServerShutdown(message: String) {
                clientHandler?.handleServerShutdown(message)
            }

            override fun handleDispose() {
                clientHandler?.handleDispose()
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