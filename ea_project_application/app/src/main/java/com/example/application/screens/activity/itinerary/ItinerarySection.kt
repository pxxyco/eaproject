package com.example.application.screens.activity.itinerary

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.application.dto.ExperienceDetail

@Composable
internal fun ItinerarySection(detail: ExperienceDetail) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(20.dp)
    ) {
        when (detail) {
            is ExperienceDetail.Activity -> PlaceInfoCard(detail.dto)

            is ExperienceDetail.Trip -> StageTimeline(detail.dto.stages.orEmpty())
        }
    }
}