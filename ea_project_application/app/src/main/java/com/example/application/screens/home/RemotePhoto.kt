
package com.example.application.screens.home

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import coil.compose.AsyncImage
import com.example.application.network.ApiService

private const val DEFAULT_IMAGE_PATH = "/photo/default/experience.jpg"

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

    Box(
        modifier = modifier.background(MaterialTheme.colorScheme.surfaceVariant)
    ) {
        AsyncImage(
            model = fullUrl,
            contentDescription = contentDescription,
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
