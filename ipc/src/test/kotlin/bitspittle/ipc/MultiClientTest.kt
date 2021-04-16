package bitspittle.ipc

import bitspittle.ipc.client.ClientContext
import bitspittle.ipc.client.ClientEnvironment
import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.IpcClient
import bitspittle.ipc.server.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.*
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch

class MultiClientTest {
    private class ReceivedEvent(val context: ClientContext, val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            return (other is ReceivedEvent &&
                    this.context === other.context && this.bytes.contentEquals(other.bytes))
        }

        override fun hashCode(): Int {
            return Objects.hash(context, bytes)
        }
    }

    @Test
    fun canCreateMultipleClients() {
        val serverContexts = mutableListOf<ServerContext>()
        val createServerHandler: (ServerContext) -> ServerHandler = { context ->
            serverContexts.add(context)
            object : ServerHandler {
                override fun handleCommand(command: ByteArray, responder: CommandResponder) {
                    responder.respond(command) // Just echo bytes back to mimic a real response
                }
            }
        }

        val clientContexts = mutableListOf<ClientContext>()
        val receivedEvents = ArrayBlockingQueue<ReceivedEvent>(1)
        val createClientHandler: (ClientContext) -> ClientHandler = { context ->
            clientContexts.add(context)
            object : ClientHandler {
                override fun handleEvent(event: ByteArray) {
                    receivedEvents.add(ReceivedEvent(context, event))
                }
            }
        }

        val server = IpcServer(createServerHandler)
        // "Disable" ping during tests
        val clients = Array(3) { IpcClient(createClientHandler, pingFrequency = Duration.ofNanos(Long.MAX_VALUE)) }

        val clientsStarted = CountDownLatch(1)
        server.start { port ->
            val address = port.toLocalAddress()
            clients[0].start(address)
            clients[1].start(address)
            clients[2].start(address)
            assertThat(clientContexts).hasSize(3)

            clientsStarted.countDown()
        }
        clientsStarted.await()

        val fakeBytes0 = byteArrayOf(0)
        val fakeBytes1 = byteArrayOf(1)
        val fakeBytes2 = byteArrayOf(2)

        assertThat(clientContexts[0]).isNotSameInstanceAs(clientContexts[1])
        assertThat(clientContexts[1]).isNotSameInstanceAs(clientContexts[2])
        assertThat(clientContexts[2]).isNotSameInstanceAs(clientContexts[0])

        runBlocking {
            assertThat(clientContexts[0].connection.sendCommand(fakeBytes0)).isEqualTo(fakeBytes0)
            assertThat(clientContexts[1].connection.sendCommand(fakeBytes1)).isEqualTo(fakeBytes1)
            assertThat(clientContexts[2].connection.sendCommand(fakeBytes2)).isEqualTo(fakeBytes2)
        }

        // At this point, since we sent commands, servers should all be initialized at this point too
        assertThat(serverContexts).hasSize(3)

        // The main messenger sends only to self
        serverContexts[1].connection.sendEvent(fakeBytes1)
        assertThat(receivedEvents.take()).isEqualTo(ReceivedEvent(clientContexts[1], fakeBytes1))
        assertThat(receivedEvents).isEmpty()

        serverContexts[0].connection.sendEvent(fakeBytes0)
        assertThat(receivedEvents.take()).isEqualTo(ReceivedEvent(clientContexts[0], fakeBytes0))
        assertThat(receivedEvents).isEmpty()

        serverContexts[2].connection.sendEvent(fakeBytes2)
        assertThat(receivedEvents.take()).isEqualTo(ReceivedEvent(clientContexts[2], fakeBytes2))
        assertThat(receivedEvents).isEmpty()
    }
}