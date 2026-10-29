package com.example.application.screens.account

import android.content.Intent
import android.nfc.NfcAdapter
import android.os.Bundle
import android.provider.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.example.application.device.findActivity
import com.example.application.device.NfcCardReader
import com.example.application.device.ScannedCard
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.atomic.AtomicBoolean

@Composable
internal fun NfcCardDialog(onDismiss: () -> Unit, onCard: (ScannedCard) -> Unit) {
    val context = LocalContext.current
    val activity = context.findActivity()
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    val adapter = remember { NfcAdapter.getDefaultAdapter(context) }
    var available by remember { mutableStateOf(adapter?.isEnabled == true) }
    var scanning by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val reader = remember { NfcCardReader() }
    val scope = rememberCoroutineScope()
    val reading = remember { AtomicBoolean(false) }
    val resultCallback by rememberUpdatedState(onCard)
    val flags = NfcAdapter.FLAG_READER_NFC_A or NfcAdapter.FLAG_READER_NFC_B or NfcAdapter.FLAG_READER_SKIP_NDEF_CHECK
    DisposableEffect(lifecycle, scanning) {
        fun enable() {
            available = adapter?.isEnabled == true
            if (scanning && available && activity != null) {
                try {
                    reader.start()
                    adapter?.enableReaderMode(activity, { tag ->
                        if (reading.compareAndSet(false, true)) scope.launch {
                            try {
                                val result = withContext(Dispatchers.IO) { reader.read(tag) }
                                scanning = false
                                resultCallback(result)
                            } catch (cancelled: kotlinx.coroutines.CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Lettura non riuscita. Mantieni la carta vicino al telefono oppure inserisci i dati a mano."; scanning = false }
                            finally { reading.set(false) }
                        }
                    }, flags, Bundle().apply { putInt(NfcAdapter.EXTRA_READER_PRESENCE_CHECK_DELAY, 250) })
                } catch (_: RuntimeException) { error = "Impossibile attivare NFC. Controlla le impostazioni del dispositivo."; scanning = false }
            }
        }
        fun disable() { if (activity != null) runCatching { adapter?.disableReaderMode(activity) }; reader.close() }
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) enable()
            if (event == Lifecycle.Event.ON_PAUSE) { disable(); scanning = false }
        }
        lifecycle.addObserver(observer)
        if (lifecycle.currentState.isAtLeast(Lifecycle.State.RESUMED)) enable()
        onDispose { lifecycle.removeObserver(observer); disable() }
    }
    LaunchedEffect(scanning) {
        if (scanning) { delay(30_000); scanning = false; reader.close(); error = "Scansione scaduta. Puoi riprovare o compilare i dati a mano." }
    }
    AlertDialog(onDismissRequest = onDismiss,
        title = { Text(if (scanning) "Avvicina la carta al telefono" else "Leggi la carta con NFC") },
        text = {
            Text(when {
                adapter == null || activity == null -> "Questo dispositivo non supporta la lettura NFC. Puoi inserire i dati manualmente."
                !available -> "NFC è disattivato. Attivalo nelle impostazioni del telefono e torna qui."
                error != null -> error!!
                scanning -> "Tieni la carta contactless sul retro del telefono fino al termine della lettura."
                else -> "Con il tuo consenso leggeremo i dati disponibili della carta per compilare il modulo. Non vengono letti PIN o movimenti. Alcune carte non condividono nome o scadenza: potrai completarli a mano."
            })
        },
        confirmButton = {
            if (adapter != null && activity != null && !scanning) TextButton(onClick = {
                if (!available) context.startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
                else { error = null; scanning = true }
            }) { Text(if (available) "Avvia scansione" else "Apri impostazioni NFC") }
        },
        dismissButton = { OutlinedButton(onClick = onDismiss) { Text("Annulla") } })
}
