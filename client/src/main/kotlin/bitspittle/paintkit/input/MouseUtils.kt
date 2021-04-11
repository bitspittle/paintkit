package bitspittle.paintkit.input

import androidx.compose.ui.Modifier
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.consumePositionChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.input.pointer.pointerMoveFilter
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.round
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive

enum class MouseButton {
    LEFT,
    MIDDLE,
    RIGHT
}

interface MouseHandler {
    fun onButtonPress(button: MouseButton, offset: IntOffset) {}
    fun onButtonRelease(button: MouseButton) {}
    fun onMove(offset: IntOffset) {}
}

fun Modifier.withMouseHandler(handler: MouseHandler): Modifier {
    return this
        .pointerMoveFilter(
            // Handles moving cursor when no buttons are pressed
            onMove = { position -> handler.onMove(position.round()); true }
        )
        .pointerInput(Unit) {
            // Handles moving cursor when one (or more) buttons are pressed
            while (currentCoroutineContext().isActive) {
                awaitPointerEventScope {
                    val pointerEvent = awaitPointerEvent()
                    pointerEvent.changes.forEach { change ->
                        val mouseEvent = pointerEvent.mouseEvent
                        if (mouseEvent != null) {
                            val button = when (mouseEvent.button) {
                                1 -> MouseButton.LEFT
                                2 -> MouseButton.MIDDLE
                                3 -> MouseButton.RIGHT
                                else -> null
                            }

                            if (button != null && change.pressed && !change.previousPressed) {
                                handler.onButtonPress(button, change.position.round())
                                change.consumeDownChange()
                            } else if (button != null && !change.pressed && change.previousPressed) {
                                handler.onButtonRelease(button)
                                change.consumeDownChange()
                            }

                            if (change.position != change.previousPosition) {
                                handler.onMove(change.position.round())
                                change.consumePositionChange()
                            }
                        }
                    }
                }
            }
        }
}
