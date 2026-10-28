package com.example.application.screens.account

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.example.application.network.PasswordStrength
import com.example.application.ui.theme.LocalDarkTheme

@Composable
internal fun PasswordStrengthMeter(password: String) {
    if (password.isEmpty()) return
    val strength = PasswordStrength.of(password)
    val dark = LocalDarkTheme.current
    val colors = if (dark) listOf(0xFFFF8A80, 0xFFF2CD71, 0xFF7DD6A5, 0xFFC5ADE8)
        else listOf(0xFFAD3028, 0xFF875B00, 0xFF23623F, 0xFF76509B)
    val color by animateColorAsState(Color(colors[strength.level - 1]), label = "strength_color")
    val progress by animateFloatAsState(strength.level / 4f, label = "strength_progress")
    Column(verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.semantics {
        contentDescription = "Robustezza password: ${strength.label}, ${strength.level} su 4"
    }) {
        LinearProgressIndicator(progress = { progress }, modifier = Modifier.fillMaxWidth().height(6.dp), color = color,
            trackColor = MaterialTheme.colorScheme.onSurface.copy(alpha = .10f))
        Text("Robustezza: ${strength.label}", style = MaterialTheme.typography.labelLarge, color = color)
    }
}
