package android.tools.common.traces.wm

class WindowDescriptor(window: IWindowContainer) {
    val id = window.id
    val name = window.name
    val isAppWindow: Boolean =
        (window is Task) || (window.parent?.let { WindowDescriptor(it).isAppWindow } ?: false)
}
