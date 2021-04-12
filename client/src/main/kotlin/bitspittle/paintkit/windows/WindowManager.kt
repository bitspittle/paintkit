package bitspittle.paintkit.windows

import androidx.compose.desktop.AppFrame
import androidx.compose.desktop.AppManager
import androidx.compose.desktop.WindowEvents
import bitspittle.paintkit.client.Session
import java.util.*
import kotlin.math.min
import kotlin.system.exitProcess

sealed class Window {
    internal abstract fun open(navigator: WindowNavigator)

    object Welcome : Window() {
        override fun open(navigator: WindowNavigator) = WelcomeWindow(navigator)
    }

    class Canvas(private val session: Session) : Window() {
        override fun open(navigator: WindowNavigator) = CanvasWindow(navigator, session)
    }
}

interface WindowNavigator {
    fun enter(window: Window, popCount: Int = 0)
    fun back()
    fun exit()
}

fun WindowNavigator.createEvents() = WindowEvents(
    onOpen = { (this as WindowNavigatorImpl).onWindowOpened() },
    onClose = { (this as WindowNavigatorImpl).onWindowClosed() },
)

private class WindowNavigatorImpl : WindowNavigator {
    private val backStack = Stack<Window>()
    private var activeAppFrame: AppFrame? = null
    private var ignoreBack = false

    init {
        // When we enter a window, we close the last and briefly have no windows open. Disable default AppManager
        // behavior which would close in this situation.
        AppManager.setEvents(onAppExit = {})
    }

    override fun enter(window: Window, popCount: Int) {
        if (activeAppFrame != null) {
            // Distinguish between a window being closed by the user pressing the (X) button vs. us closing one window
            // because we are about to enter another.
            ignoreBack = true
        }

        for (i in 0 until min(popCount, backStack.size)) backStack.pop()

        window.open(this)
        backStack.add(window)
    }

    override fun back() {
        backStack.pop()
        if (backStack.isNotEmpty()) {
            backStack.last().open(this)
        }
        else {
            exit()
        }
    }

    override fun exit() {
        AppManager.setEvents(onWindowsEmpty = { exitProcess(0) })
        AppManager.exit()
    }

    fun onWindowOpened() {
        closeActiveAppFrame()
        activeAppFrame = AppManager.windows.last()
    }

    fun onWindowClosed() {
        if (!ignoreBack) back()
        ignoreBack = false
    }


    private fun closeActiveAppFrame() {
        activeAppFrame?.let {
            if (!it.isClosed) it.close()
            activeAppFrame = null
        }
    }

}

/**
 * An invisible root window that acts as a way to prevent the Compose system from exiting prematurely when
 * switching between child windows.
 */
fun startApp(firstWindow: Window = Window.Welcome) {
    val navigator = WindowNavigatorImpl()
    navigator.enter(firstWindow)
}

