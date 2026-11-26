package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

private val SilverGlassColorScheme = lightColorScheme(
    primary = GraphitePrimary,
    onPrimary = Color(0xFFF8F9FB),
    primaryContainer = Color(0xFFE2E6EE),
    onPrimaryContainer = InkCharcoal,
    secondary = GraphiteSecondary,
    onSecondary = Color(0xFFF8F9FB),
    secondaryContainer = Color(0xFFE8ECF2),
    onSecondaryContainer = InkCharcoal,
    tertiary = SteelAccent,
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFE5E9F0),
    onTertiaryContainer = InkCharcoal,
    background = Color.Transparent,
    onBackground = InkCharcoal,
    surface = Color(0xFFF6F7FA),
    onSurface = InkCharcoal,
    surfaceVariant = Color(0xFFE8EBF2),
    onSurfaceVariant = InkSlateGray,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F7FA),
    surfaceContainer = Color(0xFFECEFF4),
    surfaceContainerHigh = Color(0xFFE4E8F0),
    surfaceContainerHighest = Color(0xFFDCE1EA),
    outline = GlassBorderSubtle,
    outlineVariant = Color(0xFFE3E7EE),
    error = CoralDanger,
    onError = Color.White,
    errorContainer = CoralContainer,
    onErrorContainer = Color(0xFF7A1224)
)

val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(26.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

@Composable
fun OtgDriveTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = SilverGlassColorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}
