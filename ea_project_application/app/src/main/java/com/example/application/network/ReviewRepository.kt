package com.example.application.network

import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType
import com.example.application.dto.ResponseDTO
import com.example.application.dto.ReviewDTO

object ReviewRepository {

    suspend fun getReviews(ref: ExperienceRef): List<ReviewDTO> {
        val path = when (ref.type) {
            ExperienceType.ACTIVITY -> "/api/reviews/activity/${ref.id}"
            ExperienceType.TRIP -> "/api/reviews/trip/${ref.id}"
        }
        return ApiService.get(path, Array<ReviewDTO>::class.java).orThrow().toList()
    }

    suspend fun createReview(ref: ExperienceRef, rating: Int, comment: String?): ReviewDTO {
        val body = NewReviewRequest(
            activityId = ref.id.takeIf { ref.type == ExperienceType.ACTIVITY },
            tripId = ref.id.takeIf { ref.type == ExperienceType.TRIP },
            rating = rating,
            comment = comment
        )
        return ApiService.post("/api/reviews", body, ReviewDTO::class.java).orThrow()
    }

    suspend fun updateReview(reviewId: Long, rating: Int, comment: String?): ReviewDTO =
        ApiService.put("/api/reviews/$reviewId", EditReviewRequest(rating, comment), ReviewDTO::class.java).orThrow()

    suspend fun deleteReview(reviewId: Long) {
        ApiService.delete("/api/reviews/$reviewId", Unit::class.java).orThrow()
    }

    suspend fun createResponse(reviewId: Long, comment: String): ResponseDTO =
        ApiService.post("/api/responses", ResponseRequest(reviewId, comment), ResponseDTO::class.java).orThrow()

    suspend fun updateResponse(responseId: Long, comment: String): ResponseDTO =
        ApiService.put("/api/responses/$responseId", ResponseRequest(null, comment), ResponseDTO::class.java).orThrow()

    suspend fun deleteResponse(responseId: Long) {
        ApiService.delete("/api/responses/$responseId", Unit::class.java).orThrow()
    }

    private data class NewReviewRequest(
        val activityId: Long?,
        val tripId: Long?,
        val rating: Int,
        val comment: String?
    )

    private data class EditReviewRequest(val rating: Int, val comment: String?)

    private data class ResponseRequest(val reviewId: Long?, val comment: String)

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