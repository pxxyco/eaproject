package com.example.application.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import com.example.application.dto.ActivityHomeDTO
import com.example.application.dto.TripHomeDTO

internal data class RecommendedTrip(val item: TripHomeDTO, val reason: String)
internal data class RecommendedActivity(val item: ActivityHomeDTO, val reason: String)
internal data class Recommendations(val personalized: Boolean, val trips: List<RecommendedTrip>, val activities: List<RecommendedActivity>)

internal object RecommendationRepository {
    var revision by mutableIntStateOf(0)
        private set
    fun invalidate() { revision++ }
    suspend fun load() = ApiService.get("/api/recommendations/mine?limit=4", Recommendations::class.java)
}
