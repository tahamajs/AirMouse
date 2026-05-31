// app/src/main/java/com/airmouse/presentation/theme/Theme.kt
package com.airmouse.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF007ACC),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFFFF5722),
    background = Color(0xFF0F1115),
    surface = Color(0xFF1D2430),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB)
)

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF007ACC),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFFFF5722),
    background = Color(0xFFF5F5F5),
    surface = Color.White,
    onPrimary = Color.White,
    onSecondary = Color.Black,
    onBackground = Color.Black,
    onSurface = Color.Black
)

@Composable
fun AirMouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(),
        content = content
    )
}