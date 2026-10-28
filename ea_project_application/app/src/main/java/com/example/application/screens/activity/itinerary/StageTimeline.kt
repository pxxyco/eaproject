package com.example.application.screens.activity.itinerary

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.StageDTO
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400

private val STAGE_CATEGORY_LABELS = mapOf(
    "EXCURSION" to "Escursione",
    "VISIT" to "Visita",
    "TRANSPORT" to "Trasporto",
    "HOTEL" to "Hotel",
    "MEAL" to "Ristorazione",
    "OTHER" to "Altro"
)


@Composable
internal fun StageTimeline(stages: List<StageDTO>) {
    if (stages.isEmpty()) {
        Text(
            text = "Nessuna tappa in programma.",
            style = MaterialTheme.typography.bodyMedium,
            color = Stone400
        )
        return
    }

    val stagesByDay = stages
        .sortedWith(compareBy<StageDTO>({ it.day ?: Int.MAX_VALUE }, { it.orderInDay ?: Int.MAX_VALUE }))
        .groupBy { it.day }

    Column(
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        stagesByDay.forEach { (day, dayStages) ->
            Column {
                Text(
                    text = if (day != null) "GIORNO $day" else "ALTRE TAPPE",
                    style = MaterialTheme.typography.labelLarge,
                    color = Stone400,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(12.dp))

                dayStages.forEachIndexed { index, stage ->
                    key(stage.stageId) {
                        StageItem(stage = stage, isLast = index == dayStages.lastIndex)
                    }
                }
            }
        }
    }
}

@Composable
private fun StageItem(stage: StageDTO, isLast: Boolean) {
    var expanded by rememberSaveable { mutableStateOf(false) }

    val time = formatStageTime(stage.startTime, stage.endTime)
    val location = listOfNotNull(stage.locationName, stage.city)
        .filter { it.isNotBlank() }
        .joinToString(", ")
    val category = STAGE_CATEGORY_LABELS[stage.category]
    val hasDetails = !stage.description.isNullOrBlank() ||
            !stage.notes.isNullOrBlank() ||
            !stage.photoUrl.isNullOrBlank()

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Min)
    ) {
        Column(
            modifier = Modifier
                .width(20.dp)
                .fillMaxHeight(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .padding(top = 4.dp)
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Stone200)
            )
            if (!isLast) {
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp)
                        .width(2.dp)
                        .weight(1f)
                        .background(GlassBorder)
                )
            }
        }

        Spacer(Modifier.width(12.dp))

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(bottom = if (isLast) 0.dp else 20.dp)
                .clickable(enabled = hasDetails) { expanded = !expanded }
        ) {
            if (time != null) {
                Text(
                    text = time,
                    style = MaterialTheme.typography.labelMedium,
                    color = Stone400,
                )
            }

            Text(
                text = stage.title?.takeIf { it.isNotBlank() } ?: "Tappa",
                style = MaterialTheme.typography.titleSmall,
                color = Color.White,
                fontWeight = FontWeight.SemiBold
            )

            if (category != null || location.isNotBlank()) {
                Spacer(Modifier.height(6.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (category != null) {
                        Surface(shape = RoundedCornerShape(6.dp), color = GlassFill) {
                            Text(
                                text = category,
                                style = MaterialTheme.typography.labelSmall,
                                color = Stone200,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                        if (location.isNotBlank()) Spacer(Modifier.width(8.dp))
                    }
                    if (location.isNotBlank()) {
                        Text(text = location, style = MaterialTheme.typography.bodySmall, color = Stone400)
                    }
                }
            }
            if (expanded) {
                StageDetails(stage)
            }

            if (hasDetails) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = if (expanded) "Mostra meno" else "Mostra dettagli",
                    style = MaterialTheme.typography.labelSmall,
                    color = Stone200
                )
            }
        }
    }
}

@Composable
private fun StageDetails(stage: StageDTO) {
    Column(
        modifier = Modifier.padding(top = 10.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (!stage.description.isNullOrBlank()) {
            Text(text = stage.description, style = MaterialTheme.typography.bodyMedium, color = Stone200)
        }
        if (!stage.notes.isNullOrBlank()) {
            Text(text = "Note: ${stage.notes}", style = MaterialTheme.typography.bodySmall, color = Stone400)
        }
    }
}

private fun formatStageTime(start: String?, end: String?): String? {
    val startTime = start?.take(5)?.takeIf { it.isNotBlank() }
    val endTime = end?.take(5)?.takeIf { it.isNotBlank() }
    return when {
        startTime != null && endTime != null -> "$startTime – $endTime"
        startTime != null -> startTime
        endTime != null -> "Fino alle $endTime"
        else -> null
    }
}