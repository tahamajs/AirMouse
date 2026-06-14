package com.airmouse.presentation.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class HelpViewModel @Inject constructor(
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HelpUiState())
    val uiState: StateFlow<HelpUiState> = _uiState.asStateFlow()

    val helpSections = listOf(
        HelpSection(
            id = "getting_started",
            title = "Getting Started",
            content = "Air Mouse Pro turns your phone into a wireless mouse. Follow these steps to get started:",
            category = HelpCategory.GETTING_STARTED,
            steps = listOf(
                "Install the Air Mouse app on your Android phone",
                "Download and run the Air Mouse server on your PC",
                "Ensure both devices are on the same WiFi network",
                "Open the app and enter your PC's IP address",
                "Tap 'Connect' to establish connection",
                "Calibrate sensors for best accuracy",
                "Start moving your phone to control the cursor"
            ),
            tips = listOf(
                "Keep your phone steady during calibration",
                "Hold the phone with screen facing you",
                "Start with medium sensitivity settings"
            ),
            relatedTopics = listOf("Connection Guide", "Calibration Guide")
        ),
        HelpSection(
            id = "connection",
            title = "Connection Guide",
            content = "How to connect your phone to the PC server:",
            category = HelpCategory.CONNECTION,
            steps = listOf(
                "Run the Air Mouse server on your PC",
                "Note the IP address displayed in the server window",
                "On your phone, enter the IP address and port (default: 8080)",
                "Alternatively, scan the QR code shown on the server",
                "Tap 'Connect' - you should see 'Connected' status",
                "The cursor will now follow your phone's movement"
            ),
            tips = listOf(
                "Both devices must be on the same WiFi network",
                "Check firewall settings if connection fails",
                "Use 5GHz WiFi for lower latency"
            ),
            relatedTopics = listOf("Troubleshooting", "Network Discovery")
        ),
        HelpSection(
            id = "gestures",
            title = "Gesture Controls",
            content = "Learn the basic gestures to control your PC:",
            category = HelpCategory.GESTURES,
            steps = listOf(
                "• Move cursor: Rotate phone around X and Z axes",
                "• Left click: Quick flick/rotation around Y axis",
                "• Right click: Hold tilt for 0.5 seconds",
                "• Double click: Two quick flicks in succession",
                "• Scroll up: Fast upward movement",
                "• Scroll down: Fast downward movement",
                "• Custom gestures: Create your own in Gesture Studio"
            ),
            tips = listOf(
                "Adjust gesture sensitivity in Settings",
                "Practice gestures for better accuracy",
                "Use voice commands as alternative"
            ),
            relatedTopics = listOf("Gesture Studio", "Voice Commands")
        ),
        HelpSection(
            id = "calibration",
            title = "Calibration Guide",
            content = "Proper calibration ensures accurate cursor tracking:",
            category = HelpCategory.CALIBRATION,
            steps = listOf(
                "Open Calibration from the main menu",
                "Gyroscope: Place phone flat and keep still for 10 seconds",
                "Accelerometer: Hold phone in 6 different positions",
                "Magnetometer: Move phone in figure-8 pattern",
                "Test the cursor movement after calibration",
                "Re-calibrate if you notice drift or inaccuracy"
            ),
            tips = listOf(
                "Calibrate on a flat, stable surface",
                "Recalibrate when switching holding hands",
                "Calibration data persists across app restarts"
            ),
            relatedTopics = listOf("Sensor Settings", "Troubleshooting")
        ),
        HelpSection(
            id = "troubleshooting",
            title = "Troubleshooting",
            content = "Common issues and solutions:",
            category = HelpCategory.TROUBLESHOOTING,
            steps = listOf(
                "Cannot connect:",
                "  • Check if both devices are on same WiFi",
                "  • Disable firewall temporarily",
                "  • Try the QR code method",
                "",
                "Laggy cursor:",
                "  • Reduce sensitivity in Settings",
                "  • Enable predictive movement",
                "  • Use 5GHz WiFi if available",
                "",
                "Gestures not detected:",
                "  • Recalibrate sensors",
                "  • Increase gesture sensitivity",
                "  • Practice the movement pattern"
            ),
            tips = listOf(
                "Restart the app if issues persist",
                "Clear app cache in Android settings",
                "Update to latest version"
            ),
            relatedTopics = listOf("Connection Guide", "Calibration Guide")
        ),
        HelpSection(
            id = "advanced",
            title = "Advanced Features",
            content = "Explore additional features:",
            category = HelpCategory.ADVANCED,
            steps = listOf(
                "Voice Commands: Say 'click', 'scroll', 'right click'",
                "Custom Gestures: Train your own gestures in Gesture Studio",
                "Proximity Lock: Auto-locks PC when you walk away",
                "Edge Gestures: Use volume buttons for quick actions",
                "Multiple Profiles: Save different sensitivity settings",
                "AI Smoothing: Machine learning for smoother cursor",
                "Predictive Movement: Kalman filter reduces lag"
            ),
            tips = listOf(
                "Enable AI features for smoother experience",
                "Create profiles for different activities",
                "Use voice commands for hands-free control"
            ),
            relatedTopics = listOf("Voice Commands", "Proximity Lock")
        ),
        HelpSection(
            id = "accessibility",
            title = "Accessibility Features",
            content = "Accessibility options for all users:",
            category = HelpCategory.ACCESSIBILITY,
            steps = listOf(
                "High Contrast Mode: Better visibility",
                "Large Text: Increased font size",
                "Screen Reader: Voice feedback for actions",
                "Announce Movement: Voice announcements",
                "Reduce Motion: Minimize animations",
                "Color Blind Mode: Adjust colors for visibility"
            ),
            tips = listOf(
                "Enable features in Accessibility settings",
                "Customize to your preference",
                "Features work with system screen readers"
            ),
            relatedTopics = listOf("Display Settings", "Voice Feedback")
        ),
        HelpSection(
            id = "faq",
            title = "Frequently Asked Questions",
            content = "Answers to common questions:",
            category = HelpCategory.FAQ,
            steps = listOf(
                "Q: Does it work without internet?",
                "A: Yes, works on local WiFi network only",
                "",
                "Q: What Android versions are supported?",
                "A: Android 10 (API 29) and above",
                "",
                "Q: Does it drain battery?",
                "A: Moderate usage, optimized for efficiency",
                "",
                "Q: Can I use Bluetooth instead?",
                "A: Yes, Bluetooth HID mouse mode available",
                "",
                "Q: Is my data private?",
                "A: All communication is local - no cloud servers"
            ),
            tips = listOf(
                "Check GitHub for latest updates",
                "Report issues on GitHub",
                "Join our Discord for support"
            ),
            relatedTopics = listOf("Getting Started", "Troubleshooting")
        )
    )

    init {
        loadFavorites()
        loadExpandedSections()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val favorites = prefs.getString("help_favorites", "")
            val favoriteSet = if (favorites.isNotEmpty()) favorites.split(",").toSet() else emptySet()
            _uiState.update { it.copy(favoriteSections = favoriteSet) }
        }
    }

    private fun loadExpandedSections() {
        viewModelScope.launch {
            val expanded = prefs.getString("help_expanded", "")
            val expandedSet = if (expanded.isNotEmpty()) expanded.split(",").toSet() else emptySet()
            _uiState.update { it.copy(expandedSections = expandedSet) }
        }
    }

    private fun saveExpandedSections() {
        val expanded = _uiState.value.expandedSections.joinToString(",")
        prefs.putString("help_expanded", expanded)
    }

    fun toggleSection(sectionId: String) {
        _uiState.update { state ->
            val newSet = if (state.expandedSections.contains(sectionId)) {
                state.expandedSections - sectionId
            } else {
                state.expandedSections + sectionId
            }
            state.copy(expandedSections = newSet)
        }
        saveExpandedSections()
    }

    fun updateSearchQuery(query: String) {
        _uiState.update { it.copy(searchQuery = query) }
    }

    fun selectCategory(category: HelpCategory) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun toggleFavorite(sectionId: String) {
        val newFavorites = if (_uiState.value.favoriteSections.contains(sectionId)) {
            _uiState.value.favoriteSections - sectionId
        } else {
            _uiState.value.favoriteSections + sectionId
        }

        _uiState.update { it.copy(favoriteSections = newFavorites) }
        prefs.putString("help_favorites", newFavorites.joinToString(","))
    }

    fun toggleShowFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun getFilteredSections(): List<HelpSection> {
        var sections = helpSections

        if (_uiState.value.selectedCategory != HelpCategory.ALL) {
            sections = sections.filter { it.category == _uiState.value.selectedCategory }
        }

        if (_uiState.value.showFavoritesOnly) {
            sections = sections.filter { _uiState.value.favoriteSections.contains(it.id) }
        }

        if (_uiState.value.searchQuery.isNotEmpty()) {
            val query = _uiState.value.searchQuery.lowercase()
            sections = sections.filter {
                it.title.lowercase().contains(query) ||
                it.content.lowercase().contains(query) ||
                it.steps.any { step -> step.lowercase().contains(query) } ||
                it.tips.any { tip -> tip.lowercase().contains(query) } ||
                it.relatedTopics.any { topic -> topic.lowercase().contains(query) }
            }
        }

        return sections
    }
}

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
    GESTURES("Gestures"),
    CALIBRATION("Calibration"),
    TROUBLESHOOTING("Troubleshooting"),
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