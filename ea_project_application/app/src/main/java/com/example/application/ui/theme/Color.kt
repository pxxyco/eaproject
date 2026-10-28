package com.example.application.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color

internal val LocalDarkTheme = staticCompositionLocalOf { true }

// Semantic tokens are shared by every screen and follow the active Compose theme.
val Stone50: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onBackground
val Stone200: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val Stone300: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Stone400: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurfaceVariant
val Stone600: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceVariant
val Stone900: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onPrimary
val PopupSurface: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainer
val ScreenBackground: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.background
val SuccessColor: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondary
val RatingAmber: Color @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color(0xFFF5A623) else Color(0xFF815100)
val GlassFill: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surfaceContainerHigh.copy(alpha = if (LocalDarkTheme.current) .72f else .90f)
val GlassFillStrong: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.onSurface.copy(alpha = .08f)
val GlassBorder: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outlineVariant
val GlassBorderStrong: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
val NavigationGlass: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.surface.copy(alpha = .78f)
val WallpaperShade: Color @Composable @ReadOnlyComposable get() = if (LocalDarkTheme.current) Color.Black.copy(alpha = .57f) else MaterialTheme.colorScheme.background.copy(alpha = .60f)
// Image captions retain white text and a dark scrim in either theme.
val OverlayDark = Color(0xFF0C0C0C).copy(alpha = .82f)

val NavigationAccent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val NavigationSelection: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondaryContainer
val NavigationOutline: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline
