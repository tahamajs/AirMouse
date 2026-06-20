// app/src/main/java/com/airmouse/presentation/navigation/Destinations.kt
@file:Suppress("unused")

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
    companion object {
        const val ROUTE_HOME = "home"
        const val ROUTE_STATISTICS = "statistics"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_HELP = "help"
        const val ROUTE_ABOUT = "about"
        const val ROUTE_CALIBRATION = "calibration"
        const val ROUTE_SENSOR_VISUALIZER = "sensor_visualizer"
        const val ROUTE_GESTURE_STUDIO = "gesture_studio"
        const val ROUTE_EDGE_GESTURES = "edge_gestures"
        const val ROUTE_TOUCHPAD = "touchpad"
        const val ROUTE_NETWORK_DISCOVERY = "network_discovery"
        const val ROUTE_SERVER_LOGS = "server_logs"
        const val ROUTE_PROXIMITY = "proximity"
        const val ROUTE_VOICE_COMMANDS = "voice_commands"
        const val ROUTE_PROFILES = "profiles"
        const val ROUTE_THEMES = "themes"
        const val ROUTE_BATTERY = "battery"
        const val ROUTE_ACCESSIBILITY = "accessibility"
        const val ROUTE_TOUCHPAD_SETTINGS = "touchpad_settings"
        const val ROUTE_ONBOARDING = "onboarding"
        const val ROUTE_CALIBRATION_RESULT = "calibration_result"

        object Home : Destinations(ROUTE_HOME, "Home", Icons.Filled.Home)
        object Statistics : Destinations(ROUTE_STATISTICS, "Stats", Icons.Filled.BarChart)
        object Settings : Destinations(ROUTE_SETTINGS, "Settings", Icons.Filled.Settings)
        object Help : Destinations(ROUTE_HELP, "Help", Icons.AutoMirrored.Filled.Help)
        object About : Destinations(ROUTE_ABOUT, "About", Icons.Filled.Info)
        object Calibration : Destinations(ROUTE_CALIBRATION, "Calibrate", Icons.Filled.Tune)
        object SensorVisualizer : Destinations(ROUTE_SENSOR_VISUALIZER, "Sensors", Icons.Filled.Sensors)
        object GestureStudio : Destinations(ROUTE_GESTURE_STUDIO, "Gestures", Icons.Filled.Gesture)
        object EdgeGestures : Destinations(ROUTE_EDGE_GESTURES, "Edge", Icons.Filled.Swipe)
        object Touchpad : Destinations(ROUTE_TOUCHPAD, "Touchpad", Icons.Filled.TouchApp)
        object NetworkDiscovery : Destinations(ROUTE_NETWORK_DISCOVERY, "Network", Icons.Filled.Wifi)
        object ServerLogs : Destinations(ROUTE_SERVER_LOGS, "Logs", Icons.AutoMirrored.Filled.ListAlt)
        object Proximity : Destinations(ROUTE_PROXIMITY, "Proximity", Icons.Filled.NearMe)
        object VoiceCommands : Destinations(ROUTE_VOICE_COMMANDS, "Voice", Icons.Filled.Mic)
        object Profiles : Destinations(ROUTE_PROFILES, "Profiles", Icons.Filled.Person)
        object Themes : Destinations(ROUTE_THEMES, "Themes", Icons.Filled.Palette)
        object Battery : Destinations(ROUTE_BATTERY, "Battery", Icons.Filled.BatteryFull)
        object Accessibility : Destinations(ROUTE_ACCESSIBILITY, "Access", Icons.Filled.Accessibility)
        object TouchpadSettings : Destinations(ROUTE_TOUCHPAD_SETTINGS, "Touchpad Settings", Icons.Filled.Settings)
        object Onboarding : Destinations(ROUTE_ONBOARDING, "Onboarding", Icons.Filled.Apps)
        object CalibrationResult : Destinations(ROUTE_CALIBRATION_RESULT, "Calibration Result", Icons.Filled.CheckCircle)

        private val bottomNavRoutes: Set<String>
            get() = setOf(ROUTE_HOME, ROUTE_STATISTICS, ROUTE_SETTINGS, ROUTE_HELP)

        val bottomNavDestinations: List<Destinations>
            get() = listOf(Home, Statistics, Settings, Help)

        fun isBottomNavScreen(route: String?): Boolean {
            return route != null && bottomNavRoutes.contains(route)
        }

        fun fromRoute(route: String): Destinations? {
            return when (route) {
                ROUTE_HOME -> Home
                ROUTE_STATISTICS -> Statistics
                ROUTE_SETTINGS -> Settings
                ROUTE_HELP -> Help
                ROUTE_ABOUT -> About
                ROUTE_CALIBRATION -> Calibration
                ROUTE_SENSOR_VISUALIZER -> SensorVisualizer
                ROUTE_GESTURE_STUDIO -> GestureStudio
                ROUTE_EDGE_GESTURES -> EdgeGestures
                ROUTE_TOUCHPAD -> Touchpad
                ROUTE_NETWORK_DISCOVERY -> NetworkDiscovery
                ROUTE_SERVER_LOGS -> ServerLogs
                ROUTE_PROXIMITY -> Proximity
                ROUTE_VOICE_COMMANDS -> VoiceCommands
                ROUTE_PROFILES -> Profiles
                ROUTE_THEMES -> Themes
                ROUTE_BATTERY -> Battery
                ROUTE_ACCESSIBILITY -> Accessibility
                ROUTE_TOUCHPAD_SETTINGS -> TouchpadSettings
                ROUTE_ONBOARDING -> Onboarding
                ROUTE_CALIBRATION_RESULT -> CalibrationResult
                else -> null
            }
        }
    }
}
