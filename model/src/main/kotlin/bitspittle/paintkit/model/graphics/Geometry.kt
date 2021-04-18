package bitspittle.paintkit.model.graphics

import java.lang.Integer.min
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.roundToInt

data class Pt(val x: Int, val y: Int) {
    companion object {
        val ORIGIN = Pt(0, 0)
    }

    fun toSize() = Size(x, y)

    operator fun plus(rhs: Size): Pt {
        return Pt(x + rhs.w, y + rhs.h)
    }

    operator fun plus(rhs: Pt): Pt {
        return Pt(x + rhs.x, y + rhs.y)
    }

    operator fun unaryMinus(): Pt {
        return Pt(-x, -y)
    }

    operator fun minus(rhs: Pt): Pt {
        return Pt(x - rhs.x, y - rhs.y)
    }

    operator fun times(value: Int): Pt {
        return Pt(x * value, y * value)
    }

    operator fun times(value: Float): Pt {
        return Pt((x * value).roundToInt(), (y * value).roundToInt())
    }

    operator fun times(rhs: Pt): Pt {
        return Pt(x * rhs.x, y * rhs.y)
    }

    operator fun times(size: Size): Pt {
        return Pt(x * size.w, y * size.h)
    }

    operator fun div(value: Int): Pt {
        return Pt(x / value, y / value)
    }

    operator fun div(value: Float): Pt {
        return Pt((x / value).roundToInt(), (y / value).roundToInt())
    }

    operator fun div(rhs: Pt): Pt {
        return Pt(x / rhs.x, y / rhs.y)
    }

    operator fun div(size: Size): Pt {
        return Pt(x / size.w, y / size.h)
    }
}

data class Size(val w: Int, val h: Int) {
    companion object {
        val EMPTY = Size(0, 0)
    }

    init {
        require(w >= 0)
        require(h >= 0)
    }

    fun toPt() = Pt(w, h)
    fun toRect() = Rect(Pt.ORIGIN, this)

    operator fun plus(rhs: Size): Size {
        return Size(w + rhs.w, h + rhs.h)
    }

    operator fun minus(rhs: Size): Size {
        return Size(max(0, w - rhs.w), max(0, h - rhs.h))
    }

    operator fun times(value: Int): Size {
        return Size(w * value, h * value)
    }

    operator fun times(value: Float): Size {
        return Size((w * value).roundToInt(), (h * value).roundToInt())
    }

    operator fun div(value: Int): Size {
        return Size(w / value, h / value)
    }

    operator fun div(value: Float): Size {
        return Size((w / value).roundToInt(), (h / value).roundToInt())
    }
}

data class Line(val pt1: Pt, val pt2: Pt) {
    /**
     * This line, broken up into individual points.
     *
     * The first point in the returned iteration will match pt1 and the last will match pt2.
     */
    val points = object : Iterable<Pt> {
        override fun iterator(): Iterator<Pt> {
            val rise = pt2.y - pt1.y
            val run = pt2.x - pt1.x

            val pointsList = mutableListOf<Pt>()
            when {
                pt1 == pt2 -> {
                    pointsList.add(pt1)
                }
                run == 0 -> {
                    if (rise > 0) {
                        for (y in 0..rise) {
                            pointsList.add(Pt(pt1.x, pt1.y + y))
                        }
                    } else {
                        for (y in 0..-rise) {
                            pointsList.add(Pt(pt1.x, pt1.y - y))
                        }
                    }
                }
                rise == 0 -> {
                    if (run > 0) {
                        for (x in 0..run) {
                            pointsList.add(Pt(pt1.x + x, pt1.y))
                        }
                    } else {
                        for (x in 0..-run) {
                            pointsList.add(Pt(pt1.x - x, pt1.y))
                        }
                    }
                }

                else -> {
                    val slope = rise.toFloat() / run
                    // Naive algorithm for now, we can revisit later if it matters, but we're not
                    // going to be drawing lines that often, unlike the underlying rendering system
                    if (abs(run) >= abs(rise)) {
                        // y = mx (b == 0)
                        if (run > 0) {
                            for (x in 0..run) {
                                val y = (x * slope).roundToInt()
                                pointsList.add(Pt(pt1.x + x, pt1.y + y))
                            }
                        } else {
                            for (x in 0..-run) {
                                val y = (-x * slope).roundToInt()
                                pointsList.add(Pt(pt1.x - x, pt1.y + y))
                            }
                        }
                    } else {
                        // x = y/m (b == 0)
                        if (rise > 0) {
                            for (y in 0..rise) {
                                val x = (y / slope).roundToInt()
                                pointsList.add(Pt(pt1.x + x, pt1.y + y))
                            }
                        } else {
                            for (y in 0..-rise) {
                                val x = (-y / slope).roundToInt()
                                pointsList.add(Pt(pt1.x + x, pt1.y - y))
                            }
                        }
                    }
                }
            }

            return pointsList.iterator()
        }
    }
}

data class Rect(val pt: Pt, val size: Size) {
    val top = pt.y
    val bot = top + size.h
    val left = pt.x
    val right = left + size.w

    val topLeft = pt
    val topRight = Pt(right, top)
    val botLeft = Pt(left, bot)
    val botRight = Pt(right, bot)

    fun contains(pt: Pt): Boolean {
        return this.pt.x <= pt.x && pt.x <= botRight.x && this.pt.y <= pt.y && pt.y <= botRight.y
    }

    fun intersect(other: Rect): Rect? {
        if (this.right < other.left
            || this.bot < other.top
            || this.left > other.right
            || this.top > other.bot
        ) {
            return null
        }

        val intersectTop = max(this.top, other.top)
        val intersectBot = min(this.bot, other.bot)
        val intersectLeft = max(this.left, other.left)
        val intersectRight = min(this.right, other.right)
        return Rect(Pt(intersectLeft, intersectTop), Size(intersectRight - intersectLeft, intersectBot - intersectTop))
    }
}
