package bitspittle.paintkit.model.graphics.image

import bitspittle.paintkit.model.graphics.Color
import bitspittle.paintkit.model.graphics.Pt
import bitspittle.paintkit.model.graphics.Size

/** An image pointer which acts as a view into a subsection of an actual image */
class ImageSection(private val source: Image, val pt: Pt, override val size: Size) : Image {
    init {
        source.bounds.let { bounds ->
            require(bounds.contains(pt))
            require(bounds.contains(pt + size))
        }
    }

    override fun getColor(pt: Pt): Color {
        require(size.toRect().contains(pt))
        val absPt = this.pt + pt
        return source.getColor(absPt)
    }
}