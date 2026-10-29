package com.example.application.screens.home

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import coil.ImageLoader
import coil.compose.AsyncImage
import com.example.application.network.ApiService

private const val DEFAULT_IMAGE_PATH = "/photo/default/experience.jpg"

internal object PhotoLoader {
    @Volatile private var loader: ImageLoader? = null

    fun get(context: Context): ImageLoader = loader ?: synchronized(this) {
        loader ?: ImageLoader.Builder(context.applicationContext)
            .respectCacheHeaders(false)
            .crossfade(true)
            .build()
            .also { loader = it }
    }
}

@Composable
fun RemotePhoto(
    imageUrl: String?,
    modifier: Modifier = Modifier,
    contentDescription: String? = null
) {
    var useDefault by remember(imageUrl) {
        mutableStateOf(imageUrl.isNullOrBlank())
    }

    val path = if (useDefault) {
        DEFAULT_IMAGE_PATH
    } else {
        imageUrl!!
    }

    val fullUrl = remember(path) {
        ApiService.resolveUrl(path)
    }
    val imageLoader = PhotoLoader.get(LocalContext.current)

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = fullUrl,
            contentDescription = contentDescription,
            imageLoader = imageLoader,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop,
            onError = {
                if (!useDefault) {
                    useDefault = true
                }
            }
        )
    }
}