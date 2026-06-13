package com.airmouse.presentation.ui.help

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
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
                "1. Install the Air Mouse app on your Android phone",
                "2. Download and run the Air Mouse server on your PC",
                "3. Ensure both devices are on the same WiFi network",
                "4. Open the app and enter your PC's IP address",
                "5. Tap 'Connect' to establish connection",
                "6. Calibrate sensors for best accuracy",
                "7. Start moving your phone to control the cursor"
            ),
            tips = listOf(
                "Keep your phone steady during calibration",
                "Hold the phone with screen facing you",
                "Start with medium sensitivity settings"
            )
        ),
        HelpSection(
            id = "connection",
            title = "Connection Guide",
            content = "How to connect your phone to the PC server:",
            category = HelpCategory.CONNECTION,
            steps = listOf(
                "1. Run the Air Mouse server on your PC",
                "2. Note the IP address displayed in the server window",
                "3. On your phone, enter the IP address and port (default: 8080)",
                "4. Alternatively, scan the QR code shown on the server",
                "5. Tap 'Connect' - you should see 'Connected' status",
                "6. The cursor will now follow your phone's movement"
            ),
            tips = listOf(
                "Both devices must be on the same WiFi network",
                "Check firewall settings if connection fails",
                "Use 5GHz WiFi for lower latency"
            )
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
            )
        ),
        HelpSection(
            id = "calibration",
            title = "Calibration Guide",
            content = "Proper calibration ensures accurate cursor tracking:",
            category = HelpCategory.CALIBRATION,
            steps = listOf(
                "1. Open Calibration from the main menu",
                "2. Gyroscope: Place phone flat and keep still for 10 seconds",
                "3. Accelerometer: Hold phone in 6 different positions",
                "4. Magnetometer: Move phone in figure-8 pattern",
                "5. Test the cursor movement after calibration",
                "6. Re-calibrate if you notice drift or inaccuracy"
            ),
            tips = listOf(
                "Calibrate on a flat, stable surface",
                "Recalibrate when switching holding hands",
                "Calibration data persists across app restarts"
            )
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
            )
        ),
        HelpSection(
            id = "advanced",
            title = "Advanced Features",
            content = "Explore additional features:",
            category = HelpCategory.ADVANCED,
            steps = listOf(
                "• Voice Commands: Say 'click', 'scroll', 'right click'",
                "• Custom Gestures: Train your own gestures in Gesture Studio",
                "• Proximity Lock: Auto-locks PC when you walk away",
                "• Edge Gestures: Use volume buttons for quick actions",
                "• Multiple Profiles: Save different sensitivity settings",
                "• AI Smoothing: Machine learning for smoother cursor",
                "• Predictive Movement: Kalman filter reduces lag"
            ),
            tips = listOf(
                "Enable AI features for smoother experience",
                "Create profiles for different activities",
                "Use voice commands for hands-free control"
            )
        ),
        HelpSection(
            id = "accessibility",
            title = "Accessibility Features",
            content = "Accessibility options for all users:",
            category = HelpCategory.ACCESSIBILITY,
            steps = listOf(
                "• High Contrast Mode: Better visibility",
                "• Large Text: Increased font size",
                "• Screen Reader: Voice feedback for actions",
                "• Announce Movement: Voice announcements",
                "• Reduce Motion: Minimize animations",
                "• Color Blind Mode: Adjust colors for visibility"
            ),
            tips = listOf(
                "Enable features in Accessibility settings",
                "Customize to your preference",
                "Features work with system screen readers"
            )
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
            )
        )
    )

    init {
        loadFavorites()
    }

    private fun loadFavorites() {
        viewModelScope.launch {
            val favorites = prefs.getString("help_favorites", "")
            val favoriteSet = if (favorites.isNotEmpty()) favorites.split(",").toSet() else emptySet()
            _uiState.update { it.copy(favoriteSections = favoriteSet) }
        }
    }

    fun toggleSection(section: String) {
        _uiState.update { state ->
            val newSet = if (state.expandedSections.contains(section)) {
                state.expandedSections - section
            } else {
                state.expandedSections + section
            }
            state.copy(expandedSections = newSet)
        }
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

        // Save to preferences
        prefs.putString("help_favorites", newFavorites.joinToString(","))
    }

    fun toggleShowFavoritesOnly() {
        _uiState.update { it.copy(showFavoritesOnly = !it.showFavoritesOnly) }
    }

    fun getFilteredSections(): List<HelpSection> {
        var sections = helpSections

        // Filter by category
        if (_uiState.value.selectedCategory != HelpCategory.ALL) {
            sections = sections.filter { it.category == _uiState.value.selectedCategory }
        }

        // Filter by favorites
        if (_uiState.value.showFavoritesOnly) {
            sections = sections.filter { _uiState.value.favoriteSections.contains(it.id) }
        }

        // Filter by search query
        if (_uiState.value.searchQuery.isNotEmpty()) {
            val query = _uiState.value.searchQuery.lowercase()
            sections = sections.filter {
                it.title.lowercase().contains(query) ||
                        it.content.lowercase().contains(query) ||
                        it.steps.any { step -> step.lowercase().contains(query) } ||
                        it.tips.any { tip -> tip.lowercase().contains(query) }
            }
        }

        return sections
    }
}