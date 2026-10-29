package com.example.application.dto

data class ActivitySuggestionDTO(
    val activityId: Long? = null,
    val title: String? = null,
    val category: String? = null,
    val city: String? = null
)

data class TripSuggestionDTO(
    val tripId: Long? = null,
    val title: String? = null,
    val location: String? = null
)

data class SearchSuggestion(
    val title: String,
    val subtitle: String
)