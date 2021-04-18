package bitspittle.paintkit.model.graphics.image

interface Canvas {
    val images: List<Image>
}

class MutableCanvas : Canvas {
    override val images = mutableListOf<MutableImage>()
}