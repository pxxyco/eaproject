package com.example.application.network

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

object AppStatus {

    data class ErrorState(val code: AppErrorCode, val retryAfterSeconds: Int = 0)

    var error by mutableStateOf<ErrorState?>(null)
        private set

    var reloadTick by mutableIntStateOf(0)
        private set

    @Synchronized fun showNetworkError() {
        if (error == null) error = ErrorState(AppErrorCode.ERR_NETWORK_UNREACHABLE)
    }

    @Synchronized fun showRateLimited(retryAfterSeconds: Int) {
        if (error == null) error = ErrorState(AppErrorCode.ERR_RATE_LIMITED, retryAfterSeconds.coerceIn(5, 120))
    }

    @Synchronized fun showAuthExpired() {
        if (error == null) error = ErrorState(AppErrorCode.ERR_AUTH_EXPIRED)
    }

    @Synchronized fun dismiss() { error = null }

    fun requestReload() { reloadTick++ }
}