package bitspittle.paintkit.model.graphics.image

import bitspittle.paintkit.model.graphics.Color
import bitspittle.paintkit.model.graphics.Colors
import bitspittle.paintkit.model.graphics.Pt
import bitspittle.paintkit.model.graphics.Size

/**
 * An image optimized for memory savings, in that it behaves as a normal image but only allocates parts of it on
 * demand as needed. Unallocated areas are treated as completely transparent (i.e. [Colors.TRANSPARENT]).
 */
class SparseImage(override val size: Size, val chunkSize: Size = size / 40) : MutableImage {
    private val numChunksX = size.w / chunkSize.w + if (size.w % chunkSize.w != 0) 1 else 0
    private val numChunksY = size.h / chunkSize.h + if (size.h % chunkSize.h != 0) 1 else 0

    private val chunks = Array<MutableImage?>(numChunksX * numChunksY) { null }
    private operator fun Array<MutableImage?>.get(x: Int, y: Int) = chunks[y * numChunksX + x]
    private operator fun Array<MutableImage?>.set(x: Int, y: Int, chunk: MutableImage) {
        chunks[y * numChunksX + x] = chunk
    }

    override fun getColor(pt: Pt): Color {
        require(size.toRect().contains(pt))
        val chunkX = pt.x / chunkSize.w
        val chunkY = pt.y / chunkSize.h
        val relX = pt.x % chunkSize.w
        val relY = pt.y % chunkSize.h

        val chunk = chunks[chunkX, chunkY] ?: return Colors.TRANSPARENT
        return chunk.getColor(Pt(relX, relY))
    }

    override fun setColor(pt: Pt, color: Color) {
        require(size.toRect().contains(pt))
        val chunkX = pt.x / chunkSize.w
        val chunkY = pt.y / chunkSize.h
        val relX = pt.x % chunkSize.w
        val relY = pt.y % chunkSize.h

        val maybeChunk = chunks[chunkX, chunkY]
        if (maybeChunk == null && color == Colors.TRANSPARENT) return
        val chunk = maybeChunk ?: DefaultImage(chunkSize).also { chunks[chunkX, chunkY] = it }
        chunk.setColor(Pt(relX, relY), color)
    }

    fun reset() {
        for (i in chunks.indices) {
            chunks[i] = null
        }
    }
}

