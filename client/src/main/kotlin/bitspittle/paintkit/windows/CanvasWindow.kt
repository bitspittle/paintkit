package bitspittle.paintkit.windows

import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.forEachGesture
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.consumeDownChange
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuItem
import bitspittle.ipc.client.ClientMessenger
import bitspittle.paintkit.input.MouseButton
import bitspittle.paintkit.input.MouseHandler
import bitspittle.paintkit.input.withMouseHandler
import bitspittle.paintkit.l18n._t
import bitspittle.paintkit.theme.PaintKitTheme

fun CanvasWindow(navigator: WindowNavigator, messenger: ClientMessenger) = Window(
    title = _t("canvas.window.title", _t("paintkit.title"), "untitled.pkt", " (*)"),
    size = IntSize(1280, 720),
    events = navigator.createEvents() + WindowEvents(
        onClose = { messenger.disconnect() }
    ),
    menuBar = MenuBar(
        Menu(
            name = "File",
            MenuItem(
                name = "Close",
                onClick = { navigator.back() },
            ),
            MenuItem(
                name = "Exit",
                onClick = { navigator.exit() },
            ),
        ),
    ),
) {
    PaintKitTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.LightGray)
        ) {
            Canvas(
                modifier = Modifier
                    .background(Color.White)
                    .fillMaxSize(0.9f)
                    .align(Alignment.Center)
                    .withMouseHandler(object : MouseHandler {
                        private val isPressed = BooleanArray(MouseButton.values().size) { false }

                        override fun onButtonPress(button: MouseButton, offset: IntOffset) {
                            isPressed[button.ordinal] = true
                            if (button == MouseButton.LEFT) {
                                println("onDrawStart: $offset")
                            }
                        }

                        override fun onButtonRelease(button: MouseButton) {
                            if (button == MouseButton.LEFT) {
                                println("onDrawEnd")
                            }
                            isPressed[button.ordinal] = false
                        }

                        override fun onMove(offset: IntOffset) {
                            if (isPressed[MouseButton.LEFT.ordinal]) {
                                println("onDraw: $offset")
                            }
                        }
                    })
            ) {
                val canvasWidth = size.width
                val canvasHeight = size.height

                drawLine(
                    start = Offset(x = canvasWidth, y = 0f),
                    end = Offset(x = 0f, y = canvasHeight),
                    color = Color.Blue
                )
            }
        }
    }
}