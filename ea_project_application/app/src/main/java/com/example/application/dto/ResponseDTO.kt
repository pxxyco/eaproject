package com.example.application.dto

data class ResponseDTO(
    val id: Long? = null,
    val organizerId: Long? = null,
    val reviewId: Long? = null,
    val comment: String? = null,
    val createdAt: String? = null,
    val updatedAt: String? = null
)