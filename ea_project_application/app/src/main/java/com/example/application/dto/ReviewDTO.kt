package com.example.application.dto

data class ReviewDTO(
    val reviewId: Long? = null,
    val userId: Long? = null,
    val username: String? = null,
    val response: ResponseDTO? = null,
    val tripId: Long? = null,
    val activityId: Long? = null,
    val rating: Int? = null,
    val comment: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)