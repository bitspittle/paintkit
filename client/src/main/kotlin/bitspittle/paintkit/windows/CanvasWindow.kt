package bitspittle.paintkit.windows

import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuItem
import bitspittle.paintkit.client.Session
import bitspittle.paintkit.input.MouseButton
import bitspittle.paintkit.input.MouseHandler
import bitspittle.paintkit.input.MouseState
import bitspittle.paintkit.input.withMouseHandler
import bitspittle.paintkit.l18n._t
import bitspittle.paintkit.theme.PaintKitTheme

fun CanvasWindow(navigator: WindowNavigator, session: Session) {
    val initialCanvas = session.canvases.first()

    Window(
        title = _t("canvas.window.title", _t("paintkit.title"), "untitled.pkt", " (*)"),
        size = IntSize(1280, 720),
        events = navigator.createEvents() + WindowEvents(
            onClose = { session.disconnect() }
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
                var mouseState: MouseState by remember { mutableStateOf(MouseState.Released) }
                Canvas(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxSize(0.9f)
                        .align(Alignment.Center)
                        .withMouseHandler(object : MouseHandler {
                            override fun onButtonPress(button: MouseButton, offset: IntOffset) {
                                mouseState = MouseState.Pressed(button, offset)
                            }

                            override fun onButtonRelease(button: MouseButton) {
                                mouseState = MouseState.Released
                            }

                            override fun onMove(offset: IntOffset) {
                                mouseState = MouseState.Moved(offset)
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
}

