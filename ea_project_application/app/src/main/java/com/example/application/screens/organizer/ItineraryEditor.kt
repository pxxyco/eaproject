package com.example.application.screens.organizer

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import java.time.LocalDate
import java.time.LocalTime
import java.time.temporal.ChronoUnit

/** Keeps identifiers and attributes from the desktop when modifying an existing stage. */
internal fun itineraryPayload(stages: List<JsonObject>, start: String, end: String): JsonArray {
    val first = LocalDate.parse(start.trim())
    val last = LocalDate.parse(end.trim())
    val days = ChronoUnit.DAYS.between(first, last) + 1
    require(days > 0) { "Controlla le date del viaggio." }
    val orders = mutableMapOf<Int, Int>()
    return JsonArray().apply {
        stages.sortedBy { it.number("day") }.forEach { original ->
            val stage = original.deepCopy()
            val day = stage.number("day")
            require(day in 1..days) { "Il giorno di ogni tappa deve rientrare nella durata del viaggio." }
            require(stage.text("title").isNotBlank()) { "Ogni tappa deve avere un titolo." }
            require(stage.text("category") in listOf("VISIT", "EXCURSION", "HOTEL", "MEAL", "TRANSPORT", "OTHER")) { "Categoria della tappa non valida." }
            listOf("createdAt", "updatedAt", "tripId").forEach(stage::remove)
            stage.addProperty("day", day)
            stage.addProperty("orderInDay", (orders[day] ?: 0) + 1)
            orders[day] = stage.number("orderInDay")
            listOf("startTime", "endTime").forEach { key ->
                val value = stage.get(key)
                if (value != null && value.isJsonArray) {
                    val parts = value.asJsonArray.map { it.asInt }
                    stage.addProperty(key, LocalTime.of(parts[0], parts[1], parts.getOrElse(2) { 0 }, parts.getOrElse(3) { 0 }).toString())
                }
            }
            add(stage)
        }
    }
}

internal fun stagePayload(original: JsonObject, fields: Map<String, String>, category: String): JsonObject {
    val stage = original.deepCopy()
    require(fields.getValue("title").isNotBlank()) { "Inserisci il titolo della tappa." }
    val day = fields.getValue("day").trim().toIntOrNull()
    require(day != null && day > 0) { "Inserisci un giorno positivo." }
    fields.forEach { (key, value) -> stage.addProperty(key, value.trim()) }
    stage.addProperty("day", day)
    stage.addProperty("category", category)
    val latitude = fields.getValue("latitude").trim().replace(',', '.')
    val longitude = fields.getValue("longitude").trim().replace(',', '.')
    if (latitude.isBlank() && longitude.isBlank()) {
        stage.remove("latitude"); stage.remove("longitude")
    } else {
        val lat = latitude.toDoubleOrNull(); val lon = longitude.toDoubleOrNull()
        require(lat != null && lon != null && lat.isFinite() && lon.isFinite() && lat in -90.0..90.0 && lon in -180.0..180.0) { "Inserisci entrambe le coordinate con valori validi." }
        stage.addProperty("latitude", lat); stage.addProperty("longitude", lon)
    }
    return stage
}

@Composable
internal fun ItineraryEditor(stages: MutableList<JsonObject>, enabled: Boolean) {
    var selected by remember { mutableStateOf<Int?>(null) }
    Text("Itinerario", style = MaterialTheme.typography.titleLarge)
    Text("Le tappe sono ordinate per giorno; puoi riordinare quelle dello stesso giorno.")
    if (stages.isEmpty()) Text("Aggiungi le tappe del viaggio, oppure salva senza itinerario.")
    stages.forEachIndexed { index, stage ->
        OutlinedCard(Modifier.fillMaxWidth()) {
            Column(Modifier.padding(12.dp)) {
                Text("Giorno ${stage.number("day")} · ${stage.text("title")}", style = MaterialTheme.typography.titleMedium)
                Text(stage.text("city"))
                Row {
                    TextButton(enabled = enabled, onClick = { selected = index }) { Text("Modifica tappa ${index + 1}") }
                    TextButton(enabled = enabled, onClick = { stages.removeAt(index) }) { Text("Rimuovi") }
                }
                Row {
                    TextButton(enabled = enabled && index > 0 && stages[index - 1].number("day") == stage.number("day"), onClick = {
                        val value = stages.removeAt(index); stages.add(index - 1, value)
                    }) { Text("Sposta su") }
                    TextButton(enabled = enabled && index < stages.lastIndex && stages[index + 1].number("day") == stage.number("day"), onClick = {
                        val value = stages.removeAt(index); stages.add(index + 1, value)
                    }) { Text("Sposta giù") }
                }
            }
        }
    }
    OutlinedButton(enabled = enabled, onClick = { selected = stages.size }) { Text("+ Aggiungi tappa") }
    selected?.let { index ->
        val original = stages.getOrNull(index) ?: JsonObject().apply { addProperty("day", 1) }
        StageForm(original, onDismiss = { selected = null }, onSave = {
            if (index < stages.size) stages[index] = it else stages.add(it)
            selected = null
        })
    }
}

@Composable
private fun StageForm(original: JsonObject, onDismiss: () -> Unit, onSave: (JsonObject) -> Unit) {
    val labels = linkedMapOf("title" to "Titolo tappa", "day" to "Giorno del viaggio", "locationName" to "Luogo",
        "city" to "Città", "description" to "Descrizione", "latitude" to "Latitudine (facoltativa)", "longitude" to "Longitudine (facoltativa)")
    val fields = remember { mutableStateMapOf<String, String>().apply { labels.keys.forEach { put(it, original.text(it)) } } }
    var category by remember { mutableStateOf(original.text("category").ifBlank { "OTHER" }) }
    var error by remember { mutableStateOf<String?>(null) }
    AlertDialog(onDismissRequest = onDismiss, title = { Text("Tappa del viaggio") }, text = {
        Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            labels.forEach { (key, label) -> OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, label = { Text(label) }, modifier = Modifier.fillMaxWidth()) }
            CategorySelector(category, { category = it }, true)
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
    }, confirmButton = { TextButton(onClick = {
        try { onSave(stagePayload(original, fields.toMap(), category)) } catch (exception: IllegalArgumentException) { error = exception.message }
    }) { Text("Conferma tappa") } }, dismissButton = { TextButton(onClick = onDismiss) { Text("Annulla") } })
}

@Composable
internal fun CategorySelector(selected: String, onSelect: (String) -> Unit, enabled: Boolean) {
    Text("Categoria")
    listOf("EXCURSION" to "Escursione", "VISIT" to "Visita", "TRANSPORT" to "Trasporto", "HOTEL" to "Hotel", "MEAL" to "Ristorazione", "OTHER" to "Altro").forEach { (value, label) ->
        FilterChip(selected = selected == value, onClick = { onSelect(value) }, enabled = enabled, label = { Text(label) })
    }
}
