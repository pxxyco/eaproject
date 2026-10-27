package com.example.application.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.material3.LocalContentColor
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.example.application.device.findActivity
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp


internal val DarkAppColors = darkColorScheme(
    primary = Color(0xFFE7E5E4), onPrimary = Color(0xFF1C1917),
    secondary = Color(0xFFD3DED6), onSecondary = Color(0xFF17231C),
    secondaryContainer = Color(0xFF354A3E), onSecondaryContainer = Color(0xFFF0F6EF),
    background = Color(0xFF101813), onBackground = Color(0xFFF5F7F5),
    surface = Color(0xFF1C2922), onSurface = Color(0xFFF5F7F5),
    surfaceVariant = Color(0xFF354139), onSurfaceVariant = Color(0xFFC8D3CB),
    outline = Color(0xFF7E9184), outlineVariant = Color(0xFF506356),
    surfaceContainerLow = Color(0xFF17221C), surfaceContainer = Color(0xFF213027),
    surfaceContainerHigh = Color(0xFF2A3A30), surfaceContainerHighest = Color(0xFF33443A),
    inverseSurface = Color(0xFFE7E5E4), inverseOnSurface = Color(0xFF1C1917), inversePrimary = Color(0xFF203D2D)
)
internal val LightAppColors = lightColorScheme(
    primary = Color(0xFF244B36), onPrimary = Color.White,
    secondary = Color(0xFF476150), onSecondary = Color.White,
    secondaryContainer = Color(0xFFDCEADD), onSecondaryContainer = Color(0xFF203A28),
    background = Color(0xFFF5F7F1), onBackground = Color(0xFF19291F),
    surface = Color(0xFFF3F7F0), onSurface = Color(0xFF19291F),
    surfaceVariant = Color(0xFFDFE8DD), onSurfaceVariant = Color(0xFF46594B),
    outline = Color(0xFF758575), outlineVariant = Color(0xFFB9C9B9),
    surfaceContainerLow = Color(0xFFF5F8F1), surfaceContainer = Color(0xFFEDF3E9),
    surfaceContainerHigh = Color(0xFFE6EEE2), surfaceContainerHighest = Color(0xFFDEE8D9),
    inverseSurface = Color(0xFF243B2C), inverseOnSurface = Color(0xFFF4F8F1), inversePrimary = Color(0xFFD4E8D4)
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
fun ApplicationTheme(darkTheme: Boolean = isSystemInDarkTheme(), content: @Composable () -> Unit) {
    val colors = if (darkTheme) DarkAppColors else LightAppColors
    val view = LocalView.current
    if (!view.isInEditMode) SideEffect {
        view.context.findActivity()?.window?.let { window ->
            window.isNavigationBarContrastEnforced = false
            WindowCompat.getInsetsController(window, view).apply {
                isAppearanceLightStatusBars = !darkTheme
                isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }
    MaterialTheme(colorScheme = colors, typography = AppTypography) {
        CompositionLocalProvider(LocalDarkTheme provides darkTheme, LocalContentColor provides colors.onBackground, content = content)
    }
}
