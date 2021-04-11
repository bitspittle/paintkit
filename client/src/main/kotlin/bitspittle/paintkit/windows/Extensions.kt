package bitspittle.paintkit.windows

import androidx.compose.desktop.WindowEvents

operator fun WindowEvents.plus(other: WindowEvents): WindowEvents {
    return WindowEvents(
        onOpen = if (this.onOpen != null || other.onOpen != null) {
            { this.onOpen?.invoke(); other.onOpen?.invoke() }
        } else null,
        onClose = if (this.onClose != null || other.onClose != null) {
            { this.onClose?.invoke(); other.onClose?.invoke() }
        } else null,
        onMinimize = if (this.onMinimize != null || other.onMinimize != null) {
            { this.onMinimize?.invoke(); other.onMinimize?.invoke() }
        } else null,
        onMaximize = if (this.onMaximize != null || other.onMaximize != null) {
            { this.onMaximize?.invoke(); other.onMaximize?.invoke() }
        } else null,
        onRestore = if (this.onRestore != null || other.onRestore != null) {
            { this.onRestore?.invoke(); other.onRestore?.invoke() }
        } else null,
        onFocusGet = if (this.onFocusGet != null || other.onFocusGet != null) {
            { this.onFocusGet?.invoke(); other.onFocusGet?.invoke() }
        } else null,
        onFocusLost = if (this.onFocusLost != null || other.onFocusLost != null) {
            { this.onFocusLost?.invoke(); other.onFocusLost?.invoke() }
        } else null,
        onResize = if (this.onResize != null || other.onResize != null) {
            { intSize -> this.onResize?.invoke(intSize); other.onResize?.invoke(intSize) }
        } else null,
        onRelocate = if (this.onRelocate != null || other.onRelocate != null) {
            { intOffset -> this.onRelocate?.invoke(intOffset); other.onRelocate?.invoke(intOffset) }
        } else null,
    )
}
