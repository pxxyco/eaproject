package com.example.application.dto

import org.junit.Assert.*
import org.junit.Test
import java.time.LocalDateTime

class BookingRulesTest {
    private val now = LocalDateTime.of(2026, 10, 26, 12, 0)
    private val future = BookingItemDTO(bookingId = 42, bookingStatus = "CONFIRMED", itemDate = "2026-10-27T12:00:00", itemStatus = "PUBLISHED")
    @Test fun futureConfirmedBookingCanBeCancelled() { assertTrue(future.canCancel(now)) }
    @Test fun completedOrCancelledBookingCannotBeCancelled() {
        listOf("COMPLETED", "CANCELLED").forEach { assertFalse(future.copy(bookingStatus = it).canCancel(now)) }
    }
    @Test fun cancelledExperienceCannotBeCancelled() { assertFalse(future.copy(itemStatus = "CANCELLED").canCancel(now)) }
    @Test fun previousDayOrUndatedBookingCannotBeCancelled() {
        assertFalse(future.copy(itemDate = now.minusDays(1).toString()).canCancel(now))
        assertFalse(future.copy(itemDate = null).canCancel(now))
    }
    @Test fun dateOnlyTripsCanBeCancelledBeforeDeparture() {
        assertTrue(future.copy(bookingType = "TRIP", itemDate = "2026-10-27").canCancel(now))
        assertTrue(future.copy(bookingType = "TRIP", itemDate = "2026-10-26").canCancel(now))
    }
    @Test fun reviewsRequirePaymentOrAFreeConfirmedBookingAndRejectCancellation() {
        assertFalse(future.copy(price = 10.0).canReview(now))
        assertTrue(future.copy(price = 10.0, paymentStatus = "CONFIRMED").canReview(now))
        assertTrue(future.copy(price = 0.0).canReview(now))
        assertFalse(future.copy(price = 10.0, paymentStatus = "CONFIRMED", bookingStatus = "CANCELLED").canReview(now))
    }
    @Test fun sameDayReservationStaysCancellableUntilMidnight() {
        val b = future.copy(itemDate = "2026-10-26T00:00:00")
        assertTrue(b.canCancel(now.toLocalDate().atTime(java.time.LocalTime.MAX)))
        assertFalse(b.canCancel(now.toLocalDate().plusDays(1).atStartOfDay()))
    }
}
