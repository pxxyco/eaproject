package com.example.application.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


private val AppColorScheme = darkColorScheme(
    primary = Stone200,
    onPrimary = Stone900,
    secondary = Stone300,
    onSecondary = Stone900,
    secondaryContainer = Stone600,
    onSecondaryContainer = Color.White,
    background = ScreenBackground,
    onBackground = Color.White,
    surface = PopupSurface,
    onSurface = Color(0xFFF8FAFC),
    onSurfaceVariant = Color.White.copy(alpha = 0.7f),
    outline = Color.White.copy(alpha = 0.25f),
    outlineVariant = Color.White.copy(alpha = 0.15f),

    surfaceContainerLow = Color(0xFF1C1C1B),
    surfaceContainer = PopupSurface,
    surfaceContainerHigh = PopupSurface,
    surfaceContainerHighest = PopupSurface,
)


val AppTypography = Typography(
    headlineMedium = TextStyle(fontSize = 28.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 34.sp), // hero-title
    titleLarge = TextStyle(fontSize = 20.sp, fontWeight = FontWeight.Bold),                              // section-title
    titleMedium = TextStyle(fontSize = 15.sp, fontWeight = FontWeight.Bold),                             // card-title
    bodyLarge = TextStyle(fontSize = 15.sp),                                                             // hero-subtitle
    bodySmall = TextStyle(fontSize = 12.sp),                                                             // card-location, section-subtitle
    labelSmall = TextStyle(fontSize = 10.sp, fontWeight = FontWeight.Bold),                              // field-label
)

@Composable
fun ApplicationTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = AppColorScheme,
        typography = AppTypography,
        content = content
    )
}