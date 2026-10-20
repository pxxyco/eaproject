package com.example.application.network

import com.example.application.dto.UserDTO
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

data class LoginRequest(val identifier: String, val password: String)

data class RegisterRequest(
    val username: String,
    val email: String,
    val fullName: String,
    val password: String,
    val role: String
)

object AuthRepository {

    suspend fun login(identifier: String, password: String): String? =
        when (val r = ApiService.postRaw("/api/users/login", LoginRequest(identifier.trim(), password))) {
            is ApiResult.Success -> {
                val (body, headers) = r.data
                val access = headers["Authorization"]?.removePrefix("Bearer ")
                val refresh = headers["X-Refresh-Token"]
                val user = runCatching { ApiService.gson.fromJson(body, UserDTO::class.java) }.getOrNull()

                if (access.isNullOrBlank() || refresh.isNullOrBlank() || user == null) {
                    "Risposta di login non valida"
                } else {
                    UserSession.setTokens(access, refresh)
                    TokenStorage.saveRefreshToken(refresh)
                    UserSession.setSession(user)
                    null
                }
            }
            is ApiResult.HttpError ->
                if (r.code == 401) "Credenziali non valide" else "Errore ${r.code}"
            is ApiResult.RateLimited -> "Troppi tentativi, riprova tra ${r.retryAfterSeconds}s"
            is ApiResult.NetworkError -> "Server non raggiungibile"
        }

    suspend fun register(req: RegisterRequest): String? =
        when (val r = ApiService.post("/api/users", req, UserDTO::class.java)) {
            is ApiResult.Success -> login(req.username, req.password)
            is ApiResult.HttpError ->
                if (r.code == 400) "Dati di registrazione non validi" else "Errore ${r.code}"
            is ApiResult.RateLimited -> "Troppi tentativi, riprova tra ${r.retryAfterSeconds}s"
            is ApiResult.NetworkError -> "Server non raggiungibile"
        }

    suspend fun restoreSession() {
        if (UserSession.isLoggedIn) return
        val refresh = TokenStorage.getRefreshToken()
        if (refresh.isNullOrBlank()) return

        UserSession.setTokens("", refresh)
        when (withContext(Dispatchers.IO) { ApiService.refreshBlocking() }) {
            ApiService.RefreshOutcome.OK -> loadMe()
            ApiService.RefreshOutcome.INVALID -> { UserSession.clear(); TokenStorage.clear() }
            ApiService.RefreshOutcome.NETWORK -> AppStatus.showNetworkError()
        }
    }

    fun logout() {
        UserSession.clear()
        TokenStorage.clear()
    }

    private suspend fun loadMe() {
        val me = ApiService.get("/api/users/me", UserDTO::class.java)
        if (me is ApiResult.Success) UserSession.setSession(me.data)
    }
}