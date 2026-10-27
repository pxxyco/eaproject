package com.example.application.screens.home

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.application.device.CalendarEvent
import com.example.application.device.openCalendar
import com.example.application.network.*
import java.util.Locale

private data class Suggestion(val key: String, val title: String, val location: String, val reason: String,
    val photo: String?, val price: String, val date: String?, val end: String?)

@Composable
internal fun RecommendationsSection(load: suspend () -> ApiResult<Recommendations> = { RecommendationRepository.load() }) {
    val user = UserSession.user
    if (!user?.role.equals("TRAVELER", true)) return
    var response by remember(user?.id) { mutableStateOf<Recommendations?>(null) }
    var error by remember(user?.id) { mutableStateOf<String?>(null) }
    var retry by remember { mutableIntStateOf(0) }
    var selected by remember { mutableStateOf<Suggestion?>(null) }
    val context = LocalContext.current
    LaunchedEffect(user?.id, RecommendationRepository.revision, retry) {
        response = null; error = null
        when (val result = load()) {
            is ApiResult.Success -> response = result.data
            else -> error = result.accountError()
        }
    }
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        SectionTitle(if (response?.personalized == true) "Scelte per te" else "Idee da scoprire")
        Text(if (response?.personalized == true) "In base ai tuoi preferiti, alle prenotazioni e alle recensioni."
            else "Esperienze disponibili: i consigli si adattano alle tue preferenze quando la personalizzazione è attiva.",
            style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (response == null && error == null) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error)
            OutlinedButton(onClick = { retry++ }) { Text("Riprova consigli") }
        }
        val suggestions = response?.let { data ->
            data.trips.map { pick -> pick.item.let { Suggestion("trip-${it.tripId}", it.title ?: "Viaggio", it.location.orEmpty(), pick.reason,
                it.coverPhotoUrl, it.totalPrice?.let { price -> "€%.2f".format(Locale.ITALY, price) }.orEmpty(), it.startDate, it.endDate) } } +
            data.activities.map { pick -> pick.item.let { Suggestion("activity-${it.activityId}", it.title ?: "Attività", it.city.orEmpty(), pick.reason,
                it.imageUrl, it.price?.let { price -> "€%.2f".format(Locale.ITALY, price) }.orEmpty(), it.startDate, it.endDate) } }
        }.orEmpty()
        LazyRow(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(suggestions, key = { it.key }) { suggestion ->
                Card(onClick = { selected = suggestion }, modifier = Modifier.width(260.dp),
                    colors = CardDefaults.cardColors(containerColor = com.example.application.ui.theme.GlassFill)) {
                    RemotePhoto(suggestion.photo, Modifier.fillMaxWidth().height(120.dp), suggestion.title)
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(suggestion.title, style = MaterialTheme.typography.titleMedium)
                        Text(suggestion.location, style = MaterialTheme.typography.bodySmall)
                        Text(suggestion.reason, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(suggestion.price, style = MaterialTheme.typography.labelLarge)
                        Text("Vedi i dettagli  ›", style = MaterialTheme.typography.labelMedium)
                    }
                }
            }
        }
        if (response != null && suggestions.isEmpty()) Text("Non ci sono altre esperienze disponibili al momento.")
    }
    selected?.let { suggestion ->
        var calendarError by remember(suggestion) { mutableStateOf<String?>(null) }
        AlertDialog(onDismissRequest = { selected = null }, title = { Text(suggestion.title) },
            text = { Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(suggestion.location)
                Text(suggestion.reason)
                Text(listOfNotNull(suggestion.date, suggestion.end).joinToString(" → "))
                Text(suggestion.price)
                calendarError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            } },
            confirmButton = { CalendarEvent.from(suggestion.title, suggestion.location, suggestion.date, suggestion.end)?.let { event ->
                Button(onClick = { calendarError = context.openCalendar(event) }) { Text("Aggiungi al calendario") }
            } }, dismissButton = { OutlinedButton(onClick = { selected = null }) { Text("Chiudi") } })
    }
}
