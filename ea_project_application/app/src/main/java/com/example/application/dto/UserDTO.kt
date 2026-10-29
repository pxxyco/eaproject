package com.example.application.dto

data class UserDTO(
    val id: Long? = null,
    val username: String? = null,
    val email: String? = null,
    val fullName: String? = null,
    val profilePhotoUrl: String? = null,
    val role: String? = null,
    val oauthProvider: Boolean = false
)