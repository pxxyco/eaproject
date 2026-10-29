package com.example.application.ui.theme

import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp

internal val LocalDarkTheme = staticCompositionLocalOf { true }

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
val OverlayDark = Color(0xFF0C0C0C).copy(alpha = .82f)

val NavigationAccent: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.primary
val NavigationSelection: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.secondaryContainer
val NavigationOutline: Color @Composable @ReadOnlyComposable get() = MaterialTheme.colorScheme.outline

object WallpaperPalette {
    var accent by mutableStateOf<Color?>(null)
}

fun dominantWallpaperColor(pixels: IntArray): Color {
    val counts = IntArray(4096)
    for (p in pixels) {
        val r = (p shr 16) and 0xFF; val g = (p shr 8) and 0xFF; val b = p and 0xFF
        val brightness = maxOf(r, g, b) / 255f
        if (brightness < .11f || brightness > .96f) continue
        counts[(r shr 4) shl 8 or ((g shr 4) shl 4) or (b shr 4)]++
    }
    val dominant = counts.indices.maxBy { counts[it] }
    val sample = if (counts[dominant] == 0) Color(0xFFB8A48B)
        else Color(((dominant shr 8) and 15) * 16 + 8, ((dominant shr 4) and 15) * 16 + 8, (dominant and 15) * 16 + 8)
    return lerp(sample, Color.White, .45f)
}

fun ColorScheme.withWallpaper(accent: Color?, darkTheme: Boolean): ColorScheme {
    if (accent == null) return this
    val strong = if (darkTheme) accent else lerp(accent, Color.Black, .55f)
    val surfaceTint = if (darkTheme) .12f else .06f
    return copy(
        primary = strong,
        secondary = lerp(secondary, strong, .5f),
        secondaryContainer = lerp(secondaryContainer, accent, if (darkTheme) .3f else .4f),
        inversePrimary = lerp(inversePrimary, accent, .5f),
        background = lerp(background, accent, surfaceTint / 2),
        surface = lerp(surface, accent, surfaceTint / 2),
        surfaceContainerLow = lerp(surfaceContainerLow, accent, surfaceTint),
        surfaceContainer = lerp(surfaceContainer, accent, surfaceTint),
        surfaceContainerHigh = lerp(surfaceContainerHigh, accent, surfaceTint),
        surfaceContainerHighest = lerp(surfaceContainerHighest, accent, surfaceTint),
        outline = lerp(outline, accent, .3f),
        outlineVariant = lerp(outlineVariant, accent, .2f),
    )
}
