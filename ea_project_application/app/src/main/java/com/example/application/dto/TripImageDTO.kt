package com.example.application.dto

data class TripImageDTO(
    val imageId: Long? = null,
    val tripId: Long? = null,
    val imageUrl: String? = null,
    val orderIndex: Int? = null
)