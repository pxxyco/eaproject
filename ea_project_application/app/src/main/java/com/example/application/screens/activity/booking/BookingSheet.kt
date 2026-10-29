package com.example.application.screens.activity.booking

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedIconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.BookingItemDTO
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.launch
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun BookingSheet(
    title: String,
    price: Double?,
    availableSeats: Int,
    onDismiss: () -> Unit,
    onConfirm: suspend (seats: Int) -> BookingItemDTO
) {
    var seats by rememberSaveable { mutableIntStateOf(1) }
    var saving by remember { mutableStateOf(false) }
    var done by remember { mutableStateOf(false) }
    var awaitingPayment by remember { mutableStateOf(false) }
    var error by remember { mutableStateOf<String?>(null) }
    val scope = rememberCoroutineScope()

    ModalBottomSheet(
        onDismissRequest = { if (!saving) onDismiss() },
        containerColor = PopupSurface
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = 20.dp, end = 20.dp, bottom = 24.dp)
        ) {
            Text(
                text = if (done) { if (awaitingPayment) "In attesa di pagamento" else "Prenotazione confermata" } else "Prenota",
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.Bold
            )
            Text(text = title, style = MaterialTheme.typography.bodyMedium, color = Stone400)

            if (done) {
                Spacer(Modifier.height(12.dp))
                Text(
                    text = if (seats == 1) "Hai prenotato 1 posto." else "Hai prenotato $seats posti.",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone200
                )
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Stone200,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    Text("Chiudi")
                }
                return@Column
            }

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Posti",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                OutlinedIconButton(
                    onClick = { seats-- },
                    enabled = seats > 1 && !saving
                ) {
                    Text("−", style = MaterialTheme.typography.titleLarge, color = Stone200)
                }
                Text(
                    text = seats.toString(),
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                OutlinedIconButton(
                    onClick = { seats++ },
                    enabled = seats < availableSeats && !saving
                ) {
                    Text("+", style = MaterialTheme.typography.titleLarge, color = Stone200)
                }
            }
            Text(
                text = "Massimo $availableSeats",
                style = MaterialTheme.typography.labelSmall,
                color = Stone400
            )

            Spacer(Modifier.height(16.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = "Totale",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f)
                )
                Text(
                    text = price?.let { "€ %.2f".format(Locale.ITALY, it * seats) } ?: "N/D",
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface,
                    fontWeight = FontWeight.Bold
                )
            }

            error?.let {
                Spacer(Modifier.height(8.dp))
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.error
                )
            }
            Spacer(Modifier.height(16.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onDismiss, enabled = !saving) {
                    Text("Annulla", color = Stone200)
                }
                Spacer(Modifier.width(8.dp))
                Button(
                    onClick = {
                        saving = true
                        error = null
                        scope.launch {
                            try {
                                awaitingPayment = onConfirm(seats).bookingStatus == "PENDING"
                                done = true
                            } catch (e: CancellationException) {
                                throw e
                            } catch (e: Exception) {
                                error = e.message ?: "Impossibile completare la prenotazione"
                            } finally {
                                saving = false
                            }
                        }
                    },
                    enabled = !saving,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Stone200,
                        contentColor = MaterialTheme.colorScheme.onPrimary
                    )
                ) {
                    if (saving) {
                        CircularProgressIndicator(
                            color = MaterialTheme.colorScheme.onPrimary,
                            strokeWidth = 2.dp,
                            modifier = Modifier.size(18.dp)
                        )
                    } else {
                        Text("Conferma")
                    }
                }
            }
        }
    }
}
