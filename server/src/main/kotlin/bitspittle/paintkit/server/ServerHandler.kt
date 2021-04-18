package bitspittle.paintkit.server

import bitspittle.ipc.server.CommandResponder
import bitspittle.ipc.server.ServerConnection
import bitspittle.ipc.server.ServerContext
import bitspittle.ipc.server.ServerHandler
import bitspittle.paintkit.api.proto.ApiProto
import bitspittle.paintkit.api.proto.toModel
import bitspittle.paintkit.api.proto.toProto
import bitspittle.paintkit.api.proto.toUUID
import bitspittle.paintkit.model.graphics.Size
import bitspittle.paintkit.model.graphics.image.DefaultImage
import bitspittle.paintkit.model.graphics.image.MutableCanvas
import bitspittle.paintkit.model.graphics.image.MutableImage
import bitspittle.paintkit.model.graphics.image.SparseImage
import java.util.*

class ServerHandlerImpl(private val ctx: ServerContext) : ServerHandler {
    private val canvases = mutableMapOf<UUID, MutableCanvas>()
    private val images = mutableMapOf<UUID, MutableImage>()

    override fun handleCommand(command: ByteArray, responder: CommandResponder) {
        val command = ApiProto.Command.parseFrom(command)
        when (command.specializedCase) {
            ApiProto.Command.SpecializedCase.CREATECANVASCOMMAND -> {
                handle(command.createCanvasCommand, responder)
            }
            ApiProto.Command.SpecializedCase.DELETECANVASCOMMAND -> {
                handle(command.deleteCanvasCommand, responder)
            }
            ApiProto.Command.SpecializedCase.UPDATEPIXELSCOMMAND -> {
                handle(command.updatePixelsCommand, responder)

            }
            else -> responder.fail("Unhandled command case: " + command.specializedCase)
        }
    }

    private fun handle(createCanvasCommand: ApiProto.Command.CreateCanvas, responder: CommandResponder) {
        val canvasId = UUID.randomUUID()
        val imageId = UUID.randomUUID()

        val image = SparseImage(Size(createCanvasCommand.size.w, createCanvasCommand.size.h))
        val canvas = MutableCanvas()
        canvas.images.add(image)

        images[imageId] = image
        canvases[canvasId] = canvas

        responder.respond {
            createCanvasResponse = ApiProto.Response.CreateCanvas.newBuilder()
                .setCanvasId(canvasId.toProto())
                .setImageId(imageId.toProto())
                .build()
        }
    }

    private fun handle(deleteCanvasCommand: ApiProto.Command.DeleteCanvas, responder: CommandResponder) {
        images.remove(deleteCanvasCommand.canvasId.toUUID())
        responder.respond {
            deleteCanvasResponse = ApiProto.Response.DeleteCanvas.getDefaultInstance()
        }
    }

    private fun handle(updatePixelsCommand: ApiProto.Command.UpdatePixels, responder: CommandResponder) {
        val id = updatePixelsCommand.imageId.toUUID()
        images[id]?.let { image ->
            updatePixelsCommand.pixelsList.forEach { protoPixel ->
                image.setColor(protoPixel.pt.toModel(), protoPixel.color.toModel())
            }
        }
        responder.respond {
            updatePixelsResponse = ApiProto.Response.UpdatePixels.getDefaultInstance()
        }
    }
}

private fun ServerConnection.sendEvent(init: ApiProto.Event.Builder.() -> Unit) {
    sendEvent(ApiProto.Event.newBuilder().apply(init).build().toByteArray())
}

private fun CommandResponder.respond(init: ApiProto.Response.Builder.() -> Unit) {
    respond(ApiProto.Response.newBuilder().apply(init).build().toByteArray())
}

