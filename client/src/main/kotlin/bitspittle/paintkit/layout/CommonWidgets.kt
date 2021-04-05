package bitspittle.paintkit.layout

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material.Button
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

object CommonWidgets {
    @Composable
    fun LargeSpacer() = Spacer(modifier = Modifier.width(Spacing.Large))

    @Composable
    fun Button(text: String, onClick: () -> Unit = {}) {
        Button(onClick = onClick) {
            Text(text)
        }
    }
}