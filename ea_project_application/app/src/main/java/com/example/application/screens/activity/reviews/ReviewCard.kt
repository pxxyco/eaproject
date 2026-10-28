package com.example.application.screens.activity.reviews

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.ResponseDTO
import com.example.application.dto.ReviewDTO
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.PopupSurface
import com.example.application.ui.theme.RatingAmber
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
internal fun ReviewCard(
    review: ReviewDTO,
    onEdit: (() -> Unit)? = null,
    onDelete: (() -> Unit)? = null,
    onRespond: (() -> Unit)? = null,
    onEditResponse: (() -> Unit)? = null,
    onDeleteResponse: (() -> Unit)? = null
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(GlassFill)
            .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = review.username?.takeIf { it.isNotBlank() } ?: "Utente",
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurface,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Stars(rating = review.rating ?: 0)
            if (onEdit != null || onDelete != null) {
                ReviewMenu(onEdit = onEdit, onDelete = onDelete)
            }
        }

        formatReviewDate(review.createdAt)?.let { date ->
            Text(text = date, style = MaterialTheme.typography.labelSmall, color = Stone400)
        }

        if (!review.comment.isNullOrBlank()) {
            Spacer(Modifier.height(8.dp))
            Text(text = review.comment, style = MaterialTheme.typography.bodyMedium, color = Stone200)
        }

        review.response?.let { response ->
            Spacer(Modifier.height(12.dp))
            ResponseBlock(
                response = response,
                onEdit = onEditResponse,
                onDelete = onDeleteResponse
            )
        }

        if (onRespond != null) {
            Spacer(Modifier.height(4.dp))
            TextButton(onClick = onRespond) {
                Text("Rispondi", color = Stone200)
            }
        }
    }
}

@Composable
private fun Stars(rating: Int) {
    val filled = rating.coerceIn(0, 5)
    Text(
        text = "★".repeat(filled) + "☆".repeat(5 - filled),
        color = RatingAmber,
        style = MaterialTheme.typography.titleSmall
    )
}

@Composable
private fun ResponseBlock(
    response: ResponseDTO,
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?
) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Stone400)
        )
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = "Risposta dell'organizzatore",
                style = MaterialTheme.typography.labelSmall,
                color = Stone400,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = response.comment.orEmpty(),
                style = MaterialTheme.typography.bodyMedium,
                color = Stone200
            )
        }
        if (onEdit != null || onDelete != null) {
            ReviewMenu(onEdit = onEdit, onDelete = onDelete, contentDescription = "Azioni risposta")
        }
    }
}

private fun formatReviewDate(value: String?): String? =
    value?.let { runCatching { LocalDate.parse(it.take(10)).format(REVIEW_DATE_FORMAT) }.getOrNull() }


@Composable
private fun ReviewMenu(
    onEdit: (() -> Unit)?,
    onDelete: (() -> Unit)?,
    contentDescription: String = "Azioni recensione"
) {
    var expanded by remember { mutableStateOf(false) }

    Box {
        IconButton(
            onClick = { expanded = true },
            modifier = Modifier.size(32.dp)
        ) {
            Icon(Icons.Default.MoreVert, contentDescription = contentDescription, tint = Stone400)
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            containerColor = PopupSurface
        ) {
            if (onEdit != null) {
                DropdownMenuItem(
                    text = { Text("Modifica", color = Color.White) },
                    onClick = {
                        expanded = false
                        onEdit()
                    }
                )
            }
            if (onDelete != null) {
                DropdownMenuItem(
                    text = { Text("Elimina", color = MaterialTheme.colorScheme.error) },
                    onClick = {
                        expanded = false
                        onDelete()
                    }
                )
            }
        }
    }
}