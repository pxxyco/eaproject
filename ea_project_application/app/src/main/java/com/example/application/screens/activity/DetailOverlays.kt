package com.example.application.screens.activity

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.dropShadow
import androidx.compose.ui.graphics.shadow.Shadow
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.GlassBorder
import java.util.Locale

@Composable
internal fun BackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    OutlinedButton(
        onClick = onBack,
        modifier = modifier.heightIn(min = 48.dp),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, GlassBorder),
        colors = ButtonDefaults.outlinedButtonColors(containerColor = GlassFill, contentColor = MaterialTheme.colorScheme.onSurface),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 10.dp)
    ) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, null, Modifier.size(20.dp))
        Spacer(Modifier.width(8.dp))
        Text("Indietro", style = MaterialTheme.typography.labelLarge)
    }
}

@Composable
internal fun BookingBar(
    price: Double?,
    availableSeats: Int?,
    onBookClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val soldOut = availableSeats != null && availableSeats <= 0
    val shape = RoundedCornerShape(28.dp)
    val colors = MaterialTheme.colorScheme

    Surface(
        modifier = modifier.widthIn(max = 560.dp).fillMaxWidth()
            .testTag("experience_booking_bar")
            .dropShadow(shape, Shadow(radius = 12.dp, color = Color.Black.copy(alpha = .16f), offset = DpOffset(0.dp, 4.dp))),
        color = GlassFill,
        contentColor = colors.onSurface,
        shape = shape,
        border = BorderStroke(1.dp, GlassBorder)
    ) {
        BoxWithConstraints(Modifier.padding(16.dp)) {
            val stacked = maxWidth < 300.dp || LocalDensity.current.fontScale >= 1.4f
            val priceContent: @Composable () -> Unit = {
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    Text("Prezzo", style = MaterialTheme.typography.labelSmall, color = colors.onSurfaceVariant)
                    Text(
                        text = price?.let { "€ %.2f".format(Locale.ITALY, it) } ?: "Prezzo N/D",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    availableSeats?.let {
                        Text(
                            text = when {
                                soldOut -> "Posti esauriti"
                                it == 1 -> "Ultimo posto disponibile"
                                else -> "$it posti disponibili"
                            },
                            style = MaterialTheme.typography.bodySmall,
                            color = if (soldOut) colors.error else colors.onSurfaceVariant
                        )
                    }
                }
            }
            val bookingButton: @Composable (Modifier) -> Unit = { buttonModifier ->
                Button(
                    onClick = onBookClick,
                    enabled = !soldOut,
                    modifier = buttonModifier.heightIn(min = 52.dp),
                    shape = RoundedCornerShape(18.dp),
                    contentPadding = PaddingValues(horizontal = 24.dp, vertical = 12.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = colors.primary, contentColor = colors.onPrimary)
                ) {
                    Text(if (soldOut) "Esaurito" else "Prenota", style = MaterialTheme.typography.labelLarge)
                }
            }
            if (stacked) {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    priceContent()
                    bookingButton(Modifier.fillMaxWidth())
                }
            } else {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    Box(Modifier.weight(1f)) { priceContent() }
                    bookingButton(Modifier.widthIn(min = 128.dp))
                }
            }
        }
    }
}
