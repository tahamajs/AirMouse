// app/src/main/java/com/airmouse/presentation/navigation/Destinations.kt
package com.airmouse.presentation.navigation

import android.os.Bundle
import androidx.navigation.NavType
import kotlinx.serialization.Serializable

/**
 * Sealed class for all navigation destinations in the app.
 * Provides type-safe navigation with route definitions and argument handling.
 */
sealed class Destinations(
    val route: String,
    val title: String = "",
    val icon: Int? = null,
    val showInBottomBar: Boolean = false
) {
    object Touchpad : Destinations("touchpad")

    // ==================== MAIN SCREENS ====================
    
    /** Home screen - main dashboard */
    object Home : Destinations(
        route = "home",
        title = "Air Mouse Pro",
        showInBottomBar = true
    )
    
    /** Statistics screen - gesture usage and performance metrics */
    object Statistics : Destinations(
        route = "statistics",
        title = "Statistics",
        showInBottomBar = true
    )
    
    /** Settings screen - app configuration */
    object Settings : Destinations(
        route = "settings",
        title = "Settings",
        showInBottomBar = true
    )
    
    /** Help screen - user guide and documentation */
    object Help : Destinations(
        route = "help",
        title = "Help",
        showInBottomBar = true
    )
    
    /** About screen - app information */
    object About : Destinations(
        route = "about",
        title = "About"
    )
    
    // ==================== ADVANCED FEATURES ====================
    
    /** Network discovery - find servers on local network */
    object NetworkDiscovery : Destinations(
        route = "network_discovery",
        title = "Network Discovery"
    )
    
    /** Profiles - save and load different configurations */
    object Profiles : Destinations(
        route = "profiles",
        title = "Profiles"
    )
    
    /** Gesture Studio - create and train custom gestures */
    object GestureStudio : Destinations(
        route = "gesture_studio",
        title = "Gesture Studio"
    )
    
    /** Edge Gestures - configure edge-based gestures */
    object EdgeGestures : Destinations(
        route = "edge_gestures",
        title = "Edge Gestures"
    )
    
    /** Voice Commands - voice control configuration */
    object VoiceCommands : Destinations(
        route = "voice_commands",
        title = "Voice Commands"
    )
    
    /** Themes - customize app appearance */
    object Themes : Destinations(
        route = "themes",
        title = "Themes"
    )
    
    /** Server Logs - view and export server logs */
    object ServerLogs : Destinations(
        route = "server_logs",
        title = "Server Logs"
    )
    
    /** Battery - battery usage and optimization */
    object Battery : Destinations(
        route = "battery",
        title = "Battery"
    )
    
    /** Accessibility - accessibility features */
    object Accessibility : Destinations(
        route = "accessibility",
        title = "Accessibility"
    )
    
    /** Proximity - proximity-based auto-lock settings */
    object Proximity : Destinations(
        route = "proximity",
        title = "Proximity Lock"
    )
    
    /** Sensor Visualizer - real-time sensor data visualization */
    object SensorVisualizer : Destinations(
        route = "sensor_visualizer",
        title = "Sensor Visualizer"
    )
    
    // ==================== CALIBRATION FLOW ====================
    
    /** Calibration - sensor calibration wizard */
    object Calibration : Destinations(
        route = "calibration",
        title = "Calibration"
    )
    
    /** Calibration with step parameter */
    object CalibrationWithStep : Destinations(
        route = "calibration/{step}",
        title = "Calibration"
    ) {
        fun createRoute(step: Int) = "calibration/$step"
        const val STEP_ARG = "step"
    }
    
    // ==================== GESTURE DETAIL ====================
    
    /** Gesture detail - view/edit specific gesture */
    object GestureDetail : Destinations(
        route = "gesture/{gestureName}",
        title = "Gesture Details"
    ) {
        fun createRoute(gestureName: String) = "gesture/$gestureName"
        const val GESTURE_NAME_ARG = "gestureName"
    }
    
    // ==================== PROFILE DETAIL ====================
    
    /** Profile detail - view/edit specific profile */
    object ProfileDetail : Destinations(
        route = "profile/{profileName}",
        title = "Profile Details"
    ) {
        fun createRoute(profileName: String) = "profile/$profileName"
        const val PROFILE_NAME_ARG = "profileName"
    }
    
    // ==================== ONBOARDING ====================
    
    /** Onboarding - first-time user experience */
    object Onboarding : Destinations(
        route = "onboarding",
        title = "Welcome"
    )
    package com.airmouse.presentation.navigation

    sealed class Destinations(val route: String) {
        // Main Screens (Bottom Navigation)
        object Home : Destinations("home")
        object Statistics : Destinations("statistics")
        object Settings : Destinations("settings")
        object Help : Destinations("help")

        // Secondary Screens
        object About : Destinations("about")
        object Calibration : Destinations("calibration")
        object GestureStudio : Destinations("gesture_studio")
        object Proximity : Destinations("proximity")
        object VoiceCommands : Destinations("voice_commands")
        object EdgeGestures : Destinations("edge_gestures")
        object Themes : Destinations("themes")
        object Profiles : Destinations("profiles")
        object NetworkDiscovery : Destinations("network_discovery")
        object ServerLogs : Destinations("server_logs")
        object Battery : Destinations("battery")
        object Accessibility : Destinations("accessibility")
        object SensorVisualizer : Destinations("sensor_visualizer")
        object Touchpad : Destinations("touchpad")
        object Onboarding : Destinations("onboarding")

        // Parameterized routes
        object CalibrationWithStep : Destinations("calibration/{step}") {
            const val STEP_ARG = "step"
            fun passStep(step: Int) = "calibration/$step"
        }
    }
    // ==================== SETTINGS SUB-SECTIONS ====================
    
    /** Settings General - general app settings */
    object SettingsGeneral : Destinations(
        route = "settings/general",
        title = "General Settings"
    )
    
    /** Settings Sensors - sensor configuration */
    object SettingsSensors : Destinations(
        route = "settings/sensors",
        title = "Sensor Settings"
    )
    
    /** Settings Gestures - gesture sensitivity and thresholds */
    object SettingsGestures : Destinations(
        route = "settings/gestures",
        title = "Gesture Settings"
    )
    
    /** Settings Network - network configuration */
    object SettingsNetwork : Destinations(
        route = "settings/network",
        title = "Network Settings"
    )
    
    /** Settings Bluetooth - Bluetooth configuration */
    object SettingsBluetooth : Destinations(
        route = "settings/bluetooth",
        title = "Bluetooth Settings"
    )
    
    /** Settings Appearance - appearance settings */
    object SettingsAppearance : Destinations(
        route = "settings/appearance",
        title = "Appearance"
    )
    
    // ==================== HELPER PROPERTIES ====================
    
    /** All main destinations for bottom navigation */
    companion object {
        val bottomNavDestinations = listOf(
            Home,
            Statistics,
            Settings,
            Help
        )
        
        val allDestinations = listOf(
            Home, Statistics, Settings, Help, About,
            NetworkDiscovery, Profiles, GestureStudio, EdgeGestures,
            VoiceCommands, Themes, ServerLogs, Battery, Accessibility,
            Proximity, SensorVisualizer, Calibration, Onboarding
        )
        
        val settingsDestinations = listOf(
            SettingsGeneral, SettingsSensors, SettingsGestures,
            SettingsNetwork, SettingsBluetooth, SettingsAppearance
        )
    }
    
    /**
     * Get the route with URL-encoded arguments
     */
    fun withArgs(vararg args: Pair<String, Any>): String {
        if (args.isEmpty()) return route
        
        val queryParams = args.joinToString("&") { (key, value) ->
            "$key=${value.toString().replace(" ", "%20")}"
        }
        
        return if (route.contains("?")) {
            "$route&$queryParams"
        } else {
            "$route?$queryParams"
        }
    }
    
    /**
     * Get the route with a single argument
     */
    fun withArg(key: String, value: Any): String {
        return withArgs(key to value)
    }
    
    /**
     * Check if this destination matches a given route
     */
    fun matches(route: String): Boolean {
        return this.route == route || route.startsWith(this.route.split("{").first())
    }
}

// ==================== ARGUMENT KEYS ====================

object NavigationArguments {
    const val STEP = "step"
    const val GESTURE_NAME = "gestureName"
    const val PROFILE_NAME = "profileName"
    const val SERVER_IP = "serverIp"
    const val SERVER_PORT = "serverPort"
    const val CALIBRATION_TYPE = "calibrationType"
    const val SENSOR_TYPE = "sensorType"
}

// ==================== ARGUMENT TYPES ====================

object NavigationArgTypes {
    val step = NavType.IntType
    val gestureName = NavType.StringType
    val profileName = NavType.StringType
    val serverIp = NavType.StringType
    val serverPort = NavType.IntType
}

// ==================== EXTENSION FUNCTIONS ====================

/**
 * Convert a destination to a Bundle for argument passing
 */
fun Destinations.toBundle(vararg args: Pair<String, Any>): Bundle {
    return Bundle().apply {
        args.forEach { (key, value) ->
            when (value) {
                is String -> putString(key, value)
                is Int -> putInt(key, value)
                is Boolean -> putBoolean(key, value)
                is Long -> putLong(key, value)
                is Float -> putFloat(key, value)
                is Double -> putDouble(key, value)
                else -> putString(key, value.toString())
            }
        }
    }
}

/**
 * Get the title of a destination from its route
 */
fun getTitleFromRoute(route: String?): String {
    return when {
        route?.startsWith(Destinations.Home.route) == true -> Destinations.Home.title
        route?.startsWith(Destinations.Statistics.route) == true -> Destinations.Statistics.title
        route?.startsWith(Destinations.Settings.route) == true -> Destinations.Settings.title
        route?.startsWith(Destinations.Help.route) == true -> Destinations.Help.title
        route?.startsWith(Destinations.About.route) == true -> Destinations.About.title
        route?.startsWith(Destinations.Calibration.route) == true -> Destinations.Calibration.title
        route?.startsWith(Destinations.GestureStudio.route) == true -> Destinations.GestureStudio.title
        route?.startsWith(Destinations.Proximity.route) == true -> Destinations.Proximity.title
        route?.startsWith(Destinations.VoiceCommands.route) == true -> Destinations.VoiceCommands.title
        route?.startsWith(Destinations.Themes.route) == true -> Destinations.Themes.title
        route?.startsWith(Destinations.Profiles.route) == true -> Destinations.Profiles.title
        route?.startsWith(Destinations.Battery.route) == true -> Destinations.Battery.title
        route?.startsWith(Destinations.Accessibility.route) == true -> Destinations.Accessibility.title
        route?.startsWith(Destinations.SensorVisualizer.route) == true -> Destinations.SensorVisualizer.title
        route?.startsWith(Destinations.NetworkDiscovery.route) == true -> Destinations.NetworkDiscovery.title
        route?.startsWith(Destinations.EdgeGestures.route) == true -> Destinations.EdgeGestures.title
        route?.startsWith(Destinations.ServerLogs.route) == true -> Destinations.ServerLogs.title
        else -> "Air Mouse"
    }
}

/**
 * Check if a destination should show the bottom bar
 */
fun Destinations.shouldShowBottomBar(): Boolean {
    return showInBottomBar
}

/**
 * Get the icon resource for a destination
 */
fun Destinations.getIcon(): Int? {
    return icon
}