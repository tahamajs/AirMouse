package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.navigation.NavType

/**
 * Central navigation destinations for the entire app.
 * Provides type-safe routes, titles, icons, and argument handling.
 */
sealed class Destinations(
    val route: String,
    val title: String = "",
    val icon: ImageVector? = null,
    val showInBottomBar: Boolean = false
) {
    // ==================== BOTTOM NAVIGATION SCREENS ====================
    object Home : Destinations(
        route = "home",
        title = "Home",
        icon = Icons.Outlined.Home,
        showInBottomBar = true
    )

    object Statistics : Destinations(
        route = "statistics",
        title = "Statistics",
        icon = Icons.Outlined.BarChart,
        showInBottomBar = true
    )

    object GestureStudio : Destinations(
        route = "gesture_studio",
        title = "Gesture Studio",
        icon = Icons.Outlined.Gesture,
        showInBottomBar = true
    )

    object Settings : Destinations(
        route = "settings",
        title = "Settings",
        icon = Icons.Outlined.Settings,
        showInBottomBar = true
    )

    // ==================== HELP & INFO ====================
    object Help : Destinations(
        route = "help",
        title = "Help",
        icon = Icons.Outlined.Help
    )

    object About : Destinations(
        route = "about",
        title = "About",
        icon = Icons.Outlined.Info
    )

    // ==================== ADVANCED FEATURES ====================
    object NetworkDiscovery : Destinations(
        route = "network_discovery",
        title = "Network Discovery",
        icon = Icons.Outlined.Wifi
    )

    object Profiles : Destinations(
        route = "profiles",
        title = "Profiles",
        icon = Icons.Outlined.Person
    )

    object EdgeGestures : Destinations(
        route = "edge_gestures",
        title = "Edge Gestures",
        icon = Icons.Outlined.Swipe
    )

    object VoiceCommands : Destinations(
        route = "voice_commands",
        title = "Voice Commands",
        icon = Icons.Outlined.Mic
    )

    object Themes : Destinations(
        route = "themes",
        title = "Themes",
        icon = Icons.Outlined.Palette
    )

    object ServerLogs : Destinations(
        route = "server_logs",
        title = "Server Logs",
        icon = Icons.Outlined.ListAlt
    )

    object Battery : Destinations(
        route = "battery",
        title = "Battery Monitor",
        icon = Icons.Outlined.BatteryFull
    )

    object Accessibility : Destinations(
        route = "accessibility",
        title = "Accessibility",
        icon = Icons.Outlined.Accessibility
    )

    object Proximity : Destinations(
        route = "proximity",
        title = "Proximity Lock",
        icon = Icons.Outlined.LocationOn
    )

    object SensorVisualizer : Destinations(
        route = "sensor_visualizer",
        title = "Sensor Visualizer",
        icon = Icons.Outlined.GraphicEq
    )

    object Calibration : Destinations(
        route = "calibration",
        title = "Calibration",
        icon = Icons.Outlined.Build
    )

    object Touchpad : Destinations(
        route = "touchpad",
        title = "Touchpad Mode",
        icon = Icons.Outlined.TouchApp
    )

    // ==================== ONBOARDING ====================
    object Onboarding : Destinations(
        route = "onboarding",
        title = "Onboarding"
    )

    // ==================== PARAMETERISED ROUTES ====================
    object CalibrationWithStep : Destinations(
        route = "calibration/{step}",
        title = "Calibration Step"
    ) {
        const val STEP_ARG = "step"
        fun createRoute(step: Int) = "calibration/$step"
        fun getStep(arguments: Map<String, String>): Int? =
            arguments[STEP_ARG]?.toIntOrNull()
    }

    object GestureDetail : Destinations(
        route = "gesture/{gestureName}",
        title = "Gesture Details"
    ) {
        const val GESTURE_NAME_ARG = "gestureName"
        fun createRoute(name: String) = "gesture/$name"
        fun getGestureName(arguments: Map<String, String>): String? =
            arguments[GESTURE_NAME_ARG]
    }

    object ProfileDetail : Destinations(
        route = "profile/{profileName}",
        title = "Profile Details"
    ) {
        const val PROFILE_NAME_ARG = "profileName"
        fun createRoute(name: String) = "profile/$name"
        fun getProfileName(arguments: Map<String, String>): String? =
            arguments[PROFILE_NAME_ARG]
    }

    // ==================== COMPANION ====================
    companion object {
        /**
         * List of destinations shown in the bottom navigation bar.
         * Order determines their position.
         */
        val bottomNavDestinations = listOf(Home, Statistics, GestureStudio, Settings)

        /**
         * All destinations for reference (useful for debugging or dynamic menus).
         */
        val allDestinations = listOf(
            Home, Statistics, GestureStudio, Settings,
            Help, About,
            NetworkDiscovery, Profiles, EdgeGestures, VoiceCommands,
            Themes, ServerLogs, Battery, Accessibility,
            Proximity, SensorVisualizer, Calibration, Touchpad,
            Onboarding
        )

        /**
         * Get the bottom navigation index for a given route.
         */
        fun getBottomNavIndex(route: String?): Int =
            bottomNavDestinations.indexOfFirst { route?.startsWith(it.route) == true }
                .takeIf { it >= 0 } ?: 0

        /**
         * Check if a route belongs to a bottom navigation screen.
         */
        fun isBottomNavRoute(route: String?): Boolean =
            bottomNavDestinations.any { route?.startsWith(it.route) == true }

        /**
         * Get the title of a destination from its route.
         */
        fun getTitleFromRoute(route: String?): String =
            allDestinations.firstOrNull { route?.startsWith(it.route) == true }?.title
                ?: "Air Mouse"
    }
}