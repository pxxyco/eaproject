
package com.example.application.screens.common

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

@Composable
fun LoadingOverlay(
    message: String = "Caricamento in corso...",
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color(0xD10C0C0C),
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = Color(0xFFE7E5E4),
                strokeWidth = 4.dp
            )

            Text(
                text = message,
                color = Color(0xFFE7E5E4),
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}
