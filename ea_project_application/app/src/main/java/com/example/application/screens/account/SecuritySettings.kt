package com.example.application.screens.account

import androidx.compose.material3.*
import androidx.compose.runtime.*
import com.example.application.dto.UserDTO
import com.example.application.network.*
import com.example.application.device.NotificationChannels
import com.example.application.device.rememberNotificationAccess
import androidx.compose.ui.platform.LocalContext

@Composable
internal fun SecuritySettings(user: UserDTO, actions: AccountActions, onLogout: () -> Unit, gateway: AccountGateway) {
    val context = LocalContext.current
    val notifications = rememberNotificationAccess()
    // Read Compose state so returning from system settings refreshes channel availability.
    val bookingAllowed = remember(notifications.revision) { NotificationChannels.allowed(context, NotificationChannels.BOOKINGS) }
    val messagesAllowed = remember(notifications.revision) { NotificationChannels.allowed(context, NotificationChannels.MESSAGES) }
    var preferences by remember { mutableStateOf<AccountPreferences?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var current by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var revokeDialog by remember { mutableStateOf(false) }
    var passwordChanged by remember { mutableStateOf(false) }
    fun load() = actions.run {
        when (val result = gateway.preferences()) {
            is ApiResult.Success -> { preferences = result.data; loadError = null }
            else -> loadError = result.accountError()
        }; null
    }
    LaunchedEffect(Unit) { load() }
    AccountPanel("Scegli cosa ricevere", "Le preferenze sono condivise con il tuo account desktop.") {
        val p = preferences
        if (p == null) {
            Text(loadError ?: "Caricamento preferenze…")
            if (loadError != null) OutlinedButton(enabled = !actions.busy, onClick = { load() }) { Text("Riprova") }
        } else {
            AccountToggle("Prenotazioni", if (bookingAllowed) "Autorizzate dal telefono" else "Da autorizzare sul telefono", p.bookingAlerts, !actions.busy) { enabled ->
                if (!enabled) preferences = p.copy(bookingAlerts = false)
                else notifications.request(NotificationChannels.BOOKINGS) { preferences = preferences?.copy(bookingAlerts = true) }
            }
            AccountToggle("Messaggi", if (messagesAllowed) "Autorizzati dal telefono" else "Da autorizzare sul telefono", p.chatAlerts, !actions.busy) { enabled ->
                if (!enabled) preferences = p.copy(chatAlerts = false)
                else notifications.request(NotificationChannels.MESSAGES) { preferences = preferences?.copy(chatAlerts = true) }
            }
            if (p.bookingAlerts && !bookingAllowed) OutlinedButton(enabled = !actions.busy,
                onClick = { notifications.request(NotificationChannels.BOOKINGS) {} }) { Text("Autorizza avvisi prenotazioni") }
            if (p.chatAlerts && !messagesAllowed) OutlinedButton(enabled = !actions.busy,
                onClick = { notifications.request(NotificationChannels.MESSAGES) {} }) { Text("Autorizza avvisi messaggi") }
            if (!bookingAllowed || !messagesAllowed) Text("La preferenza dell’account non sostituisce il permesso del telefono: autorizza i canali per ricevere gli avvisi qui.", style = MaterialTheme.typography.bodySmall)
            notifications.message?.let { Text(it, color = MaterialTheme.colorScheme.error) }
            OutlinedButton(onClick = { NotificationChannels.settings(context) }) { Text("Permessi e notifiche del telefono") }
            AccountToggle("Consigli personalizzati", "Usa preferiti, prenotazioni e recensioni per riconoscere città e categorie che mi interessano", p.personalized, !actions.busy) { preferences = p.copy(personalized = it) }
            Button(enabled = !actions.busy, onClick = { actions.run {
                when (val result = gateway.savePreferences(p)) {
                    is ApiResult.Success -> { preferences = result.data; "Preferenze salvate." }
                    else -> result.accountError()
                }
            } }) { Text("Salva preferenze") }
        }
    }
    AccountPanel("Password e accesso", "Almeno 8 caratteri, una lettera, un numero e un simbolo. Massimo 128 caratteri.") {
        if (user.oauthProvider) Text("La password si gestisce dal provider con cui hai effettuato l’accesso.")
        else {
            AccountField("Password attuale", current, { current = it }, !actions.busy, 128, password = true)
            AccountField("Nuova password", password, { password = it }, !actions.busy, 128, password = true)
            PasswordStrengthMeter(password)
            AccountField("Ripeti la nuova password", confirm, { confirm = it }, !actions.busy, 128, password = true)
            Text("Dopo la modifica dovrai accedere di nuovo su tutti i dispositivi.", style = MaterialTheme.typography.bodySmall)
            Button(enabled = !actions.busy, onClick = { actions.run {
                when {
                    current.isBlank() -> "Inserisci la password attuale."
                    !PasswordPolicy.isValid(password) -> "La nuova password non rispetta i requisiti."
                    password != confirm -> "Le nuove password non coincidono."
                    password == current -> "Scegli una password diversa da quella attuale."
                    else -> when (val result = gateway.changePassword(current, password)) {
                        is ApiResult.Success -> { current = ""; password = ""; confirm = ""; passwordChanged = true; null }
                        else -> result.accountError()
                    }
                }
            } }) { Text("Aggiorna password") }
        }
    }
    AccountPanel("Sessioni attive", "Chiudi tutti gli accessi, incluso questo dispositivo.") {
        OutlinedButton(enabled = !actions.busy, onClick = { revokeDialog = true }) { Text("Esci da tutti i dispositivi") }
    }
    if (revokeDialog) AlertDialog(onDismissRequest = { if (!actions.busy) revokeDialog = false },
        title = { Text("Chiudere tutti gli accessi?") }, text = { Text("Per continuare dovrai accedere nuovamente.") },
        confirmButton = { TextButton(enabled = !actions.busy, onClick = { actions.run {
            when (val result = gateway.revokeSessions()) {
                is ApiResult.Success -> { revokeDialog = false; onLogout(); null }
                else -> { revokeDialog = false; result.accountError() }
            }
        } }) { Text("Conferma") } },
        dismissButton = { TextButton(enabled = !actions.busy, onClick = { revokeDialog = false }) { Text("Annulla") } })
    if (passwordChanged) AlertDialog(onDismissRequest = onLogout, title = { Text("Password aggiornata") },
        text = { Text("Tutte le sessioni sono state chiuse. Accedi con la nuova password.") },
        confirmButton = { TextButton(onClick = onLogout) { Text("Torna all’accesso") } })
}
