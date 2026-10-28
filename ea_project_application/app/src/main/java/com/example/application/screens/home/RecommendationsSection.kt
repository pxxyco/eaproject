package com.example.application.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceRef
import com.example.application.network.*

@Composable
internal fun RecommendationsSection(
    onExperienceClick: (ExperienceRef) -> Unit,
    load: suspend () -> ApiResult<Recommendations> = { RecommendationRepository.load() }
) {
    val user = UserSession.user
    if (!user?.role.equals("TRAVELER", true)) return
    var response by remember(user?.id) { mutableStateOf<Recommendations?>(null) }
    var error by remember(user?.id) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    LaunchedEffect(user?.id, RecommendationRepository.revision, retry) {
        response = null
        error = null
        when (val result = load()) {
            is ApiResult.Success -> response = result.data
            else -> error = result.accountError()
        }
    }
    val trips = response?.trips.orEmpty().filter { it.item.tripId != null }.distinctBy { it.item.tripId }
    val activities = response?.activities.orEmpty().filter { it.item.activityId != null }.distinctBy { it.item.activityId }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(if (response?.personalized == true) "Scelte per te" else "Idee da scoprire")
        Text(
            if (response?.personalized == true) "In base ai tuoi preferiti, alle prenotazioni e alle recensioni."
            else "Esperienze disponibili: i consigli si adattano alle tue preferenze quando la personalizzazione è attiva.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (response == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { retry++ }) { Text("Riprova consigli") }
        }
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(trips, key = { "trip-${it.item.tripId}" }) { pick ->
                Column(Modifier.width(280.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    TripCardItem(pick.item, onExperienceClick)
                }
            }
            items(activities, key = { "activity-${it.item.activityId}" }) { pick ->
                Column(Modifier.width(180.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    ActivityCardItem(pick.item, onExperienceClick)
                }
            }
        }
        if (response != null && trips.isEmpty() && activities.isEmpty()) {
            Text("Non ci sono altre esperienze disponibili al momento.")
        }
    }
}
