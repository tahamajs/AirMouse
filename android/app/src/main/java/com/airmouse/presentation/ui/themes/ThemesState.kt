// app/src/main/java/com/airmouse/presentation/ui/themes/ThemesState.kt
package com.airmouse.presentation.ui.themes

import androidx.compose.ui.graphics.Color

// ==========================================
// UI STATE
// ==========================================

data class ThemesUiState(
    val currentTheme: String = "system",
    val accentColor: AccentColor = AccentColor.ORANGE,
    val isCustomizing: Boolean = false,
    val previewTheme: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null,
    val themeApplied: Boolean = false
)

// ==========================================
// ACCENT COLORS
// ==========================================

enum class AccentColor(
    val displayName: String,
    val colorCode: Long,
    val lightColor: Long,
    val darkColor: Long
) {
    ORANGE("Orange", 0xFFFF5722, 0xFFFF8A65, 0xFFBF360C),
    BLUE("Blue", 0xFF2196F3, 0xFF64B5F6, 0xFF0D47A1),
    GREEN("Green", 0xFF4CAF50, 0xFF81C784, 0xFF1B5E20),
    PURPLE("Purple", 0xFF9C27B0, 0xFFCE93D8, 0xFF4A148C),
    PINK("Pink", 0xFFE91E63, 0xFFF06292, 0xFF880E4F),
    RED("Red", 0xFFF44336, 0xFFEF9A9A, 0xFFB71C1C),
    TEAL("Teal", 0xFF009688, 0xFF4DB6AC, 0xFF004D40),
    INDIGO("Indigo", 0xFF3F51B5, 0xFF7986CB, 0xFF1A237E),
    CYAN("Cyan", 0xFF00BCD4, 0xFF4DD0E1, 0xFF006064),
    AMBER("Amber", 0xFFFFC107, 0xFFFFD54F, 0xFFFF6F00),
    ROSE("Rose", 0xFFE91E63, 0xFFF48FB1, 0xFF880E4F),
    LIME("Lime", 0xFFCDDC39, 0xFFD4E157, 0xFF827717),
    BROWN("Brown", 0xFF795548, 0xFFA1887F, 0xFF3E2723),
    GREY("Grey", 0xFF607D8B, 0xFF90A4AE, 0xFF263238)
}

// ==========================================
// THEME OPTIONS
// ==========================================

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val previewColors: List<Long>,
    val isPremium: Boolean = false,
    val isSystem: Boolean = false
)

// ==========================================
// THEME DEFINITIONS
// ==========================================

object ThemeDefinitions {
    val themes = listOf(
        ThemeOption(
            id = "system",
            name = "System Default",
            description = "Follows system theme",
            previewColors = listOf(0xFF607D8B, 0xFF90A4AE, 0xFFCFD8DC),
            isSystem = true
        ),
        ThemeOption(
            id = "light",
            name = "Light",
            description = "Clean bright interface",
            previewColors = listOf(0xFFFFFFFF, 0xFFF5F5F5, 0xFFE0E0E0)
        ),
        ThemeOption(
            id = "dark",
            name = "Dark",
            description = "Easy on the eyes",
            previewColors = listOf(0xFF1E1E1E, 0xFF2D2D2D, 0xFF424242)
        ),
        ThemeOption(
            id = "pure_black",
            name = "Pure Black",
            description = "AMOLED friendly",
            previewColors = listOf(0xFF000000, 0xFF0D0D0D, 0xFF1A1A1A)
        ),
        ThemeOption(
            id = "ocean",
            name = "Ocean Blue",
            description = "Calm blue tones",
            previewColors = listOf(0xFF0D47A1, 0xFF1565C0, 0xFF1E88E5)
        ),
        ThemeOption(
            id = "sunset",
            name = "Sunset",
            description = "Warm orange hues",
            previewColors = listOf(0xFFBF360C, 0xFFE65100, 0xFFFF6F00)
        ),
        ThemeOption(
            id = "forest",
            name = "Forest Green",
            description = "Natural green tones",
            previewColors = listOf(0xFF1B5E20, 0xFF2E7D32, 0xFF388E3C)
        ),
        ThemeOption(
            id = "purple_haze",
            name = "Purple Haze",
            description = "Mystical purple",
            previewColors = listOf(0xFF4A148C, 0xFF6A1B9A, 0xFF8E24AA)
        ),
        ThemeOption(
            id = "cherry",
            name = "Cherry Blossom",
            description = "Soft pink tones",
            previewColors = listOf(0xFF880E4F, 0xFFAD1457, 0xFFC2185B)
        ),
        ThemeOption(
            id = "neon",
            name = "Neon Cyber",
            description = "Vibrant cyberpunk",
            previewColors = listOf(0xFF00BCD4, 0xFF00E5FF, 0xFF18FFFF)
        ),
        ThemeOption(
            id = "lavender",
            name = "Lavender",
            description = "Gentle purple tones",
            previewColors = listOf(0xFF311B92, 0xFF4527A0, 0xFF5E35B1)
        ),
        ThemeOption(
            id = "mint",
            name = "Mint Fresh",
            description = "Cool mint green",
            previewColors = listOf(0xFF004D40, 0xFF00695C, 0xFF00897B)
        ),
        ThemeOption(
            id = "peach",
            name = "Peach",
            description = "Warm peach tones",
            previewColors = listOf(0xFFBF360C, 0xFFD84315, 0xFFE64A19)
        ),
        ThemeOption(
            id = "sky",
            name = "Sky Blue",
            description = "Bright sky blue",
            previewColors = listOf(0xFF0D47A1, 0xFF1565C0, 0xFF1E88E5)
        ),
        // Premium Themes
        ThemeOption(
            id = "midnight",
            name = "Midnight",
            description = "Deep night theme",
            previewColors = listOf(0xFF0A0A0A, 0xFF1A1A2E, 0xFF16213E),
            isPremium = true
        ),
        ThemeOption(
            id = "gold",
            name = "Golden Luxe",
            description = "Elegant gold accents",
            previewColors = listOf(0xFF3E2723, 0xFF4E342E, 0xFFFFD54F),
            isPremium = true
        ),
        ThemeOption(
            id = "matrix",
            name = "Matrix",
            description = "Green matrix style",
            previewColors = listOf(0xFF003300, 0xFF00FF00, 0xFF00CC00),
            isPremium = true
        ),
        ThemeOption(
            id = "cotton_candy",
            name = "Cotton Candy",
            description = "Sweet pastel pink/blue",
            previewColors = listOf(0xFF4A148C, 0xFFE91E63, 0xFF00BCD4),
            isPremium = true
        ),
        ThemeOption(
            id = "coffee",
            name = "Coffee",
            description = "Warm coffee tones",
            previewColors = listOf(0xFF3E2723, 0xFF5D4037, 0xFF795548),
            isPremium = true
        )
    )

    fun getTheme(id: String): ThemeOption? = themes.find { it.id == id }
    fun getDefaultTheme(): ThemeOption = themes.first { it.id == "dark" }
}