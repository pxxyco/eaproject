package com.example.application.screens.activity.reviews

import com.example.application.dto.*
import com.example.application.network.UserSession
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Test

class ReviewsStateTest {
    private val ref = ExperienceRef(ExperienceType.TRIP, 7)
    @After fun clearSession() { UserSession.clear() }
    @Test fun newReviewRequiresConfirmedParticipationAndDoesNotDuplicateOwnReview() = runBlocking {
        UserSession.setSession(UserDTO(id = 3, role = "TRAVELER"))
        var booking = BookingItemDTO(bookingId = 1, tripId = 7, bookingStatus = "PENDING")
        var reviews = emptyList<ReviewDTO>()
        val state = ReviewsState(ref, 9, loadBookings = { listOf(booking) }, loadReviews = { reviews })
        state.reload()
        assertFalse(state.canWrite)
        state.openNew(); assertNull(state.form)
        booking = booking.copy(bookingStatus = "CONFIRMED")
        state.refreshParticipation(); assertTrue(state.canWrite)
        state.openNew(); assertEquals(ReviewForm.New, state.form)
        state.closeForm()
        reviews = listOf(ReviewDTO(reviewId = 4, userId = 3, tripId = 7, rating = 5))
        state.reload(); assertFalse(state.canWrite); assertEquals(5.0, state.average!!, .0)
        state.openEdit(reviews.single()); assertTrue(state.form is ReviewForm.Edit)
    }
    @Test fun cancelledExperienceAndFailedParticipationCheckNeverEnableWriting() = runBlocking {
        UserSession.setSession(UserDTO(id = 3, role = "TRAVELER"))
        val state = ReviewsState(ref, 9,
            loadBookings = { listOf(BookingItemDTO(bookingStatus = "CONFIRMED", itemStatus = "CANCELLED")) },
            loadReviews = { emptyList() })
        state.reload(); assertFalse(state.canWrite)
        val failed = ReviewsState(ref, 9, loadBookings = { error("offline") }, loadReviews = { emptyList() })
        failed.reload(); assertFalse(failed.canWrite); assertFalse(failed.participationLoaded); assertFalse(failed.participationBusy)
    }
    @Test fun onlyOfferOwnerCanManageOrganizerResponse() = runBlocking {
        UserSession.setSession(UserDTO(id = 9, role = "ORGANIZER"))
        val state = ReviewsState(ref, 9, loadBookings = { error("Organizer must not load traveler bookings") },
            loadReviews = { emptyList() })
        state.reload()
        val review = ReviewDTO(reviewId = 4, userId = 3)
        assertTrue(state.canRespond(review)); assertFalse(state.canWrite)
        assertTrue(state.canManageResponse(review.copy(response = ResponseDTO(id = 5))))
        UserSession.setSession(UserDTO(id = 8, role = "ORGANIZER"))
        assertFalse(state.canRespond(review))
    }
}
