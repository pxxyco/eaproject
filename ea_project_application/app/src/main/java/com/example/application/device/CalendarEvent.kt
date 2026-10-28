package com.example.application.device

import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset

internal data class CalendarEvent(val title: String, val location: String, val start: Long, val end: Long, val allDay: Boolean) {
    companion object {
        /** Matches the backend ICS export: inclusive trip dates, Rome time for activities. */
        fun from(title: String?, location: String?, start: String?, end: String?): CalendarEvent? = runCatching {
            require(!start.isNullOrBlank())
            val allDay = start.length == 10
            val first: Long
            val last: Long
            if (allDay) {
                val date = LocalDate.parse(start)
                val finish = end?.takeIf(String::isNotBlank)?.let { LocalDate.parse(it.take(10)) } ?: date
                require(!finish.isBefore(date))
                first = date.atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
                last = finish.plusDays(1).atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
            } else {
                fun millis(value: String): Long = runCatching { Instant.parse(value).toEpochMilli() }.getOrElse {
                    LocalDateTime.parse(value).atZone(ZoneId.of("Europe/Rome")).toInstant().toEpochMilli()
                }
                first = millis(start)
                last = end?.takeIf(String::isNotBlank)?.let(::millis) ?: (first + 3_600_000)
                require(last > first)
            }
            CalendarEvent(title?.takeIf(String::isNotBlank) ?: "Esperienza EA Project", location.orEmpty(), first, last, allDay)
        }.getOrNull()
    }
}
