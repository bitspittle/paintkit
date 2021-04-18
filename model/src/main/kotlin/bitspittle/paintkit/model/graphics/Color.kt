package bitspittle.paintkit.model.graphics

data class Color(val r: Char, val g: Char, val b: Char, val a: Char = 255.toChar()) {
    constructor(r: Int, g: Int, b: Int, a: Int = 255) : this(
        r.coerceIn(0, 255).toChar(),
        g.coerceIn(0, 255).toChar(),
        b.coerceIn(0, 255).toChar(),
        a.coerceIn(0, 255).toChar(),
    )

    constructor(r: Float, g: Float, b: Float, a: Float = 1f) : this(
        (r.coerceIn(0f, 1f) * 255f).toChar(),
        (g.coerceIn(0f, 1f) * 255f).toChar(),
        (b.coerceIn(0f, 1f) * 255f).toChar(),
        (a.coerceIn(0f, 1f) * 255f).toChar(),
    )

    constructor(packed: Int) : this(
        packed.and(0x000000FF),
        packed.and(0x0000FF00),
        packed.and(0x00FF0000),
        packed.and(0xFF000000.toInt()),
    )

    /** This color, encoded inside a 32-bit integer. Use the constructor later to unpack it. */
    val packed =
        r.toInt()
            .and(g.toInt().shl(8))
            .and(b.toInt().shl(16))
            .and(a.toInt().shl(24))
}

object Colors {
    val TRANSPARENT = Color(0)
    val WHITE = Color(255, 255, 255)
    val BLACK = Color(0, 0, 0)
}
