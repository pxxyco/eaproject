package com.example.application.ui.effects

import android.graphics.RuntimeShader
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.graphics.*
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp

/** Extra scrollable space keeps the last action above the fading region at the end of a page. */
internal val LocalNavigationTail = staticCompositionLocalOf { 0.dp }

/** Visible overlap between the page viewport and the top of the floating navigation capsule. */
internal val LocalNavigationOverlap = staticCompositionLocalOf { 0.dp }

private const val SHADER = """
    uniform shader content;
    uniform float height;
    uniform float fade;
    uniform float radius;
    half4 main(float2 p) {
        float t = smoothstep(height - fade, height, p.y);
        half4 original = content.eval(p);
        if (t <= 0.0) return original;
        float r = radius * t;
        half4 blurred = original * 0.25;
        blurred += (content.eval(p + float2(r, 0)) + content.eval(p - float2(r, 0))
            + content.eval(p + float2(0, r)) + content.eval(p - float2(0, r))) * 0.125;
        blurred += (content.eval(p + float2(r, r)) + content.eval(p - float2(r, r))
            + content.eval(p + float2(r, -r)) + content.eval(p + float2(-r, r))) * 0.0625;
        return mix(original, blurred, half(t)) * half(1.0 - t);
    }
"""

@RequiresApi(33)
internal class BottomBlur {
    val shader = RuntimeShader(SHADER)
    private var cachedEffect: RenderEffect? = null
    private var lastHeight = Float.NaN
    private var lastFade = Float.NaN
    private var lastRadius = Float.NaN
    val effect: RenderEffect get() = checkNotNull(cachedEffect)
    fun uniforms(height: Float, fade: Float, radius: Float) {
        if (height == lastHeight && fade == lastFade && radius == lastRadius) return
        lastHeight = height; lastFade = fade; lastRadius = radius
        shader.setFloatUniform("height", height)
        shader.setFloatUniform("fade", fade)
        shader.setFloatUniform("radius", radius)
        // Android snapshots uniforms when creating the effect. Build it AFTER setting the viewport.
        cachedEffect = android.graphics.RenderEffect.createRuntimeShaderEffect(shader, "content").asComposeRenderEffect()
    }
}

/** GPU sampling is limited to the bottom strip. Older Android versions use a soft alpha fade. */
@Composable
internal fun Modifier.navigationFade(): Modifier {
    val fade = with(LocalDensity.current) { 72.dp.toPx() }
    val radius = with(LocalDensity.current) { 9.dp.toPx() }
    val blur = remember { if (Build.VERSION.SDK_INT >= 33) runCatching { BottomBlur() }.getOrNull() else null }
    return if (Build.VERSION.SDK_INT >= 33 && blur != null) {
        graphicsLayer {
            compositingStrategy = CompositingStrategy.Offscreen
            blur.uniforms(size.height, fade, radius)
            renderEffect = blur.effect
        }
    } else {
        graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }.drawWithCache {
            val mask = Brush.verticalGradient(0f to Color.White,
                ((size.height - fade) / size.height).coerceIn(0f, 1f) to Color.White, 1f to Color.Transparent)
            onDrawWithContent { drawContent(); drawRect(mask, blendMode = BlendMode.DstIn) }
        }
    }
}
