package com.airmouse.presentation.ui.search

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.UserProfile
import com.airmouse.domain.repository.IProfileRepository
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.utils.LogManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class SearchCategory(val displayName: String) {
    ALL("All"),
    SETTINGS("Settings"),
    SCREENS("Screens"),
    PROFILES("Profiles"),
    HELP("Help"),
    LOGS("Logs")
}

sealed class SearchResult(
    val id: String,
    val title: String,
    val description: String,
    val category: SearchCategory
) {
    class SettingResult(
        id: String,
        title: String,
        description: String,
        val settingKey: String
    ) : SearchResult(id, title, description, SearchCategory.SETTINGS)

    class ProfileResult(
        id: String,
        title: String,
        description: String,
        val profile: UserProfile
    ) : SearchResult(id, title, description, SearchCategory.PROFILES)

    class HelpResult(
        id: String,
        title: String,
        description: String,
        val helpSectionId: String
    ) : SearchResult(id, title, description, SearchCategory.HELP)

    class LogResult(
        id: String,
        title: String,
        description: String,
        val level: String,
        val tag: String,
        val timestamp: String
    ) : SearchResult(id, title, description, SearchCategory.LOGS)

    class ScreenResult(
        id: String,
        title: String,
        description: String,
        val route: String
    ) : SearchResult(id, title, description, SearchCategory.SCREENS)
}

data class SearchUiState(
    val searchQuery: String = "",
    val selectedCategory: SearchCategory = SearchCategory.ALL,
    val results: List<SearchResult> = emptyList(),
    val isLoading: Boolean = false
)

@HiltViewModel
class SearchViewModel @Inject constructor(
    private val profileRepository: IProfileRepository,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow(SearchCategory.ALL)
    private val _profiles = MutableStateFlow<List<UserProfile>>(emptyList())
    private val _isLoading = MutableStateFlow(false)

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            try {
                val userProfiles = profileRepository.getAllProfiles()
                _profiles.value = userProfiles
            } catch (e: Exception) {
                // handle error
            }
        }
    }

    val uiState: StateFlow<SearchUiState> = combine(
        _searchQuery,
        _selectedCategory,
        _profiles,
        _isLoading
    ) { query, category, profiles, loading ->
        val results = if (query.isBlank()) {
            emptyList()
        } else {
            performSearch(query, category, profiles)
        }
        SearchUiState(
            searchQuery = query,
            selectedCategory = category,
            results = results,
            isLoading = loading
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = SearchUiState()
    )

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: SearchCategory) {
        _selectedCategory.value = category
    }

    private fun performSearch(
        query: String,
        category: SearchCategory,
        profiles: List<UserProfile>
    ): List<SearchResult> {
        val q = query.lowercase().trim()
        val list = mutableListOf<SearchResult>()

        // 1. Settings Search Index (Static)
        if (category == SearchCategory.ALL || category == SearchCategory.SETTINGS) {
            val settings = listOf(
                SearchResult.SettingResult("set_sens", "Cursor Sensitivity", "Movement and sensitivity tuning of the mouse cursor", "CURSOR"),
                SearchResult.SettingResult("set_accel", "Cursor Acceleration", "Enable or disable physical movement acceleration scaling", "CURSOR"),
                SearchResult.SettingResult("set_smoothing", "AI Motion Smoothing", "Jitter filter and predictive alignment blending", "AI"),
                SearchResult.SettingResult("set_haptic", "Haptic Strength", "Configure vibration duration and click response strength", "HAPTIC"),
                SearchResult.SettingResult("set_sound", "Sound Effects", "Auditory cues when clicking, scrolling, or shaking device", "HAPTIC"),
                SearchResult.SettingResult("set_theme", "App Theme Mode", "Switch between Light, Midnight, Cyberpunk, and AMOLED Black", "THEMES"),
                SearchResult.SettingResult("set_auto_connect", "Auto Reconnect", "Automatically connect to the last server on network detection", "CONNECTION"),
                SearchResult.SettingResult("set_protocols", "UDP vs TCP Protocols", "Select low-latency UDP or reliable delivery TCP connections", "CONNECTION"),
                SearchResult.SettingResult("set_laser", "Laser Pointer Speed", "Speed settings for presentation virtual pointer", "PRESENTATION"),
                SearchResult.SettingResult("set_file_sharing", "File Sharing Transfer", "Toggle receiver mode and select download directories", "FILE_TRANSFER"),
                SearchResult.SettingResult("set_mirroring", "Screen Mirroring Quality", "Change streaming resolution and frame rate latency", "SCREEN_MIRRORING")
            )
            list.addAll(settings.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) })
        }

        // 2. Screens / Destinations Search Index (Static)
        if (category == SearchCategory.ALL || category == SearchCategory.SCREENS) {
            val screens = listOf(
                SearchResult.ScreenResult("scr_home", "Home / Dashboard", "Main remote control panel & connection toggle", Destinations.Home.route),
                SearchResult.ScreenResult("scr_calib", "Calibrate Sensors", "Calibrate gyroscope, accelerometer, and magnetometer", Destinations.Calibration.route),
                SearchResult.ScreenResult("scr_touch", "Touchpad Screen", "Use phone screen as a virtual laptop trackpad", Destinations.Touchpad.route),
                SearchResult.ScreenResult("scr_bt", "Bluetooth Mouse Mode", "Connect as standard wireless HID controller", Destinations.BluetoothMouse.route),
                SearchResult.ScreenResult("scr_gaming", "Gaming Gamepad", "On-screen joystick, D-pad, and custom button mapping", Destinations.GamingMode.route),
                SearchResult.ScreenResult("scr_sensors", "Sensor Graphs", "Real-time visualizer for raw physics sensors", Destinations.SensorVisualizer.route),
                SearchResult.ScreenResult("scr_logs", "Connectivity Logs", "Live socket debugging logs and packet information", Destinations.ServerLogs.route),
                SearchResult.ScreenResult("scr_themes", "Palette & Styling", "Modify primary accents, backgrounds, and presets", Destinations.Themes.route),
                SearchResult.ScreenResult("scr_about", "App Information", "View University of Tehran project details & credits", Destinations.About.route),
                SearchResult.ScreenResult("scr_help", "Help & FAQ", "Search instructions, connection guides, and FAQs", Destinations.Help.route)
            )
            list.addAll(screens.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) })
        }

        // 3. Profiles Search Index (Dynamic)
        if (category == SearchCategory.ALL || category == SearchCategory.PROFILES) {
            val matchedProfiles = profiles.filter { it.name.lowercase().contains(q) }
            list.addAll(matchedProfiles.map {
                SearchResult.ProfileResult(
                    id = "prof_${it.id}",
                    title = "${it.name} Profile",
                    description = "Switch settings to profile configuration (Usage: ${it.usageCount} times)",
                    profile = it
                )
            })
        }

        // 4. Help Topics Search Index (Static based on Help sections)
        if (category == SearchCategory.ALL || category == SearchCategory.HELP) {
            val helpTopics = listOf(
                SearchResult.HelpResult("help_getting_started", "How to Get Started", "First-time setup walkthrough, server download, and WiFi connection guide", "getting_started"),
                SearchResult.HelpResult("help_connection", "WiFi & Firewall Guide", "Learn to troubleshoot port 8080 blocking, local networks, and UDP", "connection"),
                SearchResult.HelpResult("help_calibration", "Sensor Drift Calibration", "Step-by-step instructions to calibrate gyroscope on flat surface", "calibration"),
                SearchResult.HelpResult("help_gestures", "Flicks & Edge Gestures", "Configure swipe-from-edge actions and custom gesture mapping", "gestures"),
                SearchResult.HelpResult("help_gaming", "Gamepad Customization", "Steer phone like a wheel in racing games and map virtual buttons", "gaming"),
                SearchResult.HelpResult("help_mirroring", "Screen Mirroring Lag", "Optimizing stream fps, low quality mode, and connection drops", "screen_mirroring"),
                SearchResult.HelpResult("help_files", "Wireless File Transfer", "Transfer files to PC downloads or drag-and-drop to phone", "file_transfer"),
                SearchResult.HelpResult("help_voice", "Always-Listening Voice Control", "Configure local speech recognition commands ('click', 'scroll up')", "voice_commands"),
                SearchResult.HelpResult("help_battery", "Reducing Battery Drain", "Enable OLED Amoled Black theme and adjust sensor polling rate", "battery"),
                SearchResult.HelpResult("help_accessibility", "Screen Reader & Contrast", "TalkBack instructions, larger fonts, and motor tremor filters", "accessibility")
            )
            list.addAll(helpTopics.filter { it.title.lowercase().contains(q) || it.description.lowercase().contains(q) })
        }

        // 5. Server Logs Search Index (Dynamic)
        if (category == SearchCategory.ALL || category == SearchCategory.LOGS) {
            val logs = LogManager.getEntries()
            val matchedLogs = logs.filter { it.message.lowercase().contains(q) || it.tag.lowercase().contains(q) }
            list.addAll(matchedLogs.take(50).map {
                SearchResult.LogResult(
                    id = "log_${it.timestampMs}_${it.message.hashCode()}",
                    title = "[${it.level}] ${it.tag}",
                    description = it.message,
                    level = it.level,
                    tag = it.tag,
                    timestamp = it.timestamp
                )
            })
        }

        return list
    }
}
