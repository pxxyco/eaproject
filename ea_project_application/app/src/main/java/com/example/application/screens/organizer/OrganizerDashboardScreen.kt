package com.example.application.screens.organizer

import com.example.application.ui.effects.LocalNavigationTail
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
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
    var activitiesLoaded by remember { mutableStateOf(false) }
    var tripsLoaded by remember { mutableStateOf(false) }
    val offersLoaded = activitiesLoaded && tripsLoaded
    var bookingsLoaded by remember { mutableStateOf(false) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var refresh by remember { mutableIntStateOf(0) }
    var editor by remember { mutableStateOf<OrganizerOffer?>(null) }
    var deleting by remember { mutableStateOf<OrganizerOffer?>(null) }
    var choosing by remember { mutableStateOf(false) }
    var photoOffer by remember { mutableStateOf<OrganizerOffer?>(null) }
    var query by rememberSaveable { mutableStateOf("") }
    var statusFilter by rememberSaveable { mutableStateOf("ALL") }
    var bookingQuery by rememberSaveable { mutableStateOf("") }
    val filteredBookings = bookings.filter { booking ->
        listOf("participantName", "participantEmail", "itemTitle").any { booking.text(it).contains(bookingQuery.trim(), true) }
    }
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
                if (a is ApiResult.Success) {
                    offers = offers.filter { it.trip } + a.data.map { OrganizerOffer(false, it) }
                    activitiesLoaded = true
                } else errors += "Attività: ${a.errorMessage()}"
                if (t is ApiResult.Success) {
                    offers = offers.filter { !it.trip } + t.data.map { OrganizerOffer(true, it) }
                    tripsLoaded = true
                } else errors += "Viaggi: ${t.errorMessage()}"
                if (b is ApiResult.Success) {
                    bookings = b.data.filter { !it.text("bookingStatus").equals("CANCELLED", true) }.sortedByDescending { it.text("createdAt") }
                    bookingsLoaded = true
                } else errors += "Prenotazioni: ${b.errorMessage()}"
                if (p is ApiResult.Success) revenue = p.data.toPlainString() else errors += "Incassi: ${p.errorMessage()}"
                error = errors.takeIf { it.isNotEmpty() }?.joinToString("\n")
            }
        } finally { loading = false }
    }
    LazyColumn(modifier.fillMaxSize().background(androidx.compose.ui.graphics.Color.Transparent), contentPadding = PaddingValues(start = 16.dp, top = 16.dp, end = 16.dp, bottom = 16.dp + LocalNavigationTail.current), verticalArrangement = Arrangement.spacedBy(16.dp)) {
        item {
            Text("Dashboard Organizzatore", style = MaterialTheme.typography.headlineMedium)
            Text("Gestisci le tue offerte e controlla le prenotazioni", color = Stone400)
            Spacer(Modifier.height(16.dp))
            Button(onClick = { choosing = true }, enabled = !busy && !loading,
                modifier = Modifier.fillMaxWidth().heightIn(min = 52.dp), shape = RoundedCornerShape(18.dp),
                border = BorderStroke(1.dp, GlassBorder),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.secondaryContainer,
                    contentColor = MaterialTheme.colorScheme.onSecondaryContainer)) { Text("+ Nuova offerta") }
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
            Metric("Incassi da pagamenti", revenue?.let(::money) ?: "—", Modifier.fillMaxWidth(), SuccessColor)
        }
        item {
            OutlinedTextField(query, { query = it }, label = { Text("Cerca offerte per titolo o città") }, singleLine = true, modifier = Modifier.fillMaxWidth())
            Row(Modifier.horizontalScroll(rememberScrollState()), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                (listOf("ALL" to "Tutte") + offerStatuses(true)).forEach { (value, label) ->
                    FilterChip(selected = statusFilter == value, onClick = { statusFilter = value }, label = { Text(label) })
                }
            }
        }
        listOf(false, true).forEach { trip ->
            val filtered = offers.filter { it.trip == trip && it.matches(query, statusFilter) }.sortedByDescending { it.data.text("createdAt") }
            item { Text(if (trip) "I tuoi viaggi" else "Le tue attività", style = MaterialTheme.typography.titleLarge) }
            if ((if (trip) tripsLoaded else activitiesLoaded) && filtered.isEmpty()) item {
                Text(if (query.isNotBlank() || statusFilter != "ALL") "Nessuna offerta corrisponde ai filtri." else "Non hai ancora ${if (trip) "viaggi" else "attività"}. Crea la tua prima offerta.", color = Stone400)
            }
            items(filtered, key = { "${it.trip}:${it.id}" }) { offer ->
                DashboardCard {
                    OfferImage(offer)
                    Text(when (offer.data.text("status")) { "PUBLISHED" -> "Pubblicata"; "DRAFT" -> "Bozza"; "CANCELLED" -> "Annullata"; "COMPLETED" -> "Conclusa"; else -> "" }, color = SuccessColor)
                    Text(offer.title, style = MaterialTheme.typography.titleLarge)
                    Text(offer.city, color = Stone400)
                    Text("${dateLabel(offer.data.text("startDate"))} – ${dateLabel(offer.data.text("endDate"))}", style = MaterialTheme.typography.bodySmall)
                    Text("${offer.data.number("availableSeats")} / ${offer.data.number("maxSeats")} posti disponibili", color = Stone400)
                    Text(money(offer.price), style = MaterialTheme.typography.titleMedium)
                    if (offer.trip) Text("${offer.data.get("stages")?.takeIf { it.isJsonArray }?.asJsonArray?.size() ?: 0} tappe nell'itinerario")
                    TextButton(onClick = { photoOffer = offer }, enabled = !busy && !loading) { Text("Gestisci foto") }
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        OutlinedButton(onClick = { editor = offer }, enabled = !busy && !loading, modifier = Modifier.weight(1f)) { Text("Modifica") }
                        TextButton(onClick = { deleting = offer }, enabled = !busy && !loading, modifier = Modifier.weight(1f)) { Text("Elimina", color = MaterialTheme.colorScheme.error) }
                    }
                }
            }
        }
        item { Text("Ultimi iscritti ai tuoi eventi", style = MaterialTheme.typography.titleLarge) }
        item { OutlinedTextField(bookingQuery, { bookingQuery = it }, label = { Text("Cerca iscritti o prenotazioni") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
        if (bookingsLoaded && bookings.isEmpty()) item { Text("Non hai ancora ricevuto prenotazioni.", color = Stone400) }
        if (bookingsLoaded && bookings.isNotEmpty() && filteredBookings.isEmpty()) item { Text("Nessuna prenotazione corrisponde alla ricerca.", color = Stone400) }
        items(filteredBookings, key = { "booking:${it.text("bookingId")}" }) { booking ->
            DashboardCard {
                Text(booking.text("participantName").ifBlank { "Partecipante" }, style = MaterialTheme.typography.titleMedium)
                Text(booking.text("participantEmail"), color = Stone400)
                Text(booking.text("itemTitle"))
                Text(when (booking.text("bookingStatus")) { "PENDING" -> "In attesa"; "CONFIRMED" -> "Confermata"; "COMPLETED" -> "Completata"; else -> booking.text("bookingStatus") })
                Text("${dateLabel(booking.text("createdAt"))} · ${booking.number("seats")} posti", color = Stone400)
            }
        }
    }
    if (choosing) AlertDialog(onDismissRequest = { choosing = false }, title = { Text("Nuova offerta") }, text = { Text("Che cosa vuoi organizzare?") },
        confirmButton = { TextButton(onClick = { choosing = false; editor = OrganizerOffer(false, JsonObject()) }) { Text("Attività") } },
        dismissButton = { TextButton(onClick = { choosing = false; editor = OrganizerOffer(true, JsonObject()) }) { Text("Viaggio") } })
    editor?.let { offer -> OfferEditor(offer, { editor = null }, { saved ->
        editor = null; refresh++
        if (offer.id.isBlank()) photoOffer = saved
    }) }
    photoOffer?.let { offer -> OfferPhotosDialog(offer, onDismiss = { photoOffer = null; refresh++ }, onChanged = { refresh++ }) }
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
private fun Metric(label: String, value: String, modifier: Modifier, color: Color = MaterialTheme.colorScheme.onSurface) {
    Surface(modifier, shape = RoundedCornerShape(12.dp), color = GlassFill, border = BorderStroke(1.dp, GlassBorder)) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(label, color = Stone400, style = MaterialTheme.typography.labelMedium)
            Text(value, color = color, style = MaterialTheme.typography.headlineMedium)
        }
    }
}

@Composable
internal fun OfferEditor(offer: OrganizerOffer, onDismiss: () -> Unit, onSaved: (OrganizerOffer) -> Unit) {
    val scope = rememberCoroutineScope()
    val labels = remember(offer) { linkedMapOf(
        "title" to "Titolo", "description" to "Descrizione",
        (if (offer.trip) "destinationCity" else "city") to "Città",
        (if (offer.trip) "destinationCountry" else "placeName") to (if (offer.trip) "Paese" else "Luogo"),
        "startDate" to (if (offer.trip) "Data di inizio" else "Data e ora di inizio"),
        "endDate" to (if (offer.trip) "Data di fine" else "Data e ora di fine"),
        (if (offer.trip) "totalPrice" else "price") to "Prezzo (€)", "maxSeats" to "Posti totali"
    ).apply { if (!offer.trip) { put("durationMinutes", "Durata (minuti)"); put("latitude", "Latitudine (facoltativa)"); put("longitude", "Longitudine (facoltativa)"); put("notes", "Note (facoltative)") } } }
    val fields = remember(offer) { mutableStateMapOf<String, String>().apply { labels.keys.forEach { put(it, offer.data.text(it)) } } }
    var category by remember { mutableStateOf(offer.data.text("category").ifBlank { "EXCURSION" }) }
    var status by remember { mutableStateOf(offer.data.text("status").ifBlank { "PUBLISHED" }) }
    val stages = remember(offer) { mutableStateListOf<JsonObject>().apply {
        offer.data.get("stages")?.takeIf { it.isJsonArray }?.asJsonArray?.forEach { if (it.isJsonObject) add(it.asJsonObject.deepCopy()) }
    } }
    var persisted by remember { mutableStateOf<OrganizerOffer?>(null) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize().imePadding(), color = ScreenBackground) {
            Column(Modifier.safeDrawingPadding().padding(16.dp)) {
                Text("${if (offer.id.isBlank()) "Nuova offerta" else "Modifica offerta"} · ${if (offer.trip) "Viaggio" else "Attività"}", style = MaterialTheme.typography.titleLarge)
                Column(Modifier.weight(1f).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Spacer(Modifier.height(4.dp))
                    labels.forEach { (key, label) ->
                        if (key == "startDate" || key == "endDate") {
                            OfferDateField(label, fields[key].orEmpty(), !offer.trip, !busy) { fields[key] = it }
                        } else OutlinedTextField(fields[key].orEmpty(), { fields[key] = it }, label = { Text(label) }, enabled = !busy,
                            singleLine = key != "description" && key != "notes", modifier = Modifier.fillMaxWidth())
                    }
                    if (!offer.trip) CategorySelector(category, { category = it }, !busy)
                    Text("Stato dell'offerta", style = MaterialTheme.typography.titleMedium)
                    offerStatuses(offer.trip).forEach { (value, label) ->
                        FilterChip(selected = status == value, onClick = { status = value }, enabled = !busy, label = { Text(label) })
                    }
                    if (status == "CANCELLED") Text("L'offerta non sarà prenotabile. Le prenotazioni ricevute restano nello storico.")
                    if (offer.trip) ItineraryEditor(stages, !busy)
                    if (offer.id.isBlank()) Text("Dopo il salvataggio potrai aggiungere le foto.")
                    error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                    Spacer(Modifier.height(8.dp))
                }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                    OutlinedButton(onClick = onDismiss, enabled = !busy, modifier = Modifier.weight(1f)) { Text("Annulla") }
                    Button(enabled = !busy, modifier = Modifier.weight(1f), onClick = {
                        val payload = try { offerPayload(persisted ?: offer, fields.toMap()).apply {
                            addProperty("status", status)
                            if (!offer.trip) addProperty("category", category)
                            else add("stages", itineraryPayload(stages, fields.getValue("startDate"), fields.getValue("endDate")))
                        } }
                        catch (exception: IllegalArgumentException) { error = exception.message; return@Button }
                        busy = true
                        error = null
                        scope.launch {
                            try {
                                val current = persisted ?: offer
                                val result = if (current.id.isBlank()) ApiService.post(current.path, payload, JsonObject::class.java)
                                    else ApiService.put("${current.path}/${current.id}", payload, JsonObject::class.java)
                                if (result is ApiResult.Success) {
                                    val saved = OrganizerOffer(offer.trip, result.data)
                                    persisted = saved
                                    onSaved(saved)
                                } else error = result.errorMessage()
                            } finally { busy = false }
                        }
                    }) { Text(if (busy) "Salvataggio…" else "Salva") }
                }
            }
        }
    }
}
