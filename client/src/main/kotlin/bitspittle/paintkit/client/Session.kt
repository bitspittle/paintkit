package bitspittle.paintkit.client

import bitspittle.ipc.client.ClientConnection
import bitspittle.ipc.client.ClientContext
import bitspittle.ipc.client.ClientHandler
import bitspittle.paintkit.api.proto.ApiProto
import bitspittle.paintkit.api.proto.toProto
import bitspittle.paintkit.api.proto.toUUID
import bitspittle.paintkit.model.graphics.Size
import bitspittle.paintkit.model.graphics.image.DefaultImage
import bitspittle.paintkit.model.graphics.image.Image
import bitspittle.paintkit.model.graphics.image.MutableImage
import java.io.File
import java.util.*

/**
 * Represents the entire state of an active session (canvas, user configs, history, layers, etc., plus a connection to
 * a backing server.)
 */
interface Session {
    val userId: UUID
    val clientContext: ClientContext
    val canvases: List<CanvasState>

    fun disconnect()

    suspend fun createCanvas(size: Size): CanvasState
}

class CanvasState(val id: UUID, val imageId: UUID, size: Size) {
    val image = DefaultImage(size)
    var file: File? = null
}

internal class ClientHandlerImpl(override val userId: UUID, override val clientContext: ClientContext) : ClientHandler, Session {
    override val canvases = mutableListOf<CanvasState>()

    override fun disconnect() {
        clientContext.connection.disconnect()
    }

    override fun handleEvent(event: ByteArray) {
        // TODO: handle events
    }

    override suspend fun createCanvas(size: Size): CanvasState {
        return clientContext.connection.sendCommand {
            createCanvasCommand = ApiProto.Command.CreateCanvas.newBuilder()
                .setUserId(userId.toProto())
                .setSize(size.toProto())
                .build()
        }.let { response ->
            CanvasState(
                response.createCanvasResponse.canvasId.toUUID(),
                response.createCanvasResponse.imageId.toUUID(),
                size
            ).also {
                canvases.add(it)
            }
        }
    }
}

suspend fun ClientConnection.sendCommand(init: ApiProto.Command.Builder.() -> Unit): ApiProto.Response {
    val command = ApiProto.Command.newBuilder().apply(init).build()
    return sendCommand(command.toByteArray()).let { bytes -> ApiProto.Response.parseFrom(bytes) }
}
