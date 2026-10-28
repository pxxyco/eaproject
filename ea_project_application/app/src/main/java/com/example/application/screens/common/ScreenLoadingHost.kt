package com.example.application.screens.common

import androidx.compose.runtime.*

internal class ScreenLoadingHost {
    var message by mutableStateOf<String?>(null)
        private set
    private var owner: Any? = null
    fun show(token: Any, text: String) { owner = token; message = text }
    fun clear(token: Any) { if (owner === token) { owner = null; message = null } }
}
internal val LocalScreenLoadingHost = staticCompositionLocalOf<ScreenLoadingHost?> { null }

/** Returns true when navigation owns the overlay, so a page does not draw a second cropped one. */
@Composable
internal fun publishScreenLoading(message: String?): Boolean {
    val host = LocalScreenLoadingHost.current
    val owner = remember { Any() }
    DisposableEffect(host, message) {
        if (message != null) host?.show(owner, message) else host?.clear(owner)
        onDispose { host?.clear(owner) }
    }
    return host != null
}
