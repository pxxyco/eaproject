package com.example.application.screens.activity

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail
import com.example.application.ui.theme.NavigationSelection
import com.example.application.ui.theme.NavigationAccent
import com.example.application.ui.theme.RatingAmber
import com.example.application.ui.theme.Stone400
import java.util.Locale

@Composable
internal fun ExperienceHeaderSheet(
    detail: ExperienceDetail,
    rating: Double? = detail.averageRating
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp))
            .padding(20.dp)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = NavigationSelection
        ) {
            Text(
                text = detail.categoryLabel,
                style = MaterialTheme.typography.labelMedium,
                color = NavigationAccent,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
            )
        }
        Spacer(Modifier.height(10.dp))

        Text(
            text = detail.title,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.onSurface,
            fontWeight = FontWeight.Bold
        )

        if (detail.location.isNotBlank()) {
            Spacer(Modifier.height(6.dp))
            Text(
                text = detail.location,
                style = MaterialTheme.typography.bodyMedium,
                color = Stone400
            )
        }

        Text(
            text = "⭐ " + (rating?.let { "%.1f".format(Locale.ITALY, it) } ?: "-"),
            style = MaterialTheme.typography.bodyMedium,
            color = RatingAmber,
            fontWeight = FontWeight.Bold
        )
    }
}