package com.example.application.dto

data class StageDTO(
    val stageId: Long? = null,
    val tripId: Long? = null,
    val title: String? = null,
    val description: String? = null,
    val category: String? = null,
    val locationName: String? = null,
    val city: String? = null,
    val country: String? = null,
    val latitude: Double? = null,
    val longitude: Double? = null,
    val day: Int? = null,
    val orderInDay: Int? = null,
    val startTime: String? = null,
    val endTime: String? = null,
    val photoUrl: String? = null,
    val notes: String? = null
)