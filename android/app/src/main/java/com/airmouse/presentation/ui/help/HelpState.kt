package com.airmouse.presentation.ui.help

data class HelpUiState(
    val expandedSections: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: HelpCategory = HelpCategory.ALL,
    val favoriteSections: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false
)

enum class HelpCategory(val displayName: String) {
    ALL("All"),
    GETTING_STARTED("Getting Started"),
    CONNECTION("Connection"),
    GESTURES("Gestures"),
    CALIBRATION("Calibration"),
    TROUBLESHOOTING("Troubleshooting"),
    ADVANCED("Advanced Features"),
    ACCESSIBILITY("Accessibility"),
    FAQ("FAQ")
}

data class HelpSection(
    val id: String,
    val title: String,
    val content: String,
    val category: HelpCategory,
    val steps: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val imageRes: Int? = null,
    val videoUrl: String? = null,
    val isFavorite: Boolean = false
)package com.airmouse.presentation.ui.help

data class HelpUiState(
    val expandedSections: Set<String> = emptySet(),
    val searchQuery: String = "",
    val selectedCategory: HelpCategory = HelpCategory.ALL,
    val favoriteSections: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false
)

enum class HelpCategory(val displayName: String) {
    ALL("All"),
    GETTING_STARTED("Getting Started"),
    CONNECTION("Connection"),
    GESTURES("Gestures"),
    CALIBRATION("Calibration"),
    TROUBLESHOOTING("Troubleshooting"),
    ADVANCED("Advanced Features"),
    ACCESSIBILITY("Accessibility"),
    FAQ("FAQ")
}

data class HelpSection(
    val id: String,
    val title: String,
    val content: String,
    val category: HelpCategory,
    val steps: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val imageRes: Int? = null,
    val videoUrl: String? = null,
    val isFavorite: Boolean = false
)