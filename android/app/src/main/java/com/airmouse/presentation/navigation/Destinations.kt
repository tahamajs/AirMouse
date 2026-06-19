// app/src/main/java/com/airmouse/presentation/navigation/Destinations.kt
package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {
    object Home : Destinations("home", "Home", Icons.Filled.Home)
    object Statistics : Destinations("statistics", "Stats", Icons.Filled.BarChart)
    object Settings : Destinations("settings", "Settings", Icons.Filled.Settings)
    object Help : Destinations("help", "Help", Icons.AutoMirrored.Filled.Help)
    object About : Destinations("about", "About", Icons.Filled.Info)
    object Calibration : Destinations("calibration", "Calibrate", Icons.Filled.Tune)
    object SensorVisualizer : Destinations("sensor_visualizer", "Sensors", Icons.Filled.Sensors)
    object GestureStudio : Destinations("gesture_studio", "Gestures", Icons.Filled.Gesture)
    object EdgeGestures : Destinations("edge_gestures", "Edge", Icons.Filled.Swipe)
    object Touchpad : Destinations("touchpad", "Touchpad", Icons.Filled.TouchApp)
    object NetworkDiscovery : Destinations("network_discovery", "Network", Icons.Filled.Wifi)
    object ServerLogs : Destinations("server_logs", "Logs", Icons.AutoMirrored.Filled.ListAlt)
    object Proximity : Destinations("proximity", "Proximity", Icons.Filled.NearMe)
    object VoiceCommands : Destinations("voice_commands", "Voice", Icons.Filled.Mic)
    object Profiles : Destinations("profiles", "Profiles", Icons.Filled.Person)
    object Themes : Destinations("themes", "Themes", Icons.Filled.Palette)
    object Battery : Destinations("battery", "Battery", Icons.Filled.BatteryFull)
    object Accessibility : Destinations("accessibility", "Access", Icons.Filled.Accessibility)
    object TouchpadSettings : Destinations("touchpad_settings", "Touchpad Settings", Icons.Filled.Settings)
    object Onboarding : Destinations("onboarding", "Onboarding", Icons.Filled.Apps)

    companion object {
        private val bottomNavRoutes = setOf(
            Home.route,
            Statistics.route,
            Settings.route,
            Help.route
        )

        val bottomNavDestinations = listOf(Home, Statistics, Settings, Help)

        fun isBottomNavScreen(route: String?): Boolean {
            return route != null && bottomNavRoutes.contains(route)
        }

        fun fromRoute(route: String): Destinations? {
            return when (route) {
                Home.route -> Home
                Statistics.route -> Statistics
                Settings.route -> Settings
                Help.route -> Help
                About.route -> About
                Calibration.route -> Calibration
                SensorVisualizer.route -> SensorVisualizer
                GestureStudio.route -> GestureStudio
                EdgeGestures.route -> EdgeGestures
                Touchpad.route -> Touchpad
                NetworkDiscovery.route -> NetworkDiscovery
                ServerLogs.route -> ServerLogs
                Proximity.route -> Proximity
                VoiceCommands.route -> VoiceCommands
                Profiles.route -> Profiles
                Themes.route -> Themes
                Battery.route -> Battery
                Accessibility.route -> Accessibility
                TouchpadSettings.route -> TouchpadSettings
                Onboarding.route -> Onboarding
                else -> null
            }
        }
    }
}