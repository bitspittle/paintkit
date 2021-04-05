package bitspittle.paintkit.windows

import androidx.compose.desktop.Window
import androidx.compose.desktop.WindowEvents
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.window.Menu
import androidx.compose.ui.window.MenuBar
import androidx.compose.ui.window.MenuItem
import bitspittle.ipc.client.ClientMessenger
import bitspittle.ipc.proto.IpcProto
import bitspittle.ipc.proto.IpcProto.ClientMessage
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
        )
    }
}