package bitspittle.ipc

import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.IpcException
import bitspittle.ipc.server.CommandResponder
import bitspittle.ipc.server.ServerHandler
import com.google.common.truth.Truth.assertThat
import kotlinx.coroutines.runBlocking
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
        val serverShutdown = CountDownLatch(1)
        val clientDisposed = CountDownLatch(1)
        ipcExtension.clientHandler = object : ClientHandler {
            override fun handleEvent(event: ByteArray) = throw NotImplementedError()
            override fun handleServerShutdown(message: String) {
                assertThat(message).isEqualTo(shutdownMessage)
                serverShutdown.countDown()
            }
            override fun handleDispose() {
                // Dispose happens after shutdown
                serverShutdown.await()
                clientDisposed.countDown()
            }
        }

        ipcExtension.server.shutdown(shutdownMessage)
        clientDisposed.await()

        assertThat(ipcExtension.client.isConnected).isFalse()
        assertThat(ipcExtension.server.isConnected).isFalse()
    }

    @Test
    fun disconnectingClientLeavesServerRunning() {
        assertThat(ipcExtension.client.isConnected).isTrue()
        assertThat(ipcExtension.server.isConnected).isTrue()
        assertThat(ipcExtension.server.numClientsConnected).isEqualTo(1)

        val serverDisposed = CountDownLatch(1)
        ipcExtension.serverHandler = object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder) = throw NotImplementedError()
            override fun handleDispose() {
                serverDisposed.countDown()
            }
        }

        ipcExtension.client.disconnect()
        serverDisposed.await()

        assertThat(ipcExtension.client.isConnected).isFalse()
        assertThat(ipcExtension.server.isConnected).isTrue()
        assertThat(ipcExtension.server.numClientsConnected).isEqualTo(0)
    }

    @Test
    fun sendAndReceiveCommand() {
        val fakeCommand = byteArrayOf(1)
        val fakeResponse = byteArrayOf(2)

        ipcExtension.serverHandler = object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder) {
                assertThat(command).isEqualTo(fakeCommand)
                responder.respond(fakeResponse)
            }
        }

        runBlocking {
            val response = ipcExtension.clientEnvironment.messenger.sendCommand(fakeCommand)
            assertThat(response).isEqualTo(fakeResponse)
        }
    }

    @Test
    fun commandFailureResponse() {
        val fakeCommand = byteArrayOf(1)
        val fakeError = "I don't feel like doing work today"

        ipcExtension.serverHandler = object : ServerHandler {
            override fun handleCommand(command: ByteArray, responder: CommandResponder) {
                assertThat(command).isEqualTo(fakeCommand)
                responder.fail(fakeError)
            }
        }

        runBlocking {
            try {
                ipcExtension.clientEnvironment.messenger.sendCommand(fakeCommand)
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
            override fun handleEvent(event: ByteArray) {
                assertThat(event).isEqualTo(fakeEvent)
                eventReceivedLatch.countDown()
            }
        }

        runBlocking {
            ipcExtension.serverEnvironment.messenger.sendEvent(fakeEvent)
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
            override fun handleCommand(command: ByteArray, responder: CommandResponder) {
                when {
                    command.contentEquals(fakeCommand1) -> {
                        ipcExtension.serverEnvironment.messenger.sendEvent(fakeEvent)
                        responder.respond(fakeResponse1)
                    }
                    command.contentEquals(fakeCommand2) -> responder.respond(fakeResponse2)
                    else -> fail()
                }
            }
        }
        var eventReceived = false
        ipcExtension.clientHandler = object : ClientHandler {
            override fun handleEvent(event: ByteArray) {
                assertThat(event).isEqualTo(fakeEvent)
                eventReceived = true
            }
        }

        ipcExtension.clientEnvironment.messenger.let { clientMessenger ->
            runBlocking {
                assertThat(clientMessenger.sendCommand(fakeCommand1)).isEqualTo(fakeResponse1)
                assertThat(eventReceived).isTrue() // Should be sent before response arrives
                assertThat(clientMessenger.sendCommand(fakeCommand2)).isEqualTo(fakeResponse2)
            }
        }
    }
}