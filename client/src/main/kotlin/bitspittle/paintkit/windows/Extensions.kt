package bitspittle.paintkit.windows

import androidx.compose.desktop.WindowEvents

operator fun WindowEvents.plus(other: WindowEvents): WindowEvents {
    return WindowEvents(
        onOpen = { this.onOpen?.invoke(); other.onOpen?.invoke() },
        onClose = { this.onClose?.invoke(); other.onClose?.invoke() },
        onMinimize = { this.onMinimize?.invoke(); other.onMinimize?.invoke() },
        onMaximize = { this.onMaximize?.invoke(); other.onMaximize?.invoke() },
        onRestore = { this.onRestore?.invoke(); other.onRestore?.invoke() },
        onFocusGet = { this.onFocusGet?.invoke(); other.onFocusGet?.invoke() },
        onFocusLost = { this.onFocusLost?.invoke(); other.onFocusLost?.invoke() },
        onResize = { intSize -> this.onResize?.invoke(intSize); other.onResize?.invoke(intSize) },
        onRelocate = { intOffset -> this.onRelocate?.invoke(intOffset); other.onRelocate?.invoke(intOffset) },
    )
}
