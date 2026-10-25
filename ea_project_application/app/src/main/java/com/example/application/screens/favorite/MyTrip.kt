package com.example.application.screens.favorite

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.example.application.screens.home.RemotePhoto
import com.example.application.screens.home.SectionTitle
import com.example.application.ui.theme.*
import java.time.format.DateTimeFormatter
import java.util.Locale

private val dateFormatter = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyTrip(modifier: Modifier = Modifier) {
    val bookings = MyTripRepository.bookings
    var filter by remember { mutableStateOf<BookingStatus?>(null) }
    var toCancel by remember { mutableStateOf<BookedActivity?>(null) }

    val visible = bookings
        .filter { filter == null || it.status == filter }
        .sortedWith(compareBy({ it.status.ordinal }, { it.date }))

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(ScreenBackground)
            .padding(horizontal = 16.dp)
    ) {
        Spacer(Modifier.height(16.dp))
        SectionTitle("Le mie prenotazioni")
        Spacer(Modifier.height(12.dp))

        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            item {
                FilterChip(
                    selected = filter == null,
                    onClick = { filter = null },
                    label = { Text("Tutte") },
                    colors = chipColors()
                )
            }
            items(BookingStatus.entries.toList()) { status ->
                FilterChip(
                    selected = filter == status,
                    onClick = { filter = status },
                    label = { Text(status.label) },
                    colors = chipColors()
                )
            }
        }

        Spacer(Modifier.height(16.dp))

        if (visible.isEmpty()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = "Nessuna prenotazione in questa categoria.",
                    color = Stone400,
                    style = MaterialTheme.typography.bodyLarge
                )
            }
        } else {
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(bottom = 24.dp)
            ) {
                items(visible, key = { it.bookingId }) { booking ->
                    BookingCard(booking, onCancelClick = { toCancel = booking })
                }
            }
        }
    }

    toCancel?.let { booking ->
        AlertDialog(
            onDismissRequest = { toCancel = null },
            shape = RoundedCornerShape(28.dp),
            title = { Text("Annullare la prenotazione?") },
            text = { Text("\"${booking.title}\" del ${booking.date.format(dateFormatter)} verrà annullata.") },
            confirmButton = {
                TextButton(onClick = {
                    MyTripRepository.cancel(booking.bookingId)
                    toCancel = null
                }) { Text("Annulla prenotazione") }
            },
            dismissButton = {
                TextButton(onClick = { toCancel = null }) { Text("Indietro") }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun chipColors() = FilterChipDefaults.filterChipColors(
    labelColor = Color.White,
    selectedContainerColor = Stone200,
    selectedLabelColor = Stone900
)

@Composable
private fun BookingCard(booking: BookedActivity, onCancelClick: () -> Unit) {
    val status = booking.status
    val dimmed = status == BookingStatus.CANCELLED

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = PopupSurface)
    ) {
        Row(modifier = Modifier.padding(12.dp)) {
            RemotePhoto(
                imageUrl = booking.imageUrl,
                contentDescription = booking.title,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(12.dp))
            )

            Spacer(Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = booking.category,
                        style = MaterialTheme.typography.labelSmall,
                        color = Stone400,
                        modifier = Modifier.weight(1f)
                    )
                    Surface(shape = RoundedCornerShape(8.dp), color = OverlayDark) {
                        Text(
                            text = when (status) {
                                BookingStatus.UPCOMING -> "In arrivo"
                                BookingStatus.COMPLETED -> "Conclusa"
                                BookingStatus.CANCELLED -> "Annullata"
                            },
                            style = MaterialTheme.typography.labelSmall,
                            color = if (dimmed) MaterialTheme.colorScheme.error else Color.White,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                        )
                    }
                }

                Spacer(Modifier.height(4.dp))

                Text(
                    text = booking.title,
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dimmed) Stone400 else Color.White,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Text(
                    text = "${booking.city} · ${booking.date.format(dateFormatter)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Stone300,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(6.dp))

                Text(
                    text = "${booking.seats} ${if (booking.seats == 1) "posto" else "posti"} · " +
                            "€%.2f".format(Locale.ITALY, booking.totalPrice),
                    style = MaterialTheme.typography.titleSmall,
                    color = Stone200,
                    fontWeight = FontWeight.Bold
                )

                if (status == BookingStatus.UPCOMING) {
                    TextButton(
                        onClick = onCancelClick,
                        contentPadding = PaddingValues(0.dp)
                    ) {
                        Text("Annulla prenotazione", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
    }
}