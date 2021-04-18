package bitspittle.paintkit.api.proto

import bitspittle.paintkit.model.graphics.Color
import bitspittle.paintkit.model.graphics.Colors
import bitspittle.paintkit.model.graphics.Pt
import bitspittle.paintkit.model.graphics.Size
import bitspittle.paintkit.model.graphics.image.*
import com.google.protobuf.ByteString
import java.nio.ByteBuffer
import java.util.*

fun ByteArray.toByteString(): ByteString {
    return ByteString.copyFrom(this)
}

fun ApiProto.Id.toUUID(): UUID {
    val buffer = ByteBuffer.wrap(value.toByteArray())
    return UUID(buffer.long, buffer.long)
}

fun UUID.toProto(): ApiProto.Id {
    val buffer = ByteBuffer.wrap(ByteArray(16));
    buffer.putLong(mostSignificantBits)
    buffer.putLong(leastSignificantBits)

    return ApiProto.Id.newBuilder()
        .setValue(buffer.array().toByteString())
        .build()
}

fun ApiProto.Pt.toModel(): Pt = Pt(x, y)
fun Pt.toProto(): ApiProto.Pt = ApiProto.Pt.newBuilder().setX(x).setY(y).build()

fun ApiProto.Size.toModel(): Size = Size(w, h)
fun Size.toProto(): ApiProto.Size = ApiProto.Size.newBuilder().setW(w).setH(h).build()

fun ApiProto.Color.toModel(): Color = Color(rgba)
fun Color.toProto(): ApiProto.Color = ApiProto.Color.newBuilder().setRgba(packed).build()

fun ApiProto.Pixel.toModel(): Pixel = Pixel(pt.toModel(), color.toModel())
fun Pixel.toProto(): ApiProto.Pixel = ApiProto.Pixel.newBuilder()
    .setColor(color.toProto())
    .setPt(pt.toProto())
    .build()


fun ApiProto.Image.toModel(): MutableImage {
    val modelImage = DefaultImage(size.toModel())
    pixelsList.forEach { pixel ->
        modelImage.setColor(pixel.pt.toModel(), pixel.color.toModel())
    }
    return modelImage
}

fun Image.toProto(id: UUID): ApiProto.Image {
    val protoImage = ApiProto.Image.newBuilder()
    protoImage.id = id.toProto()
    protoImage.size = size.toProto()
    this.pixels
        .filter { it.color != Colors.TRANSPARENT }
        .forEach { pixel ->
            protoImage.addPixels(pixel.toProto())
        }

    return protoImage.build()
}

