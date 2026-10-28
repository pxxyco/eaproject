package com.example.application.screens.activity.photos

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail
import com.example.application.network.ExperienceRepository
import com.example.application.screens.home.RemotePhoto
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import kotlinx.coroutines.CancellationException

private const val PREVIEW_COUNT = 6

@Composable
internal fun PhotoSection(
    detail: ExperienceDetail,
    onPhotoClick: (photo: List<String>, index: Int) -> Unit
) {
    var photos by remember(detail.ref) { mutableStateOf(activityPhotos(detail)) }
    var error by remember(detail.ref){ mutableStateOf<String?>(null) }
    var expanded by rememberSaveable(detail.ref.id) { mutableStateOf(false) }

    LaunchedEffect(detail.ref) {
        if(detail !is ExperienceDetail.Trip) return@LaunchedEffect
        val tripId = detail.dto.tripId ?: return@LaunchedEffect
        try {
            photos = ExperienceRepository.getTripImages(tripId)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Impossibile caricare le foto"
            photos = emptyList()
        }
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBackground)
            .padding(20.dp)
    ) {
        val current = photos
        when {
            current == null -> CircularProgressIndicator(
                color = Stone200,
                modifier = Modifier
                    .size(32.dp)
                    .align(Alignment.CenterHorizontally)
            )

            error != null -> Text(text = error!!, style = MaterialTheme.typography.bodyMedium, color = Stone400)

            current.isEmpty() -> Text(
                text = "Nessuna foto disponibile.",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone400
            )

            else -> PhotoGrid(
                photos = current,
                expanded = expanded,
                onToggle = { expanded = !expanded },
                onPhotoClick = onPhotoClick
            )
        }
    }
}

@Composable
internal fun PhotoGrid(
    photos: List<String>,
    expanded: Boolean,
    onToggle: () -> Unit,
    onPhotoClick: (List<String>, Int) -> Unit
) {
    val visible = if(expanded) photos else photos.take(PREVIEW_COUNT)

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        visible.chunked(2).forEachIndexed { rowIndex, row ->
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                row.forEachIndexed { column, url ->
                    val index = rowIndex * 2 + column
                    RemotePhoto(
                        imageUrl = url,
                        contentDescription = "Foto ${index + 1}",
                        modifier = Modifier
                            .weight(1f)
                            .aspectRatio(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onPhotoClick(photos, index) }
                    )
                }
                if (row.size == 1) Spacer(Modifier.weight(1f))
            }
        }
        if (photos.size > PREVIEW_COUNT) {
            Spacer(Modifier.height(4.dp))
            OutlinedButton(
                onClick = onToggle,
                modifier = Modifier.fillMaxWidth(),
                border = BorderStroke(1.dp, GlassBorder),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = Stone200)
            ) {
                Text(if (expanded) "Mostra meno" else "Mostra altre (${photos.size - PREVIEW_COUNT})")
            }
        }
    }
}

private fun activityPhotos(detail: ExperienceDetail): List<String>? = when (detail) {
    is ExperienceDetail.Activity -> detail.dto.images.orEmpty()
        .sortedBy { it.orderIndex ?: Int.MAX_VALUE }
        .mapNotNull { it.imageUrl?.takeIf { url -> url.isNotBlank() } }
    is ExperienceDetail.Trip -> null
}
