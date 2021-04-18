package bitspittle.paintkit.model

import bitspittle.paintkit.model.graphics.Line
import bitspittle.paintkit.model.graphics.Pt
import com.google.common.truth.Truth.assertThat
import org.junit.Test

class GeometryTest {
    @Test
    fun pointsOnALine() {
        // Same point
        run {
            val line = Line(Pt(123, 456), Pt(123, 456))

            assertThat(line.points).containsExactly(
                Pt(123, 456),
            )
        }

        // Horizontal line
        run {
            val line = Line(Pt(123, 456), Pt(127, 456))

            assertThat(line.points).containsExactly(
                Pt(123, 456),
                Pt(124, 456),
                Pt(125, 456),
                Pt(126, 456),
                Pt(127, 456),
            ).inOrder()
        }

        // Horizontal line (reversed)
        run {
            val line = Line(Pt(127, 456), Pt(123, 456))

            assertThat(line.points).containsExactly(
                Pt(127, 456),
                Pt(126, 456),
                Pt(125, 456),
                Pt(124, 456),
                Pt(123, 456),
            ).inOrder()
        }

        // Vertical line
        run {
            val line = Line(Pt(123, 456), Pt(123, 461))

            assertThat(line.points).containsExactly(
                Pt(123, 456),
                Pt(123, 457),
                Pt(123, 458),
                Pt(123, 459),
                Pt(123, 460),
                Pt(123, 461),
            ).inOrder()
        }

        // Vertical line (reversed)
        run {
            val line = Line(Pt(123, 461), Pt(123, 456))

            assertThat(line.points).containsExactly(
                Pt(123, 461),
                Pt(123, 460),
                Pt(123, 459),
                Pt(123, 458),
                Pt(123, 457),
                Pt(123, 456),
            ).inOrder()
        }

        // Slope (left to right, bottom to top, run > rise)
        run {
            val line = Line(Pt(1, 1), Pt(7, 3))

            assertThat(line.points).containsExactly(
                Pt(1, 1),
                Pt(2, 1),
                Pt(3, 2),
                Pt(4, 2),
                Pt(5, 2),
                Pt(6, 3),
                Pt(7, 3),
            ).inOrder()
        }

        // Slope (left to right, bottom to top, run == rise)
        run {
            val line = Line(Pt(1, 1), Pt(7, 7))

            assertThat(line.points).containsExactly(
                Pt(1, 1),
                Pt(2, 2),
                Pt(3, 3),
                Pt(4, 4),
                Pt(5, 5),
                Pt(6, 6),
                Pt(7, 7),
            ).inOrder()
        }

        // Slope (left to right, bottom to top, run < rise)
        run {
            val line = Line(Pt(1, 1), Pt(3, 7))

            assertThat(line.points).containsExactly(
                Pt(1, 1),
                Pt(1, 2),
                Pt(2, 3),
                Pt(2, 4),
                Pt(2, 5),
                Pt(3, 6),
                Pt(3, 7),
            ).inOrder()
        }

        // Slope (left to right, top to bottom, run > rise)
        run {
            val line = Line(Pt(1, 3), Pt(7, 1))

            assertThat(line.points).containsExactly(
                Pt(1, 3),
                Pt(2, 3),
                Pt(3, 2),
                Pt(4, 2),
                Pt(5, 2),
                Pt(6, 1),
                Pt(7, 1),
            ).inOrder()
        }

        // Slope (left to right, top to bottom, run == rise)
        run {
            val line = Line(Pt(1, 7), Pt(7, 1))

            assertThat(line.points).containsExactly(
                Pt(1, 7),
                Pt(2, 6),
                Pt(3, 5),
                Pt(4, 4),
                Pt(5, 3),
                Pt(6, 2),
                Pt(7, 1),
            ).inOrder()
        }

        // Slope (left to right, top to bottom, run < rise)
        run {
            val line = Line(Pt(1, 7), Pt(3, 1))

            assertThat(line.points).containsExactly(
                Pt(1, 7),
                Pt(1, 6),
                Pt(2, 5),
                Pt(2, 4),
                Pt(2, 3),
                Pt(3, 2),
                Pt(3, 1),
            ).inOrder()
        }

        /////////////////

        // Slope (right to left, bottom to top, run > rise)
        run {
            val line = Line(Pt(7, 1), Pt(1, 3))

            assertThat(line.points).containsExactly(
                Pt(7, 1),
                Pt(6, 1),
                Pt(5, 2),
                Pt(4, 2),
                Pt(3, 2),
                Pt(2, 3),
                Pt(1, 3),
            ).inOrder()
        }

        // Slope (right to left, bottom to top, run == rise)
        run {
            val line = Line(Pt(7, 1), Pt(1, 7))

            assertThat(line.points).containsExactly(
                Pt(7, 1),
                Pt(6, 2),
                Pt(5, 3),
                Pt(4, 4),
                Pt(3, 5),
                Pt(2, 6),
                Pt(1, 7),
            ).inOrder()
        }

        // Slope(right to left, bottom to top, run < rise)
        run {
            val line = Line(Pt(3, 1), Pt(1, 7))

            assertThat(line.points).containsExactly(
                Pt(3, 1),
                Pt(3, 2),
                Pt(2, 3),
                Pt(2, 4),
                Pt(2, 5),
                Pt(1, 6),
                Pt(1, 7),
            ).inOrder()
        }

        // Slope (right to left, top to bottom, run > rise)
        run {
            val line = Line(Pt(7, 3), Pt(1, 1))

            assertThat(line.points).containsExactly(
                Pt(7, 3),
                Pt(6, 3),
                Pt(5, 2),
                Pt(4, 2),
                Pt(3, 2),
                Pt(2, 1),
                Pt(1, 1),
            ).inOrder()
        }

        // Slope(right to left, top to bottom, run == rise)
        run {
            val line = Line(Pt(7, 7), Pt(1, 1))

            assertThat(line.points).containsExactly(
                Pt(7, 7),
                Pt(6, 6),
                Pt(5, 5),
                Pt(4, 4),
                Pt(3, 3),
                Pt(2, 2),
                Pt(1, 1),
            ).inOrder()
        }

        // Slope (right to left, top to bottom, run < rise)
        run {
            val line = Line(Pt(3, 7), Pt(1, 1))

            assertThat(line.points).containsExactly(
                Pt(3, 7),
                Pt(3, 6),
                Pt(2, 5),
                Pt(2, 4),
                Pt(2, 3),
                Pt(1, 2),
                Pt(1, 1),
            ).inOrder()
        }
    }
}