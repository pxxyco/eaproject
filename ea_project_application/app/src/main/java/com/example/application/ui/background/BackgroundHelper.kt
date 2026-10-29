package com.example.application.ui.background

import com.example.application.ui.theme.WallpaperPalette
import com.example.application.ui.theme.dominantWallpaperColor
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
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

@Composable
internal fun BackgroundHelper(content: @Composable BoxScope.() -> Unit) {
    val context = LocalContext.current.applicationContext
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var wallpaper by remember { mutableStateOf(WallpaperService.readyImage()) }
    LaunchedEffect(context, lifecycle) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) { wallpaper = WallpaperService.daily(context) }
    }
    LaunchedEffect(wallpaper) {
        wallpaper?.let { bitmap ->
            WallpaperPalette.accent = withContext(Dispatchers.Default) {
                val samples = IntArray(32 * 24)
                for (y in 0 until 24) for (x in 0 until 32)
                    samples[y * 32 + x] = bitmap.getPixel((x * bitmap.width / 32).coerceAtMost(bitmap.width - 1), (y * bitmap.height / 24).coerceAtMost(bitmap.height - 1))
                dominantWallpaperColor(samples)
            }
        }
    }
    Box(Modifier.fillMaxSize().background(ScreenBackground)) {
        wallpaper?.let { bitmap ->
            val image = remember(bitmap) { bitmap.asImageBitmap() }
            Image(image, contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize()) }
        Box(Modifier.fillMaxSize().background(WallpaperShade))
        content()
    }
}
