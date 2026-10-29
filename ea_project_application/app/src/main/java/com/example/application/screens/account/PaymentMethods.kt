package com.example.application.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.example.application.network.*
import com.example.application.device.findActivity
import com.example.application.device.CardExpiry
import com.example.application.device.CardNumbers
import com.example.application.device.ScannedCard
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import com.example.application.R
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import android.view.WindowManager

@Composable
internal fun PaymentMethods(actions: AccountActions, gateway: AccountGateway) {
    var cards by remember { mutableStateOf<List<SavedCard>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var removing by remember { mutableStateOf<SavedCard?>(null) }
    var number by remember { mutableStateOf("") }
    var scanned by remember { mutableStateOf<ScannedCard?>(null) }
    var scanning by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf(false) }
    var scanSource by remember { mutableStateOf("NFC") }
    val brand = scanned?.brand ?: CardNumbers.brand(number)
    val context = LocalContext.current
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    DisposableEffect(lifecycle) {
        val activity = context.findActivity()
        val alreadySecure = ((activity?.window?.attributes?.flags ?: 0) and WindowManager.LayoutParams.FLAG_SECURE) != 0
        activity?.window?.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_STOP) { number = ""; scanned = null }
        }
        lifecycle.addObserver(observer)
        onDispose {
            lifecycle.removeObserver(observer)
            if (!alreadySecure) activity?.window?.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
    var holder by remember { mutableStateOf("") }
    var expiry by remember { mutableStateOf("") }
    fun load() = actions.run {
        when (val result = gateway.cards()) {
            is ApiResult.Success -> { cards = result.data.toList(); loadError = null }
            else -> loadError = result.accountError()
        }; null
    }
    LaunchedEffect(Unit) { load() }
    AccountPanel("Le tue carte", "Sono salvati solo circuito, ultime quattro cifre, intestatario e scadenza. Il progetto non esegue addebiti reali.") {
        when {
            cards == null -> {
                Text(loadError ?: "Caricamento carte…")
                if (loadError != null) OutlinedButton(enabled = !actions.busy, onClick = { load() }) { Text("Riprova") }
            }
            cards!!.isEmpty() -> Text("Non hai ancora salvato carte.")
            else -> cards!!.forEach { card -> WalletCard(card, !actions.busy) { removing = card } }
        }
    }
    AccountPanel("Aggiungi una carta", "Scansiona una carta contactless oppure inserisci i dati. Il circuito viene riconosciuto automaticamente.") {
        val enabled = !actions.busy && cards != null && cards!!.size < 10
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(enabled = enabled, onClick = { scanning = true }, modifier = Modifier.weight(1f)) { Icon(painterResource(R.drawable.ic_scan_nfc), null, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("NFC") }
            OutlinedButton(enabled = enabled, onClick = { camera = true }, modifier = Modifier.weight(1f)) { Icon(painterResource(R.drawable.ic_scan_camera), null, Modifier.size(22.dp)); Spacer(Modifier.width(8.dp)); Text("Camera") }
        }
        val found = scanned
        if (found != null) {
            Surface(shape = androidx.compose.foundation.shape.RoundedCornerShape(20.dp), color = com.example.application.ui.theme.GlassFillStrong) {
                Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("💳  ${found.brand}", style = MaterialTheme.typography.titleLarge)
                    Text("••••  ••••  ••••  ${found.last4}", style = MaterialTheme.typography.titleMedium)
                    Text("Carta acquisita con $scanSource", style = MaterialTheme.typography.bodySmall)
                }
            }
            OutlinedButton(enabled = enabled, onClick = { scanned = null; number = "" }) { Text("Inserisci a mano") }
        } else {
            AccountField("Numero carta", number, { input -> number = input.filter { it in '0'..'9' }.take(19) }, enabled,
                max = 23, keyboard = KeyboardType.NumberPassword, password = true)
            Text("Circuito: ${brand ?: "inserisci il numero della carta"}", style = MaterialTheme.typography.labelLarge)
        }
        AccountField("Intestatario", holder, { holder = it }, enabled)
        CardExpiryField(expiry, enabled) { expiry = it }
        Text("Il numero completo serve solo al riconoscimento sul telefono: non viene salvato né inviato. Non inserire PIN o CVV.", style = MaterialTheme.typography.bodySmall)
        Button(enabled = enabled, onClick = { actions.run {
            val date = CardExpiry.parse(expiry)
            val input = CardInput(brand ?: "Altro", scanned?.last4 ?: number.takeLast(4), holder.trim(), date?.monthValue ?: 0, date?.year ?: 0)
            (if (scanned == null && !CardNumbers.valid(number)) "Controlla il numero della carta." else CardExpiry.error(date) ?: input.error()) ?: when (val result = gateway.addCard(input)) {
                is ApiResult.Success -> { cards = listOf(result.data) + cards.orEmpty(); holder = ""; number = ""; scanned = null; expiry = ""; "Carta salvata." }
                else -> result.accountError()
            }
        } }) { Text("Salva carta") }
        if ((cards?.size ?: 0) >= 10) Text("Hai raggiunto il limite di 10 carte.")
    }
    if (scanning) NfcCardDialog(onDismiss = { scanning = false }, onCard = { card ->
        scanSource = "NFC"
        scanned = card
        number = ""
        card.holder?.let { holder = it }
        expiry = if (card.month != null && card.year != null) "%02d%02d".format(card.month, card.year % 100) else ""
        scanning = false
    })
    if (camera) CardCameraDialog(onDismiss = { camera = false }, onCard = { card ->
        camera = false
        scanSource = "fotocamera"
        scanned = card; number = ""; holder = card.holder.orEmpty()
        expiry = if (card.month != null && card.year != null) "%02d%02d".format(card.month, card.year % 100) else ""
        val input = CardInput(card.brand, card.last4, holder.trim(), card.month ?: 0, card.year ?: 0)
        if (input.error() == null) actions.run {
            when (val result = gateway.addCard(input)) {
                is ApiResult.Success -> {
                    cards = listOf(result.data) + cards.orEmpty(); scanned = null; holder = ""; expiry = ""
                    "Carta aggiunta dalla fotocamera."
                }
                else -> result.accountError()
            }
        } else actions.run { "Dati acquisiti. Completa o correggi i campi mancanti prima di salvare." }
    })
    removing?.let { card ->
        AlertDialog(onDismissRequest = { if (!actions.busy) removing = null }, title = { Text("Rimuovere la carta?") },
            text = { Text("${card.brand} •••• ${card.last4}") },
            confirmButton = { TextButton(enabled = !actions.busy, onClick = { actions.run {
                when (val result = gateway.removeCard(card.id)) {
                    is ApiResult.Success -> { cards = cards.orEmpty().filter { it.id != card.id }; removing = null; "Carta rimossa." }
                    else -> { removing = null; result.accountError() }
                }
            } }) { Text("Rimuovi") } },
            dismissButton = { TextButton(enabled = !actions.busy, onClick = { removing = null }) { Text("Annulla") } })
    }
}
