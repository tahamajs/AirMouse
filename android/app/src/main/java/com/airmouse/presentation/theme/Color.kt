// app/src/main/java/com/airmouse/presentation/theme/Colors.kt
package com.airmouse.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver

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

// ==================== SECONDARY / ACCENT COLORS ====================

// Amber (Secondary)
val Amber50 = Color(0xFFFFF8E1)
val Amber100 = Color(0xFFFFECB3)
val Amber200 = Color(0xFFFFE082)
val Amber300 = Color(0xFFFFD54F)
val Amber400 = Color(0xFFFFCA28)
val Amber500 = Color(0xFFFFC107)
val Amber600 = Color(0xFFFFB300)
val Amber700 = Color(0xFFFFA000)
val Amber800 = Color(0xFFFF8F00)
val Amber900 = Color(0xFFF57F17)

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

// ==================== ADDITIONAL ACCENT COLORS ====================

// Purple (Alternative Accent)
val Purple50 = Color(0xFFF3E5F5)
val Purple100 = Color(0xFFE1BEE7)
val Purple200 = Color(0xFFCE93D8)
val Purple300 = Color(0xFFBA68C8)
val Purple400 = Color(0xFFAB47BC)
val Purple500 = Color(0xFF9C27B0)
val Purple600 = Color(0xFF8E24AA)
val Purple700 = Color(0xFF7B1FA2)
val Purple800 = Color(0xFF6A1B9A)
val Purple900 = Color(0xFF4A148C)

// Blue (Alternative Accent)
val Blue50 = Color(0xFFE3F2FD)
val Blue100 = Color(0xFFBBDEFB)
val Blue200 = Color(0xFF90CAF9)
val Blue300 = Color(0xFF64B5F6)
val Blue400 = Color(0xFF42A5F5)
val Blue500 = Color(0xFF2196F3)
val Blue600 = Color(0xFF1E88E5)
val Blue700 = Color(0xFF1976D2)
val Blue800 = Color(0xFF1565C0)
val Blue900 = Color(0xFF0D47A1)

// Green (Alternative Accent)
val Green50 = Color(0xFFE8F5E9)
val Green100 = Color(0xFFC8E6C9)
val Green200 = Color(0xFFA5D6A7)
val Green300 = Color(0xFF81C784)
val Green400 = Color(0xFF66BB6A)
val Green500 = Color(0xFF4CAF50)
val Green600 = Color(0xFF43A047)
val Green700 = Color(0xFF388E3C)
val Green800 = Color(0xFF2E7D32)
val Green900 = Color(0xFF1B5E20)

// Pink (Alternative Accent)
val Pink50 = Color(0xFFFCE4EC)
val Pink100 = Color(0xFFF8BBD0)
val Pink200 = Color(0xFFF48FB1)
val Pink300 = Color(0xFFF06292)
val Pink400 = Color(0xFFEC407A)
val Pink500 = Color(0xFFE91E63)
val Pink600 = Color(0xFFD81B60)
val Pink700 = Color(0xFFC2185B)
val Pink800 = Color(0xFFAD1457)
val Pink900 = Color(0xFF880E4F)

// ==================== NEUTRAL COLORS ====================

// Dark Theme Neutral Colors
val DarkBackground = Color(0xFF0F1115)
val DarkSurface = Color(0xFF1D2430)
val DarkSurfaceVariant = Color(0xFF2B3341)
val DarkSurfaceBright = Color(0xFF2D3748)
val DarkSurfaceDim = Color(0xFF141824)
val DarkOnSurface = Color(0xFFE5E7EB)
val DarkOnSurfaceMedium = Color(0xFF9CA3AF)
val DarkOnSurfaceVariant = Color(0xFF96A0AE)
val DarkOnSurfaceDisabled = Color(0xFF6B7280)

// Light Theme Neutral Colors
val LightBackground = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightSurfaceVariant = Color(0xFFF1F5F9)
val LightSurfaceBright = Color(0xFFF8FAFC)
val LightSurfaceDim = Color(0xFFE2E8F0)
val LightOnSurface = Color(0xFF1E293B)
val LightOnSurfaceMedium = Color(0xFF64748B)
val LightOnSurfaceVariant = Color(0xFF94A3B8)
val LightOnSurfaceDisabled = Color(0xFFCBD5E1)

// Pure Black Theme
val PureBlack = Color(0xFF000000)
val PureBlackSurface = Color(0xFF0A0A0A)
val PureBlackSurfaceVariant = Color(0xFF141414)
val PureBlackSurfaceBright = Color(0xFF1A1A1A)
val PureBlackOnSurface = Color(0xFFF0F0F0)
val PureBlackOnSurfaceMedium = Color(0xFFA0A0A0)
val PureBlackOnSurfaceVariant = Color(0xFF808080)

// High Contrast Theme
val HighContrastPrimary = Color(0xFF000000)
val HighContrastOnPrimary = Color(0xFFFFFFFF)
val HighContrastBackground = Color(0xFFFFFFFF)
val HighContrastSurface = Color(0xFFFFFFFF)
val HighContrastSurfaceVariant = Color(0xFFF0F0F0)
val HighContrastOnSurface = Color(0xFF000000)
val HighContrastOnSurfaceMedium = Color(0xFF333333)
val HighContrastOnSurfaceVariant = Color(0xFF666666)

// ==================== SEMANTIC COLORS ====================

// Status Colors
val Success = Color(0xFF10B981)
val SuccessLight = Color(0xFFD1FAE5)
val SuccessDark = Color(0xFF065F46)
val Warning = Color(0xFFF59E0B)
val WarningLight = Color(0xFFFEF3C7)
val WarningDark = Color(0xFF92400E)
val Error = Color(0xFFEF4444)
val ErrorLight = Color(0xFFFEE2E2)
val ErrorDark = Color(0xFF991B1B)
val Info = Color(0xFF3B82F6)
val InfoLight = Color(0xFFDBEAFE)
val InfoDark = Color(0xFF1E3A5F)

// ==================== GRADIENT COLORS ====================

object Gradients {
    val primaryGradient = listOf(DeepOrange500, DeepOrange700)
    val secondaryGradient = listOf(Amber500, Amber800)
    val tertiaryGradient = listOf(Teal500, Teal700)
    val successGradient = listOf(Success, SuccessDark)
    val warningGradient = listOf(Warning, WarningDark)
    val errorGradient = listOf(Error, ErrorDark)
    val infoGradient = listOf(Info, InfoDark)
    val darkGradient = listOf(DarkSurface, DarkSurfaceDim)
    val lightGradient = listOf(LightSurface, LightSurfaceDim)
}

// ==================== THEME COLOR SCHEMES ====================

object ThemeColors {

    // ==================== DARK THEME ====================
    val Dark = ColorScheme(
        primary = DeepOrange500,
        primaryContainer = DeepOrange900,
        secondary = Amber500,
        secondaryContainer = Amber800,
        tertiary = Teal500,
        tertiaryContainer = Teal800,
        background = DarkBackground,
        surface = DarkSurface,
        surfaceVariant = DarkSurfaceVariant,
        onBackground = DarkOnSurface,
        onSurface = DarkOnSurface,
        onSurfaceVariant = DarkOnSurfaceVariant,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.White,
        onPrimaryContainer = DeepOrange100,
        onSecondaryContainer = Amber100,
        onTertiaryContainer = Teal100,
        error = Error,
        onError = Color.White,
        success = Success,
        onSuccess = Color.White,
        warning = Warning,
        onWarning = Color.Black
    )

    // ==================== LIGHT THEME ====================
    val Light = ColorScheme(
        primary = DeepOrange700,
        primaryContainer = DeepOrange200,
        secondary = Amber800,
        secondaryContainer = Amber200,
        tertiary = Teal700,
        tertiaryContainer = Teal200,
        background = LightBackground,
        surface = LightSurface,
        surfaceVariant = LightSurfaceVariant,
        onBackground = LightOnSurface,
        onSurface = LightOnSurface,
        onSurfaceVariant = LightOnSurfaceVariant,
        onPrimary = Color.White,
        onSecondary = Color.White,
        onTertiary = Color.White,
        onPrimaryContainer = DeepOrange900,
        onSecondaryContainer = Amber900,
        onTertiaryContainer = Teal900,
        error = Error,
        onError = Color.White,
        success = Success,
        onSuccess = Color.White,
        warning = Warning,
        onWarning = Color.Black
    )

    // ==================== PURE BLACK THEME ====================
    val PureBlackTheme = ColorScheme(
        primary = DeepOrange500,
        primaryContainer = DeepOrange900,
        secondary = Amber500,
        secondaryContainer = Amber800,
        tertiary = Teal500,
        tertiaryContainer = Teal800,
        background = PureBlack,
        surface = PureBlackSurface,
        surfaceVariant = PureBlackSurfaceVariant,
        onBackground = PureBlackOnSurface,
        onSurface = PureBlackOnSurface,
        onSurfaceVariant = PureBlackOnSurfaceVariant,
        onPrimary = Color.White,
        onSecondary = Color.Black,
        onTertiary = Color.White,
        onPrimaryContainer = DeepOrange100,
        onSecondaryContainer = Amber100,
        onTertiaryContainer = Teal100,
        error = Error,
        onError = Color.White,
        success = Success,
        onSuccess = Color.White,
        warning = Warning,
        onWarning = Color.Black
    )

    // ==================== HIGH CONTRAST THEME ====================
    val HighContrast = ColorScheme(
        primary = HighContrastPrimary,
        primaryContainer = HighContrastPrimary,
        secondary = HighContrastPrimary,
        secondaryContainer = HighContrastPrimary,
        tertiary = HighContrastPrimary,
        tertiaryContainer = HighContrastPrimary,
        background = HighContrastBackground,
        surface = HighContrastSurface,
        surfaceVariant = HighContrastSurfaceVariant,
        onBackground = HighContrastOnSurface,
        onSurface = HighContrastOnSurface,
        onSurfaceVariant = HighContrastOnSurfaceVariant,
        onPrimary = HighContrastOnPrimary,
        onSecondary = HighContrastOnPrimary,
        onTertiary = HighContrastOnPrimary,
        onPrimaryContainer = HighContrastOnPrimary,
        onSecondaryContainer = HighContrastOnPrimary,
        onTertiaryContainer = HighContrastOnPrimary,
        error = Error,
        onError = HighContrastOnPrimary,
        success = Success,
        onSuccess = HighContrastOnPrimary,
        warning = Warning,
        onWarning = HighContrastOnPrimary
    )
}

// ==================== COLOR SCHEME DATA CLASS ====================

data class ColorScheme(
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onPrimary: Color,
    val onSecondary: Color,
    val onTertiary: Color,
    val onPrimaryContainer: Color,
    val onSecondaryContainer: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val success: Color,
    val onSuccess: Color,
    val warning: Color,
    val onWarning: Color
)

// ==================== HELPER FUNCTIONS ====================

fun getAccentColorFromTheme(themeName: String): Color {
    return when (themeName) {
        "orange" -> DeepOrange500
        "amber" -> Amber500
        "teal" -> Teal500
        "purple" -> Purple500
        "blue" -> Blue500
        "green" -> Green500
        "pink" -> Pink500
        else -> DeepOrange500
    }
}

fun getAccentColorFamily(themeName: String): List<Color> {
    return when (themeName) {
        "orange" -> listOf(DeepOrange50, DeepOrange100, DeepOrange200, DeepOrange300, DeepOrange400, DeepOrange500, DeepOrange600, DeepOrange700, DeepOrange800, DeepOrange900)
        "amber" -> listOf(Amber50, Amber100, Amber200, Amber300, Amber400, Amber500, Amber600, Amber700, Amber800, Amber900)
        "teal" -> listOf(Teal50, Teal100, Teal200, Teal300, Teal400, Teal500, Teal600, Teal700, Teal800, Teal900)
        "purple" -> listOf(Purple50, Purple100, Purple200, Purple300, Purple400, Purple500, Purple600, Purple700, Purple800, Purple900)
        "blue" -> listOf(Blue50, Blue100, Blue200, Blue300, Blue400, Blue500, Blue600, Blue700, Blue800, Blue900)
        "green" -> listOf(Green50, Green100, Green200, Green300, Green400, Green500, Green600, Green700, Green800, Green900)
        "pink" -> listOf(Pink50, Pink100, Pink200, Pink300, Pink400, Pink500, Pink600, Pink700, Pink800, Pink900)
        else -> listOf(DeepOrange50, DeepOrange100, DeepOrange200, DeepOrange300, DeepOrange400, DeepOrange500, DeepOrange600, DeepOrange700, DeepOrange800, DeepOrange900)
    }
}

fun isDarkThemeColor(background: Color): Boolean {
    val luminance = background.red * 0.299 + background.green * 0.587 + background.blue * 0.114
    return luminance < 0.5
}

fun Color.contrastText(): Color {
    return if (isDarkThemeColor(this)) Color.White else Color.Black
}

fun Color.withAlpha(alpha: Float): Color {
    return this.copy(alpha = alpha)
}

fun Color.withBrightness(factor: Float): Color {
    val r = (this.red * factor).coerceIn(0f, 1f)
    val g = (this.green * factor).coerceIn(0f, 1f)
    val b = (this.blue * factor).coerceIn(0f, 1f)
    return Color(r, g, b, this.alpha)
}

// ==================== PREVIEW COLORS ====================

object PreviewColors {
    val allPrimary = listOf(
        DeepOrange50, DeepOrange100, DeepOrange200, DeepOrange300, DeepOrange400,
        DeepOrange500, DeepOrange600, DeepOrange700, DeepOrange800, DeepOrange900
    )

    val allSecondary = listOf(
        Amber50, Amber100, Amber200, Amber300, Amber400,
        Amber500, Amber600, Amber700, Amber800, Amber900
    )

    val allTertiary = listOf(
        Teal50, Teal100, Teal200, Teal300, Teal400,
        Teal500, Teal600, Teal700, Teal800, Teal900
    )

    val allAccents = listOf(
        DeepOrange500, Amber500, Teal500, Purple500, Blue500, Green500, Pink500
    )

    val allStatus = listOf(
        Success, Warning, Error, Info
    )
}