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

internal class RecommendationsState {
    var response by mutableStateOf<Recommendations?>(null)
    var error by mutableStateOf<String?>(null)
    var retry by mutableIntStateOf(0)
    var loadedKey: Any? = null
}

@Composable
internal fun RecommendationsSection(
    onExperienceClick: (ExperienceRef) -> Unit,
    state: RecommendationsState = remember { RecommendationsState() },
    load: suspend () -> ApiResult<Recommendations> = { RecommendationRepository.load() }
) {
    val user = UserSession.user
    if (!user?.role.equals("TRAVELER", true)) return
    val key = Triple(user?.id, RecommendationRepository.revision, state.retry)
    LaunchedEffect(key) {
        if (state.loadedKey == key) return@LaunchedEffect
        state.error = null
        when (val result = load()) {
            is ApiResult.Success -> {
                state.response = result.data
                state.loadedKey = key
            }
            else -> state.error = result.accountError()
        }
    }
    val response = state.response
    val trips = remember(response) {
        response?.trips.orEmpty().filter { it.item.tripId != null }.distinctBy { it.item.tripId }
    }
    val activities = remember(response) {
        response?.activities.orEmpty().filter { it.item.activityId != null }.distinctBy { it.item.activityId }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(if (response?.personalized == true) "Scelte per te" else "Idee da scoprire")
        Text(
            if (response?.personalized == true) "In base ai tuoi preferiti, alle prenotazioni e alle recensioni."
            else "Esperienze disponibili: i consigli si adattano alle tue preferenze quando la personalizzazione è attiva.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (response == null && state.error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        state.error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { state.retry++ }) { Text("Riprova consigli") }
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