package bitspittle.ipc

import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.ClientMessenger
import bitspittle.ipc.client.IpcException
import bitspittle.ipc.server.CommandResponder
import bitspittle.ipc.server.ServerHandler
import bitspittle.ipc.server.ServerMessenger
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
import org.junit.Ignore
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension
import java.util.concurrent.CountDownLatch
import kotlin.test.fail

class IpcTest {
    @RegisterExtension
    @JvmField
    val ipcExtension = IpcExtension()

    @Test
    fun shuttingDownServerDisconnectsClient() {
        assertThat(ipcExtension.client.isConnected).isTrue()
        assertThat(ipcExtension.server.isConnected).isTrue()

        val shutdownMessage = "The server is coming down for maintenance or something"
        val serverShutdownLatch = CountDownLatch(1)
        ipcExtension.clientHandler = object : ClientHandler {
            override fun handleEvent(event: ByteArray, messenger: ClientMessenger) = throw NotImplementedError()
            override fun handleServerShutdown(message: String) {
                assertThat(message).isEqualTo(shutdownMessage)
                serverShutdownLatch.countDown()
            }
        }

        ipcExtension.server.shutdown(shutdownMessage)
        serverShutdownLatch.await()

        assertThat(ipcExtension.client.isConnected).isFalse()
        assertThat(ipcExtension.server.isConnected).isFalse()
    }

    @Test
    fun disconnectingClientLeavesServerRunning() {
        assertThat(ipcExtension.client.isConnected).isTrue()
        assertThat(ipcExtension.server.numClientsConnected).isEqualTo(1)

        ipcExtension.client.disconnect()
        // Currently, no great way to get notified when a client is disconnected from the server, because it
        // intentionally hides that information (outside of the server guts, no one should really care).
        while (ipcExtension.server.numClientsConnected > 0) {
            Thread.sleep(1)
        }

        assertThat(ipcExtension.client.isConnected).isFalse()
        assertThat(ipcExtension.server.isConnected).isTrue()
    }

    @Test
    fun sendAndReceiveCommand() {
        val fakeCommand = byteArrayOf(1)
        val fakeResponse = byteArrayOf(2)

        ipcExtension.serverHandler = object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder, messenger: ServerMessenger) {
                assertThat(command).isEqualTo(fakeCommand)
                responder.respond(fakeResponse)
            }
        }

        runBlocking {
            val response = ipcExtension.clientMessenger.await().sendCommand(fakeCommand)
            assertThat(response).isEqualTo(fakeResponse)
        }
    }

    @Test
    fun commandFailureResponse() {
        val fakeCommand = byteArrayOf(1)
        val fakeError = "I don't feel like doing work today"

        ipcExtension.serverHandler = object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder, messenger: ServerMessenger) {
                assertThat(command).isEqualTo(fakeCommand)
                responder.fail(fakeError)
            }
        }

        runBlocking {
            try {
                ipcExtension.clientMessenger.await().sendCommand(fakeCommand)
                fail()
            }
            catch (ex: IpcException) {
                assertThat(ex.message).isEqualTo(fakeError)
            }
        }
    }


    @Test
    fun sendEvent() {
        val fakeEvent = byteArrayOf(1)

        val eventReceivedLatch = CountDownLatch(1)
        ipcExtension.clientHandler = object : ClientHandler {
            override fun handleEvent(event: ByteArray, messenger: ClientMessenger) {
                assertThat(event).isEqualTo(fakeEvent)
                eventReceivedLatch.countDown()
            }
        }

        runBlocking {
            ipcExtension.serverMessenger.await().sendEvent(fakeEvent)
        }

        eventReceivedLatch.await()
    }

    @Test
    fun sendAndReceiveMultipleMessages() {
        val fakeCommand1 = byteArrayOf(1)
        val fakeResponse1 = byteArrayOf(2)
        val fakeEvent = byteArrayOf(3)
        val fakeCommand2 = byteArrayOf(4)
        val fakeResponse2 = byteArrayOf(5)

        ipcExtension.serverHandler = object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder, messenger: ServerMessenger) {
                when {
                    command.contentEquals(fakeCommand1) -> {
                        messenger.sendEvent(fakeEvent)
                        responder.respond(fakeResponse1)
                    }
                    command.contentEquals(fakeCommand2) -> responder.respond(fakeResponse2)
                    else -> fail()
                }
            }
        }
        var eventReceived = false
        ipcExtension.clientHandler = object : ClientHandler {
            override fun handleEvent(event: ByteArray, messenger: ClientMessenger) {
                assertThat(event).isEqualTo(fakeEvent)
                eventReceived = true
            }
        }

        runBlocking {
            ipcExtension.clientMessenger.await().let { clientMessenger ->
                assertThat(clientMessenger.sendCommand(fakeCommand1)).isEqualTo(fakeResponse1)
                assertThat(eventReceived).isTrue() // Should be sent before response arrives
                assertThat(clientMessenger.sendCommand(fakeCommand2)).isEqualTo(fakeResponse2)
            }
        }
    }
}