package bitspittle.paintkit.windows

import Settings
import androidx.compose.desktop.Window
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.CircularProgressIndicator
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.IntSize
import bitspittle.paintkit.client.ClientHandlerImpl
import bitspittle.paintkit.client.PaintKitClient
import bitspittle.paintkit.client.Session
import bitspittle.paintkit.l18n._t
import bitspittle.paintkit.layout.CommonWidgets
import bitspittle.paintkit.layout.Padding
import bitspittle.paintkit.layout.Shapes
import bitspittle.paintkit.model.graphics.Size
import bitspittle.paintkit.theme.PaintKitTheme
import kotlinx.coroutines.launch
import java.util.*

fun WelcomeWindow(navigator: WindowNavigator) = Window(
    title = _t("welcome.window.title", _t("paintkit.title")),
    size = IntSize(400, 100),
    events = navigator.createEvents()
) {
    var showingConnectionMessage by remember { mutableStateOf(false) }

    PaintKitTheme {
        Box {
            Row(
                modifier = Modifier.fillMaxSize(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                CommonWidgets.Button(_t("action.new")) { showingConnectionMessage = true }
                CommonWidgets.LargeSpacer()
                CommonWidgets.Button(_t("action.open"))
                CommonWidgets.LargeSpacer()
                CommonWidgets.Button(_t("action.join"))
            }

            if (showingConnectionMessage) {
                ConnectingMessage(
                    onConnected = { session -> navigator.enter(Window.Canvas(session)) },
                    // TODO: Show error message on failure
                    onFailed = { showingConnectionMessage = false },
                )
            }
        }
    }
}

@Composable
fun ConnectingMessage(onConnected: (Session) -> Unit, onFailed: (String) -> Unit) {
    val connectingScope = rememberCoroutineScope()
    connectingScope.launch {
        try {
            val adminId = UUID.randomUUID()
            // TODO: Collect size & other params from the user
            val size = Size(800, 600)
            val client = PaintKitClient { ctx ->
                val handler = ClientHandlerImpl(adminId, ctx)
                connectingScope.launch {
                    handler.createCanvas(size)
                    onConnected(handler)
                }
                handler
            }
            Settings.debugPort?.let { port ->
                client.start(port)
            } ?: run {
                client.start(adminId)
            }
        }
        catch (ex: Exception) {
            onFailed(ex.toString())
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(MaterialTheme.colors.background.copy(alpha = 0.5f))
    ) {
        Row(
            modifier = Modifier
                .align(Alignment.Center)
                .clip(Shapes.RoundedCornerShape)
                .background(MaterialTheme.colors.background)
                .padding(Padding.Large)
        ) {
            Text(_t("welcome.connecting"), modifier = Modifier.align(Alignment.CenterVertically))
            CommonWidgets.LargeSpacer()
            CircularProgressIndicator()
        }
    }

}

