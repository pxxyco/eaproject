package com.example.application.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import com.example.application.dto.UserDTO

object UserSession {
    var user by mutableStateOf<UserDTO?>(null)
        private set

    @Volatile var accessToken: String? = null
        private set
    @Volatile var refreshToken: String? = null
        private set

    val isLoggedIn: Boolean get() = user != null
    val role: String? get() = user?.role

    fun setTokens(access: String, refresh: String) {
        accessToken = access
        refreshToken = refresh
    }

    fun setSession(dto: UserDTO) {
        user = dto
    }

    fun clear() {
        RecommendationRepository.invalidate()
        user = null
        accessToken = null
        refreshToken = null
    }
}