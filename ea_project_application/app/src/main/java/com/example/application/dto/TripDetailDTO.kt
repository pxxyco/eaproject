package com.example.application.dto

import java.math.BigDecimal

data class TripDetailDTO(
    val tripId: Long? = null,
    val createdByUserId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val destinationCountry: String? = null,
    val destinationCity: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val totalPrice: BigDecimal? = null,
    val maxSeats: Int? = null,
    val availableSeats: Int? = null,
    val status: String? = null,
    val averageRating: BigDecimal? = null,
    val coverPhotoUrl: String? = null,
    val stages: List<StageDTO>? = null
)