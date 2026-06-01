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