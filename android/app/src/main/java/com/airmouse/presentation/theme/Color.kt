package com.airmouse.presentation.theme

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext

// ==================== PRIMARY BRAND COLORS ====================

// Deep Orange (Primary)
val DeepOrange50 = Color(0xFFFBE9E7)
val DeepOrange100 = Color(0xFFFFCCBC)
val DeepOrange200 = Color(0xFFFFAB91)
val DeepOrange300 = Color(0xFFFF8A65)
val DeepOrange400 = Color(0xFFFF7043)
val DeepOrange500 = Color(0xFFFF5722)
val DeepOrange600 = Color(0xFFF4511E)
val DeepOrange700 = Color(0xFFE64A19)
val DeepOrange800 = Color(0xFFD84315)
val DeepOrange900 = Color(0xFFBF360C)
val DeepOrangeA100 = Color(0xFFFF9E80)
val DeepOrangeA200 = Color(0xFFFF6E40)
val DeepOrangeA400 = Color(0xFFFF3D00)
val DeepOrangeA700 = Color(0xFFDD2C00)

// ==================== SECONDARY / ACCENT COLORS ====================

// Amber (Secondary)
val Amber50 = Color(0xFFFFF8E1)
val Amber100 = Color(0xFFFFF8E1)
val Amber200 = Color(0xFFFFE082)
val Amber300 = Color(0xFFFFD54F)
val Amber400 = Color(0xFFFFCA28)
val Amber500 = Color(0xFFFFC107)
val Amber600 = Color(0xFFFFB300)
val Amber700 = Color(0xFFFFA000)
val Amber800 = Color(0xFFFF8F00)
val Amber900 = Color(0xFFF57F17)
val AmberA100 = Color(0xFFFFE57F)
val AmberA200 = Color(0xFFFFD740)
val AmberA400 = Color(0xFFFFC400)
val AmberA700 = Color(0xFFFFAB00)

// ==================== TERTIARY COLORS ====================

// Teal (Tertiary)
val Teal50 = Color(0xFFE0F2F1)
val Teal100 = Color(0xFFB2DFDB)
val Teal200 = Color(0xFF80CBC4)
val Teal300 = Color(0xFF4DB6AC)
val Teal400 = Color(0xFF26A69A)
val Teal500 = Color(0xFF009688)
val Teal600 = Color(0xFF00897B)
val Teal700 = Color(0xFF00796B)
val Teal800 = Color(0xFF00695C)
val Teal900 = Color(0xFF004D40)

// ==================== NEUTRAL COLORS ====================

// Dark Theme Neutral Colors
val DarkBackground = Color(0xFF0F1115)
val DarkSurface = Color(0xFF1D2430)
val DarkSurfaceVariant = Color(0xFF2B3341)
val DarkSurfaceTint = Color(0xFF3A4459)
val DarkOnSurface = Color(0xFFE5E7EB)
val DarkOnSurfaceMedium = Color(0xFF9CA3AF)
val DarkOnSurfaceVariant = Color(0xFF96A0AE)
val DarkSurfaceBright = Color(0xFF2D3748)
val DarkSurfaceDim = Color(0xFF141824)

// Light Theme Neutral Colors
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightSurfaceTint = Color(0xFFE2E8F0)
val LightOnSurface = Color(0xFF1E293B)
val LightOnSurfaceMedium = Color(0xFF64748B)
val LightOnSurfaceVariant = Color(0xFF94A3B8)
val LightSurfaceBright = Color(0xFFF8FAFC)
val LightSurfaceDim = Color(0xFFE2E8F0)

// ==================== SEMANTIC COLORS ====================

// Status Colors
val Success = Color(0xFF10B981)
val SuccessLight = Color(0xFFD1FAE5)
val SuccessDark = Color(0xFF059669)

val Warning = Color(0xFFF59E0B)
val WarningLight = Color(0xFFFEF3C7)
val WarningDark = Color(0xFFD97706)

val Error = Color(0xFFEF4444)
val ErrorLight = Color(0xFFFEE2E2)
val ErrorDark = Color(0xFFDC2626)

val Info = Color(0xFF3B82F6)
val InfoLight = Color(0xFFDBEAFE)
val InfoDark = Color(0xFF2563EB)

// Special Colors
val PureBlack = Color(0xFF000000)
val PureBlackSurface = Color(0xFF0A0A0A)
val PureBlackSurfaceVariant = Color(0xFF141414)

val HighContrastPrimary = Color(0xFF000000)
val HighContrastOnPrimary = Color(0xFFFFFFFF)
val HighContrastBackground = Color(0xFFFFFFFF)
val HighContrastSurface = Color(0xFFFFFFFF)
val HighContrastOnSurface = Color(0xFF000000)

// ==================== GRADIENT COLORS ====================

object Gradients {
    val primaryGradient = listOf(DeepOrange500, DeepOrange700)
    val secondaryGradient = listOf(Amber500, Amber700)
    val darkSurfaceGradient = listOf(DarkSurface, DarkSurfaceVariant)
    val successGradient = listOf(Success, SuccessDark)
    val errorGradient = listOf(Error, ErrorDark)
    val infoGradient = listOf(Info, InfoDark)
}

// ==================== ALPHA VARIANTS ====================

fun Color.withAlpha(alpha: Float): Color = copy(alpha = alpha)

val Color.transparent: Color get() = withAlpha(0f)
val Color.semiTransparent: Color get() = withAlpha(0.5f)
val Color.highlyTransparent: Color get() = withAlpha(0.2f)

// ==================== EXTENSION FUNCTIONS ====================

fun Color.isLight(): Boolean {
    val luminance = (0.299 * red + 0.587 * green + 0.114 * blue)
    return luminance > 0.5
}

fun Color.darken(factor: Float): Color {
    return Color(
        red = red * (1 - factor),
        green = green * (1 - factor),
        blue = blue * (1 - factor),
        alpha = alpha
    )
}

fun Color.lighten(factor: Float): Color {
    return Color(
        red = red + (1 - red) * factor,
        green = green + (1 - green) * factor,
        blue = blue + (1 - blue) * factor,
        alpha = alpha
    )
}

// ==================== MATERIAL 3 COLOR SCHEMES ====================

val LightColorScheme = lightColorScheme(
    primary = DeepOrange500,
    onPrimary = Color.White,
    primaryContainer = DeepOrange100,
    onPrimaryContainer = DeepOrange900,
    secondary = Amber500,
    onSecondary = Color.Black,
    secondaryContainer = Amber200,
    onSecondaryContainer = Amber900,
    tertiary = Teal500,
    onTertiary = Color.White,
    tertiaryContainer = Teal100,
    onTertiaryContainer = Teal900,
    background = LightBackground,
    onBackground = LightOnSurface,
    surface = LightSurface,
    onSurface = LightOnSurface,
    surfaceVariant = LightSurfaceVariant,
    onSurfaceVariant = LightOnSurfaceVariant,
    surfaceTint = DeepOrange500,
    inverseSurface = DarkSurface,
    inverseOnSurface = DarkOnSurface,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorLight,
    onErrorContainer = ErrorDark,
    outline = LightOnSurfaceVariant,
    outlineVariant = LightSurfaceTint,
    scrim = PureBlack,
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
    tertiary = Teal500,
    onTertiary = Color.White,
    tertiaryContainer = Teal700,
    onTertiaryContainer = Teal100,
    background = DarkBackground,
    onBackground = DarkOnSurface,
    surface = DarkSurface,
    onSurface = DarkOnSurface,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = DarkOnSurfaceVariant,
    surfaceTint = DeepOrange500,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight,
    outline = DarkOnSurfaceVariant,
    outlineVariant = DarkSurfaceVariant,
    scrim = PureBlack,
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
    tertiary = Teal500,
    onTertiary = Color.White,
    tertiaryContainer = Teal800,
    onTertiaryContainer = Teal100,
    background = PureBlack,
    onBackground = Color.White,
    surface = PureBlackSurface,
    onSurface = Color.White,
    surfaceVariant = PureBlackSurfaceVariant,
    onSurfaceVariant = Color.Gray,
    surfaceTint = DeepOrange500,
    inverseSurface = LightSurface,
    inverseOnSurface = LightOnSurface,
    error = Error,
    onError = Color.White,
    errorContainer = ErrorDark,
    onErrorContainer = ErrorLight,
    outline = Color.Gray,
    outlineVariant = PureBlackSurfaceVariant,
    scrim = PureBlack,
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
    tertiary = HighContrastPrimary,
    onTertiary = HighContrastOnPrimary,
    tertiaryContainer = HighContrastPrimary,
    onTertiaryContainer = HighContrastOnPrimary,
    background = HighContrastBackground,
    onBackground = HighContrastOnSurface,
    surface = HighContrastSurface,
    onSurface = HighContrastOnSurface,
    surfaceVariant = HighContrastSurface,
    onSurfaceVariant = HighContrastOnSurface,
    error = Error,
    onError = Color.White,
    errorContainer = Error,
    onErrorContainer = Color.White,
    outline = HighContrastOnSurface,
    outlineVariant = HighContrastOnSurface,
    scrim = HighContrastOnSurface,
)

// ==================== DYNAMIC COLOR SCHEMES ====================

@Composable
fun getDynamicColorScheme(theme: String): ColorScheme {
    val context = LocalContext.current
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        when (theme) {
            "light" -> dynamicLightColorScheme(context)
            "dark" -> dynamicDarkColorScheme(context)
            else -> if (isSystemInDarkTheme()) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
    } else {
        getStaticColorScheme(theme)
    }
}

// ==================== STATIC COLOR SCHEME HELPER ====================

fun getStaticColorScheme(theme: String): ColorScheme {
    return when (theme) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "pure_black" -> PureBlackColorScheme
        "high_contrast" -> HighContrastColorScheme
        else -> if (isSystemInDarkTheme()) DarkColorScheme else LightColorScheme
    }
}

// ==================== EXTENDED COLOR SCHEME ====================

data class ExtendedColorScheme(
    val colorScheme: ColorScheme,
    val gradientColors: List<Color> = Gradients.primaryGradient,
    val success: Color = Success,
    val warning: Color = Warning,
    val error: Color = Error,
    val info: Color = Info,
    val surfaceBright: Color,
    val surfaceDim: Color,
    val onSurfaceMedium: Color
)

@Composable
fun ColorScheme.toExtended(): ExtendedColorScheme {
    val isDark = background == DarkBackground || background == PureBlack
    return ExtendedColorScheme(
        colorScheme = this,
        success = Success,
        warning = Warning,
        error = Error,
        info = Info,
        surfaceBright = if (isDark) DarkSurfaceBright else LightSurfaceBright,
        surfaceDim = if (isDark) DarkSurfaceDim else LightSurfaceDim,
        onSurfaceMedium = if (isDark) DarkOnSurfaceMedium else LightOnSurfaceMedium
    )
}

// ==================== THEME TYPES ====================

enum class AppTheme(val value: String) {
    LIGHT("light"),
    DARK("dark"),
    PURE_BLACK("pure_black"),
    HIGH_CONTRAST("high_contrast"),
    SYSTEM("system"),
    DYNAMIC("dynamic")
}

fun getThemeFromString(theme: String): AppTheme {
    return when (theme) {
        "light" -> AppTheme.LIGHT
        "dark" -> AppTheme.DARK
        "pure_black" -> AppTheme.PURE_BLACK
        "high_contrast" -> AppTheme.HIGH_CONTRAST
        "dynamic" -> AppTheme.DYNAMIC
        else -> AppTheme.SYSTEM
    }
}

// ==================== PREVIEW COLORS ====================

object PreviewColors {
    val primaryVariants = listOf(
        DeepOrange500, DeepOrange600, DeepOrange700,
        DeepOrange800, DeepOrange900
    )

    val secondaryVariants = listOf(
        Amber500, Amber600, Amber700,
        Amber800, Amber900
    )

    val neutralVariants = listOf(
        LightOnSurface, LightOnSurfaceMedium, LightOnSurfaceVariant,
        DarkOnSurface, DarkOnSurfaceMedium, DarkOnSurfaceVariant
    )

    val semanticColors = listOf(
        Success, Warning, Error, Info
    )
}

// ==================== COLOR UTILITIES ====================

object ColorUtils {
    fun blend(color1: Color, color2: Color, ratio: Float): Color {
        val r = color1.red * (1 - ratio) + color2.red * ratio
        val g = color1.green * (1 - ratio) + color2.green * ratio
        val b = color1.blue * (1 - ratio) + color2.blue * ratio
        val a = color1.alpha * (1 - ratio) + color2.alpha * ratio
        return Color(r, g, b, a)
    }

    fun getContrastColor(backgroundColor: Color): Color {
        return if (backgroundColor.isLight()) Color.Black else Color.White
    }
}

// ==================== HELPER FUNCTIONS ====================

@Composable
fun getStaticColorScheme(theme: String, isDarkTheme: Boolean): ColorScheme {
    return when (theme) {
        "light" -> LightColorScheme
        "dark" -> DarkColorScheme
        "pure_black" -> PureBlackColorScheme
        "high_contrast" -> HighContrastColorScheme
        else -> if (isDarkTheme) DarkColorScheme else LightColorScheme
    }
}

@Composable
fun getColorScheme(theme: String, useDynamicColors: Boolean = false): ColorScheme {
    val context = LocalContext.current
    val isDarkTheme = isSystemInDarkTheme()

    return if (useDynamicColors && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        if (isDarkTheme) dynamicDarkColorScheme(context)
        else dynamicLightColorScheme(context)
    } else {
        getStaticColorScheme(theme, isDarkTheme)
    }
}

@Composable
fun getExtendedColorScheme(theme: String, useDynamicColors: Boolean = false): ExtendedColorScheme {
    return getColorScheme(theme, useDynamicColors).toExtended()
}