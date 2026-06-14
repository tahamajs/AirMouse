package com.airmouse.presentation.ui.navigation

sealed class Destinations(val route: String) {
    object Home : Destinations("home")
    object Statistics : Destinations("statistics")
    object Settings : Destinations("settings")
    object Help : Destinations("help")
    object About : Destinations("about")
    object NetworkDiscovery : Destinations("network_discovery")
    object Profiles : Destinations("profiles")
    object GestureStudio : Destinations("gesture_studio")
    object EdgeGestures : Destinations("edge_gestures")
    object VoiceCommands : Destinations("voice_commands")
    object Themes : Destinations("themes")
    object ServerLogs : Destinations("server_logs")
    object Battery : Destinations("battery")
    object Accessibility : Destinations("accessibility")
    object Proximity : Destinations("proximity")
    object SensorVisualizer : Destinations("sensor_visualizer")
    object Calibration : Destinations("calibration")
    object Onboarding : Destinations("onboarding")
}

package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destinations(val route: String, val title: String, val icon: ImageVector? = null) {
    // Main Bottom Navigation Screens
    data object Home : Destinations("home", "Home", Icons.Default.Home)
    data object Statistics : Destinations("statistics", "Statistics", Icons.Default.BarChart)
    data object Settings : Destinations("settings", "Settings", Icons.Default.Settings)
    data object Help : Destinations("help", "Help", Icons.Default.Help)
    
    // Info Screens
    data object About : Destinations("about", "About", Icons.Default.Info)
    
    // Calibration & Sensors
    data object Calibration : Destinations("calibration", "Calibration", Icons.Default.Build)
    data object SensorVisualizer : Destinations("sensor_visualizer", "Sensor Visualizer", Icons.Default.GraphicEq)
    
    // Gesture & Touch
    data object GestureStudio : Destinations("gesture_studio", "Gesture Studio", Icons.Default.Gesture)
    data object EdgeGestures : Destinations("edge_gestures", "Edge Gestures", Icons.Default.Swipe)
    data object Touchpad : Destinations("touchpad", "Touchpad Mode", Icons.Default.TouchApp)
    
    // Connectivity
    data object NetworkDiscovery : Destinations("network_discovery", "Network Discovery", Icons.Default.Wifi)
    data object ServerLogs : Destinations("server_logs", "Server Logs", Icons.Default.ListAlt)
    
    // Security & Privacy
    data object Proximity : Destinations("proximity", "Proximity Lock", Icons.Default.LocationOn)
    data object VoiceCommands : Destinations("voice_commands", "Voice Commands", Icons.Default.Mic)
    
    // Customization
    data object Profiles : Destinations("profiles", "Profiles", Icons.Default.Person)
    data object Themes : Destinations("themes", "Themes", Icons.Default.Palette)
    
    // System
    data object Battery : Destinations("battery", "Battery Monitor", Icons.Default.BatteryFull)
    data object Accessibility : Destinations("accessibility", "Accessibility", Icons.Default.Accessibility)
    
    // Onboarding
    data object Onboarding : Destinations("onboarding", "Onboarding")
}

val bottomNavItems = listOf(
    Destinations.Home,
    Destinations.Statistics,
    Destinations.Settings,
    Destinations.Help
)

fun getSelectedBottomNavIndex(route: String?): Int {
    return when (route) {
        Destinations.Home.route -> 0
        Destinations.Statistics.route -> 1
        Destinations.Settings.route -> 2
        Destinations.Help.route -> 3
        else -> 0
    }
}