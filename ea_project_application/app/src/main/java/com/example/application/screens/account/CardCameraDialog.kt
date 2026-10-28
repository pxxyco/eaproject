package com.example.application.screens.account

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.SecureFlagPolicy
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.application.device.CardScanSession
import com.example.application.device.ScannedCard
import kotlinx.coroutines.delay
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun CardCameraDialog(onDismiss: () -> Unit, onCard: (ScannedCard) -> Unit) {
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    var scanning by remember { mutableStateOf(false) }
    var denied by remember { mutableStateOf(false) }
    var timedOut by remember { mutableStateOf(false) }
    var rear by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf<ScannedCard?>(null) }
    val session = remember { CardScanSession() }
    val cameraExists = remember { context.packageManager.hasSystemFeature(PackageManager.FEATURE_CAMERA_ANY) }
    val permission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        scanning = granted; denied = !granted
    }
    val deliver by rememberUpdatedState(onCard)
    val delivered = remember { AtomicBoolean(false) }
    fun finish(card: ScannedCard) {
        if (delivered.compareAndSet(false, true)) { scanning = false; session.clear(); deliver(card) }
    }
    fun dismiss() { delivered.set(true); scanning = false; session.clear(); onDismiss() }
    DisposableEffect(lifecycle) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_PAUSE) { scanning = false; session.clear(); snapshot = null }
        }
        lifecycle.addObserver(observer)
        onDispose { lifecycle.removeObserver(observer); session.clear() }
    }
    LaunchedEffect(scanning) {
        if (scanning) { delay(60_000); scanning = false; session.clear(); snapshot = null; timedOut = true }
    }
    if (!scanning) {
        AlertDialog(onDismissRequest = { dismiss() }, properties = DialogProperties(securePolicy = SecureFlagPolicy.SecureOn),
            title = { Text("Scansiona la carta") },
            text = { Text(when {
                !cameraExists -> "Questo dispositivo non dispone di una fotocamera. Puoi usare NFC o compilare i campi a mano."
                timedOut -> "Scansione terminata dopo un minuto. Puoi riprovare oppure compilare il modulo a mano."
                denied -> "La fotocamera non è autorizzata. Puoi concedere il permesso nelle impostazioni oppure continuare a mano."
                else -> "Inquadra il fronte e, se necessario, il retro della stessa carta. I fotogrammi vengono analizzati sul dispositivo senza salvare fotografie. Non vengono estratti PIN o CVV. Quando numero, nome e scadenza sono completi e validi, la carta verrà aggiunta automaticamente con i soli dati previsti dall’account."
            }) },
            confirmButton = { if (cameraExists) Button(onClick = {
                timedOut = false
                if (denied) context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                else if (ContextCompat.checkSelfPermission(context, Manifest.permission.CAMERA) == PackageManager.PERMISSION_GRANTED) scanning = true
                else permission.launch(Manifest.permission.CAMERA)
            }) { Text(if (denied) "Apri impostazioni" else "Avvia camera") } },
            dismissButton = { OutlinedButton(onClick = { dismiss() }) { Text("Annulla") } })
        return
    }
    Dialog(onDismissRequest = { dismiss() }, properties = DialogProperties(usePlatformDefaultWidth = false, securePolicy = SecureFlagPolicy.SecureOn)) {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            Column(Modifier.safeDrawingPadding().padding(20.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                Text(if (rear) "Ora inquadra il retro" else "Inquadra il fronte", style = MaterialTheme.typography.headlineSmall)
                Text("Mantieni visibile una sola carta. Usa una buona luce ed evita riflessi. Nessuna foto viene salvata.", style = MaterialTheme.typography.bodyMedium)
                CameraCardPreview(Modifier.fillMaxWidth().weight(1f), session, onProgress = { snapshot = it }, onComplete = { finish(it) })
                snapshot?.let { card ->
                    Text("${card.brand}  •••• ${card.last4}", style = MaterialTheme.typography.titleMedium)
                    Text("Nome: ${if (card.holder == null) "da rilevare" else "rilevato"} · Scadenza: ${if (card.month == null) "da rilevare" else "rilevata"}", style = MaterialTheme.typography.bodySmall)
                }
                OutlinedButton(onClick = { rear = !rear }, modifier = Modifier.fillMaxWidth()) { Text(if (rear) "Torna al fronte" else "Inquadra il retro") }
                snapshot?.let { card -> OutlinedButton(onClick = { finish(card) }, modifier = Modifier.fillMaxWidth()) { Text("Completa i campi a mano") } }
                OutlinedButton(onClick = { dismiss() }, modifier = Modifier.fillMaxWidth()) { Text("Annulla scansione") }
            }
        }
    }
}
