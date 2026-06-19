package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Navigation destinations for the Air Mouse application.
 * Each destination represents a screen in the app.
 */
sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector? = null,
    val isBottomNav: Boolean = false
) {

    // ==================== BOTTOM NAVIGATION ====================
    object Home : Destinations(
        route = "home",
        title = "Home",
        icon = Icons.Default.Home,
        isBottomNav = true
    )

    object Statistics : Destinations(
        route = "statistics",
        title = "Statistics",
        icon = Icons.Default.BarChart,
        isBottomNav = true
    )

    object Settings : Destinations(
        route = "settings",
        title = "Settings",
        icon = Icons.Default.Settings,
        isBottomNav = true
    )

    object Help : Destinations(
        route = "help",
        title = "Help",
        icon = Icons.Default.Help,
        isBottomNav = true
    )

    // ==================== INFO ====================
    object About : Destinations(
        route = "about",
        title = "About",
        icon = Icons.Default.Info
    )

    // ==================== CALIBRATION & SENSORS ====================
    object Calibration : Destinations(
        route = "calibration",
        title = "Calibration",
        icon = Icons.Default.Tune
    )

    object SensorVisualizer : Destinations(
        route = "sensor_visualizer",
        title = "Sensor Visualizer",
        icon = Icons.Default.Analytics
    )

    // ==================== GESTURE & TOUCH ====================
    object GestureStudio : Destinations(
        route = "gesture_studio",
        title = "Gesture Studio",
        icon = Icons.Default.Gesture
    )

    object EdgeGestures : Destinations(
        route = "edge_gestures",
        title = "Edge Gestures",
        icon = Icons.Default.TouchApp
    )

    object Touchpad : Destinations(
        route = "touchpad",
        title = "Touchpad",
        icon = Icons.Default.Laptop
    )

    // ==================== CONNECTIVITY ====================
    object NetworkDiscovery : Destinations(
        route = "network_discovery",
        title = "Network Discovery",
        icon = Icons.Default.Wifi
    )

    object ServerLogs : Destinations(
        route = "server_logs",
        title = "Server Logs",
        icon = Icons.Default.ListAlt
    )

    // ==================== SECURITY & PRIVACY ====================
    object Proximity : Destinations(
        route = "proximity",
        title = "Proximity",
        icon = Icons.Default.Bluetooth
    )

    object VoiceCommands : Destinations(
        route = "voice_commands",
        title = "Voice Commands",
        icon = Icons.Default.Mic
    )

    // ==================== CUSTOMIZATION ====================
    object Profiles : Destinations(
        route = "profiles",
        title = "Profiles",
        icon = Icons.Default.Person
    )

    object Themes : Destinations(
        route = "themes",
        title = "Themes",
        icon = Icons.Default.Palette
    )

    // ==================== SYSTEM ====================
    object Battery : Destinations(
        route = "battery",
        title = "Battery",
        icon = Icons.Default.BatteryFull
    )

    object Accessibility : Destinations(
        route = "accessibility",
        title = "Accessibility",
        icon = Icons.Default.Accessibility
    )

    // ==================== ONBOARDING ====================
    object Onboarding : Destinations(
        route = "onboarding",
        title = "Onboarding",
        icon = null
    )

    // ==================== TOUCHPAD SETTINGS (ALIAS) ====================
    object TouchpadSettings : Destinations(
        route = "touchpad_settings",
        title = "Touchpad Settings",
        icon = Icons.Default.Laptop
    )

    companion object {
        /**
         * All bottom navigation destinations
         */
        val bottomNavDestinations: List<Destinations> = listOf(
            Home, Statistics, Settings, Help
        )

        /**
         * All destinations
         */
        val allDestinations: List<Destinations> = listOf(
            Home, Statistics, Settings, Help,
            About, Calibration, SensorVisualizer,
            GestureStudio, EdgeGestures, Touchpad,
            NetworkDiscovery, ServerLogs,
            Proximity, VoiceCommands,
            Profiles, Themes,
            Battery, Accessibility,
            Onboarding, TouchpadSettings
        )

        /**
         * Check if a route is a bottom navigation screen
         */
        fun isBottomNavScreen(route: String?): Boolean {
            return bottomNavDestinations.any { it.route == route }
        }

        /**
         * Get a destination by route
         */
        fun fromRoute(route: String?): Destinations? {
            return allDestinations.find { it.route == route }
        }

        /**
         * Get the title for a destination
         */
        fun getTitle(destination: Destinations): String = destination.title

        /**
         * Get the title by route
         */
        fun getTitleByRoute(route: String?): String {
            return fromRoute(route)?.title ?: "Unknown"
        }

        /**
         * Get the icon for a destination
         */
        fun getIcon(destination: Destinations): ImageVector? = destination.icon

        /**
         * Get the icon by route
         */
        fun getIconByRoute(route: String?): ImageVector? {
            return fromRoute(route)?.icon
        }

        /**
         * Check if a route is valid
         */
        fun isValidRoute(route: String?): Boolean {
            return allDestinations.any { it.route == route }
        }

        /**
         * Get the start destination (Home)
         */
        fun getStartDestination(): Destinations = Home

        /**
         * Get the onboarding destination
         */
        fun getOnboardingDestination(): Destinations = Onboarding

        /**
         * Get the parent destination for a given destination
         */
        fun getParent(destination: Destinations): Destinations? {
            return when (destination) {
                About, Calibration, SensorVisualizer,
                GestureStudio, EdgeGestures, Touchpad,
                NetworkDiscovery, ServerLogs,
                Proximity, VoiceCommands,
                Profiles, Themes,
                Battery, Accessibility,
                TouchpadSettings -> Home
                else -> null
            }
        }
    }
}