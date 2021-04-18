package bitspittle.paintkit.input

import androidx.compose.ui.unit.IntOffset

sealed class MouseState {
    class Pressed(val button: MouseButton, val pos: IntOffset) : MouseState()
    class Moved(val pos: IntOffset) : MouseState()
    object Released : MouseState()
}
