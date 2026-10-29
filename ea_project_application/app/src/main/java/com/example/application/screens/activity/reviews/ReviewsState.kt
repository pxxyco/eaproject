package com.example.application.screens.activity.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.application.dto.BookingItemDTO
import com.example.application.dto.canReview
import com.example.application.network.BookingRepository
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ResponseDTO
import com.example.application.dto.ReviewDTO
import com.example.application.network.ReviewRepository
import kotlinx.coroutines.CancellationException
import com.example.application.network.UserSession

internal sealed interface ReviewForm {
    data object New : ReviewForm
    data class Edit(val review: ReviewDTO) : ReviewForm
}

internal sealed interface ResponseForm {
    val review: ReviewDTO
    data class New(override val review: ReviewDTO) : ResponseForm
    data class Edit(override val review: ReviewDTO, val response: ResponseDTO) : ResponseForm
}

internal class ReviewsState(
    private val ref: ExperienceRef,
    private val ownerId: Long?,
    private val loadBookings: suspend (ExperienceRef) -> List<BookingItemDTO> = { BookingRepository.mine(it) },
    private val loadReviews: suspend (ExperienceRef) -> List<ReviewDTO> = { ReviewRepository.getReviews(it) }
) {
    var form by mutableStateOf<ReviewForm?>(null)
        private set

    var pendingDelete by mutableStateOf<ReviewDTO?>(null)
        private set

    var responseForm by mutableStateOf<ResponseForm?>(null)
        private set

    var pendingResponseDelete by mutableStateOf<ResponseDTO?>(null)
        private set
    var reviews by mutableStateOf<List<ReviewDTO>?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var loaded = false
    var bookings by mutableStateOf<List<BookingItemDTO>>(emptyList())
        private set
    var participationLoaded by mutableStateOf(false)
        private set

    var participationBusy by mutableStateOf(false)
        private set

    suspend fun refreshParticipation() {
        participationBusy = true
        participationLoaded = false
        try {
            bookings = if (UserSession.role.equals("TRAVELER", true)) loadBookings(ref) else emptyList()
            participationLoaded = true
        } catch (e: CancellationException) { throw e }
        catch (e: Exception) { bookings = emptyList(); error = "Impossibile verificare le prenotazioni. Riprova." }
        finally { participationBusy = false }
    }

    val myReview: ReviewDTO?
        get() {
            val myId = UserSession.user?.id ?: return null
            return reviews?.firstOrNull { it.userId == myId }
        }

    val canWrite: Boolean
        get() = UserSession.role.equals("TRAVELER", ignoreCase = true) &&
                reviews != null && participationLoaded && !participationBusy &&
                myReview == null && bookings.any {
                    it.canReview()
                }

    val average: Double?
        get() = reviews
            ?.mapNotNull { it.rating }
            ?.takeIf { it.isNotEmpty() }
            ?.average()

    fun isMine(review: ReviewDTO): Boolean {
        val myId = UserSession.user?.id ?: return false
        return review.userId == myId
    }

    private val isOwner: Boolean
        get() {
            val myId = UserSession.user?.id ?: return false
            return UserSession.role.equals("ORGANIZER", ignoreCase = true) && myId == ownerId
        }

    fun canRespond(review: ReviewDTO): Boolean = isOwner && review.response == null

    fun canManageResponse(review: ReviewDTO): Boolean = isOwner && review.response != null

    fun openNewResponse(review: ReviewDTO) { responseForm = ResponseForm.New(review) }
    fun openEditResponse(review: ReviewDTO) {
        val response = review.response ?: return
        responseForm = ResponseForm.Edit(review, response)
    }
    fun closeResponseForm() { responseForm = null }

    fun askDeleteResponse(review: ReviewDTO) { pendingResponseDelete = review.response }
    fun cancelDeleteResponse() { pendingResponseDelete = null }

    fun openNew() { if (canWrite) form = ReviewForm.New }
    fun openEdit(review: ReviewDTO) { form = ReviewForm.Edit(review) }
    fun closeForm() { form = null }

    fun askDelete(review: ReviewDTO) { pendingDelete = review }
    fun cancelDelete() { pendingDelete = null }

    suspend fun loadIfNeeded() {
        if (!loaded) reload()
    }

    suspend fun save(rating: Int, comment: String) {
        val text = comment.trim().takeIf { it.isNotEmpty() }
        when (val current = form) {
            ReviewForm.New -> ReviewRepository.createReview(ref, rating, text)
            is ReviewForm.Edit -> {
                val id = current.review.reviewId ?: return
                ReviewRepository.updateReview(id, rating, text)
            }
            null -> return
        }
        reload()
        form = null
    }

    suspend fun confirmDelete() {
        val id = pendingDelete?.reviewId ?: return
        ReviewRepository.deleteReview(id)
        reload()
        pendingDelete = null
    }

    suspend fun saveResponse(comment: String) {
        val text = comment.trim()
        when (val current = responseForm) {
            is ResponseForm.New -> {
                val reviewId = current.review.reviewId ?: return
                ReviewRepository.createResponse(reviewId, text)
            }
            is ResponseForm.Edit -> {
                val id = current.response.id ?: return
                ReviewRepository.updateResponse(id, text)
            }
            null -> return
        }
        reload()
        responseForm = null
    }

    suspend fun confirmDeleteResponse() {
        val id = pendingResponseDelete?.id ?: return
        ReviewRepository.deleteResponse(id)
        reload()
        pendingResponseDelete = null
    }

    suspend fun reload() {
        error = null
        try {
            reviews = loadReviews(ref)
            refreshParticipation()
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Impossibile caricare le recensioni"
        }
    }
}

@Composable
internal fun rememberReviewsState(ref: ExperienceRef, ownerId: Long?): ReviewsState =
    remember(ref, ownerId) { ReviewsState(ref, ownerId) }
