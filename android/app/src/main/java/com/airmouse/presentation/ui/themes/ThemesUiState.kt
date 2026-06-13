package com.airmouse.presentation.ui.themes

data class ThemesUiState(
    val currentTheme: String = "system",
    val accentColor: AccentColor = AccentColor.ORANGE,
    val isCustomizing: Boolean = false,
    val previewTheme: String? = null,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)

enum class AccentColor(val displayName: String, val colorCode: Long) {
    ORANGE("Orange", 0xFFFF5722),
    BLUE("Blue", 0xFF2196F3),
    GREEN("Green", 0xFF4CAF50),
    PURPLE("Purple", 0xFF9C27B0),
    PINK("Pink", 0xFFE91E63),
    RED("Red", 0xFFF44336),
    TEAL("Teal", 0xFF009688),
    INDIGO("Indigo", 0xFF3F51B5),
    CYAN("Cyan", 0xFF00BCD4),
    AMBER("Amber", 0xFFFFC107)
}

data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val previewColors: List<Long>,
    val isPremium: Boolean = false
)