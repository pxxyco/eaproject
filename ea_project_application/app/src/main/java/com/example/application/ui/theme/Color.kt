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
    val sample = accent ?: Color(0xFFB8A48B)
    val high = maxOf(sample.red, sample.green, sample.blue)
    val low = minOf(sample.red, sample.green, sample.blue)
    val delta = high - low
    val hue = if (delta < .001f) 30f else {
        val sector = when (high) {
            sample.red -> (sample.green - sample.blue) / delta
            sample.green -> (sample.blue - sample.red) / delta + 2f
            else -> (sample.red - sample.green) / delta + 4f
        }
        (sector * 60f + 360f) % 360f
    }
    val saturation = if (high == 0f) 0f else delta / high
    val tint = (saturation * .3f).coerceAtMost(.16f)
    fun surface(light: Float) = Color.hsl(hue, tint, light)
    fun accentColor(light: Float) = Color.hsl(hue, saturation.coerceIn(.10f, .45f), light)
    return copy(
        primary = accentColor(if (darkTheme) .83f else .27f), onPrimary = surface(if (darkTheme) .10f else .99f),
        secondary = accentColor(if (darkTheme) .76f else .32f), onSecondary = surface(if (darkTheme) .10f else .99f),
        primaryContainer = surface(if (darkTheme) .24f else .88f), onPrimaryContainer = surface(if (darkTheme) .95f else .12f),
        secondaryContainer = surface(if (darkTheme) .24f else .88f), onSecondaryContainer = surface(if (darkTheme) .95f else .12f),
        tertiary = accentColor(if (darkTheme) .76f else .32f), onTertiary = surface(if (darkTheme) .10f else .99f),
        tertiaryContainer = surface(if (darkTheme) .24f else .88f), onTertiaryContainer = surface(if (darkTheme) .95f else .12f),
        background = surface(if (darkTheme) .07f else .97f), onBackground = surface(if (darkTheme) .96f else .10f),
        surface = surface(if (darkTheme) .12f else .97f), onSurface = surface(if (darkTheme) .96f else .10f),
        surfaceVariant = surface(if (darkTheme) .23f else .90f), onSurfaceVariant = surface(if (darkTheme) .80f else .29f),
        surfaceTint = accentColor(if (darkTheme) .83f else .27f),
        outline = surface(if (darkTheme) .58f else .45f), outlineVariant = surface(if (darkTheme) .35f else .73f),
        surfaceDim = surface(if (darkTheme) .07f else .87f), surfaceBright = surface(if (darkTheme) .22f else .98f),
        surfaceContainerLowest = surface(if (darkTheme) .05f else .99f),
        surfaceContainerLow = surface(if (darkTheme) .10f else .96f), surfaceContainer = surface(if (darkTheme) .14f else .94f),
        surfaceContainerHigh = surface(if (darkTheme) .18f else .92f), surfaceContainerHighest = surface(if (darkTheme) .22f else .89f),
        inverseSurface = surface(if (darkTheme) .94f else .16f), inverseOnSurface = surface(if (darkTheme) .12f else .97f),
        inversePrimary = accentColor(if (darkTheme) .27f else .83f)
    )
}

val FavoriteRed = Color(0xFFEF4444)
