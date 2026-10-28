package com.example.application.screens.account

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.network.*
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Composable
internal fun SupportCenter(actions: AccountActions, gateway: AccountGateway) {
    var tickets by remember { mutableStateOf<List<SupportTicket>?>(null) }
    var loadError by remember { mutableStateOf<String?>(null) }
    var page by remember { mutableIntStateOf(0) }
    var category by remember { mutableStateOf("Account") }
    var subject by remember { mutableStateOf("") }
    var message by remember { mutableStateOf("") }
    var opened by remember { mutableStateOf<SupportTicket?>(null) }
    fun load(target: Int) = actions.run {
        when (val result = gateway.tickets(target)) {
            is ApiResult.Success -> { tickets = result.data.toList(); page = target; loadError = null }
            else -> loadError = result.accountError()
        }; null
    }
    LaunchedEffect(Unit) { load(0) }
    AccountPanel("Trova una risposta") {
        Faq("Come gestisco i preferiti?", "Apri Preferiti dalla barra di navigazione. Puoi raccogliere viaggi e attività in liste personali o condivise.")
        Faq("I pagamenti sono reali?", "No. EA Project utilizza un sistema dimostrativo. Le carte salvate contengono solo circuito, intestatario, scadenza e ultime quattro cifre.")
        Faq("Come proteggo il mio account?", "In Impostazioni e sicurezza puoi cambiare password e chiudere gli accessi su tutti i dispositivi.")
    }
    AccountPanel("Descrivi il problema", "Registra la tua richiesta e ritrovala anche sul desktop.") {
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("Account", "Prenotazioni", "Pagamenti", "Altro").forEach { value ->
                FilterChip(selected = category == value, onClick = { category = value }, enabled = !actions.busy, label = { Text(value) })
            }
        }
        AccountField("Oggetto", subject, { subject = it }, !actions.busy, 180)
        AccountField("Messaggio", message, { message = it }, !actions.busy, 4000, multiline = true)
        Button(enabled = !actions.busy, onClick = { actions.run {
            val input = TicketInput(category, subject.trim(), message.trim())
            input.error() ?: when (val result = gateway.sendTicket(input)) {
                is ApiResult.Success -> {
                    subject = ""; message = ""
                    when (val refreshed = gateway.tickets(0)) {
                        is ApiResult.Success -> { tickets = refreshed.data.toList(); page = 0; loadError = null }
                        else -> { tickets = null; loadError = "Richiesta registrata. Ricarica l’elenco per visualizzarla." }
                    }
                    "Richiesta registrata."
                }
                else -> result.accountError()
            }
        } }) { Text("Registra richiesta") }
    }
    AccountPanel("Le tue richieste") {
        loadError?.let { Text(it, color = MaterialTheme.colorScheme.error) }
        if (tickets == null) {
            if (loadError == null) Text("Caricamento richieste…")
            else OutlinedButton(enabled = !actions.busy, onClick = { load(page) }) { Text("Riprova") }
        } else {
            if (tickets!!.isEmpty()) Text("Non ci sono richieste in questa pagina.")
            tickets!!.forEach { ticket ->
                OutlinedCard(onClick = { opened = ticket }, modifier = Modifier.fillMaxWidth()) {
                    Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(ticket.subject, style = MaterialTheme.typography.titleMedium)
                        Text("${ticket.category} · ${ticket.status}", style = MaterialTheme.typography.labelMedium)
                        ticket.createdAt?.let { date -> Text(formatTicketDate(date), style = MaterialTheme.typography.bodySmall) }
                        Text("Apri il dettaglio  ›", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                TextButton(enabled = page > 0 && !actions.busy, onClick = { load(page - 1) }) { Text("Precedente") }
                Text("Pagina ${page + 1}", modifier = Modifier.padding(top = 12.dp))
                TextButton(enabled = tickets!!.size == 20 && !actions.busy, onClick = { load(page + 1) }) { Text("Successiva") }
            }
        }
    }
    opened?.let { ticket ->
        AlertDialog(onDismissRequest = { opened = null },
            title = { Text(ticket.subject) },
            text = {
                Column(Modifier.heightIn(max = 440.dp).verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("${ticket.category} · ${ticket.status}", style = MaterialTheme.typography.labelLarge)
                    ticket.createdAt?.let { Text(formatTicketDate(it), style = MaterialTheme.typography.bodySmall) }
                    HorizontalDivider()
                    androidx.compose.foundation.text.selection.SelectionContainer { Text(ticket.message) }
                }
            },
            confirmButton = { OutlinedButton(onClick = { opened = null }) { Text("Chiudi dettaglio") } })
    }
}

private fun formatTicketDate(value: String): String = runCatching {
    DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm").withZone(ZoneId.systemDefault()).format(Instant.parse(value))
}.getOrDefault("Data non disponibile")

@Composable
private fun Faq(question: String, answer: String) {
    var expanded by remember { mutableStateOf(false) }
    OutlinedCard(onClick = { expanded = !expanded }, modifier = Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text((if (expanded) "− " else "+ ") + question, style = MaterialTheme.typography.titleMedium)
            if (expanded) Text(answer, style = MaterialTheme.typography.bodyMedium)
        }
    }
}
