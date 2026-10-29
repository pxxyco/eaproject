package com.example.application.network

import com.example.application.dto.ActivityDetailDTO
import com.example.application.dto.ExperienceDetail
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType
import com.example.application.dto.TripDetailDTO
import com.example.application.dto.TripImageDTO

object ExperienceRepository {

    suspend fun getDetail(ref: ExperienceRef): ExperienceDetail = when (ref.type) {
        ExperienceType.ACTIVITY ->
            ExperienceDetail.Activity(fetch("/api/activities/${ref.id}", ActivityDetailDTO::class.java))

        ExperienceType.TRIP ->
            ExperienceDetail.Trip(fetch("/api/trips/${ref.id}", TripDetailDTO::class.java))
    }

    private suspend fun <T> fetch(path: String, type: Class<T>): T =
        when (val result = ApiService.get(path, type)) {
            is ApiResult.Success -> result.data
            is ApiResult.HttpError -> throw IllegalStateException("Errore HTTP ${result.code}: ${result.message}")
            is ApiResult.RateLimited -> throw IllegalStateException("Troppe richieste. Riprova tra ${result.retryAfterSeconds} secondi.")
            is ApiResult.NetworkError -> throw result.cause
        }


    suspend fun getTripImages(tripId: Long): List<String> =
        fetch("/api/trip-images/trip/$tripId", Array<TripImageDTO>::class.java)
            .sortedBy { it.orderIndex ?: Int.MAX_VALUE }
            .mapNotNull { it.imageUrl?.takeIf { url -> url.isNotBlank() } }
}