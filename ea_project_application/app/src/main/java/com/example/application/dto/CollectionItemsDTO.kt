package com.example.application.dto

data class CollectionItemsDTO(
    val trips: List<TripHomeDTO>? = null,
    val activities: List<ActivityHomeDTO>? = null,
    val bookings: List<BookingItemDTO> = emptyList()
)
