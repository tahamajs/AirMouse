package com.airmouse.presentation.ui.help

data class HelpUiState(
    val searchQuery: String = "",
    val selectedCategory: HelpCategory = HelpCategory.ALL,
    val expandedSections: Set<String> = emptySet(),
    val favoriteSections: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false
)

enum class HelpCategory(val displayName: String) {
    ALL("All"),
    GETTING_STARTED("Getting Started"),
    CONNECTION("Connection"),
    MOUSE_CONTROL("Mouse Control"),
    GESTURES("Gestures"),
    CALIBRATION("Calibration"),
    GAMING("Gaming Mode"),
    SCREEN_MIRRORING("Screen Mirroring"),
    FILE_TRANSFER("File Transfer"),
    VOICE_COMMANDS("Voice Commands"),
    THEMES("Themes & Appearance"),
    NOTIFICATIONS("Notifications"),
    BATTERY("Battery & Performance"),
    PROFILES("Profiles"),
    SERVER_SETUP("Server Setup Guide"),
    TROUBLESHOOTING("Troubleshooting"),
    SERVER_SHORTCUTS("Server Shortcuts"),
    BLUETOOTH("Bluetooth"),
    BACKGROUND_MODE("Background Mode"),
    SETTINGS("Settings"),
    ADVANCED("Advanced"),
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
    val relatedTopics: List<String> = emptyList()
)