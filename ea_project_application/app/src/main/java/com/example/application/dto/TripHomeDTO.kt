package com.example.application.dto

import java.math.BigDecimal

data class TripHomeDTO(
    val tripId: Long? = null,
    val title: String? = null,
    val location: String? = null,
    val totalPrice: BigDecimal? = null,
    val averageRating: BigDecimal? = null,
    val coverPhotoUrl: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)