package com.example.application.screens.activity.reviews

import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ReviewDTO
import com.example.application.network.ReviewRepository
import kotlinx.coroutines.CancellationException

internal class ReviewsState(private val ref: ExperienceRef) {

    var reviews by mutableStateOf<List<ReviewDTO>?>(null)
        private set

    var error by mutableStateOf<String?>(null)
        private set

    private var loaded = false

    suspend fun loadIfNeeded() {
        if (!loaded) reload()
    }

    suspend fun reload() {
        error = null
        try {
            reviews = ReviewRepository.getReviews(ref)
            loaded = true
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            error = e.message ?: "Impossibile caricare le recensioni"
        }
    }
}

@Composable
internal fun rememberReviewsState(ref: ExperienceRef): ReviewsState =
    remember(ref) { ReviewsState(ref) }