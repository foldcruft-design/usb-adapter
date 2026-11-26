package com.example.ui.components

import android.os.Build
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.ui.theme.SilverBgBottom
import com.example.ui.theme.SilverBgMid
import com.example.ui.theme.SilverBgTop
import com.kyant.backdrop.backdrops.LayerBackdrop
import com.kyant.backdrop.drawBackdrop
import com.kyant.backdrop.effects.blur
import com.kyant.backdrop.effects.lens
import com.kyant.backdrop.effects.vibrancy

val LocalGlassBackdrop = compositionLocalOf<LayerBackdrop?> { null }
val LocalChromeBackdrop = compositionLocalOf<LayerBackdrop?> { null }

/**
 * Applies Kyant0's Liquid Glass (Backdrop) on Android 13+ (API 33+) with AGSL refraction,
 * blur, vibrancy, and milky white tint, plus clean frosted fallback on older Android versions.
 */
fun Modifier.liquidGlass(
    backdrop: LayerBackdrop?,
    shape: Shape = RoundedCornerShape(22.dp),
    fillAlpha: Float = 0.45f,
    blurRadius: Dp = 6.dp,
    useLens: Boolean = true,
    refractionHeight: Dp = 14.dp,
    refractionAmount: Dp = 28.dp,
    borderAlpha: Float = 0.78f
): Modifier {
    return if (backdrop != null && Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        this
            .drawBackdrop(
                backdrop = backdrop,
                shape = { shape },
                effects = {
                    vibrancy()
                    blur(blurRadius.toPx())
                    if (useLens) {
                        lens(refractionHeight.toPx(), refractionAmount.toPx())
                    }
                },
                onDrawSurface = {
                    drawRect(Color.White.copy(alpha = fillAlpha))
                }
            )
            .border(
                width = 1.dp,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color.White.copy(alpha = borderAlpha),
                        Color(0xFFC8CFD9).copy(alpha = borderAlpha * 0.65f),
                        Color.White.copy(alpha = borderAlpha * 0.85f)
                    )
                ),
                shape = shape
            )
    } else {
        val fallbackAlpha = (fillAlpha + 0.28f).coerceIn(0.60f, 0.90f)
        this
            .clip(shape)
            .background(Color.White.copy(alpha = fallbackAlpha), shape)
            .border(
                width = 1.dp,
                color = Color.White.copy(alpha = 0.92f),
                shape = shape
            )
    }
}

/**
 * Soft silver-gray atmospheric background with subtle cool-gray & slate-pearl shapes
 * so Liquid Glass lenses have rich light/contrast gradients to refract.
 */
@Composable
fun SilverAmbientBackground(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(SilverBgTop, SilverBgMid, SilverBgBottom)
                )
            )
    ) {
        Canvas(modifier = Modifier.fillMaxSize()) {
            val w = size.width
            val h = size.height

            // Soft cool slate-silver orb top-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFBFC8D8).copy(alpha = 0.55f),
                        Color(0xFFBFC8D8).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.16f, h * 0.14f),
                    radius = w * 0.62f
                ),
                radius = w * 0.62f,
                center = Offset(w * 0.16f, h * 0.14f)
            )

            // Soft cool pearl-gray orb mid-right
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFAEBACB).copy(alpha = 0.50f),
                        Color(0xFFAEBACB).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.88f, h * 0.42f),
                    radius = w * 0.58f
                ),
                radius = w * 0.58f,
                center = Offset(w * 0.88f, h * 0.42f)
            )

            // Luminous white highlight orb center-left
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color.White.copy(alpha = 0.78f),
                        Color.White.copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.24f, h * 0.64f),
                    radius = w * 0.52f
                ),
                radius = w * 0.52f,
                center = Offset(w * 0.24f, h * 0.64f)
            )

            // Deep silver-slate orb bottom-right (behind bottom bar refraction)
            drawCircle(
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFB6C0D0).copy(alpha = 0.58f),
                        Color(0xFFB6C0D0).copy(alpha = 0.0f)
                    ),
                    center = Offset(w * 0.76f, h * 0.88f),
                    radius = w * 0.64f
                ),
                radius = w * 0.64f,
                center = Offset(w * 0.76f, h * 0.88f)
            )
        }
    }
}
