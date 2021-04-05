package bitspittle.paintkit.theme

import androidx.compose.material.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val DEFAULT_SPACING_LARGE = 10.dp

@Composable
fun PaintKitTheme(content: @Composable () -> Unit) {
    MaterialTheme(content = content)
}