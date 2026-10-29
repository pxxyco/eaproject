package com.example.application.network

import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType

object BookingRepository {

    suspend fun book(ref: ExperienceRef, seats: Int) {
        val path = when (ref.type) {
            ExperienceType.ACTIVITY -> "/api/bookings/activities/${ref.id}?seats=$seats"
            ExperienceType.TRIP -> "/api/bookings/trips/${ref.id}?seats=$seats"
        }
        ApiService.post(path, null, Unit::class.java).orThrow()
    }

    private data class ApiErrorBody(val message: String? = null)

    private fun <T> ApiResult<T>.orThrow(): T = when (this) {
        is ApiResult.Success -> data
        is ApiResult.HttpError -> throw IllegalStateException(backendMessage(message) ?: "Errore HTTP $code")
        is ApiResult.RateLimited -> throw IllegalStateException("Troppe richieste. Riprova tra $retryAfterSeconds secondi.")
        is ApiResult.NetworkError -> throw cause
    }

    private fun backendMessage(body: String): String? =
        runCatching { ApiService.gson.fromJson(body, ApiErrorBody::class.java)?.message }
            .getOrNull()
            ?.takeIf { it.isNotBlank() }
}
