package com.example.application.dto

data class ActivityHomeDTO(
    val activityId: Long? = null,
    val title: String? = null,
    val city: String? = null,
    val category: String? = null,
    val price: Double? = null,
    val averageRating: Double? = null,
    val imageUrl: String? = null,
    val startDate: String? = null,
    val endDate: String? = null
)