package com.example.application.ui.background

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.example.application.service.media.WallpaperService
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.WallpaperShade

/** One background behind navigation; switching screens never reloads or replaces it. */
@Composable
internal fun BackgroundHelper(content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var wallpaper by remember { mutableStateOf(WallpaperService.readyImage()) }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { wallpaper = WallpaperService.daily(context) }
    }
    Box(Modifier.fillMaxSize().background(ScreenBackground)) {
        wallpaper?.let { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(WallpaperShade))
        content()
    }
}
