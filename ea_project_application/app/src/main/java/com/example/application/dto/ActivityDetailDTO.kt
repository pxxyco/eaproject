package com.example.application.dto

data class ActivityDetailDTO(
    val activityId: Long? = null,
    val createdByUserId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val placeName: String? = null,
    val city: String? = null,
    val startDate: String? = null,
    val endDate: String? = null,
    val durationMinutes: Int? = null,
    val price: Double? = null,
    val maxSeats: Int? = null,
    val availableSeats: Int? = null,
    val status: String? = null,
    val averageRating: Double? = null,
    val notes: String? = null,
    val images: List<ActivityImageDTO>? = null
)

data class ActivityImageDTO(
    val imageId: Long? = null,
    val activityId: Long? = null,
    val imageUrl: String? = null,
    val orderIndex: Int? = null
)