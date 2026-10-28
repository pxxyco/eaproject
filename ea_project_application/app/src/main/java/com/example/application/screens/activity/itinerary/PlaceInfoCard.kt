package com.example.application.screens.activity.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Place
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.example.application.dto.ActivityDetailDTO
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.Stone400
import java.time.LocalDateTime
import java.time.format.DateTimeFormatter

private val DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")
private val TIME_FORMAT = DateTimeFormatter.ofPattern("HH:mm")

@Composable
internal fun PlaceInfoCard(activity: ActivityDetailDTO) {
    val location = listOfNotNull(activity.placeName, activity.city)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val schedule = formatSchedule(activity.startDate, activity.endDate)
    val duration = formatDuration(activity.durationMinutes)

    Column(
        modifier = Modifier.run {
            fillMaxWidth()
                .clip(RoundedCornerShape(16.dp))
                .background(GlassFill)
                .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                .padding(16.dp)
        },
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        if (location.isNotBlank()) InfoRow(Icons.Default.Place, "Luogo", location)
        if (schedule != null) InfoRow(Icons.Default.DateRange, "Data e orario", schedule)
        if (duration != null) InfoRow(Icons.Default.Info, "Durata", duration)

        if (location.isBlank() && schedule == null && duration == null) {
            Text(
                text = "Nessuna informazione sul luogo.",
                style = MaterialTheme.typography.bodyMedium,
                color = Stone400
            )
        }
    }
}

@Composable
private fun InfoRow(icon: ImageVector, label: String, value: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = Stone400,
            modifier = Modifier.size(20.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column {
            Text(text = label, style = MaterialTheme.typography.labelSmall, color = Stone400)
            Text(text = value, style = MaterialTheme.typography.bodyMedium, color = Color.White)
        }
    }
}


private fun parseDateTime(value: String?): LocalDateTime? =
    value?.let { runCatching { LocalDateTime.parse(it) }.getOrNull() }

private fun formatSchedule(start: String?, end: String?): String? {
    val startAt = parseDateTime(start) ?: return null
    val endAt = parseDateTime(end)
    val base = "${startAt.format(DATE_FORMAT)} · ${startAt.format(TIME_FORMAT)}"

    return when {
        endAt == null -> base
        endAt.toLocalDate() == startAt.toLocalDate() -> "$base – ${endAt.format(TIME_FORMAT)}"
        else -> "$base → ${endAt.format(DATE_FORMAT)} ${endAt.format(TIME_FORMAT)}"
    }
}

private fun formatDuration(minutes: Int?): String? {
    if (minutes == null || minutes <= 0) return null
    val hours = minutes / 60
    val rest = minutes % 60
    return when {
        hours == 0 -> "$rest min"
        rest == 0 -> "$hours h"
        else -> "$hours h $rest min"
    }
}