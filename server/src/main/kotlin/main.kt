import bitspittle.ipc.common.Port
import bitspittle.paintkit.server.PaintKitServer
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType
import kotlinx.cli.default
import kotlinx.coroutines.runBlocking
import java.time.Duration
import java.util.*

object Settings {
    lateinit var waitFor: Duration
        internal set
}

fun main(args: Array<String>) {
    val parser = ArgParser("server")

    val portArg by parser.option(
        ArgType.Int,
        fullName = "port",
        description = "Port used for socket connection. Leave at 0 to let the system choose it."
    ).default(Port.DYNAMIC.value)

    val userArg by parser.option(
        ArgType.String,
        fullName = "admin",
        description = "Admin user ID (UUID), which you can optionally specify if you would rather create it on the client."
    )

    val waitForArg by parser.option(
        ArgType.Int,
        fullName = "wait_for",
        description = "How long to wait (in seconds) for the server to automatically shutdown when no clients are connected."
    ).default(5)

    parser.parse(args)

    val port = Port(portArg)

    val defaultId = UUID.randomUUID()!!
    val userArgOrDefault = userArg ?: defaultId.toString()
    val userId = try { UUID.fromString(userArgOrDefault)!! } catch (ex: IllegalArgumentException) { defaultId }

    Settings.waitFor = Duration.ofSeconds(waitForArg.toLong())

    runBlocking {
        PaintKitServer().run(port) { port ->
            println("PaintKit server is starting on port $port, waiting for admin $userId")
            println("Will shut down if there is ever a period of ${Settings.waitFor.toSeconds()} seconds with no clients connected.")
        }
    }
}