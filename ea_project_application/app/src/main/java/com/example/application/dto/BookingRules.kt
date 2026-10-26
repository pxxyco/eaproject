package com.example.application.dto

import java.time.LocalDateTime

internal fun BookingItemDTO.canCancel(now: LocalDateTime = LocalDateTime.now()): Boolean {
    if (bookingId == null || bookingStatus !in listOf("PENDING", "CONFIRMED")) return false
    if (itemStatus in listOf("CANCELLED", "COMPLETED")) return false
    val start = runCatching { LocalDateTime.parse(itemDate) }.getOrNull() ?: return false
    return start.isAfter(now)
}
