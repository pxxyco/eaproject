package com.example.application.dto

import java.time.LocalDateTime
import java.time.LocalDate
import java.time.LocalTime

internal fun BookingItemDTO.canCancel(now: LocalDateTime = LocalDateTime.now()): Boolean {
    if (bookingId == null || bookingStatus !in listOf("PENDING", "CONFIRMED")) return false
    if (itemStatus in listOf("CANCELLED", "COMPLETED")) return false
    val start = parseBookingDate(itemDate) ?: return false
    return !start.toLocalDate().isBefore(now.toLocalDate())
}

internal fun BookingItemDTO.canReview(now: LocalDateTime = LocalDateTime.now()): Boolean {
    if (bookingStatus !in listOf("CONFIRMED", "COMPLETED") || itemStatus == "CANCELLED") return false
    if (paymentStatus in listOf("CONFIRMED", "COMPLETED") || price == 0.0) return true
    if (bookingStatus == "COMPLETED" || itemStatus == "COMPLETED") return true
    val end = parseBookingDate(itemEndDate, endOfDay = true) ?: parseBookingDate(itemDate)?.let {
        if (bookingType == "TRIP") it.toLocalDate().atTime(LocalTime.MAX) else it.plusHours(1)
    }
    return end != null && end.toLocalDate().isBefore(now.toLocalDate())
}

private fun parseBookingDate(value: String?, endOfDay: Boolean = false): LocalDateTime? {
    if (value.isNullOrBlank()) return null
    return runCatching { LocalDateTime.parse(value) }.getOrNull()
        ?: runCatching { LocalDate.parse(value).atTime(if (endOfDay) LocalTime.MAX else LocalTime.MIN) }.getOrNull()
}
