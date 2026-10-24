package com.example.application.screens.organizer

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.application.network.*
import com.example.application.ui.theme.*
import com.google.gson.JsonObject
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.launch

@Composable
fun OrganizerDashboardScreen(userId: Long, modifier: Modifier = Modifier) {
    key(userId) { Dashboard(userId, modifier) }
}

@Composable
private fun Dashboard(userId: Long, modifier: Modifier) {
    val scope = rememberCoroutineScope()
    var offers by remember { mutableStateOf<List<OrganizerOffer>>(emptyList()) }
    var bookings by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var revenue by remember { mutableStateOf<String?>(null) }
    var offersLoaded by remember { mutableStateOf(false) }
    var bookingsLoaded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<OrganizerOffer?>(null) }
    var deleting by remember { mutableStateOf<OrganizerOffer?>(null) }
    var choosing by remember { mutableStateOf(false) }
    LaunchedEffect(refresh) {
        loading = true
        try {
            coroutineScope {
                val activities = async { loadOrganizerList("/api/activities/organizer/mine") }
                val trips = async { loadOrganizerList("/api/trips/organizer/$userId") }
                val subscribers = async { loadOrganizerList("/api/bookings/organizer/mine") }
                val payments = async { ApiService.get("/api/payments/organizer/revenue", java.math.BigDecimal::class.java) }
                val a = activities.await(); val t = trips.await(); val b = subscribers.await(); val p = payments.await()
                val errors = mutableListOf<String>()
                if (a is ApiResult.Success && t is ApiResult.Success) {
                    offers = a.data.map { OrganizerOffer(false, it) } + t.data.map { OrganizerOffer(true, it) }
                    offersLoaded = true
                } else errors += "Offerte: ${a.errorMessage() ?: t.errorMessage()}"
                if (b is ApiResult.Success) {
                    bookings = b.data.filter { !it.text("bookingStatus").equals("CANCELLED", true) }.sortedByDescending { it.text("createdAt") }
                    bookingsLoaded = true
                } else errors += "Prenotazioni: ${b.errorMessage()}"
                if (p is ApiResult.Success) revenue = p.data.toPlainString() else errors += "Incassi: ${p.errorMessage()}"
                error = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
            }
        } finally { loading = false }
    }
    LazyColumn(modifier.fillMaxSize().background(ScreenBackground), contentPadding = PaddingValues(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Dashboard Organizzatore", style = MaterialTheme.typography.headlineMedium)
            Text("Gestisci le tue offerte e controlla le prenotazioni", color = Stone400)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { choosing = true }, enabled = !busy && !loading, modifier = Modifier.fillMaxWidth()) { Text("+ Nuova offerta") }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(onClick = { refresh++ }, enabled = !loading && !busy) { Text("Aggiorna") }
            }
            if (loading) LinearProgressIndicator(Modifier.fillMaxWidth())
            error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        }
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Metric("Offerte pubblicate", if (offersLoaded) offers.count { it.data.text("status").equals("PUBLISHED", true) }.toString() else "—", Modifier.weight(1f))
                Metric("Iscritti totali", if (bookingsLoaded) bookings.sumOf { it.number("seats") }.toString() else "—", Modifier.weight(1f))
            }
            Spacer(Modifier.height(12.dp))
            Metric("Incassi da pagamenti", revenue?.let(::money) ?: "—", Modifier.fillMaxWidth(), Color(0xFF34D399))
        }
        listOf(false, true).forEach { trip ->
            val filtered = offers.filter { it.trip == trip }
            item { Text(if (trip) "I tuoi viaggi" else "Le tue attività", style = MaterialTheme.typography.titleLarge) }
            if (offersLoaded && filtered.isEmpty()) item { Text("Non hai ancora ${if (trip) "viaggi" else "attività"}. Crea la tua prima offerta.", color = Stone400) }
            items(filtered, key = { "${it.trip}:${it.id}" }) { offer ->
                DashboardCard {
                    OfferImage(offer)
                    Text(when (offer.data.text("status")) { "PUBLISHED" -> "Pubblicata"; "DRAFT" -> "Bozza"; "CANCELLED" -> "Annullata"; "COMPLETED" -> "Conclusa"; else -> "" }, color = Color(0xFF6EE7B7))
                    Text(offer.title, style = MaterialTheme.typography.titleLarge)
                    Text(offer.city, color = Stone400)
                    Text("${dateLabel(offer.data.text("startDate"))} – ${dateLabel(offer.data.text("endDate"))}", style = MaterialTheme.typography.bodySmall)
                    Text("${offer.data.number("availableSeats")} / ${offer.data.number("maxSeats")} posti disponibili", color = Stone400)
                    Text(money(offer.price), style = MaterialTheme.typography.titleMedium)
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { editor = offer }, enabled = !busy && !loading, modifier = Modifier.weight(1f)) { Text("Modifica") }
                        TextButton(onClick = { deleting = offer }, enabled = !busy && !loading, modifier = Modifier.weight(1f)) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        item { Text("Ultimi iscritti ai tuoi eventi", style = MaterialTheme.typography.titleLarge) }
        if (bookingsLoaded && bookings.isEmpty()) item { Text("Non hai ancora ricevuto prenotazioni.", color = Stone400) }
        items(bookings, key = { "booking:${it.text("bookingId")}" }) { booking ->
            DashboardCard {
                Text(booking.text("participantName").ifBlank { "Partecipante" }, style = MaterialTheme.typography.titleMedium)
                Text(booking.text("participantEmail"), color = Stone400)
                Text(booking.text("itemTitle"))
                Text("${dateLabel(booking.text("createdAt"))} · ${booking.number("seats")} posti", color = Stone400)
            }
        }
    }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Nuova offerta") }, text = { Text("Che cosa vuoi organizzare?") },
        confirmButton = { TextButton(onClick = { choosing = false; editor = OrganizerOffer(false, JsonObject()) }) { Text("Attività") } },
        dismissButton = { TextButton(onClick = { choosing = false; editor = OrganizerOffer(true, JsonObject()) }) { Text("Viaggio") } })
    editor?.let { offer -> OfferEditor(offer, { editor = null }, { editor = null; refresh++ }) }
    deleting?.let { offer ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("Elimina offerta") }, text = { Text("Vuoi eliminare «${offer.title}»?") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        val result = ApiService.delete("${offer.path}/${offer.id}", Unit::class.java)
                        deleting = null
                        if (result is ApiResult.Success) refresh++ else error = result.errorMessage()
                    } finally { busy = false }
                }
            }) { Text(if (busy) "Eliminazione…" else "Elimina") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Annulla") } })
    }
}

@Composable
private fun DashboardCard(content: @Composable ColumnScope.() -> Unit) {
    Surface(shape = RoundedCornerShape(16.dp), color = PopupSurface, border = BorderStroke(1.dp, GlassBorder), modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp), content = content)
    }
}

@Composable
private fun Metric(label: String, value: String, modifier: Modifier, color: Color = Color.White) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = GlassFill, border = BorderStroke(1.dp, GlassBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Stone400, style = MaterialTheme.typography.labelMedium)
            Text(value, color = color, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
private fun OfferEditor(offer: OrganizerOffer, onDismiss: () -> Unit, onSaved: () -> Unit) {
    val scope = rememberCoroutineScope()
    val labels = remember(offer) { linkedMapOf(
        "title" to "Titolo", "description" to "Descrizione",
        (if (offer.trip) "destinationCity" else "city") to "Città",
        (if (offer.trip) "destinationCountry" else "placeName") to (if (offer.trip) "Paese" else "Luogo"),
        "startDate" to (if (offer.trip) "Inizio (AAAA-MM-GG)" else "Inizio (AAAA-MM-GGTHH:MM)"),
        "endDate" to (if (offer.trip) "Fine (AAAA-MM-GG)" else "Fine (AAAA-MM-GGTHH:MM)"),
        (if (offer.trip) "totalPrice" else "price") to "Prezzo (€)", "maxSeats" to "Posti totali"
    ).apply { if (!offer.trip) { put("durationMinutes", "Durata (minuti)"); put("notes", "Note (facoltative)") } } }
    val fields = remember(offer) { mutableStateMapOf<String, String>().apply { labels.keys.forEach { put(it, offer.data.text(it)) } } }
    var category by remember { mutableStateOf(offer.data.text("category").ifBlank { "EXCURSION" }) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().imePadding(), color = ScreenBackground) {
            Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                Text("${if (offer.id.isBlank()) "Nuova offerta" else "Modifica offerta"} · ${if (offer.trip) "Viaggio" else "Attività"}", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(4.dp))
                    labels.forEach { (key, label) ->
                        OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, label = { Text(label) }, enabled = !busy,
                            singleLine = key != "description" && key != "notes", modifier = Modifier.fillMaxWidth())
                    }
                    if (!offer.trip) {
                        Text("Categoria")
                        listOf("EXCURSION" to "Escursione", "VISIT" to "Visita", "TRANSPORT" to "Trasporto", "HOTEL" to "Hotel", "MEAL" to "Ristorazione", "OTHER" to "Altro").forEach { (value, label) ->
                            FilterChip(selected = category == value, onClick = { category = value }, enabled = !busy, label = { Text(label) })
                        }
                    }
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(8.dp))
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Annulla") }
                    Button(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                        val payload = try { offerPayload(offer, fields.toMap()).apply { if (!offer.trip) addProperty("category", category) } }
                        catch (exception: IllegalArgumentException) { error = exception.message; return@Button }
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val result = if (offer.id.isBlank()) ApiService.post(offer.path, payload, JsonObject::class.java)
                                    else ApiService.put("${offer.path}/${offer.id}", payload, JsonObject::class.java)
                                if (result is ApiResult.Success) onSaved() else error = result.errorMessage()
                            } finally { busy = false }
                        }
                    }) { Text(if (busy) "Salvataggio…" else "Salva") }
                }
            }
        }
    }
}
