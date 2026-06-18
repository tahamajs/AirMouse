package com.airmouse.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.airmouse.R

// ==================== Color Schemes ====================

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
    secondaryContainer = Color(0xFF1A2B1A),
    error = Color(0xFFEF4444),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onError = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFFE5E7EB)
)

val PureBlackColorScheme = darkColorScheme(
    primary = Color(0xFF00BCD4),
    secondary = Color(0xFF4CAF50),
    tertiary = Color(0xFFFF5722),
    background = Color(0xFF000000),
    surface = Color(0xFF0A0A0A),
    surfaceVariant = Color(0xFF1A1A1A),
    onBackground = Color(0xFFF0F0F0),
    onSurface = Color(0xFFF0F0F0),
    primaryContainer = Color(0xFF001A1E),
    secondaryContainer = Color(0xFF001A00),
    error = Color(0xFFEF4444),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onError = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFFE5E7EB),
    onSecondaryContainer = Color(0xFFE5E7EB)
)

val HighContrastColorScheme = darkColorScheme(
    primary = Color(0xFFFFC107),
    secondary = Color(0xFF00E676),
    tertiary = Color(0xFFFF5252),
    background = Color(0xFF000000),
    surface = Color(0xFF121212),
    surfaceVariant = Color(0xFF1E1E1E),
    onBackground = Color(0xFFFFFFFF),
    onSurface = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF1A1A00),
    secondaryContainer = Color(0xFF001A00),
    error = Color(0xFFFF1744),
    onPrimary = Color(0xFF000000),
    onSecondary = Color(0xFF000000),
    onTertiary = Color(0xFF000000),
    onError = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFFFFFFFF),
    onSecondaryContainer = Color(0xFFFFFFFF)
)

val LightColorScheme = lightColorScheme(
    primary = Color(0xFF00838F),
    secondary = Color(0xFF2E7D32),
    tertiary = Color(0xFFE64A19),
    background = Color(0xFFF8FAFC),
    surface = Color(0xFFFFFFFF),
    surfaceVariant = Color(0xFFF1F5F9),
    onBackground = Color(0xFF1E293B),
    onSurface = Color(0xFF1E293B),
    primaryContainer = Color(0xFFE0F7FA),
    secondaryContainer = Color(0xFFE8F5E9),
    error = Color(0xFFD32F2F),
    onPrimary = Color(0xFFFFFFFF),
    onSecondary = Color(0xFFFFFFFF),
    onTertiary = Color(0xFFFFFFFF),
    onError = Color(0xFFFFFFFF),
    onPrimaryContainer = Color(0xFF1E293B),
    onSecondaryContainer = Color(0xFF1E293B)
)

// ==================== Typography ====================

private val InterFontFamily = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
    Font(R.font.inter_bold, FontWeight.Bold)
)

// Base typography definitions without copying MaterialTheme.typography directly outside context
val AirMouseTypography = Typography(
    displayLarge = Typography().displayLarge.copy(fontFamily = InterFontFamily),
    displayMedium = Typography().displayMedium.copy(fontFamily = InterFontFamily),
    displaySmall = Typography().displaySmall.copy(fontFamily = InterFontFamily),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = InterFontFamily),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = InterFontFamily),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = InterFontFamily),
    titleLarge = Typography().titleLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = InterFontFamily)
)

// ==================== Shapes ====================

val AirMouseShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(24.dp),
    extraLarge = RoundedCornerShape(32.dp)
)

// ==================== Theme Configuration ====================

@Composable
fun AirMouseTheme(
    theme: String = "system",
    useDynamicColor: Boolean = true,
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    val actualAppTheme = when (theme) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "pure_black" -> AppTheme.PURE_BLACK
        "high_contrast" -> AppTheme.HIGH_CONTRAST
        "dynamic" -> AppTheme.DYNAMIC
        else -> AppTheme.SYSTEM
    }

    val isDarkTheme = when (actualAppTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
        AppTheme.PURE_BLACK -> true
        AppTheme.HIGH_CONTRAST -> false
        AppTheme.SYSTEM -> darkTheme
        AppTheme.DYNAMIC -> darkTheme
    }

    val colorScheme = when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDarkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        actualAppTheme == AppTheme.PURE_BLACK -> PureBlackColorScheme
        actualAppTheme == AppTheme.HIGH_CONTRAST -> HighContrastColorScheme
        isDarkTheme -> DarkColorScheme
        else -> LightColorScheme
    }

    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            // Replaces deprecated Accompanist systemUiController
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = if (isDarkTheme) Color(0xFF0F1115).toArgb() else Color(0xFFF8FAFC).toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !isDarkTheme
            insetsController.isAppearanceLightNavigationBars = !isDarkTheme

            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AirMouseTypography,
        shapes = AirMouseShapes,
        content = content
    )
}

// ==================== Theme State Management ====================

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

// ==================== Theme Preview Helpers ====================

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
    Column {
        for (theme in themes) {
            AirMouseTheme(theme = theme) {
                Surface(
                    color = MaterialTheme.colorScheme.background
                ) {
                    Text(
                        text = "Theme: $theme",
                        style = MaterialTheme.typography.headlineMedium,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                }
            }
        }
    }
}

// ==================== AppTheme Enum ====================

enum class AppTheme {
    SYSTEM, LIGHT, DARK, PURE_BLACK, HIGH_CONTRAST, DYNAMIC
}

// ==================== Helper Ext/Functions ====================

fun getThemeFromString(theme: String): AppTheme {
    return when (theme.lowercase()) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "pure_black" -> AppTheme.PURE_BLACK
        "high_contrast" -> AppTheme.HIGH_CONTRAST
        "dynamic" -> AppTheme.DYNAMIC
        else -> AppTheme.SYSTEM
    }
}

@Composable
fun getAdaptiveColorScheme(theme: String, useDynamicColor: Boolean): ColorScheme {
    val context = LocalContext.current
    val actualTheme = getThemeFromString(theme)
    val isDark = when (actualTheme) {
        AppTheme.LIGHT -> false
        AppTheme.DARK, AppTheme.PURE_BLACK -> true
        else -> isSystemInDarkTheme()
    }

    return when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        actualTheme == AppTheme.PURE_BLACK -> PureBlackColorScheme
        actualTheme == AppTheme.HIGH_CONTRAST -> HighContrastColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }
}