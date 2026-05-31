// app/src/main/java/com/airmouse/presentation/theme/Color.kt
package com.airmouse.presentation.theme

import androidx.compose.ui.graphics.Color

// Primary Brand Colors
val DeepOrange400 = Color(0xFFFF7043)
val DeepOrange500 = Color(0xFFFF5722)
val DeepOrange600 = Color(0xFFF4511E)
val DeepOrange700 = Color(0xFFE64A19)

// Secondary / Accent Colors
val Amber400 = Color(0xFFFFCA28)
val Amber500 = Color(0xFFFFC107)
val Amber600 = Color(0xFFFFB300)

// Neutral Colors for Dark Theme
val DarkBackground = Color(0xFF0F1115)
val DarkSurface = Color(0xFF1D2430)
val DarkSurfaceVariant = Color(0xFF2B3341)
val DarkOnSurface = Color(0xFFE5E7EB)
val DarkOnSurfaceVariant = Color(0xFF96A0AE)

// Neutral Colors for Light Theme
val LightBackground = Color(0xFFF5F5F5)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF0F0F0)
val LightOnSurface = Color(0xFF1F2937)
val LightOnSurfaceVariant = Color(0xFF6B7280)

// Common Colors
val Success = Color(0xFF4CAF50)
val Warning = Color(0xFFFF9800)
val Error = Color(0xFFEF5350)
val Info = Color(0xFF2196F3)

// Pure Black (AMOLED)
val PureBlack = Color(0xFF000000)
val PureBlackSurface = Color(0xFF0A0A0A)

// High Contrast (Accessibility)
val HighContrastPrimary = Color(0xFF000000)
val HighContrastOnPrimary = Color(0xFFFFFFFF)
val HighContrastBackground = Color(0xFFFFFFFF)
val HighContrastSurface = Color(0xFFFFFFFF)
val HighContrastOnSurface = Color(0xFF000000)

// Dynamic Color (Android 12+ will be provided by MaterialTheme.colorScheme)

// Material 3 Color Schemes
val LightColorScheme = lightColorScheme(
    primary = DeepOrange500,
    onPrimary = Color.White,
    primaryContainer = DeepOrange100,
    onPrimaryContainer = DeepOrange900,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber200,
    onSecondaryContainer = Amber900,
    tertiary = Info,
    onTertiary = Color.White,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    error = Error,
    onError = Color.White,
)

val DarkColorScheme = darkColorScheme(
    primary = DeepOrange500,
    onPrimary = Color.White,
    primaryContainer = DeepOrange800,
    onPrimaryContainer = DeepOrange100,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber800,
    onSecondaryContainer = Amber100,
    tertiary = Info,
    onTertiary = Color.White,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    error = Error,
    onError = Color.White,
)

val PureBlackColorScheme = darkColorScheme(
    primary = DeepOrange500,
    onPrimary = Color.White,
    primaryContainer = DeepOrange800,
    onPrimaryContainer = DeepOrange100,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber800,
    onSecondaryContainer = Amber100,
    tertiary = Info,
    onTertiary = Color.White,
    background = PureBlack,
    onBackground = Color.White,
    surface = PureBlackSurface,
    onSurface = Color.White,
    surfaceVariant = PureBlackSurface,
    onSurfaceVariant = Color.Gray,
    error = Error,
    onError = Color.White,
)

val HighContrastColorScheme = lightColorScheme(
    primary = HighContrastPrimary,
    onPrimary = HighContrastOnPrimary,
    primaryContainer = HighContrastPrimary,
    onPrimaryContainer = HighContrastOnPrimary,
    secondary = HighContrastPrimary,
    onSecondary = HighContrastOnPrimary,
    secondaryContainer = HighContrastPrimary,
    onSecondaryContainer = HighContrastOnPrimary,
    background = HighContrastBackground,
    onBackground = HighContrastOnSurface,
    surface = HighContrastSurface,
    onSurface = HighContrastOnSurface,
    surfaceVariant = HighContrastSurface,
    onSurfaceVariant = HighContrastOnSurface,
    error = Error,
    onError = Color.White,
)

// Helper function to get the appropriate color scheme
fun getColorScheme(theme: String, dynamicColor: Boolean = false): ColorScheme {
    return when (theme) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "pure_black" -> PureBlackColorScheme
        "high_contrast" -> HighContrastColorScheme
        else -> DarkColorScheme
    }
}