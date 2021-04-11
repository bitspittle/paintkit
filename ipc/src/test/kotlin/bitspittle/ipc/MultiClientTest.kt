package bitspittle.ipc

import bitspittle.ipc.client.ClientEnvironment
import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.IpcClient
import bitspittle.ipc.server.*
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.jupiter.api.Test
import java.time.Duration
import java.util.concurrent.ArrayBlockingQueue
import java.util.concurrent.CountDownLatch

class MultiClientTest {
    private class ReceivedEvent(val environment: ClientEnvironment, val bytes: ByteArray) {
        override fun equals(other: Any?): Boolean {
            return (other is ReceivedEvent &&
                    this.environment === other.environment && this.bytes.contentEquals(other.bytes))
        }
    }

    @Test
    fun canCreateMultipleClients() {
        val serverEnvironments = mutableListOf<ServerEnvironment>()
        val createServerHandler: (ServerEnvironment) -> ServerHandler = { environment ->
            serverEnvironments.add(environment)
            object : ServerHandler {
                override fun handleCommand(command: ByteArray, responder: CommandResponder) {
                    responder.respond(command) // Just echo bytes back to mimic a real response
                }
            }
        }

        val clientEnvironments = mutableListOf<ClientEnvironment>()
        val receivedEvents = ArrayBlockingQueue<ReceivedEvent>(2)
        val createClientHandler: (ClientEnvironment) -> ClientHandler = { environment ->
            clientEnvironments.add(environment)
            object : ClientHandler {
                override fun handleEvent(event: ByteArray) {
                    receivedEvents.add(ReceivedEvent(environment, event))
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
            assertThat(clientEnvironments).hasSize(3)

            clientsStarted.countDown()
        }
        clientsStarted.await()

        val fakeBytes0 = byteArrayOf(0)
        val fakeBytes1 = byteArrayOf(1)
        val fakeBytes2 = byteArrayOf(2)

        assertThat(clientEnvironments[0]).isNotSameInstanceAs(clientEnvironments[1])
        assertThat(clientEnvironments[1]).isNotSameInstanceAs(clientEnvironments[2])
        assertThat(clientEnvironments[2]).isNotSameInstanceAs(clientEnvironments[0])

        runBlocking {
            assertThat(clientEnvironments[0].messenger.sendCommand(fakeBytes0)).isEqualTo(fakeBytes0)
            assertThat(clientEnvironments[1].messenger.sendCommand(fakeBytes1)).isEqualTo(fakeBytes1)
            assertThat(clientEnvironments[2].messenger.sendCommand(fakeBytes2)).isEqualTo(fakeBytes2)
        }

        // At this point, since we sent commands, servers should all be initialized at this point too
        assertThat(serverEnvironments).hasSize(3)

        // Direct messenger sends only to self
        serverEnvironments[1].messenger.sendEvent(fakeBytes1)
        assertThat(receivedEvents.take()).isEqualTo(ReceivedEvent(clientEnvironments[1], fakeBytes1))
        assertThat(receivedEvents).isEmpty()

        // Broadcast messenger sends only to others
        serverEnvironments[0].broadcastMessenger.sendEvent(fakeBytes0)
        assertThat(listOf(receivedEvents.take(), receivedEvents.take())).containsExactly(
            ReceivedEvent(clientEnvironments[1], fakeBytes0),
            ReceivedEvent(clientEnvironments[2], fakeBytes0),
        )
        assertThat(receivedEvents).isEmpty()

        serverEnvironments[2].broadcastMessenger.sendEvent(fakeBytes2)
        assertThat(listOf(receivedEvents.take(), receivedEvents.take())).containsExactly(
            ReceivedEvent(clientEnvironments[0], fakeBytes2),
            ReceivedEvent(clientEnvironments[1], fakeBytes2),
        )
        assertThat(receivedEvents).isEmpty()
    }
}