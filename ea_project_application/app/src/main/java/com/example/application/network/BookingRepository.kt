package com.example.application.network

import com.example.application.dto.BookingItemDTO
import com.example.application.dto.ExperienceRef
import com.example.application.dto.ExperienceType

object BookingRepository {

    suspend fun book(ref: ExperienceRef, seats: Int): BookingItemDTO {
        val path = when (ref.type) {
            ExperienceType.ACTIVITY -> "/api/bookings/activities/${ref.id}?seats=$seats"
            ExperienceType.TRIP -> "/api/bookings/trips/${ref.id}?seats=$seats"
        }
        return ApiService.post(path, null, BookingItemDTO::class.java).requireData()
    }

    suspend fun mine(ref: ExperienceRef): List<BookingItemDTO> {
        val key = if (ref.type == ExperienceType.TRIP) "tripId" else "activityId"
        return ApiService.get("/api/bookings/mine/experience?$key=${ref.id}", Array<BookingItemDTO>::class.java).requireData().toList()
    }

}
