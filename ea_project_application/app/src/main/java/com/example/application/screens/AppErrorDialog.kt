package com.example.application.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import com.example.application.network.ApiService
import com.example.application.network.AppErrorCode
import com.example.application.network.AppStatus
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

@Composable
fun AppErrorDialog(onExit: () -> Unit) {
    val state = AppStatus.error ?: return
    val scope = rememberCoroutineScope()
    var secondsLeft by remember(state) { mutableIntStateOf(state.retryAfterSeconds) }
    var checking by remember(state) { mutableStateOf(false) }

    var shakeTrigger by remember { mutableIntStateOf(0) }

    val shakeController = remember { Animatable(0f) }

    LaunchedEffect(state, shakeTrigger) {
        if (shakeTrigger > 0 || state != null) {
            shakeController.animateTo(
                targetValue = 0f,
                animationSpec = keyframes {
                    durationMillis = 400
                    0f at 0
                    -15f at 50
                    15f at 100
                    -15f at 150
                    15f at 200
                    -10f at 250
                    10f at 300
                    -5f at 350
                    0f at 400
                }
            )
        }
    }

    fun recovered() {
        AppStatus.dismiss()
        AppStatus.requestReload()
    }

    LaunchedEffect(state) {
        when (state.code) {
            AppErrorCode.ERR_RATE_LIMITED -> {
                while (secondsLeft > 0) { delay(1000); secondsLeft-- }
                recovered()
            }
            AppErrorCode.ERR_NETWORK_UNREACHABLE -> {
                while (true) {
                    delay(3000)
                    if (ApiService.isBackendReachable()) { recovered(); break }
                }
            }
            else -> Unit
        }
    }

    val isAuthExpired = state.code == AppErrorCode.ERR_AUTH_EXPIRED
    val isRateLimited = state.code == AppErrorCode.ERR_RATE_LIMITED

    AlertDialog(
        onDismissRequest = { },
        modifier = Modifier
            .offset { IntOffset(shakeController.value.roundToInt(), 0) },
        shape = RoundedCornerShape(28.dp),
        containerColor = MaterialTheme.colorScheme.surface,
        title = {
            Text("Oops, qualcosa è andato storto...")
        },
        text = {
            Text(
                "Codice Errore: ${state.code.code}. ${state.code.description}" +
                        if (isRateLimited) " Riprova tra $secondsLeft s." else ""
            )
        },
        confirmButton = {
            TextButton(
                enabled = !checking && !(isRateLimited && secondsLeft > 0),
                onClick = {
                    if (isAuthExpired) {
                        AppStatus.dismiss()
                    } else {
                        scope.launch {
                            checking = true
                            if (ApiService.isBackendReachable()) {
                                recovered()
                            } else {
                                shakeTrigger++
                            }
                            checking = false
                        }
                    }
                }
            ) {
                Text(
                    when {
                        isAuthExpired -> "OK"
                        isRateLimited && secondsLeft > 0 -> "Riprova tra $secondsLeft s"
                        checking -> "Controllo..."
                        else -> "Riprova"
                    }
                )
            }
        },
        dismissButton = if (isAuthExpired) null else {
            { TextButton(onClick = onExit) { Text("Esci") } }
        }
    )
}