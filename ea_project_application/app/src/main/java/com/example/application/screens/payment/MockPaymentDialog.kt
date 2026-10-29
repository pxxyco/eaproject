package com.example.application.screens.payment

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.BookingItemDTO
import com.example.application.network.PaymentReceipt
import com.example.application.network.PaymentRepository
import com.example.application.ui.theme.PopupSurface
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Locale

@Composable
internal fun MockPaymentDialog(booking: BookingItemDTO, onDismiss: () -> Unit, onPaid: suspend () -> Unit) {
    var busy by remember { mutableStateOf(false) }
    var receipt by remember { mutableStateOf<PaymentReceipt?>(null) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()
    val money = remember { NumberFormat.getCurrencyInstance(Locale.ITALY) }
    AlertDialog(onDismissRequest = { if (!busy) onDismiss() }, containerColor = PopupSurface,
        shape = RoundedCornerShape(28.dp),
        icon = { Text(if (receipt == null) "💳" else "✓", style = MaterialTheme.typography.headlineLarge) },
        title = { Text(if (receipt == null) "Completa la prenotazione" else "Pagamento riuscito") },
        text = {
            Column(Modifier.verticalScroll(rememberScrollState()), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                Text(receipt?.title ?: booking.itemTitle.orEmpty(), style = MaterialTheme.typography.titleMedium)
                booking.itemLocation?.let { Text(it) }
                booking.itemDate?.let { Text(it.replace('T', ' ')) }
                Surface(color = MaterialTheme.colorScheme.primaryContainer, shape = RoundedCornerShape(20.dp)) {
                    Column(Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("Totale prenotazione", style = MaterialTheme.typography.labelLarge)
                        Text(money.format(receipt?.amount ?: booking.price ?: 0), style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    }
                }
                Text("Pagamento simulato: nessun addebito reale e nessun dato della carta richiesto.", style = MaterialTheme.typography.bodySmall)
                receipt?.let { Text("La tua prenotazione è confermata. Ora puoi anche lasciare una recensione."); Text("Ricevuta: ${it.reference}", style = MaterialTheme.typography.labelSmall) }
                error?.let { Text(it, color = MaterialTheme.colorScheme.error) }
                if (busy) LinearProgressIndicator(Modifier.fillMaxWidth())
            }
        },
        confirmButton = {
            Button(enabled = !busy && booking.bookingId != null, onClick = {
                if (receipt != null) onDismiss() else {
                    busy = true; error = null
                    scope.launch {
                        try {
                            receipt = PaymentRepository.pay(requireNotNull(booking.bookingId))
                            try { onPaid() } catch (cancelled: CancellationException) { throw cancelled }
                            catch (_: Exception) { error = "Pagamento registrato. Riapri la schermata per aggiornare i dati." }
                        } catch (cancelled: CancellationException) { throw cancelled }
                        catch (failure: Exception) { error = failure.message ?: "Pagamento non riuscito. Riprova." }
                        finally { busy = false }
                    }
                }
            }) { Text(if (receipt != null) "Fatto" else "Conferma pagamento mock") }
        },
        dismissButton = { if (receipt == null) TextButton(enabled = !busy, onClick = onDismiss) { Text("Più tardi") } })
}
