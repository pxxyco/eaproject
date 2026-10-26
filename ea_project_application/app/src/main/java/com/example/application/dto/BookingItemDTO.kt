package com.example.application.dto

data class BookingItemDTO(
    val bookingId: Long? = null,
    val tripId: Long? = null,
    val activityId: Long? = null,
    val itemTitle: String? = null,
    val itemDate: String? = null,
    val itemEndDate: String? = null,
    val itemStatus: String? = null,
    val itemLocation: String? = null,
    val bookingType: String? = null,
    val bookingStatus: String? = null,
    val price: Double? = null
)

