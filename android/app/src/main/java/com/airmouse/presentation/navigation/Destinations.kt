// app/src/main/java/com/airmouse/presentation/navigation/Destinations.kt
@file:Suppress("unused")

package com.airmouse.presentation.navigation

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.*
import androidx.compose.material.icons.filled.*
import androidx.compose.ui.graphics.vector.ImageVector

/**
 * Sealed class representing all navigation destinations in the app.
 *
 * Each destination has:
 * - A unique route string for navigation
 * - A display title
 * - An icon for UI representation
 */
sealed class Destinations(
    val route: String,
    val title: String,
    val icon: ImageVector
) {

    // ==========================================
    // DESTINATION OBJECTS
    // ==========================================

    /** Main home screen */
    object Home : Destinations(
        route = ROUTE_HOME,
        title = "Home",
        icon = Icons.Filled.Home
    )
    object FileTransfer : Destinations(
        route = ROUTE_FILE_TRANSFER,
        title = "File Transfer",
        icon = Icons.Default.Folder
    )

    /** Statistics dashboard */
    object Statistics : Destinations(
        route = ROUTE_STATISTICS,
        title = "Stats",
        icon = Icons.Filled.BarChart
    )

    /** Settings screen */
    object Settings : Destinations(
        route = ROUTE_SETTINGS,
        title = "Settings",
        icon = Icons.Filled.Settings
    )

    /** Help & support */
    object Help : Destinations(
        route = ROUTE_HELP,
        title = "Help",
        icon = Icons.AutoMirrored.Filled.Help
    )

    /** About the app */
    object About : Destinations(
        route = ROUTE_ABOUT,
        title = "About",
        icon = Icons.Filled.Info
    )

    /** Calibration screen */
    object Calibration : Destinations(
        route = ROUTE_CALIBRATION,
        title = "Calibrate",
        icon = Icons.Filled.Tune
    )

    object CalibrationProcess : Destinations(
        route = ROUTE_CALIBRATION_PROCESS,
        title = "Calibrating...",
        icon = Icons.Filled.Tune
    )

    /** Calibration result screen */
    object CalibrationResult : Destinations(
        route = ROUTE_CALIBRATION_RESULT,
        title = "Calibration Result",
        icon = Icons.Filled.CheckCircle
    )

    /** Sensor visualizer */
    object SensorVisualizer : Destinations(
        route = ROUTE_SENSOR_VISUALIZER,
        title = "Sensors",
        icon = Icons.Filled.Sensors
    )

    /** Gesture studio for training gestures */
    object GestureStudio : Destinations(
        route = ROUTE_GESTURE_STUDIO,
        title = "Gestures",
        icon = Icons.Filled.Gesture
    )

    /** Edge gesture settings */
    object EdgeGestures : Destinations(
        route = ROUTE_EDGE_GESTURES,
        title = "Edge",
        icon = Icons.Filled.Swipe
    )

    /** Touchpad screen */
    object Touchpad : Destinations(
        route = ROUTE_TOUCHPAD,
        title = "Touchpad",
        icon = Icons.Filled.TouchApp
    )

    /** Gaming mode screen */
    object GamingMode : Destinations(
        route = ROUTE_GAMING_MODE,
        title = "Gaming",
        icon = Icons.Filled.SportsEsports
    )

    object ScreenMirroring : Destinations(
        route = ROUTE_SCREEN_MIRRORING,
        title = "Mirroring",
        icon = Icons.AutoMirrored.Filled.ScreenShare
    )

    object SyncStatus : Destinations(
        route = ROUTE_SYNC_STATUS,
        title = "Sync",
        icon = Icons.Filled.Sync
    )

    object NotificationsCenter : Destinations(
        route = ROUTE_NOTIFICATIONS_CENTER,
        title = "Notifications",
        icon = Icons.Filled.Notifications
    )

    /** Touchpad settings */
    object TouchpadSettings : Destinations(
        route = ROUTE_TOUCHPAD_SETTINGS,
        title = "Touchpad Settings",
        icon = Icons.Filled.Settings
    )

    /** Network discovery */
    object NetworkDiscovery : Destinations(
        route = ROUTE_NETWORK_DISCOVERY,
        title = "Network",
        icon = Icons.Filled.Wifi
    )

    /** Server logs */
    object ServerLogs : Destinations(
        route = ROUTE_SERVER_LOGS,
        title = "Logs",
        icon = Icons.Filled.List
    )

    /** Proximity settings */
    object Proximity : Destinations(
        route = ROUTE_PROXIMITY,
        title = "Proximity",
        icon = Icons.Filled.NearMe
    )

    /** Voice commands */
    object VoiceCommands : Destinations(
        route = ROUTE_VOICE_COMMANDS,
        title = "Voice",
        icon = Icons.Filled.Mic
    )

    /** User profiles */
    object Profiles : Destinations(
        route = ROUTE_PROFILES,
        title = "Profiles",
        icon = Icons.Filled.Person
    )

    /** Themes */
    object Themes : Destinations(
        route = ROUTE_THEMES,
        title = "Themes",
        icon = Icons.Filled.Palette
    )

    /** Battery monitoring */
    object Battery : Destinations(
        route = ROUTE_BATTERY,
        title = "Battery",
        icon = Icons.Filled.BatteryFull
    )

    /** Accessibility settings */
    object Accessibility : Destinations(
        route = ROUTE_ACCESSIBILITY,
        title = "Access",
        icon = Icons.Filled.Accessibility
    )

    /** Onboarding screen */
    object Onboarding : Destinations(
        route = ROUTE_ONBOARDING,
        title = "Onboarding",
        icon = Icons.Filled.Apps
    )

    // ==========================================
    // COMPANION OBJECT - Constants & Helpers
    // ==========================================

    companion object {
        // ------------------------------------------
        // ROUTE CONSTANTS
        // ------------------------------------------

        const val ROUTE_FILE_TRANSFER = "file_transfer"

        const val ROUTE_HOME = "home"
        const val ROUTE_STATISTICS = "statistics"
        const val ROUTE_SETTINGS = "settings"
        const val ROUTE_HELP = "help"
        const val ROUTE_ABOUT = "about"
        const val ROUTE_CALIBRATION = "calibration"
        const val ROUTE_CALIBRATION_PROCESS = "calibration_process"
        const val ROUTE_CALIBRATION_RESULT = "calibration_result"
        const val ROUTE_SENSOR_VISUALIZER = "sensor_visualizer"
        const val ROUTE_GESTURE_STUDIO = "gesture_studio"
        const val ROUTE_EDGE_GESTURES = "edge_gestures"
        const val ROUTE_TOUCHPAD = "touchpad"
        const val ROUTE_GAMING_MODE = "gaming_mode"
        const val ROUTE_SCREEN_MIRRORING = "screen_mirroring"
        const val ROUTE_SYNC_STATUS = "sync_status"
        const val ROUTE_NOTIFICATIONS_CENTER = "notifications_center"
        const val ROUTE_TOUCHPAD_SETTINGS = "touchpad_settings"
        const val ROUTE_NETWORK_DISCOVERY = "network_discovery"
        const val ROUTE_SERVER_LOGS = "server_logs"
        const val ROUTE_PROXIMITY = "proximity"
        const val ROUTE_VOICE_COMMANDS = "voice_commands"
        const val ROUTE_PROFILES = "profiles"
        const val ROUTE_THEMES = "themes"
        const val ROUTE_BATTERY = "battery"
        const val ROUTE_ACCESSIBILITY = "accessibility"
        const val ROUTE_ONBOARDING = "onboarding"

        // ------------------------------------------
        // BOTTOM NAVIGATION
        // ------------------------------------------

        private val bottomNavRoutes: Set<String> = setOf(
            ROUTE_HOME,
            ROUTE_STATISTICS,
            ROUTE_SETTINGS,
            ROUTE_HELP
        )

        /** List of destinations that appear in the bottom navigation bar */
        val bottomNavDestinations: List<Destinations>
            get() = listOf(
                Home,
                Statistics,
                Settings,
                Help
            )

        /**
         * Checks if a given route belongs to the bottom navigation.
         * @param route The route string to check
         * @return True if the route is in the bottom navigation
         */
        fun isBottomNavScreen(route: String?): Boolean =
            route != null && bottomNavRoutes.contains(route)

        /**
         * Gets the current bottom navigation destination based on route.
         * @param route The route string
         * @return The Destinations object or null if not found
         */
        fun getBottomNavDestination(route: String?): Destinations? =
            when (route) {
                ROUTE_HOME -> Home
                ROUTE_STATISTICS -> Statistics
                ROUTE_SETTINGS -> Settings
                ROUTE_HELP -> Help
                else -> null
            }

        /**
         * Converts a route string to a Destinations object.
         * @param route The route string
         * @return The Destinations object or null if not found
         */
        fun fromRoute(route: String): Destinations? =
            when (route) {
                ROUTE_HOME -> Home
                ROUTE_STATISTICS -> Statistics
                ROUTE_SETTINGS -> Settings
                ROUTE_HELP -> Help
                ROUTE_ABOUT -> About
                ROUTE_CALIBRATION -> Calibration
                ROUTE_CALIBRATION_PROCESS -> CalibrationProcess
                ROUTE_CALIBRATION_RESULT -> CalibrationResult
                ROUTE_SENSOR_VISUALIZER -> SensorVisualizer
                ROUTE_GESTURE_STUDIO -> GestureStudio
                ROUTE_EDGE_GESTURES -> EdgeGestures
                ROUTE_TOUCHPAD -> Touchpad
                ROUTE_TOUCHPAD_SETTINGS -> TouchpadSettings
                ROUTE_NETWORK_DISCOVERY -> NetworkDiscovery
                ROUTE_SERVER_LOGS -> ServerLogs
                ROUTE_PROXIMITY -> Proximity
                ROUTE_VOICE_COMMANDS -> VoiceCommands
                ROUTE_PROFILES -> Profiles
                ROUTE_THEMES -> Themes
                ROUTE_BATTERY -> Battery
                ROUTE_ACCESSIBILITY -> Accessibility
                ROUTE_ONBOARDING -> Onboarding
                else -> null
            }

        /**
         * Gets the title for a given route.
         * @param route The route string
         * @return The title or null if not found
         */
        fun getTitleForRoute(route: String): String? = fromRoute(route)?.title

        /**
         * Gets the icon for a given route.
         * @param route The route string
         * @return The icon or null if not found
         */
        fun getIconForRoute(route: String): ImageVector? = fromRoute(route)?.icon

        /**
         * Checks if a route is a top-level destination.
         * @param route The route string
         * @return True if the route is a top-level destination
         */
        fun isTopLevelDestination(route: String?): Boolean =
            route != null && route in listOf(
                ROUTE_HOME,
                ROUTE_STATISTICS,
                ROUTE_SETTINGS,
                ROUTE_HELP,
                ROUTE_ABOUT,
                ROUTE_CALIBRATION
            )

        /**
         * Checks if a route should show the back button.
         * @param route The route string
         * @return True if the route should show back button
         */
        fun shouldShowBackButton(route: String?): Boolean =
            route != null && route in listOf(
                ROUTE_ABOUT,
                ROUTE_CALIBRATION_PROCESS,
                ROUTE_CALIBRATION_RESULT,
                ROUTE_SENSOR_VISUALIZER,
                ROUTE_GESTURE_STUDIO,
                ROUTE_EDGE_GESTURES,
                ROUTE_TOUCHPAD_SETTINGS,
                ROUTE_NETWORK_DISCOVERY,
                ROUTE_SERVER_LOGS,
                ROUTE_PROXIMITY,
                ROUTE_VOICE_COMMANDS,
                ROUTE_PROFILES,
                ROUTE_THEMES,
                ROUTE_BATTERY,
                ROUTE_ACCESSIBILITY,
                ROUTE_TOUCHPAD,
                ROUTE_ONBOARDING
            )

        /**
         * Creates a navigation route with optional arguments.
         * @param args Optional arguments to append to the route
         * @return The full route string
         */
        fun createRoute(baseRoute: String, vararg args: Pair<String, String>): String =
            if (args.isEmpty()) baseRoute
            else "$baseRoute?${args.joinToString("&") { "${it.first}=${it.second}" }}"
    }

    /**
     * Checks if this destination matches a given route.
     * @param route The route to check
     * @return True if this destination matches the route
     */
    fun matchesRoute(route: String?): Boolean = this.route == route

    override fun toString(): String = "Destinations(route='$route', title='$title')"
}
