package bitspittle.paintkit.image

import bitspittle.ipc.client.ClientConnection
import bitspittle.ipc.client.IpcException
import bitspittle.paintkit.api.proto.toProto
import bitspittle.paintkit.client.sendCommand
import bitspittle.paintkit.model.graphics.Colors
import bitspittle.paintkit.model.graphics.image.Image
import bitspittle.paintkit.model.graphics.image.MutableImage
import bitspittle.paintkit.model.graphics.image.pixels
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import net.jcip.annotations.GuardedBy
import net.jcip.annotations.ThreadSafe
import java.util.*

@ThreadSafe
class PendingImages(
    private val scope: CoroutineScope,
    private val userId: UUID,
    private val connection: ClientConnection,
    private val onRejected: () -> Unit,
) {
    private val lock = Any()

    @GuardedBy("lock")
    private val imageQueue = mutableListOf<Pair<UUID, MutableImage>>()

    // TODO: Delete this, this is just for temporary testing
    val images: List<Image> get() = imageQueue.map { it.second }

    fun push(imageId: UUID, image: MutableImage) {
        synchronized(lock) {
            imageQueue.add(imageId to image)
//            if (imageQueue.size == 1) {
//                processImagesInBackground()
//            }
        }
    }

    private fun processImagesInBackground() {
        scope.launch {
            while (true) {
                val (imageId, image) = synchronized(lock) {
                    imageQueue.removeFirstOrNull()
                } ?: break

                try {
                    connection.sendCommand {
                        updatePixelsCommandBuilder.apply {
                            userId = this@PendingImages.userId.toProto()
                            this.imageId = imageId.toProto()
                            pixelsList.addAll(image.pixels.filter { it.color !== Colors.TRANSPARENT }
                                .map { it.toProto() })
                        }
                    }
                } catch (ex: IpcException) {
                    synchronized(lock) {
                        imageQueue.clear()
                        onRejected()
                    }
                }
            }
        }
    }
}