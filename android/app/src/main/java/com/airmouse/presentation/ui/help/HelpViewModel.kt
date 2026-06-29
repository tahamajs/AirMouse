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
                "Complete first-time setup walkthrough:",
                "1. Install the Air Mouse app on your Android phone.",
                "2. Download and install the Air Mouse Go Server on your PC.",
                "3. Make sure both devices are connected to the same WiFi network.",
                "4. Open the app and enter the IP address shown in the Go Server window, or use Network Discovery.",
                "5. Tap 'Connect' — a green status indicator confirms the link.",
                "6. Calibrate your phone's sensors from the Calibration screen for best accuracy.",
                "System Requirements:",
                "  • Android 10+ (API 29 and above)",
                "  • PC OS support: Windows 10/11, macOS 12+, Linux (x86_64, ARM64)",
                "  • Network: Local WiFi (5GHz recommended for low latency) or Bluetooth",
                "Quick start in under 2 minutes:",
                "Install server, run it, open app on same WiFi, tap auto-scan, connect, move phone!"
            ),
            tips = listOf(
                "Keep your phone steady during initial calibration for the best results.",
                "Start with medium sensitivity and adjust later in Settings.",
                "Use the 'Quick Start' widget to reconnect instantly."
            ),
            relatedTopics = listOf("Connection Guide", "Calibration Guide", "Server Setup Guide")
        ),

        // ── Connection Guide ─────────────────────────────────────────────
        HelpSection(
            id = "connection",
            title = "Connection Guide",
            content = "Detailed instructions for connecting your phone to the PC server securely and reliably.",
            category = HelpCategory.CONNECTION,
            steps = listOf(
                "WiFi Connection Step-by-Step:",
                "  1. Launch the Go Server on your PC. It will display an IP address (e.g. 192.168.1.42) and port (default 8080).",
                "  2. On your phone, tap 'Connect' and enter the IP and port, or use Network Discovery.",
                "UDP vs TCP vs WebSocket:",
                "  • UDP: Fastest, lowest latency. Best for mouse movement and gaming. Can drop packets.",
                "  • TCP: Reliable delivery, slightly higher latency. Best for file transfer and keyboard input.",
                "  • WebSocket: Uses HTTP/S. Good for bypassing restrictive firewalls.",
                "Bluetooth Connection:",
                "  • Go to Settings -> Connection -> Bluetooth.",
                "  • Pair your phone with the PC. No server needed; phone acts as a standard Bluetooth HID mouse.",
                "Network Discovery (Auto-scan):",
                "  • The app broadcasts a UDP packet to find the server automatically. Tap 'Scan' to use this.",
                "QR Code Scanning:",
                "  • Tap 'Scan QR Code' in the app and point your camera at the QR code shown in the PC server.",
                "Manual IP Entry:",
                "  • Type the IP directly if auto-scan fails. Find your PC's IP in your router settings or via 'ipconfig'/'ifconfig'.",
                "Firewall Configuration:",
                "  • Windows: Allow the Go Server through Windows Defender Firewall.",
                "  • Mac: Allow the app in System Settings > Network > Firewall.",
                "  • Linux: Ensure ports (8080 by default) are open in ufw or iptables.",
                "Troubleshooting Connection Drops:",
                "  • Use 5GHz WiFi.",
                "  • Disable Android battery optimization for the app.",
                "  • Enable Background Mode.",
                "Saving Server Profiles:",
                "  • Once connected, tap the 'Save' icon to add the server to your saved profiles list for quick access."
            ),
            tips = listOf(
                "Use 5 GHz WiFi instead of 2.4 GHz for noticeably lower latency.",
                "If connection fails, check your PC's firewall settings."
            ),
            relatedTopics = listOf("Troubleshooting", "Bluetooth", "Background Mode")
        ),

        // ── Mouse / Touchpad Control ─────────────────────────────────────
        HelpSection(
            id = "mouse_control",
            title = "Mouse & Touchpad Control",
            content = "Air Mouse supports motion-based (gyroscope) control and a virtual touchpad. Learn all the controls and gestures.",
            category = HelpCategory.MOUSE_CONTROL,
            steps = listOf(
                "Gyroscope/Motion Mode:",
                "  • Tilt / rotate your phone to move the cursor on-screen.",
                "  • Hold the phone naturally, screen facing you.",
                "Touchpad Mode (All Gestures):",
                "  • Move Cursor: Swipe one finger.",
                "  • Left Click: Tap once.",
                "  • Right Click: Tap with two fingers.",
                "  • Middle Click: Tap with three fingers.",
                "  • Scrolling (Vertical & Horizontal): Swipe two fingers in the desired direction.",
                "  • Click-and-Drag: Double-tap, hold on the second tap, and drag.",
                "  • Multi-finger gestures: Three-finger swipe up for task switcher, down for desktop.",
                "Sensitivity Adjustment:",
                "  • Go to Settings > Sensitivity. Adjust the slider to change cursor speed.",
                "AI Smoothing:",
                "  • Machine learning-based algorithm that reduces hand jitter without adding input lag. Enable it in Settings."
            ),
            tips = listOf(
                "Switch between Motion and Touchpad mode from the main screen's mode selector.",
                "Enable 'AI Smoothing' for smoother, less jittery cursor movement."
            ),
            relatedTopics = listOf("Gestures", "Calibration Guide", "Settings")
        ),

        // ── Calibration ─────────────────────────────────────────────────
        HelpSection(
            id = "calibration",
            title = "Calibration Guide",
            content = "Calibration aligns your phone's sensors so cursor movement is accurate and drift-free. Crucial for motion mode.",
            category = HelpCategory.CALIBRATION,
            steps = listOf(
                "Why Calibration Matters:",
                "  • Sensors drift over time due to temperature changes and magnetic interference. Calibration resets them to a zero state.",
                "Gyroscope Calibration:",
                "  • Keep phone flat on a table. Do not touch it.",
                "  • The numbers show the rotational offset being zeroed out. Wait 10 seconds.",
                "Magnetometer Calibration:",
                "  • Move your phone in a figure-8 motion in the air.",
                "  • Avoid large metal objects or strong magnetic fields.",
                "Accelerometer Calibration (6 positions):",
                "  1. Face up on table.",
                "  2. Face down on table.",
                "  3. Right edge down.",
                "  4. Left edge down.",
                "  5. Top edge down.",
                "  6. Bottom edge down.",
                "  • Hold each for 5 seconds.",
                "Calibration Quality Ratings:",
                "  • Excellent: Sensors perfectly aligned. No drift.",
                "  • Good: Minor variance, acceptable for normal use.",
                "  • Fair: Noticeable drift, recalibration recommended.",
                "  • Poor: Failed calibration, please retry.",
                "When to Recalibrate:",
                "  • When the cursor drifts on its own, or movements feel 'off'.",
                "Troubleshooting Bad Calibration:",
                "  • Move away from electronics.",
                "  • Ensure the surface is perfectly flat during gyro calibration."
            ),
            tips = listOf(
                "Calibrate on a flat, stable surface away from magnets and electronics.",
                "Calibration data persists across app restarts."
            ),
            relatedTopics = listOf("Troubleshooting", "Mouse Control")
        ),

        // ── Gestures & Shortcuts ─────────────────────────────────────────
        HelpSection(
            id = "gestures",
            title = "Gestures & Shortcuts",
            content = "Speed up your workflow using custom gestures and keyboard shortcuts.",
            category = HelpCategory.GESTURES,
            steps = listOf(
                "All Available Gestures:",
                "  • Flick left/right/up/down.",
                "  • Shake phone.",
                "  • Figure-8 motion.",
                "Edge Gestures:",
                "  • Configure swipe-from-edge actions (e.g., swipe from left edge to go Back).",
                "  • Go to Settings > Gestures > Edge Gestures.",
                "Custom Shortcut Mapping:",
                "  • Map any gesture to a custom action (e.g., open browser, mute volume).",
                "Keyboard Shortcuts Sent to PC:",
                "  • You can bind gestures to send specific keystrokes (e.g., Ctrl+C, Ctrl+V).",
                "Gaming Mode Gestures:",
                "  • In Gaming Mode, gestures can be mapped to reload, jump, or crouch."
            ),
            tips = listOf(
                "Adjust gesture sensitivity in Settings if gestures trigger too easily."
            ),
            relatedTopics = listOf("Gaming Mode", "Voice Commands")
        ),

        // ── Gaming Mode ──────────────────────────────────────────────────
        HelpSection(
            id = "gaming",
            title = "Gaming Mode",
            content = "Optimize Air Mouse for playing PC games.",
            category = HelpCategory.GAMING,
            steps = listOf(
                "How to Enable Gaming Mode:",
                "  • Select the 'Gaming' profile from the Profiles menu, or toggle Gaming Mode on the main screen.",
                "Virtual Gamepad Controls:",
                "  • Shows an on-screen D-pad, analog stick, and action buttons (A, B, X, Y).",
                "Motion-based Game Controls:",
                "  • Steer in racing games by tilting your phone left/right like a steering wheel.",
                "Button Mapping Customization:",
                "  • Long-press any virtual button to remap it to a keyboard key or mouse click.",
                "Latency Optimization for Gaming:",
                "  • Forces UDP protocol.",
                "  • Disables battery saving features to ensure maximum polling rate.",
                "Supported Game Types:",
                "  • Racing simulators, flight simulators, casual platformers, and point-and-click games."
            ),
            tips = listOf(
                "Keep your phone plugged into a charger during long gaming sessions, as Gaming Mode uses more battery."
            ),
            relatedTopics = listOf("Profiles", "Connection Guide")
        ),

        // ── Screen Mirroring ─────────────────────────────────────────────
        HelpSection(
            id = "screen_mirroring",
            title = "Screen Mirroring",
            content = "View your PC screen on your phone.",
            category = HelpCategory.SCREEN_MIRRORING,
            steps = listOf(
                "How to Set Up Screen Mirroring:",
                "  • Tap the 'Screen Mirror' icon in the app.",
                "  • Ensure the PC server has screen capture permissions enabled.",
                "Quality Settings:",
                "  • Low: 480p, 30fps (Best for low latency/poor network).",
                "  • Medium: 720p, 30fps.",
                "  • High: 1080p, 60fps (Requires strong 5GHz WiFi).",
                "Troubleshooting Lag:",
                "  • Switch to a lower quality setting.",
                "  • Move closer to your router.",
                "  • Use TCP protocol if UDP packets are dropping excessively."
            ),
            tips = listOf(
                "Screen mirroring consumes significant battery. Use it only when necessary."
            ),
            relatedTopics = listOf("Battery & Performance")
        ),

        // ── File Transfer ────────────────────────────────────────────────
        HelpSection(
            id = "file_transfer",
            title = "File Transfer",
            content = "Send files wirelessly between your phone and PC.",
            category = HelpCategory.FILE_TRANSFER,
            steps = listOf(
                "How to Send Files Between Phone and PC:",
                "  • To PC: Tap 'Send File' in the app menu, select the file. It will be saved to the PC's Downloads folder.",
                "  • To Phone: Drag and drop a file onto the Go Server window on your PC.",
                "Supported File Types:",
                "  • All file types are supported (images, documents, videos, apks, etc.).",
                "Transfer Speed Optimization:",
                "  • File transfers automatically use the reliable TCP protocol.",
                "  • For large files, ensure both devices are on a fast 5GHz WiFi network."
            ),
            tips = listOf(
                "You can change the default save location on the PC via the Go Server settings."
            ),
            relatedTopics = listOf("Connection Guide")
        ),

        // ── Voice Commands ───────────────────────────────────────────────
        HelpSection(
            id = "voice_commands",
            title = "Voice Commands",
            content = "Control your PC hands-free using your voice.",
            category = HelpCategory.VOICE_COMMANDS,
            steps = listOf(
                "How to Enable Voice Control:",
                "  • Tap the microphone icon, or enable 'Always Listening' in Voice Settings.",
                "Available Voice Commands:",
                "  • 'Click', 'Right Click', 'Double Click'",
                "  • 'Scroll Up', 'Scroll Down'",
                "  • 'Volume Up', 'Volume Down', 'Mute'",
                "  • 'Play', 'Pause', 'Next Track'",
                "  • 'Open Browser', 'Show Desktop'",
                "Custom Voice Commands:",
                "  • Go to Settings > Voice Commands > Custom to add your own trigger words and map them to keystrokes.",
                "Microphone Permissions:",
                "  • The app requires 'Record Audio' permission to process voice commands. Processing is done locally on-device."
            ),
            tips = listOf(
                "Voice commands work best in quiet environments."
            ),
            relatedTopics = listOf("Advanced")
        ),

        // ── Themes & Appearance ──────────────────────────────────────────
        HelpSection(
            id = "themes",
            title = "Themes & Appearance",
            content = "Customize the look and feel of the app.",
            category = HelpCategory.THEMES,
            steps = listOf(
                "How to Change Themes:",
                "  • Go to Settings > Appearance.",
                "Dark Mode vs Light Mode:",
                "  • Choose Dark, Light, or System Default.",
                "Available Theme Presets:",
                "  • Midnight Blue, Amoled Black, Forest Green, Cyberpunk.",
                "Color Customization:",
                "  • Use the color picker to set a custom primary accent color."
            ),
            tips = listOf(
                "Amoled Black theme saves battery on OLED screens."
            ),
            relatedTopics = listOf("Settings")
        ),

        // ── Notifications ────────────────────────────────────────────────
        HelpSection(
            id = "notifications",
            title = "Notifications",
            content = "Manage how Air Mouse alerts you.",
            category = HelpCategory.NOTIFICATIONS,
            steps = listOf(
                "Notification Types Explained:",
                "  • Connection Status: Alerts when connected/disconnected.",
                "  • Background Service: Persistent notification keeping the app alive.",
                "  • Battery Warnings: Alerts when phone battery is low.",
                "How to Configure Notifications:",
                "  • Go to Settings > Notifications to toggle specific alerts.",
                "Connection Notifications:",
                "  • Haptic feedback and sound on connect/disconnect.",
                "Battery Warnings:",
                "  • Prompts you to disable high-drain features (like Screen Mirroring) when battery drops below 20%."
            ),
            tips = listOf(
                "Keep the Background Service notification enabled for a stable connection when the app is minimized."
            ),
            relatedTopics = listOf("Background Mode", "Battery & Performance")
        ),

        // ── Battery & Performance ────────────────────────────────────────
        HelpSection(
            id = "battery",
            title = "Battery & Performance",
            content = "Optimize battery usage and performance.",
            category = HelpCategory.BATTERY,
            steps = listOf(
                "Battery Optimization Tips:",
                "  • Use the Touchpad instead of Motion Mode when possible (sensors use more power).",
                "  • Use Amoled Black theme on OLED screens.",
                "  • Lower the sensor polling rate in Advanced Settings.",
                "Background Mode Settings:",
                "  • Allows the app to run when the screen is locked, but consumes battery via WakeLocks.",
                "Reducing Battery Drain:",
                "  • Disable Screen Mirroring and always-listening Voice Commands.",
                "Performance vs Battery Tradeoffs:",
                "  • High polling rate = smoother cursor but higher battery drain.",
                "  • AI Smoothing requires processing power, slightly increasing battery usage."
            ),
            tips = listOf(
                "Check the in-app Battery monitor for real-time consumption stats."
            ),
            relatedTopics = listOf("Settings", "Background Mode")
        ),

        // ── Profiles ─────────────────────────────────────────────────────
        HelpSection(
            id = "profiles",
            title = "Profiles",
            content = "Save and switch between different configuration sets.",
            category = HelpCategory.PROFILES,
            steps = listOf(
                "Creating Usage Profiles:",
                "  • Go to Profiles > Add New.",
                "  • Recommended profiles: Work (standard settings), Gaming (high sensitivity, UDP), Presentation (low sensitivity, volume button clicks).",
                "Switching Between Profiles:",
                "  • Tap the profile name at the top of the main screen to quick-switch.",
                "Profile-Specific Settings:",
                "  • Each profile saves its own Sensitivity, Protocol, Theme, and Custom Gestures."
            ),
            tips = listOf(
                "Profiles are automatically backed up to your Google account."
            ),
            relatedTopics = listOf("Settings", "Gaming Mode")
        ),

        // ── Accessibility ────────────────────────────────────────────────
        HelpSection(
            id = "accessibility",
            title = "Accessibility Options",
            content = "Features to make the app usable by everyone.",
            category = HelpCategory.ACCESSIBILITY,
            steps = listOf(
                "Accessibility Features Overview:",
                "  • Designed to be compatible with standard Android accessibility tools.",
                "Large Text Mode:",
                "  • Scales all text in the app. Matches system font size settings.",
                "High Contrast:",
                "  • Increases contrast ratios for better visibility of buttons and text.",
                "Screen Reader Compatibility:",
                "  • Full support for Android TalkBack. Every interactive element has content descriptions.",
                "Motor Accessibility Options:",
                "  • 'Tap and Hold to Click' instead of quick taps.",
                "  • 'Stabilization' filter to ignore hand tremors."
            ),
            tips = listOf(
                "If you rely on Voice Access, use the Voice Commands feature natively built into the app for faster response."
            ),
            relatedTopics = listOf("Settings")
        ),

        // ── Server Setup Guide ───────────────────────────────────────────
        HelpSection(
            id = "server_setup",
            title = "Server Setup Guide",
            content = "How to install and configure the Go Server on your PC.",
            category = HelpCategory.SERVER_SETUP,
            steps = listOf(
                "Installing Go Server on Windows:",
                "  1. Download the .exe from our website.",
                "  2. Run the installer.",
                "  3. Allow through Windows Firewall when prompted.",
                "Installing Go Server on macOS:",
                "  1. Download the .dmg.",
                "  2. Drag the app to the Applications folder.",
                "  3. Go to System Settings > Privacy & Security > Accessibility and grant permission to Air Mouse Go Server (required for moving the mouse).",
                "Installing Go Server on Linux:",
                "  1. Download the AppImage or use the apt repository.",
                "  2. Make executable: chmod +x AirMouseServer.AppImage",
                "  3. Note: Requires uinput permissions (see documentation).",
                "Server Configuration Options:",
                "  • Right-click the system tray icon to access Settings: change port, toggle startup on boot, set password.",
                "Running Server on Startup:",
                "  • Enable 'Launch at Login' in the server settings.",
                "Multi-user Setup:",
                "  • The server runs per-user. Each user account on the PC must run their own instance."
            ),
            tips = listOf(
                "If the mouse won't move on Mac, you MUST grant Accessibility permissions in System Settings."
            ),
            relatedTopics = listOf("Connection Guide", "Troubleshooting")
        ),

        // ── Troubleshooting ─────────────────────────────────────────────
        HelpSection(
            id = "troubleshooting",
            title = "Troubleshooting",
            content = "Solutions for common issues.",
            category = HelpCategory.TROUBLESHOOTING,
            steps = listOf(
                "Connection won't establish:",
                "  • Verify both devices are on the exact same WiFi network/subnet.",
                "  • Check PC firewall settings. Disable it temporarily to test.",
                "Cursor is jittery or jumpy:",
                "  • Enable AI Smoothing in settings.",
                "  • Switch to 5GHz WiFi.",
                "Cursor moves in wrong direction:",
                "  • Recalibrate the Accelerometer.",
                "High latency / input lag:",
                "  • Use UDP protocol.",
                "  • Close background downloads.",
                "App crashes on startup:",
                "  • Clear app cache and data in Android settings.",
                "Sensor not detected:",
                "  • Ensure your phone has a physical gyroscope. Some budget phones lack it.",
                "Bluetooth pairing fails:",
                "  • Remove the device from PC Bluetooth settings and try again.",
                "Server not found on network:",
                "  • Ensure your router doesn't have 'AP Isolation' or 'Client Isolation' enabled.",
                "Calibration keeps failing:",
                "  • Keep the phone perfectly still during Gyro calibration.",
                "Phone gets hot during use:",
                "  • Disable Screen Mirroring and always-on Voice Commands."
            ),
            tips = listOf(
                "Restarting both the app and the PC server fixes 90% of issues."
            ),
            relatedTopics = listOf("Connection Guide", "Calibration Guide")
        ),

        // ── FAQ ──────────────────────────────────────────────────────────
        HelpSection(
            id = "faq",
            title = "Frequently Asked Questions",
            content = "Answers to common questions.",
            category = HelpCategory.FAQ,
            steps = listOf(
                "Q: Does it work without WiFi? A: Yes, via Bluetooth mode.",
                "Q: What's the maximum range? A: WiFi range depends on your router (typically up to 150ft). Bluetooth is ~30ft.",
                "Q: Can I use it as a presentation remote? A: Yes, use volume buttons or gestures for slides.",
                "Q: Does it work with games? A: Yes, enable Gaming Mode for low latency and custom buttons.",
                "Q: How secure is the connection? A: 100% local. Optional AES encryption can be enabled in settings.",
                "Q: Can multiple phones connect to one PC? A: Currently, only one active connection is supported.",
                "Q: Does it work on all Android versions? A: Android 10 and newer.",
                "Q: What sensors does my phone need? A: Gyroscope and Accelerometer are required for motion mode.",
                "Q: Can I use it with a tablet? A: Yes, the UI is optimized for tablets too.",
                "Q: How do I update the app? A: Via the Google Play Store.",
                "Q: Is my data sent to any servers? A: No, everything is completely local.",
                "Q: Can I use keyboard input too? A: Yes, tap the keyboard icon.",
                "Q: What's the difference between UDP and TCP? A: UDP is faster (less lag), TCP is more reliable (no dropped inputs).",
                "Q: Why does calibration matter? A: It removes drift so the cursor only moves when you intend it to.",
                "Q: Can I customize the touchpad size? A: Yes, in Accessibility settings.",
                "Q: Does it support mouse acceleration? A: Yes, toggle it in Settings.",
                "Q: How do I reduce battery usage? A: Use Amoled Black theme and disable Screen Mirroring.",
                "Q: Can I lock the screen and still use it? A: Yes, if Background Mode is enabled.",
                "Q: What ports does the server use? A: Default is 8080 (TCP/UDP), customizable in server settings.",
                "Q: How do I report bugs? A: Use the Feedback dialog in the Help Screen."
            ),
            tips = listOf(
                "Join our Discord for more help!"
            ),
            relatedTopics = listOf("Troubleshooting", "Settings")
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