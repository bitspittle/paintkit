package bitspittle.paintkit.model.graphics.image

import bitspittle.paintkit.model.graphics.*

data class Pixel(val pt: Pt, val color: Color)

interface Image {
    val size: Size
    fun getColor(pt: Pt): Color
}

val Image.pixels: Iterable<Pixel>
    get() = object : Iterable<Pixel> {
        override fun iterator(): Iterator<Pixel> {
            return iterator {
                for (x in 0 until size.w) {
                    for (y in 0 until size.h) {
                        val pt = Pt(x, y)
                        yield(Pixel(pt, getColor(pt)))
                    }
                }
            }
        }
    }


interface MutableImage : Image {
    fun setColor(pt: Pt, color: Color)
}

fun MutableImage.clear() {
    pixels.forEach { pixel -> setColor(pixel.pt, Colors.TRANSPARENT) }
}

val Image.bounds: Rect
    get() = Rect(Pt.ORIGIN, size)

/** A simple image implementation which just allocates all the memory upfront */
class DefaultImage(override val size: Size) : MutableImage {
    private val buffer = IntArray(size.w * size.h)
    private operator fun IntArray.get(x: Int, y: Int) = buffer[y * this@DefaultImage.size.w + x]
    private operator fun IntArray.set(x: Int, y: Int, value: Int) {
        buffer[y * this@DefaultImage.size.h + x] = value
    }

    override fun getColor(pt: Pt): Color {
        require(size.toRect().contains(pt))
        return Color(buffer[pt.x, pt.y])
    }

    override fun setColor(pt: Pt, color: Color) {
        require(size.toRect().contains(pt))
        buffer[pt.x, pt.y] = color.packed
    }
}