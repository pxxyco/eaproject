package com.example.application.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.List
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.ui.theme.FavoriteRed
import com.example.application.ui.theme.OverlayDark
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import com.example.application.ui.theme.Stone50
import java.util.Locale

@Composable
internal fun BackButton(
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onBack,
        modifier = modifier
            .clip(CircleShape)
            .background(OverlayDark)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Indietro",
            tint = Color.White
        )
    }
}

@Composable
internal fun FavoriteButton(
    favorite: Boolean,
    enabled: Boolean,
    onToggle: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onToggle,
        enabled = enabled,
        modifier = modifier
            .clip(CircleShape)
            .background(OverlayDark)
    ) {
        Icon(
            imageVector = if (favorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
            contentDescription = if (favorite) "Rimuovi dai preferiti" else "Aggiungi ai preferiti",
            tint = if (favorite) FavoriteRed else Color.White
        )
    }
}

@Composable
internal fun AddToListButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .clip(CircleShape)
            .background(OverlayDark)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.List,
            contentDescription = "Aggiungi a una lista",
            tint = Color.White
        )
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

    Surface(
        modifier = modifier.fillMaxWidth(),
        color = PopupSurface,
        shape = RoundedCornerShape(topStart = 20.dp, topEnd = 20.dp)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = price?.let { "€ %.2f".format(Locale.ITALY, it) } ?: "Prezzo N/D",
                    style = MaterialTheme.typography.titleLarge,
                    color = Stone50,
                    fontWeight = FontWeight.Bold
                )
                if (availableSeats != null) {
                    Text(
                        text = if (soldOut) "Esaurito" else "$availableSeats posti disponibili",
                        style = MaterialTheme.typography.bodySmall,
                        color = Stone400
                    )
                }
            }

            Button(
                onClick = onBookClick,
                enabled = !soldOut,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Stone200,
                    contentColor = Color.Black
                )
            ) {
                Text("Prenota")
            }
        }
    }
}