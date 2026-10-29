package com.example.application.screens.organizer

import android.graphics.BitmapFactory
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.dp
import com.example.application.network.ApiService
import com.example.application.ui.theme.Stone600
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.HttpURLConnection
import java.net.URL

@Composable
internal fun OfferImage(offer: OrganizerOffer) {
    val path = if (offer.trip) offer.data.text("coverPhotoUrl") else
        offer.data.get("images")?.takeIf { it.isJsonArray }?.asJsonArray
            ?.firstOrNull { it.isJsonObject }?.asJsonObject?.text("imageUrl").orEmpty()
    RemoteOfferImage(path, if (offer.trip) "Viaggio" else "Attività")
}

@Composable
internal fun RemoteOfferImage(path: String, fallback: String, revision: Int = 0) {
    val url = ApiService.resolveUrl(path.ifBlank { "/photo/default/experience.jpg" })
    val bitmap by produceState<android.graphics.Bitmap?>(null, url, revision) {
        value = withContext(Dispatchers.IO) {
            runCatching {
                val connection = URL(url).openConnection() as HttpURLConnection
                try {
                    connection.connectTimeout = 5000
                    connection.readTimeout = 5000
                    connection.inputStream.use { stream ->
                        BitmapFactory.decodeStream(stream, null, BitmapFactory.Options().apply { inSampleSize = 2 })
                    }
                } finally { connection.disconnect() }
            }.getOrNull()
        }
    }
    Box(Modifier.fillMaxWidth().height(160.dp).background(Stone600), contentAlignment = Alignment.Center) {
        val image = bitmap
        if (image != null) Image(image.asImageBitmap(), contentDescription = null, contentScale = ContentScale.Crop, modifier = Modifier.fillMaxSize())
        else Text(fallback)
    }
}
