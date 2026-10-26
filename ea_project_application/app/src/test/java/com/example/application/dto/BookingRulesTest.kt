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
    @Test fun startedOrUndatedBookingCannotBeCancelled() {
        assertFalse(future.copy(itemDate = now.toString()).canCancel(now))
        assertFalse(future.copy(itemDate = null).canCancel(now))
    }
}
