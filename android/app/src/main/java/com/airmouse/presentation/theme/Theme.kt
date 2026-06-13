// app/src/main/java/com/airmouse/presentation/theme/Theme.kt
package com.airmouse.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import com.google.accompanist.systemuicontroller.rememberSystemUiController

@Composable
fun AirMouseTheme(
    theme: String = "system",
    useDynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemUiController = rememberSystemUiController()
    val view = LocalView.current

    val actualAppTheme = getThemeFromString(theme)

    // FIXED: Corrected variable name
    val isDarkTheme = when (actualAppTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.PURE_BLACK -> true
        AppTheme.HIGH_CONTRAST -> false
        AppTheme.SYSTEM -> darkTheme
        AppTheme.DYNAMIC -> darkTheme
    }

    val colorScheme = getColorScheme(theme, useDynamicColor)

    SideEffect {
        val window = (view.context as? androidx.activity.ComponentActivity)?.window
        if (window != null) {
            systemUiController.setStatusBarColor(
                color = Color.Transparent,
                darkIcons = !isDarkTheme
            )
            systemUiController.setNavigationBarColor(
                color = colorScheme.background,
                darkIcons = !isDarkTheme
            )
            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

@Composable
fun rememberThemeState(
    initialTheme: String = "system",
    onThemeChange: (String) -> Unit = {}
): ThemeState {
    return remember {
        ThemeState(initialTheme, onThemeChange)
    }
}

class ThemeState(
    initialTheme: String,
    private val onThemeChange: (String) -> Unit
) {
    var currentTheme by mutableStateOf(initialTheme)
        private set

    fun updateTheme(theme: String) {
        currentTheme = theme
        onThemeChange(theme)
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return when (currentTheme) {
            "light" -> false
            "dark", "pure_black" -> true
            else -> isSystemInDarkTheme()
        }
    }
}

@Composable
fun AirMouseThemePreview(
    theme: String = "system",
    content: @Composable () -> Unit
) {
    AirMouseTheme(theme = theme) {
        content()
    }
}

@Composable
fun ThemePreview() {
    val themes = listOf("light", "dark", "pure_black", "high_contrast")
    for (theme in themes) {
        AirMouseTheme(theme = theme) {
            androidx.compose.material3.Surface(
                color = MaterialTheme.colorScheme.background
            ) {
                androidx.compose.material3.Text(
                    text = "Theme: $theme",
                    style = MaterialTheme.typography.headlineMedium
                )
            }
        }
    }
}


// app/src/main/java/com/airmouse/presentation/theme/Theme.kt (Enhanced)
package com.airmouse.presentation.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

// Custom Color Scheme
val DarkColorScheme = darkColorScheme(
    primary = Color(0xFF00BCD4),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFFFF5722),
    background = Color(0xFF0F1115),
    surface = Color(0xFF1A1D24),
    surfaceVariant = Color(0xFF2B3341),
    onBackground = Color(0xFFE5E7EB),
    onSurface = Color(0xFFE5E7EB),
    primaryContainer = Color(0xFF1A2B3C),
    secondaryContainer = Color(0xFF1A2B1A)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFFE64A19),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B)
)

@Composable
fun AirMouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography(
            displayLarge = MaterialTheme.typography.displayLarge.copy(fontFamily = interFont),
            displayMedium = MaterialTheme.typography.displayMedium.copy(fontFamily = interFont),
            displaySmall = MaterialTheme.typography.displaySmall.copy(fontFamily = interFont),
            headlineLarge = MaterialTheme.typography.headlineLarge.copy(fontFamily = interFont),
            headlineMedium = MaterialTheme.typography.headlineMedium.copy(fontFamily = interFont),
            headlineSmall = MaterialTheme.typography.headlineSmall.copy(fontFamily = interFont),
            titleLarge = MaterialTheme.typography.titleLarge.copy(fontFamily = interFont, fontWeight = androidx.compose.ui.text.font.FontWeight.SemiBold),
            titleMedium = MaterialTheme.typography.titleMedium.copy(fontFamily = interFont),
            titleSmall = MaterialTheme.typography.titleSmall.copy(fontFamily = interFont),
            bodyLarge = MaterialTheme.typography.bodyLarge.copy(fontFamily = interFont),
            bodyMedium = MaterialTheme.typography.bodyMedium.copy(fontFamily = interFont),
            bodySmall = MaterialTheme.typography.bodySmall.copy(fontFamily = interFont),
            labelLarge = MaterialTheme.typography.labelLarge.copy(fontFamily = interFont),
            labelMedium = MaterialTheme.typography.labelMedium.copy(fontFamily = interFont),
            labelSmall = MaterialTheme.typography.labelSmall.copy(fontFamily = interFont)
        ),
        shapes = Shapes(
            extraSmall = RoundedCornerShape(8.dp),
            small = RoundedCornerShape(12.dp),
            medium = RoundedCornerShape(16.dp),
            large = RoundedCornerShape(24.dp),
            extraLarge = RoundedCornerShape(32.dp)
        ),
        content = content
    )
}

private val interFont = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(
        androidx.compose.ui.res.fontResource(R.font.inter_regular),
        weight = androidx.compose.ui.text.font.FontWeight.Normal
    ),
    androidx.compose.ui.text.font.Font(
        androidx.compose.ui.res.fontResource(R.font.inter_medium),
        weight = androidx.compose.ui.text.font.FontWeight.Medium
    ),
    androidx.compose.ui.text.font.Font(
        androidx.compose.ui.res.fontResource(R.font.inter_semibold),
        weight = androidx.compose.ui.text.font.FontWeight.SemiBold
    ),
    androidx.compose.ui.text.font.Font(
        androidx.compose.ui.res.fontResource(R.font.inter_bold),
        weight = androidx.compose.ui.text.font.FontWeight.Bold
    )
)