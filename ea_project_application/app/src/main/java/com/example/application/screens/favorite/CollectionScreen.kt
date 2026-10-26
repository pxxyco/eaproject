package com.example.application.screens.favorite

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.dto.CollectionItemsDTO
import com.example.application.dto.canCancel
import com.example.application.network.MyTripRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch

private data class CollectionAction(val title: String, val run: suspend () -> Unit, val leave: Boolean = false)

@Composable
fun CollectionScreen(target: MyTripRepository.HubTarget, onBack: () -> Unit, modifier: Modifier = Modifier,
    load: suspend (MyTripRepository.HubTarget) -> CollectionItemsDTO = { MyTripRepository.loadItems(it) },
    deleteList: suspend (Long) -> Unit = { MyTripRepository.deleteList(it) },
    remove: suspend (MyTripRepository.HubTarget, Long, Boolean) -> Unit = { targetItem, id, trip -> MyTripRepository.removeItem(targetItem, id, trip) }
) {
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
        } catch (e: CancellationException) { throw e
        } catch (e: Exception) { error = e.message ?: "Impossibile caricare la lista."
        } finally { busy = false }
    }
    Column(modifier.fillMaxSize().padding(16.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = onBack, enabled = !busy) { Text("Indietro") }
            if (target is MyTripRepository.HubTarget.Custom) TextButton(enabled = !busy, onClick = {
                pending = CollectionAction("Eliminare la lista?", { deleteList(target.list.id) }, true)
            }) { Text("Elimina lista") }
        }
        Text(target.title, style = MaterialTheme.typography.headlineSmall)
        if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
        error?.let { Text(it, color = MaterialTheme.colorScheme.error)
            TextButton(onClick = { reload++ }, enabled = !busy) { Text("Riprova") }
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            if (target == MyTripRepository.HubTarget.Bookings) {
                items(data.bookings) { booking ->
                    CollectionEntry(booking.itemTitle ?: "Prenotazione", listOfNotNull(booking.itemLocation, booking.itemDate, booking.bookingStatus).joinToString(" · "), "Annulla prenotazione", !busy && booking.canCancel()) {
                        pending = CollectionAction("Annullare la prenotazione?", {
                            remove(target, requireNotNull(booking.bookingId), false)
                        })
                    }
                }
            } else {
                items(data.trips.orEmpty()) { trip ->
                    CollectionEntry(trip.title ?: "Viaggio", trip.location.orEmpty(), "Rimuovi dalla lista", !busy && trip.tripId != null) {
                        pending = CollectionAction("Rimuovere il viaggio dalla lista?", { remove(target, requireNotNull(trip.tripId), true) })
                    }
                }
                items(data.activities.orEmpty()) { activity ->
                    CollectionEntry(activity.title ?: "Attività", activity.city.orEmpty(), "Rimuovi dalla lista", !busy && activity.activityId != null) {
                        pending = CollectionAction("Rimuovere l’attività dalla lista?", { remove(target, requireNotNull(activity.activityId), false) })
                    }
                }
            }
            if (loaded && error == null && !busy && data.bookings.isEmpty() && data.trips.isNullOrEmpty() && data.activities.isNullOrEmpty()) {
                item { Text("Nessun elemento in questa lista.") }
            }
        }
    }
    pending?.let { action ->
        AlertDialog(onDismissRequest = { if (!busy) pending = null },
            title = { Text(action.title) },
            text = { Text(if (action.leave) "La lista verrà eliminata. Le offerte resteranno disponibili." else "Conferma l’operazione per continuare.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                scope.launch {
                    busy = true
                    try {
                        action.run()
                        pending = null
                        if (action.leave) onBack() else reload++
                    } catch (e: CancellationException) { throw e
                    } catch (e: Exception) { pending = null; error = e.message ?: "Operazione non riuscita."
                    } finally { busy = false }
                }
            }) { Text("Conferma") } },
            dismissButton = { TextButton(enabled = !busy, onClick = { pending = null }) { Text("Annulla") } })
    }
}

@Composable
private fun CollectionEntry(title: String, location: String, action: String, enabled: Boolean, onAction: () -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            if (location.isNotBlank()) Text(location)
            TextButton(onClick = onAction, enabled = enabled) { Text(action) }
        }
    }
}
