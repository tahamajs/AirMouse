
package com.airmouse.presentation.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.text.TextStyle
import androidx.core.view.WindowCompat
import com.airmouse.presentation.ui.themes.AccentColor
import com.airmouse.presentation.ui.themes.ThemeColorScheme
import com.airmouse.presentation.ui.themes.getThemeColorScheme
import android.R.attr.lineHeight



private val InterFontFamily = FontFamily.SansSerif

private fun Typography.sizedBy(scale: Float): Typography = Typography(
    displayLarge = displayLarge.scaled(scale),
    displayMedium = displayMedium.scaled(scale),
    displaySmall = displaySmall.scaled(scale),
    headlineLarge = headlineLarge.scaled(scale),
    headlineMedium = headlineMedium.scaled(scale),
    headlineSmall = headlineSmall.scaled(scale),
    titleLarge = titleLarge.scaled(scale),
    titleMedium = titleMedium.scaled(scale),
    titleSmall = titleSmall.scaled(scale),
    bodyLarge = bodyLarge.scaled(scale),
    bodyMedium = bodyMedium.scaled(scale),
    bodySmall = bodySmall.scaled(scale),
    labelLarge = labelLarge.scaled(scale),
    labelMedium = labelMedium.scaled(scale),
    labelSmall = labelSmall.scaled(scale)
)

private fun TextStyle.scaled(scale: Float) = copy(
    fontSize = fontSize * scale,
    lineHeight = lineHeight * scale
)

val LocalAppFontScale = staticCompositionLocalOf { 1f }

val AirMouseTypography = Typography().copy(
    displayLarge = Typography().displayLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    displayMedium = Typography().displayMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    displaySmall = Typography().displaySmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    headlineLarge = Typography().headlineLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    headlineSmall = Typography().headlineSmall.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleLarge = Typography().titleLarge.copy(fontFamily = InterFontFamily, fontWeight = FontWeight.SemiBold),
    titleMedium = Typography().titleMedium.copy(fontFamily = InterFontFamily),
    titleSmall = Typography().titleSmall.copy(fontFamily = InterFontFamily),
    bodyLarge = Typography().bodyLarge.copy(fontFamily = InterFontFamily),
    bodyMedium = Typography().bodyMedium.copy(fontFamily = InterFontFamily),
    bodySmall = Typography().bodySmall.copy(fontFamily = InterFontFamily),
    labelLarge = Typography().labelLarge.copy(fontFamily = InterFontFamily),
    labelMedium = Typography().labelMedium.copy(fontFamily = InterFontFamily),
    labelSmall = Typography().labelSmall.copy(fontFamily = InterFontFamily)
).sizedBy(0.92f)



val AirMouseShapes = Shapes(
    extraSmall = RoundedCornerShape(4.dp),
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(16.dp),
    extraLarge = RoundedCornerShape(24.dp)
)



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



@Composable
fun AirMouseTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    useDynamicColor: Boolean = true,
    themeColors: ThemeColorScheme? = null,
    themeId: String = "system",
    accentColor: AccentColor = AccentColor.ORANGE,
    fontScale: Float = 1f,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val view = LocalView.current

    
    val colors = themeColors ?: remember(themeId, accentColor) {
        getThemeColorScheme(themeId, accentColor)
    }

    
    val colorScheme = if (useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
    } else {
        if (darkTheme) {
            darkColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primaryContainer,
                onPrimaryContainer = colors.onPrimaryContainer,
                secondary = colors.secondary,
                onSecondary = colors.onSecondary,
                secondaryContainer = colors.secondaryContainer,
                onSecondaryContainer = colors.onSecondaryContainer,
                tertiary = colors.tertiary,
                onTertiary = colors.onTertiary,
                tertiaryContainer = colors.tertiaryContainer,
                onTertiaryContainer = colors.onTertiaryContainer,
                error = colors.error,
                onError = colors.onError,
                errorContainer = colors.errorContainer,
                onErrorContainer = colors.onErrorContainer,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                surfaceVariant = colors.surfaceVariant,
                onSurfaceVariant = colors.onSurfaceVariant,
                outline = colors.outline,
                outlineVariant = colors.outlineVariant,
                scrim = colors.scrim,
                inverseSurface = colors.inverseSurface,
                inverseOnSurface = colors.inverseOnSurface,
                inversePrimary = colors.inversePrimary,
                surfaceTint = colors.surfaceTint
            )
        } else {
            lightColorScheme(
                primary = colors.primary,
                onPrimary = colors.onPrimary,
                primaryContainer = colors.primaryContainer,
                onPrimaryContainer = colors.onPrimaryContainer,
                secondary = colors.secondary,
                onSecondary = colors.onSecondary,
                secondaryContainer = colors.secondaryContainer,
                onSecondaryContainer = colors.onSecondaryContainer,
                tertiary = colors.tertiary,
                onTertiary = colors.onTertiary,
                tertiaryContainer = colors.tertiaryContainer,
                onTertiaryContainer = colors.onTertiaryContainer,
                error = colors.error,
                onError = colors.onError,
                errorContainer = colors.errorContainer,
                onErrorContainer = colors.onErrorContainer,
                background = colors.background,
                onBackground = colors.onBackground,
                surface = colors.surface,
                onSurface = colors.onSurface,
                surfaceVariant = colors.surfaceVariant,
                onSurfaceVariant = colors.onSurfaceVariant,
                outline = colors.outline,
                outlineVariant = colors.outlineVariant,
                scrim = colors.scrim,
                inverseSurface = colors.inverseSurface,
                inverseOnSurface = colors.inverseOnSurface,
                inversePrimary = colors.inversePrimary,
                surfaceTint = colors.surfaceTint
            )
        }
    }

    
    SideEffect {
        val window = (view.context as? Activity)?.window
        if (window != null) {
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = colors.background.toArgb()

            val insetsController = WindowCompat.getInsetsController(window, view)
            insetsController.isAppearanceLightStatusBars = !darkTheme
            insetsController.isAppearanceLightNavigationBars = !darkTheme

            WindowCompat.setDecorFitsSystemWindows(window, false)
        }
    }

    CompositionLocalProvider(LocalAppFontScale provides fontScale) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = AirMouseTypography.sizedBy(fontScale.coerceIn(0.85f, 1.20f)),
            shapes = AirMouseShapes,
            content = content
        )
    }
}



@Composable
fun AirMouseThemeLegacy(
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



@Composable
fun rememberThemeState(
    initialTheme: String = "system",
    initialAccent: AccentColor = AccentColor.ORANGE,
    onThemeChange: (String) -> Unit = {},
    onAccentChange: (AccentColor) -> Unit = {}
): ThemeState {
    return remember {
        ThemeState(initialTheme, initialAccent, onThemeChange, onAccentChange)
    }
}

class ThemeState(
    initialTheme: String,
    initialAccent: AccentColor,
    private val onThemeChange: (String) -> Unit,
    private val onAccentChange: (AccentColor) -> Unit
) {
    var currentTheme by mutableStateOf(initialTheme)
        private set

    var currentAccent by mutableStateOf(initialAccent)
        private set

    fun updateTheme(theme: String) {
        currentTheme = theme
        onThemeChange(theme)
    }

    fun updateAccent(accent: AccentColor) {
        currentAccent = accent
        onAccentChange(accent)
    }

    @Composable
    fun isDarkTheme(): Boolean {
        return when (currentTheme) {
            "light" -> false
            "high_contrast" -> false
            else -> true
        }
    }

    @Composable
    fun getThemeColors(): ThemeColorScheme {
        return getThemeColorScheme(currentTheme, currentAccent)
    }
}



enum class AppTheme {
    SYSTEM, LIGHT, DARK, PURE_BLACK, HIGH_CONTRAST, DYNAMIC
}



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

fun getThemeIdFromAppTheme(appTheme: AppTheme): String {
    return when (appTheme) {
        AppTheme.SYSTEM -> "system"
        AppTheme.LIGHT -> "light"
        AppTheme.DARK -> "dark"
        AppTheme.PURE_BLACK -> "pure_black"
        AppTheme.HIGH_CONTRAST -> "high_contrast"
        AppTheme.DYNAMIC -> "dynamic"
    }
}

@Composable
fun getAdaptiveColorScheme(
    theme: String = "system",
    accent: AccentColor = AccentColor.ORANGE,
    useDynamicColor: Boolean = true
): androidx.compose.material3.ColorScheme {
    val context = LocalContext.current
    val isDark = when (theme) {
        "light" -> false
        "high_contrast" -> false
        else -> true
    }
    return when {
        useDynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        theme == "pure_black" -> PureBlackColorScheme
        theme == "high_contrast" -> HighContrastColorScheme
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }
}



@Composable
fun AirMouseThemePreview(
    theme: String = "system",
    accent: AccentColor = AccentColor.ORANGE,
    content: @Composable () -> Unit
) {
    AirMouseTheme(themeId = theme, accentColor = accent) {
        content()
    }
}

@Composable
fun ThemePreview() {
    val themes = listOf("light", "dark", "pure_black", "high_contrast", "ocean", "sunset")
    val accents = listOf(AccentColor.ORANGE, AccentColor.BLUE, AccentColor.PURPLE)

    Column {
        for (theme in themes) {
            for (accent in accents.take(1)) {
                AirMouseTheme(themeId = theme, accentColor = accent) {
                    Surface(
                        color = MaterialTheme.colorScheme.background,
                        modifier = androidx.compose.ui.Modifier.padding(16.dp)
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
}



object DefaultThemeConfig {
    const val DEFAULT_THEME = "system"
    val DEFAULT_ACCENT = AccentColor.ORANGE
}
