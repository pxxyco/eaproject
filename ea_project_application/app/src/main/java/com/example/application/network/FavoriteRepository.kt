package com.example.application.network

import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType

object FavoriteRepository {

    suspend fun isFavorite(ref: ExperienceRef): Boolean =
        ApiService.get(path(ref), FavoriteStatus::class.java).orThrow().favorite == true

    suspend fun setFavorite(ref: ExperienceRef, favorite: Boolean): Boolean {
        val result = if (favorite) ApiService.post(path(ref), null, FavoriteStatus::class.java)
        else ApiService.delete(path(ref), FavoriteStatus::class.java)
        return result.orThrow().favorite ?: favorite
    }

    private fun path(ref: ExperienceRef): String = when (ref.type) {
        ExperienceType.ACTIVITY -> "/api/favorites/activities/${ref.id}"
        ExperienceType.TRIP -> "/api/favorites/trips/${ref.id}"
    }

    private data class FavoriteStatus(val favorite: Boolean? = null)

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
