package com.example.application.screens.activity

import com.example.application.dto.ExperienceDetail
import java.time.LocalDate
import java.time.LocalDateTime

internal fun ExperienceDetail.isBookable(now: LocalDateTime = LocalDateTime.now()): Boolean {
    if ((availableSeats ?: 0) <= 0) return false
    return when (this) {
        is ExperienceDetail.Activity -> dto.status == "PUBLISHED" &&
            runCatching { LocalDateTime.parse(dto.startDate).isAfter(now) }.getOrDefault(false)
        is ExperienceDetail.Trip -> dto.status == "PUBLISHED" &&
            runCatching { LocalDate.parse(dto.startDate).isAfter(now.toLocalDate()) }.getOrDefault(false)
    }
}
