
package com.airmouse.presentation.ui.themes

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color





data class ThemeColorScheme(
    val background: Color,
    val surface: Color,
    val surfaceVariant: Color,
    val primary: Color,
    val primaryContainer: Color,
    val secondary: Color,
    val secondaryContainer: Color,
    val tertiary: Color,
    val tertiaryContainer: Color,
    val onBackground: Color,
    val onSurface: Color,
    val onSurfaceVariant: Color,
    val onPrimary: Color,
    val onPrimaryContainer: Color,
    val onSecondary: Color,
    val onSecondaryContainer: Color,
    val onTertiary: Color,
    val onTertiaryContainer: Color,
    val error: Color,
    val onError: Color,
    val errorContainer: Color,
    val onErrorContainer: Color,
    val outline: Color,
    val outlineVariant: Color,
    val scrim: Color,
    val inverseSurface: Color,
    val inverseOnSurface: Color,
    val inversePrimary: Color,
    val surfaceTint: Color
)

object ThemeColorSchemes {

    
    fun lightTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        val accentLight = Color(accent.lightColor)
        val accentDark = Color(accent.darkColor)

        return ThemeColorScheme(
            background = Color(0xFFFFFFFF),
            surface = Color(0xFFF8F9FA),
            surfaceVariant = Color(0xFFE9ECEF),
            primary = accentColor,
            primaryContainer = accentLight,
            secondary = accentDark,
            secondaryContainer = Color(0xFFE8F5E9),
            tertiary = Color(0xFF6C757D),
            tertiaryContainer = Color(0xFFDEE2E6),
            onBackground = Color(0xFF212529),
            onSurface = Color(0xFF212529),
            onSurfaceVariant = Color(0xFF495057),
            onPrimary = Color(0xFFFFFFFF),
            onPrimaryContainer = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onSecondaryContainer = Color(0xFF212529),
            onTertiary = Color(0xFFFFFFFF),
            onTertiaryContainer = Color(0xFF212529),
            error = Color(0xFFDC3545),
            onError = Color(0xFFFFFFFF),
            errorContainer = Color(0xFFF8D7DA),
            onErrorContainer = Color(0xFF721C24),
            outline = Color(0xFFDEE2E6),
            outlineVariant = Color(0xFFE9ECEF),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFF212529),
            inverseOnSurface = Color(0xFFF8F9FA),
            inversePrimary = accentLight,
            surfaceTint = accentColor
        )
    }

    
    fun darkTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        val accentLight = Color(accent.lightColor)

        return ThemeColorScheme(
            background = Color(0xFF121212),
            surface = Color(0xFF1E1E1E),
            surfaceVariant = Color(0xFF2D2D2D),
            primary = accentLight,
            primaryContainer = accentColor,
            secondary = accentColor,
            secondaryContainer = Color(0xFF1B5E20),
            tertiary = Color(0xFF6C757D),
            tertiaryContainer = Color(0xFF343A40),
            onBackground = Color(0xFFE9ECEF),
            onSurface = Color(0xFFE9ECEF),
            onSurfaceVariant = Color(0xFFADB5BD),
            onPrimary = Color(0xFF121212),
            onPrimaryContainer = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onSecondaryContainer = Color(0xFFE9ECEF),
            onTertiary = Color(0xFFFFFFFF),
            onTertiaryContainer = Color(0xFFE9ECEF),
            error = Color(0xFFF44336),
            onError = Color(0xFF121212),
            errorContainer = Color(0xFFC62828),
            onErrorContainer = Color(0xFFEF9A9A),
            outline = Color(0xFF424242),
            outlineVariant = Color(0xFF2D2D2D),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFE9ECEF),
            inverseOnSurface = Color(0xFF121212),
            inversePrimary = accentColor,
            surfaceTint = accentLight
        )
    }

    
    fun pureBlackTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        val accentLight = Color(accent.lightColor)

        return ThemeColorScheme(
            background = Color(0xFF000000),
            surface = Color(0xFF0D0D0D),
            surfaceVariant = Color(0xFF1A1A1A),
            primary = accentLight,
            primaryContainer = accentColor,
            secondary = accentColor,
            secondaryContainer = Color(0xFF1A1A1A),
            tertiary = Color(0xFF6C757D),
            tertiaryContainer = Color(0xFF2D2D2D),
            onBackground = Color(0xFFE9ECEF),
            onSurface = Color(0xFFE9ECEF),
            onSurfaceVariant = Color(0xFFADB5BD),
            onPrimary = Color(0xFF000000),
            onPrimaryContainer = Color(0xFFFFFFFF),
            onSecondary = Color(0xFFFFFFFF),
            onSecondaryContainer = Color(0xFFE9ECEF),
            onTertiary = Color(0xFFFFFFFF),
            onTertiaryContainer = Color(0xFFE9ECEF),
            error = Color(0xFFF44336),
            onError = Color(0xFF000000),
            errorContainer = Color(0xFFC62828),
            onErrorContainer = Color(0xFFEF9A9A),
            outline = Color(0xFF2D2D2D),
            outlineVariant = Color(0xFF1A1A1A),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFE9ECEF),
            inverseOnSurface = Color(0xFF000000),
            inversePrimary = accentColor,
            surfaceTint = accentLight
        )
    }

    
    fun oceanTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        val accentLight = Color(accent.lightColor)

        return ThemeColorScheme(
            background = Color(0xFF0A1628),
            surface = Color(0xFF0D2137),
            surfaceVariant = Color(0xFF1A365D),
            primary = Color(0xFF4FC3F7),
            primaryContainer = Color(0xFF0288D1),
            secondary = Color(0xFF4DD0E1),
            secondaryContainer = Color(0xFF00695C),
            tertiary = Color(0xFF78909C),
            tertiaryContainer = Color(0xFF263238),
            onBackground = Color(0xFFE3F2FD),
            onSurface = Color(0xFFE3F2FD),
            onSurfaceVariant = Color(0xFF90CAF9),
            onPrimary = Color(0xFF0A1628),
            onPrimaryContainer = Color(0xFFFFFFFF),
            onSecondary = Color(0xFF0A1628),
            onSecondaryContainer = Color(0xFFE3F2FD),
            onTertiary = Color(0xFFFFFFFF),
            onTertiaryContainer = Color(0xFFE3F2FD),
            error = Color(0xFFF44336),
            onError = Color(0xFF0A1628),
            errorContainer = Color(0xFFC62828),
            onErrorContainer = Color(0xFFEF9A9A),
            outline = Color(0xFF1A365D),
            outlineVariant = Color(0xFF0D2137),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFE3F2FD),
            inverseOnSurface = Color(0xFF0A1628),
            inversePrimary = Color(0xFF0288D1),
            surfaceTint = Color(0xFF4FC3F7)
        )
    }

    
    fun sunsetTheme(accent: AccentColor): ThemeColorScheme {
        val accentColor = Color(accent.colorCode)
        val accentLight = Color(accent.lightColor)

        return ThemeColorScheme(
            background = Color(0xFF1A0A0A),
            surface = Color(0xFF2D0A0A),
            surfaceVariant = Color(0xFF3D1A1A),
            primary = Color(0xFFFF8A65),
            primaryContainer = Color(0xFFE64A19),
            secondary = Color(0xFFFFAB91),
            secondaryContainer = Color(0xFFBF360C),
            tertiary = Color(0xFFA1887F),
            tertiaryContainer = Color(0xFF3E2723),
            onBackground = Color(0xFFFFEBEE),
            onSurface = Color(0xFFFFEBEE),
            onSurfaceVariant = Color(0xFFFFAB91),
            onPrimary = Color(0xFF1A0A0A),
            onPrimaryContainer = Color(0xFFFFFFFF),
            onSecondary = Color(0xFF1A0A0A),
            onSecondaryContainer = Color(0xFFFFEBEE),
            onTertiary = Color(0xFFFFFFFF),
            onTertiaryContainer = Color(0xFFFFEBEE),
            error = Color(0xFFF44336),
            onError = Color(0xFF1A0A0A),
            errorContainer = Color(0xFFC62828),
            onErrorContainer = Color(0xFFEF9A9A),
            outline = Color(0xFF3D1A1A),
            outlineVariant = Color(0xFF2D0A0A),
            scrim = Color(0xFF000000),
            inverseSurface = Color(0xFFFFEBEE),
            inverseOnSurface = Color(0xFF1A0A0A),
            inversePrimary = Color(0xFFE64A19),
            surfaceTint = Color(0xFFFF8A65)
        )
    }
}





val LocalThemeColors = staticCompositionLocalOf { ThemeColorSchemes.darkTheme(AccentColor.ORANGE) }

@Composable
fun ProvideThemeColors(colors: ThemeColorScheme, content: @Composable () -> Unit) {
    CompositionLocalProvider(LocalThemeColors provides colors) {
        content()
    }
}





@Composable
fun getThemeColors(): ThemeColorScheme {
    return LocalThemeColors.current
}

fun getThemeColorScheme(themeId: String, accent: AccentColor): ThemeColorScheme {
    return when (themeId) {
        "light" -> ThemeColorSchemes.lightTheme(accent)
        "dark" -> ThemeColorSchemes.darkTheme(accent)
        "pure_black" -> ThemeColorSchemes.pureBlackTheme(accent)
        "ocean" -> ThemeColorSchemes.oceanTheme(accent)
        "sunset" -> ThemeColorSchemes.sunsetTheme(accent)
        "forest" -> ThemeColorSchemes.darkTheme(AccentColor.GREEN)
        "purple_haze" -> ThemeColorSchemes.darkTheme(AccentColor.PURPLE)
        "cherry" -> ThemeColorSchemes.darkTheme(AccentColor.PINK)
        "neon" -> ThemeColorSchemes.darkTheme(AccentColor.CYAN)
        "lavender" -> ThemeColorSchemes.darkTheme(AccentColor.PURPLE)
        "mint" -> ThemeColorSchemes.darkTheme(AccentColor.GREEN)
        "peach" -> ThemeColorSchemes.darkTheme(AccentColor.ORANGE)
        "sky" -> ThemeColorSchemes.darkTheme(AccentColor.BLUE)
        "midnight" -> ThemeColorSchemes.pureBlackTheme(AccentColor.INDIGO)
        "gold" -> ThemeColorSchemes.darkTheme(AccentColor.AMBER)
        "matrix" -> ThemeColorSchemes.pureBlackTheme(AccentColor.GREEN)
        "cotton_candy" -> ThemeColorSchemes.darkTheme(AccentColor.PINK)
        "coffee" -> ThemeColorSchemes.darkTheme(AccentColor.BROWN)
        else -> ThemeColorSchemes.darkTheme(accent)
    }
}