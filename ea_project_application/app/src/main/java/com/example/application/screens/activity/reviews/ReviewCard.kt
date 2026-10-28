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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.ResponseDTO
import com.example.application.dto.ReviewDTO
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.RatingAmber
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400
import java.time.LocalDate
import java.time.format.DateTimeFormatter

private val REVIEW_DATE_FORMAT = DateTimeFormatter.ofPattern("dd/MM/yyyy")

@Composable
internal fun ReviewCard(review: ReviewDTO) {
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
                color = Color.White,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f)
            )
            Stars(rating = review.rating ?: 0)
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
            ResponseBlock(response)
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
private fun ResponseBlock(response: ResponseDTO) {
    Row(modifier = Modifier.height(IntrinsicSize.Min)) {
        Box(
            modifier = Modifier
                .width(2.dp)
                .fillMaxHeight()
                .background(Stone400)
        )
        Spacer(Modifier.width(10.dp))
        Column {
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
    }
}

private fun formatReviewDate(value: String?): String? =
    value?.let { runCatching { LocalDate.parse(it.take(10)).format(REVIEW_DATE_FORMAT) }.getOrNull() }