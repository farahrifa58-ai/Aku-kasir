package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp

private val LightColorScheme = lightColorScheme(
    primary = BrandBlue,
    onPrimary = Color.White,
    primaryContainer = BrandBlueSoft,
    onPrimaryContainer = BrandBlueDark,
    secondary = BrandGreen,
    onSecondary = Color.White,
    secondaryContainer = BrandGreenSoft,
    onSecondaryContainer = BrandGreenDark,
    tertiary = BrandGreenDark,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnBackground,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    outline = LightOutline,
    outlineVariant = LightOutline.copy(alpha = 0.6f),
    error = StatusError,
    onError = Color.White,
    errorContainer = StatusErrorSoft,
    onErrorContainer = StatusError
)

private val DarkColorScheme = darkColorScheme(
    primary = BrandBlueLight,
    onPrimary = Color(0xFF002B5B),
    primaryContainer = Color(0xFF173A69),
    onPrimaryContainer = Color(0xFFD8E8FF),
    secondary = BrandGreen,
    onSecondary = Color(0xFF003824),
    secondaryContainer = Color(0xFF064E3B),
    onSecondaryContainer = Color(0xFFA7F3D0),
    tertiary = BrandGreen,
    onTertiary = Color(0xFF003824),
    background = DarkBackground,
    onBackground = DarkOnBackground,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    outline = DarkOutline,
    outlineVariant = DarkOutline.copy(alpha = 0.6f),
    error = StatusError,
    onError = Color.White,
    errorContainer = Color(0xFF45121B),
    onErrorContainer = Color(0xFFFECACA)
)

val AdaKasirShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp)
)

@Composable
fun AdaKasirTheme(
    darkTheme: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    val baseDensity = LocalDensity.current
    val lockedDensity = Density(density = baseDensity.density, fontScale = 1.0f)

    CompositionLocalProvider(LocalDensity provides lockedDensity) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AdaKasirTypography,
            shapes = AdaKasirShapes,
            content = content
        )
    }
}
