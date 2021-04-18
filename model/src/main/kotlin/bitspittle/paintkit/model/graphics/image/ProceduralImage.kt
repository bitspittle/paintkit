package bitspittle.paintkit.model.graphics.image

import bitspittle.paintkit.model.graphics.Color
import bitspittle.paintkit.model.graphics.Colors
import bitspittle.paintkit.model.graphics.Pt
import bitspittle.paintkit.model.graphics.Size

/**
 * An image that generates its color value algorithmically, without any backing memory.
 */
class ProceduralImage(override val size: Size, private val colorProvider: (Pt) -> Color) : Image {
    override fun getColor(pt: Pt): Color {
        require(size.toRect().contains(pt))
        return colorProvider(pt)
    }
}

/**
 * Generate an image that is just a single, uniform color
 */
fun SolidColorImage(size: Size, color: Color) = ProceduralImage(size) { color }

/**
 * Generate a larger image by repeating a smaller image infinitely.
 */
fun RepeatedImage(size: Size, image: Image) = ProceduralImage(size) { pt ->
    image.getColor(Pt(pt.x % image.size.w, pt.y % image.size.h))
}

/**
 * Create a 2x2 checker tile image.
 *
 * This should work really well passed in as an argument to [RepeatedImage]
 */
fun CheckerTileImage(checkerSide: Int = 8, onColor: Color = Colors.BLACK, offColor: Color = Colors.WHITE) =
    ProceduralImage(Size(checkerSide * 2, checkerSide * 2)) { pt ->
        if (pt.x in 0 until checkerSide) {
            if (pt.y in 0 until checkerSide) {
                onColor
            } else {
                offColor
            }
        } else {
            if (pt.y in 0 until checkerSide) {
                offColor
            } else {
                onColor
            }
        }
    }