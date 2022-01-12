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
import androidx.compose.ui.graphics.PointMode
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuItem
import bitspittle.paintkit.client.Session
import bitspittle.paintkit.image.PendingImages
import bitspittle.paintkit.input.MouseButton
import bitspittle.paintkit.input.MouseHandler
import bitspittle.paintkit.input.withMouseHandler
import bitspittle.paintkit.l18n._t
import bitspittle.paintkit.model.graphics.Colors
import bitspittle.paintkit.model.graphics.Line
import bitspittle.paintkit.model.graphics.Pt
import bitspittle.paintkit.model.graphics.image.SparseImage
import bitspittle.paintkit.model.graphics.image.pixels
import bitspittle.paintkit.theme.PaintKitTheme
import kotlinx.coroutines.CoroutineScope
import java.util.*

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
                var recomposeForcer by remember { mutableStateOf(UUID.randomUUID()) }
                val pendingImages = remember {
                    PendingImages(
                        CoroutineScope(session.clientContext.environment.dispatcher),
                        session.userId,
                        session.clientContext.connection,
                        onRejected = { recomposeForcer = UUID.randomUUID() }
                    )
                }
                Canvas(
                    modifier = Modifier
                        .background(Color.White)
                        .fillMaxSize(0.9f)
                        .align(Alignment.Center)
                        .withMouseHandler(object : MouseHandler {
                            private var pendingImage: SparseImage? = null
                            private var lastOffset: IntOffset? = null
                            // TODO: Move this logic into draw scope when we have width x height
                            private fun IntOffset.toPt(): Pt = Pt(x, y)
                            override fun onButtonPress(button: MouseButton, offset: IntOffset) {
                                if (button == MouseButton.LEFT) {
                                    lastOffset = offset
                                    pendingImage = SparseImage(initialCanvas.image.size)
                                    pendingImage!!.setColor(offset.toPt(), Colors.BLACK)
                                    recomposeForcer = UUID.randomUUID()
                                }
                            }

                            override fun onMove(offset: IntOffset) {
                                pendingImage?.let { pendingImage ->
                                    val l = Line(lastOffset!!.toPt(), offset.toPt())
                                    l.points.forEach { pt -> pendingImage.setColor(pt, Colors.BLACK) }
                                    lastOffset = offset
                                    recomposeForcer = UUID.randomUUID()
                                }
                            }

                            override fun onButtonRelease(button: MouseButton) {
                                pendingImage?.let {
                                    pendingImages.push(initialCanvas.imageId, it)
                                    pendingImage = null
                                    recomposeForcer = UUID.randomUUID()
                                }
                            }
                        })
                ) {
                    val canvasWidth = size.width
                    val canvasHeight = size.height

                    // Looks like no-op but forces recompose
                    println(recomposeForcer)

                    val offsets = pendingImages.images
                        .flatMap { it.pixels }
                        .map { pixel -> Offset(pixel.pt.x.toFloat(), pixel.pt.y.toFloat()) }

                    drawPoints(offsets, PointMode.Points, Color.Black)
                }
            }
        }
    }
}

