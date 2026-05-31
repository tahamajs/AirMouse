// app/src/main/java/com/airmouse/presentation/navigation/Destinations.kt
package com.airmouse.presentation.navigation

sealed class Destinations(val route: String) {
    // Main Screens
    object Home : Destinations("home")
    object Statistics : Destinations("statistics")
    object Settings : Destinations("settings")
    object Help : Destinations("help")
    object About : Destinations("about")

    // Advanced Features
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

    // Calibration Flow
    object Calibration : Destinations("calibration")

    // Onboarding
    object Onboarding : Destinations("onboarding")
}

// Helper function to get route with arguments
fun Destinations.withArgs(vararg args: Pair<String, String>): String {
    return if (args.isEmpty()) route else {
        route + "?" + args.joinToString("&") { "${it.first}=${it.second}" }
    }
}