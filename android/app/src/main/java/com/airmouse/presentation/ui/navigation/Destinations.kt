package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for Air Mouse Pro app
 * Contains all screens with their routes, titles, and icons
 */
sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector? = null
) {
    // Main Bottom Navigation Screens (4 screens)
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
    data object TouchpadSettings : Destinations("touchpad_settings", "Touchpad Settings", Icons.Default.Settings)
    
    // Onboarding
    data object Onboarding : Destinations("onboarding", "Onboarding")
    
    companion object {
        val bottomNavItems = listOf(Home, Statistics, Settings, Help)
        
        fun getBottomNavIndex(route: String?): Int = when (route) {
            Home.route -> 0
            Statistics.route -> 1
            Settings.route -> 2
            Help.route -> 3
            else -> 0
        }
        
        fun isBottomNavScreen(route: String?): Boolean = when (route) {
            Home.route, Statistics.route, Settings.route, Help.route -> true
            else -> false
        }
    }
}