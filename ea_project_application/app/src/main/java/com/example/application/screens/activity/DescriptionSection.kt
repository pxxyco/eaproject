package com.example.application.screens.activity

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail
import com.example.application.ui.theme.GlassBorder
import com.example.application.ui.theme.GlassFill
import com.example.application.ui.theme.ScreenBackground
import com.example.application.ui.theme.Stone200
import com.example.application.ui.theme.Stone400

@Composable
internal fun DescriptionSection(
    detail: ExperienceDetail
){
    val notes = when(detail){
        is ExperienceDetail.Activity -> detail.dto.notes
        is ExperienceDetail.Trip -> null
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .background(ScreenBackground)
            .padding(20.dp)
    ) {
        Text(
            text = detail.description?.takeIf { it.isNotBlank() } ?: "Nessuna descrizione disponibile",
            style = MaterialTheme.typography.bodyLarge,
            color = Stone200
        )

        if(!notes.isNullOrBlank()) {
            Spacer(Modifier.height(20.dp))

            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(GlassFill)
                    .border(1.dp, GlassBorder, RoundedCornerShape(16.dp))
                    .padding(16.dp)
            ) {
                Text(
                    text = "Note aggiuntive",
                    style = MaterialTheme.typography.titleSmall,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = notes,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Stone400
                )
            }
        }
    }
}