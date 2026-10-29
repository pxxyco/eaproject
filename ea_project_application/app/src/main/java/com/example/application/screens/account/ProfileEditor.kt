package com.example.application.screens.account

import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.result.PickVisualMediaRequest
import com.example.application.device.launchPhotoPicker
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.BorderStroke
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.painterResource
import com.example.application.R
import com.example.application.ui.theme.*
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.example.application.dto.UserDTO
import com.example.application.network.*
import com.example.application.screens.common.PhotoTooLargeException
import com.example.application.screens.common.readPhotoBytes
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException

@Composable
internal fun ProfileEditor(user: UserDTO, actions: AccountActions, gateway: AccountGateway) {
    var name by rememberSaveable { mutableStateOf(user.fullName.orEmpty()) }
    var username by rememberSaveable { mutableStateOf(user.username.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(user.email.orEmpty()) }
    var selected by remember { mutableStateOf<Uri?>(null) }
    var captured by remember { mutableStateOf<ByteArray?>(null) }
    var showCamera by rememberSaveable { mutableStateOf(false) }
    val context = LocalContext.current
    val picker = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri != null) { selected = uri; captured = null }
    }
    if (showCamera) ProfileCameraDialog(onDismiss = { showCamera = false }, onPhoto = {
        captured = it; selected = null; showCamera = false
    })
    AccountPanel("Una foto per farti riconoscere", "PNG o JPG, fino a 5 MB.") {
        val choose = {
            launchPhotoPicker { picker.launch(PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly)) }
                ?.let { error -> actions.run { error } }
            Unit
        }
        ProfilePhotoHeader(user, captured ?: selected, !actions.busy, choose, { showCamera = true })
        if (selected != null || captured != null) {
            Button(modifier = Modifier.fillMaxWidth(), enabled = !actions.busy, onClick = {
                val uri = selected
                val photo = captured
                actions.run {
                    try {
                        val mime = if (photo != null) "image/jpeg" else uri?.let { context.contentResolver.getType(it) }
                        if (mime !in listOf("image/jpeg", "image/png")) return@run "Scegli un’immagine PNG o JPG."
                        val bytes = photo ?: withContext(Dispatchers.IO) {
                            uri?.let { context.contentResolver.openInputStream(it) }?.use { readPhotoBytes(it, 5 * 1024 * 1024) }
                                ?: throw IOException("Immagine non disponibile")
                        }
                        when (val result = gateway.uploadPhoto(bytes, mime!!)) {
                            is ApiResult.Success -> { UserSession.setSession(result.data); selected = null; captured = null; "Foto profilo aggiornata." }
                            else -> result.accountError()
                        }
                    } catch (_: PhotoTooLargeException) { "La foto supera il limite di 5 MB." }
                    catch (_: IOException) { "Impossibile leggere la foto. Selezionala nuovamente." }
                    catch (_: SecurityException) { "Accesso alla foto non disponibile. Selezionala nuovamente." }
                }
            }) { Text("Carica foto") }
            OutlinedButton(modifier = Modifier.fillMaxWidth(), onClick = { selected = null; captured = null }, enabled = !actions.busy) { Text("Annulla selezione") }
        }
        if (!user.profilePhotoUrl.isNullOrBlank() && selected == null && captured == null) OutlinedButton(
            colors = ButtonDefaults.outlinedButtonColors(contentColor = MaterialTheme.colorScheme.error),
            enabled = !actions.busy, onClick = {
            actions.run {
                when (val result = gateway.removePhoto()) {
                    is ApiResult.Success -> { UserSession.setSession(result.data); "Foto rimossa." }
                    else -> result.accountError()
                }
            }
        }) { Text("Rimuovi foto") }
    }
    AccountPanel("I tuoi dati", if (user.oauthProvider) "Account Traveler · Accesso esterno" else "Account Traveler · Accesso con email e password") {
        AccountField("Nome completo", name, { name = it }, !actions.busy)
        AccountField("Username", username, { username = it }, !actions.busy, max = 30)
        AccountField("Indirizzo email", email, { email = it }, !actions.busy, max = 254, keyboard = KeyboardType.Email)
        Button(enabled = !actions.busy, onClick = {
            actions.run {
                val input = ProfileInput(username, email, name)
                input.error() ?: when (val result = gateway.saveProfile(input)) {
                    is ApiResult.Success -> {
                        UserSession.setSession(result.data)
                        name = result.data.fullName.orEmpty(); username = result.data.username.orEmpty(); email = result.data.email.orEmpty()
                        "Profilo aggiornato."
                    }
                    else -> result.accountError()
                }
            }
        }) { Text("Salva modifiche") }
    }
}

@Composable
private fun ProfilePhotoHeader(user: UserDTO, selected: Any?, enabled: Boolean, choose: () -> Unit, takePhoto: () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = GlassFillStrong, contentColor = MaterialTheme.colorScheme.onSurface) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(18.dp)) {
                Box(Modifier.size(108.dp), contentAlignment = Alignment.Center) {
                    Surface(Modifier.fillMaxSize(), shape = CircleShape, color = GlassFill,
                        border = BorderStroke(2.dp, NavigationOutline)) {
                        Box(Modifier.padding(5.dp)) {
                            if (selected == null) AccountAvatar(user, Modifier.fillMaxSize())
                            else AsyncImage(selected, "Anteprima foto selezionata", modifier = Modifier.fillMaxSize().clip(CircleShape), contentScale = ContentScale.Crop)
                        }
                    }
                    Surface(onClick = takePhoto, enabled = enabled, modifier = Modifier.align(Alignment.BottomEnd).size(48.dp), shape = CircleShape, color = MaterialTheme.colorScheme.primary,
                        contentColor = MaterialTheme.colorScheme.onPrimary, border = BorderStroke(2.dp, MaterialTheme.colorScheme.surface)) {
                        Box(contentAlignment = Alignment.Center) {
                            Icon(painterResource(R.drawable.ic_scan_camera), "Scatta foto profilo", Modifier.size(21.dp))
                        }
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(if (selected != null) "La tua nuova immagine" else user.fullName ?: user.username.orEmpty(), style = MaterialTheme.typography.titleMedium)
                    Text(when {
                        selected != null -> "Anteprima pronta: caricala per aggiornare il profilo."
                        user.profilePhotoUrl.isNullOrBlank() -> "Aggiungi un tocco personale al tuo profilo."
                        else -> "La foto con cui ti riconoscono su EA Project."
                    }, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            FilledTonalButton(onClick = choose, enabled = enabled, modifier = Modifier.fillMaxWidth(), shape = RoundedCornerShape(14.dp)) {
                Icon(painterResource(R.drawable.ic_scan_camera), null, Modifier.size(20.dp))
                Spacer(Modifier.width(10.dp))
                Text(if (selected == null && user.profilePhotoUrl.isNullOrBlank()) "Scegli foto" else "Cambia foto")
            }
        }
    }
}
