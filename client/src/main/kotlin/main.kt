import bitspittle.ipc.common.Port
import bitspittle.paintkit.windows.startApp
import kotlinx.cli.ArgParser
import kotlinx.cli.ArgType

object Settings {
    var debugPort: Port? = null
        internal set
}

fun main(args: Array<String>) {
    val parser = ArgParser("client")

    val portArg by parser.option(
        ArgType.Int,
        fullName = "port",
        shortName = "p",
        description = "If specified, connect to an existing server rather than create a new one. Useful for running " +
                "against a debug server."
    )

    parser.parse(args)

    Settings.debugPort = portArg?.let { Port(it) }

    startApp()
}
