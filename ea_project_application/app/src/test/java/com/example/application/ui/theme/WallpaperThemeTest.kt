package com.example.application.ui.theme

import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import org.junit.Assert.*
import org.junit.Test

class WallpaperThemeTest {
    private fun contrast(a: Color, b: Color): Float {
        val x = a.luminance(); val y = b.luminance()
        return (maxOf(x, y) + .05f) / (minOf(x, y) + .05f)
    }
    @Test fun blueWallpaperReplacesGreenSurfacesInBothThemesAndKeepsTextReadable() {
        val blue = dominantWallpaperColor(IntArray(100) { if (it < 80) 0xFF2050B0.toInt() else 0xFFB04030.toInt() })
        listOf(false, true).forEach { dark ->
            val scheme = (if (dark) darkColorScheme() else lightColorScheme()).withWallpaper(blue, dark)
            assertTrue(scheme.primary.blue > scheme.primary.green)
            assertTrue(scheme.surface.blue > scheme.surface.green)
            assertTrue(contrast(scheme.primary, scheme.onPrimary) >= 4.5f)
            assertTrue(contrast(scheme.surface, scheme.onSurface) >= 4.5f)
            assertTrue(contrast(scheme.secondaryContainer, scheme.primary) >= 4.5f)
        }
    }
    @Test fun missingWallpaperStillHasANeutralReadablePalette() {
        val scheme = lightColorScheme().withWallpaper(null, false)
        assertTrue(contrast(scheme.surface, scheme.onSurface) >= 4.5f)
        assertTrue(scheme.surface.red >= scheme.surface.green)
    }
}
