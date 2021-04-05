package bitspittle.ipc.server

import bitspittle.ipc.client.ClientSocketTarget
import bitspittle.ipc.common.ConnectionTarget
import java.net.Socket

// Looks like this class doesn't need to exist, as it just passes through everything, but it's useful to have a
// target with a server type, which provides type safety when combined with extension methods
class ServerSocketTarget(socket: Socket): ConnectionTarget by ClientSocketTarget(socket)
