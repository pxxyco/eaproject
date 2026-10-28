package com.example.application.screens.organizer

import android.content.ContentResolver
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.application.network.ApiResult
import com.example.application.network.ApiService
import com.google.gson.JsonObject
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException


private suspend fun uploadPickedPhoto(resolver: ContentResolver, uri: Uri, path: String, cover: Boolean): ApiResult<Unit> = withContext(Dispatchers.IO) {
    try {
        val bytes = resolver.openInputStream(uri)?.use { input ->
            readPhotoBytes(input)
        } ?: return@withContext ApiResult.HttpError(400, "Foto non leggibile")
        if (bytes.isEmpty()) return@withContext ApiResult.HttpError(400, "Foto vuota")
        ApiService.uploadPhoto(path, if (cover) "file" else "files", bytes, resolver.getType(uri) ?: "application/octet-stream")
    } catch (exception: PhotoTooLargeException) {
        ApiResult.HttpError(413, "Foto oltre 10 MB")
    } catch (exception: SecurityException) {
        ApiResult.HttpError(400, "Seleziona di nuovo la foto per autorizzarne la lettura")
    } catch (exception: IOException) {
        ApiResult.HttpError(400, "Impossibile leggere la foto selezionata")
    }
}

@Composable
internal fun OfferPhotosDialog(offer: OrganizerOffer, onDismiss: () -> Unit, onChanged: () -> Unit) {
    val resolver = LocalContext.current.contentResolver
    val scope = rememberCoroutineScope()
    val kind = if (offer.trip) "trip" else "activity"
    val base = "/api/${if (offer.trip) "trip-images" else "activity-images"}"
    val itemPath = "$base/$kind/${offer.id}"
    var photos by remember { mutableStateOf<List<JsonObject>>(emptyList()) }
    var loading by remember { mutableStateOf(true) }
    var busy by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var tick by remember { mutableIntStateOf(0) }
    var deleting by remember { mutableStateOf<JsonObject?>(null) }
    var notice by remember { mutableStateOf<String?>(null) }
    val remaining = remember { mutableStateListOf<Uri>() }
    var coverUpload by remember { mutableStateOf(false) }

    fun uploadSelection() {
        if (busy || remaining.isEmpty()) return
        busy = true; error = null; notice = null
        scope.launch {
            try {
                while (remaining.isNotEmpty()) {
                    val result = uploadPickedPhoto(resolver, remaining.first(), "$itemPath/${if (coverUpload) "cover" else "photos"}", coverUpload)
                    if (result is ApiResult.Success) {
                        remaining.removeAt(0)
                        onChanged()
                    } else {
                        error = if (result is ApiResult.HttpError && result.code == 413) "Ogni foto deve essere più piccola di 10 MB." else if (result is ApiResult.HttpError && result.code == 400) "Foto non valida o non leggibile. Scegli un'immagine JPG o PNG." else result.errorMessage()
                        break
                    }
                }
                if (remaining.isEmpty()) notice = "Foto salvate"
                tick++
            } finally { busy = false }
        }
    }
    val coverPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) { remaining.clear(); remaining.add(uri); coverUpload = true; uploadSelection() }
    }
    val photosPicker = rememberLauncherForActivityResult(ActivityResultContracts.GetMultipleContents()) { uris ->
        if (uris.isNotEmpty()) { remaining.clear(); remaining.addAll(uris.distinct()); coverUpload = false; uploadSelection() }
    }
    LaunchedEffect(tick) {
        loading = true
        loadError = null
        try {
            when (val result = loadOrganizerList(itemPath)) {
                is ApiResult.Success -> photos = result.data.sortedBy { it.number("orderIndex") }
                else -> loadError = result.errorMessage()
            }
        } finally { loading = false }
    }
    Dialog(onDismissRequest = { if (!busy) onDismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, dismissOnBackPress = !busy, dismissOnClickOutside = false)) {
        Surface(Modifier.fillMaxSize()) {
            Column(Modifier.safeDrawingPadding().padding(16.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Foto · ${offer.title}", style = MaterialTheme.typography.titleLarge)
                Text("La prima foto è la copertina. Puoi cambiarla o aggiungere altre immagini. Massimo 10 MB per foto.")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(enabled = !busy && !loading, onClick = { coverPicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Scegli copertina") }
                    OutlinedButton(enabled = !busy && !loading, onClick = { photosPicker.launch("image/*") }, modifier = Modifier.weight(1f)) { Text("Aggiungi foto") }
                }
                if (loading || busy) LinearProgressIndicator(Modifier.fillMaxWidth())
                loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                notice?.let { Text(it) }
                if (remaining.isNotEmpty() && !busy) {
                    Text("${remaining.size} foto da caricare")
                    Row {
                        TextButton(onClick = { uploadSelection() }) { Text("Riprova caricamento") }
                        TextButton(onClick = { remaining.clear(); error = null }) { Text("Scarta selezione") }
                    }
                }
                TextButton(enabled = !busy && !loading, onClick = { tick++ }) { Text("Aggiorna foto") }
                LazyColumn(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    if (!loading && loadError == null && photos.isEmpty()) item { Text("Nessuna foto. Aggiungi la prima copertina.") }
                    items(photos, key = { it.text("imageId") }) { photo ->
                        OutlinedCard {
                            RemoteOfferImage(photo.text("imageUrl"), "Foto", tick)
                            Row(Modifier.fillMaxWidth().padding(horizontal = 12.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text(if (photo == photos.firstOrNull()) "Copertina" else "Foto ${photos.indexOf(photo) + 1}", modifier = Modifier.padding(top = 12.dp))
                                TextButton(enabled = !busy && !loading, onClick = { deleting = photo }) { Text("Elimina foto") }
                            }
                        }
                    }
                }
                Button(onClick = onDismiss, enabled = !busy, modifier = Modifier.fillMaxWidth()) { Text("Fine") }
            }
        }
    }
    deleting?.let { photo ->
        AlertDialog(onDismissRequest = { if (!busy) deleting = null }, title = { Text("Elimina foto?") },
            text = { Text("Se elimini la copertina, verrà usata la foto successiva.") },
            confirmButton = { TextButton(enabled = !busy, onClick = {
                busy = true
                scope.launch {
                    try {
                        val result = ApiService.delete("$base/${photo.text("imageId")}", Unit::class.java)
                        if (result is ApiResult.Success) { onChanged(); tick++; notice = "Foto eliminata" } else error = result.errorMessage()
                        deleting = null
                    } finally { busy = false }
                }
            }) { Text("Elimina") } }, dismissButton = { TextButton(enabled = !busy, onClick = { deleting = null }) { Text("Annulla") } })
    }
}
