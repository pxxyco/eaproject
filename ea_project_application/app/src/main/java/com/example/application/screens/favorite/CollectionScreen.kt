package com.example.application.screens.favorite

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import com.example.application.device.CalendarEvent
import com.example.application.device.openCalendar
import com.example.application.ui.effects.LocalNavigationTail
import com.example.application.dto.canCancel
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.dto.CollectionItemsDTO
import com.example.application.network.MyTripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class CollectionAction(
    val title: String,
    val message: String,
    val run: suspend () -> Unit,
    val leave: Boolean = false
)

@Composable
fun CollectionScreen(
    target: MyTripRepository.HubTarget,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    load: suspend (MyTripRepository.HubTarget) -> CollectionItemsDTO = { MyTripRepository.loadItems(it) },
    deleteList: suspend (Long) -> Unit = { MyTripRepository.deleteList(it) },
    cancelBooking: suspend (Long) -> Unit = { MyTripRepository.cancelBooking(it) },
    remove: suspend (MyTripRepository.HubTarget, Long, Boolean) -> Unit = { targetItem, id, trip ->
        MyTripRepository.removeItem(targetItem, id, trip)
    }
) {
    val context = LocalContext.current
    var busy by remember(target) { mutableStateOf(false) }
    BackHandler(enabled = !busy, onBack = onBack)
    var data by remember(target) { mutableStateOf(CollectionItemsDTO()) }
    var error by remember(target) { mutableStateOf<String?>(null) }
    var loaded by remember(target) { mutableStateOf(false) }
    var pending by remember(target) { mutableStateOf<CollectionAction?>(null) }
    var reload by remember(target) { mutableIntStateOf(0) }
    val scope = rememberCoroutineScope()

    LaunchedEffect(target, reload) {
        busy = true
        try {
            data = load(target)
            loaded = true
            error = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Impossibile caricare la lista."
        } finally {
            busy = false
        }
    }

    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack, enabled = !busy) { Text("Indietro") }
            if (target is MyTripRepository.HubTarget.Custom) {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        pending = CollectionAction(
                            title = "Eliminare la lista?",
                            message = "La lista verrà eliminata. Le offerte resteranno disponibili.",
                            run = { deleteList(target.list.id) },
                            leave = true
                        )
                    }
                ) { Text("Elimina lista") }
            }
        }
        Text(target.title, style = MaterialTheme.typography.headlineSmall)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let {
            Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { reload++ }, enabled = !busy) { Text("Riprova") }
        }
        LazyColumn(contentPadding = PaddingValues(bottom = LocalNavigationTail.current), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (target == MyTripRepository.HubTarget.Bookings) {
                items(data.bookings) { booking ->
                    val cancellable = booking.canCancel()
                    CollectionEntry(
                        title = booking.itemTitle ?: "Prenotazione",
                        location = listOfNotNull(
                            booking.itemLocation,
                            booking.itemDate,
                            booking.bookingStatus
                        ).joinToString(" · "),
                        action = "Annulla prenotazione",
                        enabled = !busy && cancellable,
                        note = if (cancellable) null else "Questa prenotazione non può essere annullata",
                        onAction = {
                            pending = CollectionAction(
                                title = "Annullare la prenotazione?",
                                message = "La prenotazione verrà annullata.",
                                run = { cancelBooking(requireNotNull(booking.bookingId)) }
                            )
                        }
                    )
                    if (!booking.bookingStatus.equals("CANCELLED", true)) {
                        CalendarEvent.from(booking.itemTitle, booking.itemLocation, booking.itemDate, booking.itemEndDate)?.let { event ->
                            OutlinedButton(onClick = { context.openCalendar(event)?.let { error = it } }, enabled = !busy) {
                                Text("Aggiungi al calendario")
                            }
                        }
                    }
                }
            } else {
                items(data.trips.orEmpty()) { trip ->
                    CollectionEntry(
                        title = trip.title ?: "Viaggio",
                        location = trip.location.orEmpty(),
                        action = "Rimuovi dalla lista",
                        enabled = !busy && trip.tripId != null,
                        onAction = {
                            pending = CollectionAction(
                                title = "Rimuovere il viaggio dalla lista?",
                                message = "Conferma l’operazione per continuare.",
                                run = { remove(target, requireNotNull(trip.tripId), true) }
                            )
                        }
                    )
                }
                items(data.activities.orEmpty()) { activity ->
                    CollectionEntry(
                        title = activity.title ?: "Attività",
                        location = activity.city.orEmpty(),
                        action = "Rimuovi dalla lista",
                        enabled = !busy && activity.activityId != null,
                        onAction = {
                            pending = CollectionAction(
                                title = "Rimuovere l’attività dalla lista?",
                                message = "Conferma l’operazione per continuare.",
                                run = { remove(target, requireNotNull(activity.activityId), false) }
                            )
                        }
                    )
                }
            }
            if (loaded && error == null && !busy &&
                data.bookings.isEmpty() && data.trips.isNullOrEmpty() && data.activities.isNullOrEmpty()
            ) {
                item { Text("Nessun elemento in questa lista.") }
            }
        }
    }

    pending?.let { action ->
        AlertDialog(
            onDismissRequest = { if (!busy) pending = null },
            title = { Text(action.title) },
            text = { Text(action.message) },
            confirmButton = {
                TextButton(
                    enabled = !busy,
                    onClick = {
                        scope.launch {
                            busy = true
                            try {
                                action.run()
                                pending = null
                                if (action.leave) onBack() else reload++
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                pending = null
                                error = e.message ?: "Operazione non riuscita."
                            } finally {
                                busy = false
                            }
                        }
                    }
                ) { Text("Conferma") }
            },
            dismissButton = {
                TextButton(enabled = !busy, onClick = { pending = null }) { Text("Annulla") }
            }
        )
    }
}

@Composable
private fun CollectionEntry(
    title: String,
    location: String,
    action: String,
    enabled: Boolean,
    onAction: () -> Unit,
    note: String? = null
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (location.isNotBlank()) Text(location)
            note?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            TextButton(onClick = onAction, enabled = enabled) { Text(action) }
        }
    }
}
