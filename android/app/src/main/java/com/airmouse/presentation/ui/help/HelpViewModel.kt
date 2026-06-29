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
        // ── Getting Started ──────────────────────────────────────────────
        HelpSection(
            id = "getting_started",
            title = "Getting Started",
            content = "Air Mouse Pro turns your Android phone into a wireless mouse, touchpad, and remote for your PC. Follow these steps to get up and running in minutes.",
            category = HelpCategory.GETTING_STARTED,
            steps = listOf(
                "Install the Air Mouse app on your Android phone from the Play Store or side-load the APK.",
                "Download and launch the Air Mouse Go Server on your Windows, macOS, or Linux PC.",
                "Make sure both devices are connected to the same WiFi network (preferably 5 GHz for lower latency).",
                "Open the app and enter the IP address shown in the Go Server window (e.g. 192.168.1.42).",
                "Choose a protocol: UDP (fast, best for mouse) or TCP (reliable, best for file transfer).",
                "Tap 'Connect' — a green status indicator confirms the link.",
                "Calibrate your phone's sensors from the Calibration screen for best accuracy.",
                "Start moving your phone to control the cursor, or switch to Touchpad mode."
            ),
            tips = listOf(
                "Keep your phone steady during initial calibration for the best results.",
                "Hold the phone with the screen facing you; tilt forward/backward and left/right to move the cursor.",
                "Start with medium sensitivity and adjust later in Settings.",
                "If you don't know your PC's IP address, use the Network Discovery feature to find it automatically."
            ),
            relatedTopics = listOf("Connection Guide", "Calibration Guide", "Touchpad Controls")
        ),

        // ── Connection Guide ─────────────────────────────────────────────
        HelpSection(
            id = "connection",
            title = "Connection Guide",
            content = "Connect your phone to the PC server over WiFi. Both devices must be on the same local network.",
            category = HelpCategory.CONNECTION,
            steps = listOf(
                "Launch the Air Mouse Go Server on your PC. The server window displays the IP address and port.",
                "Note the IP address (e.g. 192.168.1.42) and port (default: 8080).",
                "On your phone, open Air Mouse and enter the IP address and port.",
                "Alternatively, tap 'Scan QR Code' and scan the QR code shown on the Go Server.",
                "Or use Network Discovery to auto-detect servers on the local network.",
                "Choose your preferred protocol:",
                "  • UDP — Lower latency, ideal for real-time mouse movement.",
                "  • TCP — Reliable delivery, better for keyboard input and file operations.",
                "Tap 'Connect'. A green indicator confirms a successful connection.",
                "The cursor on your PC will now follow your phone's movement."
            ),
            tips = listOf(
                "Both devices must be on the same WiFi network (same subnet).",
                "Use 5 GHz WiFi instead of 2.4 GHz for noticeably lower latency.",
                "If connection fails, check your PC's firewall settings — allow the Go Server through.",
                "You can save frequently used connections for quick reconnect."
            ),
            relatedTopics = listOf("Troubleshooting", "Network Discovery", "Bluetooth Connection")
        ),

        // ── Mouse / Touchpad Control ─────────────────────────────────────
        HelpSection(
            id = "mouse_control",
            title = "Mouse & Touchpad Control",
            content = "Air Mouse supports two input modes: motion-based (gyroscope) control and a virtual touchpad. Learn how to click, scroll, drag, and more.",
            category = HelpCategory.MOUSE_CONTROL,
            steps = listOf(
                "Motion Mode (Gyroscope):",
                "  • Tilt / rotate your phone to move the cursor on-screen.",
                "  • The cursor follows the phone's orientation in real time.",
                "",
                "Touchpad Mode:",
                "  • Swipe one finger on the touchpad area to move the cursor.",
                "  • Tap once for a left click.",
                "  • Tap with two fingers for a right click.",
                "  • Swipe two fingers up/down to scroll.",
                "  • Double-tap and hold, then drag to perform a click-and-drag.",
                "",
                "Common Actions:",
                "  • Left Click — Single tap (touchpad) or quick flick (motion).",
                "  • Right Click — Two-finger tap (touchpad) or hold tilt 0.5 s (motion).",
                "  • Double Click — Double tap (touchpad) or two quick flicks (motion).",
                "  • Scroll — Two-finger swipe (touchpad) or fast up/down movement (motion).",
                "  • Drag — Double-tap-hold then move (touchpad), or click-lock gesture (motion)."
            ),
            tips = listOf(
                "Switch between Motion and Touchpad mode from the main screen's mode selector.",
                "Adjust cursor speed in Settings → Sensitivity.",
                "Enable 'AI Smoothing' for smoother, less jittery cursor movement.",
                "Use Edge Gestures (volume buttons) for quick click shortcuts."
            ),
            relatedTopics = listOf("Gesture Controls", "Calibration Guide", "Settings")
        ),

        // ── Calibration ─────────────────────────────────────────────────
        HelpSection(
            id = "calibration",
            title = "Calibration Guide",
            content = "Calibration aligns your phone's sensors so cursor movement is accurate and drift-free. Calibrate after installing the app and whenever you notice the cursor drifting.",
            category = HelpCategory.CALIBRATION,
            steps = listOf(
                "Open Calibration from the main menu or the sidebar.",
                "",
                "Gyroscope Calibration:",
                "  • Place your phone on a flat, stable surface.",
                "  • Keep perfectly still for 10 seconds while the sensor zeroes itself.",
                "  • This eliminates rotational drift.",
                "",
                "Accelerometer Calibration:",
                "  • Hold your phone in 6 different orientations as prompted (flat, on each edge, upside down).",
                "  • Each position is held for 5 seconds.",
                "",
                "Magnetometer Calibration:",
                "  • Move your phone slowly in a figure-8 pattern in the air.",
                "  • Continue for about 15 seconds until the progress bar fills.",
                "",
                "After calibration, test the cursor movement on the preview screen.",
                "If you still notice drift or inaccuracy, re-run calibration."
            ),
            tips = listOf(
                "Calibrate on a flat, stable surface away from magnets and electronics.",
                "Recalibrate when switching which hand holds the phone.",
                "Calibration data persists across app restarts — you don't need to redo it every time.",
                "If accuracy degrades over time, a quick gyroscope-only recalibration usually fixes it."
            ),
            relatedTopics = listOf("Sensor Settings", "Troubleshooting", "Mouse & Touchpad Control")
        ),

        // ── Troubleshooting ─────────────────────────────────────────────
        HelpSection(
            id = "troubleshooting",
            title = "Troubleshooting",
            content = "Solutions for the most common issues you might encounter.",
            category = HelpCategory.TROUBLESHOOTING,
            steps = listOf(
                "Cannot Connect to Server:",
                "  • Verify both devices are on the same WiFi network and subnet.",
                "  • Check that the Go Server is running and showing an IP address.",
                "  • Temporarily disable your PC firewall, or add an exception for the Go Server executable.",
                "  • On Windows, allow the server through Windows Defender Firewall → 'Allow an app through firewall'.",
                "  • Try the QR code or Network Discovery method instead of manual IP entry.",
                "  • Restart both the app and the server.",
                "",
                "Connection Drops Frequently:",
                "  • Switch to 5 GHz WiFi — 2.4 GHz is more congested.",
                "  • Move closer to the WiFi router to improve signal strength.",
                "  • Disable battery optimization for Air Mouse in Android Settings → Apps → Battery.",
                "  • Enable Background Mode (WakeLock) to prevent Android from killing the connection.",
                "",
                "Laggy / Jittery Cursor:",
                "  • Use UDP protocol instead of TCP for lower latency.",
                "  • Reduce sensitivity in Settings if the cursor is over-responsive.",
                "  • Enable 'Predictive Movement' (Kalman filter) to smooth out lag.",
                "  • Enable 'AI Smoothing' for machine-learning based jitter reduction.",
                "  • Close bandwidth-heavy apps on your network (streaming, downloads).",
                "",
                "Gestures Not Detected:",
                "  • Recalibrate sensors from the Calibration screen.",
                "  • Increase gesture sensitivity in Settings.",
                "  • Practice the movement pattern — gestures require deliberate motions.",
                "  • Check that the gesture is enabled in Gesture Studio.",
                "",
                "Cursor Drifting on Its Own:",
                "  • Recalibrate the gyroscope (place phone flat and still for 10 s).",
                "  • Ensure the phone is at room temperature — extreme temps affect sensors.",
                "  • Enable 'Auto Re-center' in Settings."
            ),
            tips = listOf(
                "Restart the app and server if issues persist after trying the above steps.",
                "Clear the app cache in Android Settings → Apps → Air Mouse → Clear Cache.",
                "Make sure you're running the latest version of both the app and the Go Server.",
                "Join our Discord community or file a GitHub issue for further help."
            ),
            relatedTopics = listOf("Connection Guide", "Calibration Guide", "Settings")
        ),

        // ── Go Server Shortcuts ─────────────────────────────────────────
        HelpSection(
            id = "server_shortcuts",
            title = "Go Server Keyboard Shortcuts",
            content = "The Air Mouse Go Server running on your PC supports keyboard shortcuts for quick control.",
            category = HelpCategory.SERVER_SHORTCUTS,
            steps = listOf(
                "Escape — Hide/minimize the server window to the system tray.",
                "Ctrl + H — Hide the server window (same as Escape).",
                "F1 — Open the server's built-in help documentation.",
                "Ctrl + Q — Quit the server gracefully.",
                "Ctrl + R — Restart the server listener (useful after network changes).",
                "Ctrl + L — Open the server log viewer.",
                "",
                "System Tray:",
                "  • Right-click the tray icon to access Show, Restart, and Quit options.",
                "  • Double-click the tray icon to restore the server window."
            ),
            tips = listOf(
                "The server runs in the background even when minimized to tray.",
                "Use Ctrl+H to keep the server hidden while presenting or sharing your screen.",
                "Server logs are useful for diagnosing connection issues — share them when reporting bugs."
            ),
            relatedTopics = listOf("Connection Guide", "Troubleshooting")
        ),

        // ── Gesture Controls ─────────────────────────────────────────────
        HelpSection(
            id = "gestures",
            title = "Gesture Controls",
            content = "Air Mouse recognizes motion gestures performed with your phone to trigger mouse and system actions.",
            category = HelpCategory.GESTURES,
            steps = listOf(
                "Built-in Gestures (Motion Mode):",
                "  • Move Cursor — Rotate phone around X-axis (tilt) and Z-axis (pan).",
                "  • Left Click — Quick flick / rotation around Y-axis.",
                "  • Right Click — Hold a tilt position for 0.5 seconds.",
                "  • Double Click — Two quick flicks in rapid succession.",
                "  • Scroll Up — Fast upward movement.",
                "  • Scroll Down — Fast downward movement.",
                "",
                "Touchpad Gestures:",
                "  • One-finger swipe — Move cursor.",
                "  • Single tap — Left click.",
                "  • Two-finger tap — Right click.",
                "  • Two-finger swipe — Scroll vertically.",
                "  • Three-finger swipe up — Show task switcher / Alt+Tab.",
                "  • Three-finger swipe down — Show desktop / Win+D.",
                "",
                "Custom Gestures:",
                "  • Open Gesture Studio to train your own gestures.",
                "  • Draw or perform a motion, then assign it to any keyboard shortcut or action.",
                "  • Custom gestures use on-device ML for recognition."
            ),
            tips = listOf(
                "Adjust gesture sensitivity in Settings → Gestures.",
                "Practice gestures a few times — the recognition improves with deliberate motion.",
                "Use voice commands ('click', 'scroll up') as an alternative to gestures.",
                "Edge Gestures (volume buttons) provide quick shortcuts without motion."
            ),
            relatedTopics = listOf("Gesture Studio", "Mouse & Touchpad Control", "Voice Commands")
        ),

        // ── Bluetooth Connection ─────────────────────────────────────────
        HelpSection(
            id = "bluetooth",
            title = "Bluetooth Connection",
            content = "Air Mouse can connect to your PC via Bluetooth HID, turning your phone into a standard Bluetooth mouse without needing the Go Server or WiFi.",
            category = HelpCategory.BLUETOOTH,
            steps = listOf(
                "Enable Bluetooth on both your phone and PC.",
                "On your phone, go to Air Mouse Settings → Connection → Bluetooth.",
                "Tap 'Scan for Devices' to discover your PC.",
                "Select your PC from the list and tap 'Pair'.",
                "Accept the pairing request on your PC (a PIN may be shown on both devices).",
                "Once paired, Air Mouse registers as a Bluetooth HID mouse.",
                "Your phone now controls the PC cursor over Bluetooth — no WiFi needed.",
                "",
                "Switching Between WiFi and Bluetooth:",
                "  • Go to Settings → Connection and choose your preferred method.",
                "  • Bluetooth has slightly higher latency than WiFi but works without a network.",
                "  • WiFi (UDP) is recommended for lowest latency."
            ),
            tips = listOf(
                "Bluetooth range is typically 10 meters (30 feet) — stay within range.",
                "Bluetooth HID mode doesn't require the Go Server to be running.",
                "If pairing fails, remove the device from your PC's Bluetooth list and try again.",
                "Some older PCs may not support Bluetooth HID — use WiFi in that case."
            ),
            relatedTopics = listOf("Connection Guide", "Troubleshooting", "Settings")
        ),

        // ── Background Mode ─────────────────────────────────────────────
        HelpSection(
            id = "background_mode",
            title = "Background Mode & WakeLock",
            content = "Background Mode keeps the connection alive even when your phone screen is off or you switch to another app. It uses Android's WakeLock to prevent the system from killing the service.",
            category = HelpCategory.BACKGROUND_MODE,
            steps = listOf(
                "Enable Background Mode in Settings → Connection → Background Mode.",
                "When enabled, Air Mouse acquires a partial WakeLock to keep the CPU active.",
                "The connection stays alive even when:",
                "  • You turn off the phone screen.",
                "  • You switch to another app.",
                "  • Android would normally suspend background processes.",
                "",
                "A persistent notification appears in the notification shade while Background Mode is active.",
                "To stop, either disable Background Mode in Settings or tap 'Disconnect' in the notification.",
                "",
                "Battery Impact:",
                "  • Background Mode increases battery usage moderately.",
                "  • Disable it when not actively using Air Mouse to conserve battery.",
                "  • Check the Battery screen in-app for real-time power consumption stats."
            ),
            tips = listOf(
                "Disable battery optimization for Air Mouse in Android Settings for best reliability.",
                "Background Mode is especially useful during presentations where you lock your phone between slides.",
                "If you notice Android killing the connection, enable Background Mode + disable battery optimization.",
                "The persistent notification shows connection status and provides a quick disconnect button."
            ),
            relatedTopics = listOf("Connection Guide", "Troubleshooting", "Settings")
        ),

        // ── Settings ─────────────────────────────────────────────────────
        HelpSection(
            id = "settings",
            title = "Settings Overview",
            content = "Customize Air Mouse's behavior to match your preferences. Here's what each setting does.",
            category = HelpCategory.SETTINGS,
            steps = listOf(
                "Sensitivity — Controls how fast the cursor moves relative to phone movement. Range: 0.1× (slow) to 5× (fast). Default: 1×.",
                "",
                "Smoothing — Applies a low-pass filter to reduce cursor jitter. Higher values = smoother but slightly delayed cursor. Range: 0–100%. Default: 50%.",
                "",
                "Acceleration — Enables mouse acceleration: slow movements = precise, fast movements = large jumps. Toggle on/off. Fine-tune the curve in Advanced Settings.",
                "",
                "Protocol — Choose between UDP (low latency, no delivery guarantee) and TCP (reliable, slightly higher latency). Default: UDP.",
                "",
                "AI Smoothing — Uses an on-device ML model for intelligent jitter reduction without adding latency. Toggle on/off.",
                "",
                "Predictive Movement — Applies a Kalman filter to predict cursor position, reducing perceived lag. Toggle on/off.",
                "",
                "Auto Re-center — Automatically re-centers the cursor reference point after a period of stillness. Prevents long-term drift.",
                "",
                "Gesture Sensitivity — Controls how easily gestures are triggered. Lower = requires more deliberate motion. Range: 1–10.",
                "",
                "Theme — Choose between Light, Dark, or System Default. Customize accent colors in the Themes screen.",
                "",
                "Connection Timeout — How long to wait before declaring a connection lost. Default: 5 seconds."
            ),
            tips = listOf(
                "Start with default settings and adjust one parameter at a time.",
                "Use Profiles to save different setting combinations for different use cases (e.g. presentations vs. gaming).",
                "If the cursor feels too fast, lower Sensitivity. If it feels laggy, enable Predictive Movement.",
                "UDP protocol is recommended for mouse control; TCP is better for keyboard/text input."
            ),
            relatedTopics = listOf("Mouse & Touchpad Control", "Calibration Guide", "Profiles")
        ),

        // ── Advanced Features ────────────────────────────────────────────
        HelpSection(
            id = "advanced",
            title = "Advanced Features",
            content = "Power-user features to get the most out of Air Mouse.",
            category = HelpCategory.ADVANCED,
            steps = listOf(
                "Voice Commands — Say 'click', 'right click', 'scroll up', 'scroll down', 'double click' for hands-free control.",
                "Custom Gestures — Train your own motion patterns in Gesture Studio and bind them to any action.",
                "Proximity Lock — Automatically locks your PC when your phone moves out of Bluetooth range, and unlocks when you return.",
                "Edge Gestures — Map volume-up/volume-down buttons to quick actions (click, scroll, etc.).",
                "Multiple Profiles — Save and switch between profiles with different sensitivity, smoothing, and gesture settings.",
                "AI Smoothing — Machine-learning powered cursor smoothing that adapts to your movement patterns.",
                "Predictive Movement — Kalman filter that predicts cursor position to compensate for network lag.",
                "Sensor Visualizer — View real-time graphs of gyroscope, accelerometer, and magnetometer data for debugging.",
                "Server Logs — View and export connection logs for troubleshooting."
            ),
            tips = listOf(
                "Enable AI features for the smoothest cursor experience.",
                "Create different profiles for presentations, general use, and gaming.",
                "Voice commands work offline using on-device speech recognition.",
                "Use Proximity Lock if you frequently step away from your PC."
            ),
            relatedTopics = listOf("Voice Commands", "Gesture Studio", "Proximity Lock")
        ),

        // ── Accessibility ────────────────────────────────────────────────
        HelpSection(
            id = "accessibility",
            title = "Accessibility Features",
            content = "Air Mouse includes accessibility options to ensure the app is usable by everyone.",
            category = HelpCategory.ACCESSIBILITY,
            steps = listOf(
                "High Contrast Mode — Increases contrast ratios for better visibility.",
                "Large Text — Enlarges all text throughout the app.",
                "Screen Reader Support — Full compatibility with Android TalkBack for voice feedback.",
                "Announce Movement — Provides spoken announcements of cursor direction and actions.",
                "Reduce Motion — Minimizes or disables animations for users who are sensitive to motion.",
                "Color Blind Mode — Adjusts the color palette to be distinguishable for common types of color blindness (deuteranopia, protanopia, tritanopia)."
            ),
            tips = listOf(
                "Enable accessibility features in Settings → Accessibility.",
                "Customize individual options to match your specific needs.",
                "All accessibility features work alongside Android's system accessibility services.",
                "If you need additional accessibility support, please contact us."
            ),
            relatedTopics = listOf("Settings", "Voice Commands")
        ),

        // ── FAQ ──────────────────────────────────────────────────────────
        HelpSection(
            id = "faq",
            title = "Frequently Asked Questions",
            content = "Quick answers to the most common questions about Air Mouse.",
            category = HelpCategory.FAQ,
            steps = listOf(
                "Q: Does Air Mouse work without an internet connection?",
                "A: Yes! It only needs a local WiFi network. No internet or cloud servers are involved.",
                "",
                "Q: What Android versions are supported?",
                "A: Android 10 (API 29) and above.",
                "",
                "Q: Does it drain a lot of battery?",
                "A: Battery usage is moderate. Disable Background Mode when not in use to save power. Check the in-app Battery screen for real-time stats.",
                "",
                "Q: Can I use Bluetooth instead of WiFi?",
                "A: Yes. Air Mouse supports Bluetooth HID mode — your phone acts as a standard Bluetooth mouse. No Go Server needed for Bluetooth.",
                "",
                "Q: Is my data private and secure?",
                "A: Absolutely. All communication is local (device-to-device on your LAN). No data is sent to the cloud, and no accounts are required.",
                "",
                "Q: What operating systems does the Go Server support?",
                "A: Windows 10/11, macOS 12+, and Linux (x86_64 and ARM64).",
                "",
                "Q: Can I control multiple PCs?",
                "A: Currently Air Mouse connects to one PC at a time. You can switch between saved connections quickly.",
                "",
                "Q: The cursor is too fast / too slow. How do I fix it?",
                "A: Go to Settings → Sensitivity and adjust the slider. You can also enable/disable acceleration.",
                "",
                "Q: How do I update the Go Server?",
                "A: Download the latest version from GitHub Releases or the Air Mouse website. The server will prompt you when an update is available.",
                "",
                "Q: Can I use Air Mouse for presentations?",
                "A: Yes! It works great as a wireless presenter. Use gestures or volume buttons for slide navigation, and enable Background Mode so the connection stays alive when your phone screen is off."
            ),
            tips = listOf(
                "Check the GitHub repository for the latest updates and release notes.",
                "Report bugs or request features on GitHub Issues.",
                "Join our Discord community for live support and tips from other users."
            ),
            relatedTopics = listOf("Getting Started", "Troubleshooting", "Settings")
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