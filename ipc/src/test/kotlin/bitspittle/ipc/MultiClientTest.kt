package bitspittle.ipc

import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.ClientMessenger
import bitspittle.ipc.client.IpcClient
import bitspittle.ipc.server.CommandResponder
import bitspittle.ipc.server.IpcServer
import bitspittle.ipc.server.ServerHandler
import bitspittle.ipc.server.ServerMessenger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.CountDownLatch

class MultiClientTest {
    @Test
    fun canCreateMultipleClients() {
        val createServerHandler: () -> ServerHandler = {
            object : ServerHandler {
                override fun handleCommand(
                    command: ByteArray,
                    responder: CommandResponder,
                    messenger: ServerMessenger
                ) {
                    responder.respond(command) // Just echo bytes back to mimic a real response
                }
            }
        }

        val createClientHandler: () -> ClientHandler = {
            object : ClientHandler {
                override fun handleEvent(event: ByteArray, messenger: ClientMessenger) = throw NotImplementedError()
            }
        }

        val server = IpcServer(createServerHandler)
        // "Disable" ping during tests
        val clients = Array(3) { IpcClient(createClientHandler, pingFrequency = Duration.ofNanos(Long.MAX_VALUE)) }

        val responsesAsserted = CountDownLatch(1)
        server.start { port ->
            val address = port.toLocalAddress()
            val messenger0 = clients[0].start(address)
            val messenger1 = clients[1].start(address)
            val messenger2 = clients[2].start(address)

            val fakeBytes0 = byteArrayOf(0)
            val fakeBytes1 = byteArrayOf(1)
            val fakeBytes2 = byteArrayOf(2)

            runBlocking {
                assertThat(messenger0.sendCommand(fakeBytes0)).isEqualTo(fakeBytes0)
                assertThat(messenger1.sendCommand(fakeBytes1)).isEqualTo(fakeBytes1)
                assertThat(messenger2.sendCommand(fakeBytes2)).isEqualTo(fakeBytes2)
            }

            responsesAsserted.countDown()
        }
        responsesAsserted.await()
    }
}