package bitspittle.paintkit.client

import bitspittle.ipc.client.ClientContext
import bitspittle.ipc.client.ClientEnvironment
import bitspittle.ipc.client.ClientHandler
import bitspittle.ipc.client.IpcClient
import bitspittle.ipc.common.Port
import kotlinx.coroutines.future.await
import java.io.BufferedReader
import java.io.File
import java.io.InputStreamReader
import java.util.*
import java.util.concurrent.CompletableFuture
import java.util.concurrent.Executors

private val SERVER_PORT_REGEX = """PaintKit server is starting on port (\d+)""".toRegex()

class PaintKitClient(createClientHandler: (ClientContext) -> ClientHandler) {
    private val client = IpcClient(createClientHandler)

    /**
     * Convenience method for calling the proper [start] method depending on the passed in argument.
     */
    suspend fun start(port: Port?) {
        if (port == null) start() else start(port)
    }

    suspend fun start() {
        val javaHome = System.getProperty("java.home")
        val adminId = UUID.randomUUID()

        val portFuture = CompletableFuture<Int>()
        Executors.newSingleThreadExecutor().submit {
            val serverJar = PaintKitClient::class.java.getResourceAsStream("/server.jar")!!.let { stream ->
                File.createTempFile("server", ".jar").apply {
                    appendBytes(stream.readAllBytes())
                    deleteOnExit()
                }
            }

            Runtime.getRuntime().exec("$javaHome/bin/java -jar ${serverJar.absolutePath} --admin $adminId").apply {
                BufferedReader(InputStreamReader(inputStream)).use {
                    try {
                        val serverOutput = it.readLine()!!
                        val result = SERVER_PORT_REGEX.find(serverOutput)
                        if (result != null) {
                            portFuture.complete(result.groupValues[1].toInt())
                        } else {
                            portFuture.completeExceptionally(Exception("Unexpected server output: $serverOutput"))
                        }
                    } catch (t: Throwable) {
                        portFuture.completeExceptionally(Exception("Unexpected server crash: $t"))
                    }
                }
            }
        }

        val port = Port(portFuture.await())
        start(port)
    }

    fun start(port: Port) {
        Runtime.getRuntime().addShutdownHook(Thread { client.disconnect() })
        return client.start(port.toLocalAddress())
    }
}
