
package com.airmouse.presentation.navigation

import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination

interface NavigationActions {
    fun navigateTo(route: String)
    fun navigateBack()
    fun navigateToHome()
    fun navigateToSettings()
    fun navigateToCalibration()
    fun navigateToCalibrationResult(quality: String)
    fun navigateToStatistics()
    fun navigateToHelp()
    fun navigateToAbout()
    fun navigateToProfiles()
    fun navigateToTouchpad()
    fun navigateToGestureStudio()
    fun navigateToNetworkDiscovery()
    fun navigateToProximity()
    fun navigateToVoiceCommands()
    fun navigateToEdgeGestures()
    fun navigateToSensorVisualizer()
    fun navigateToServerLogs()
    fun navigateToThemes()
    fun navigateToBattery()
    fun navigateToAccessibility()
    fun navigateToOnboarding()
    fun navigateToTouchpadSettings()
}

class NavigationActionsImpl(
    private val navController: NavController
) : NavigationActions {

    override fun navigateTo(route: String) {
        navController.navigate(route)
    }

    override fun navigateBack() {
        navController.popBackStack()
    }

    override fun navigateToHome() {
        navController.navigate(Destinations.Home.route) {
            popUpTo(Destinations.Home.route) { inclusive = true }
            launchSingleTop = true
        }
    }

    override fun navigateToSettings() {
        navController.navigate(Destinations.Settings.route)
    }

    override fun navigateToCalibration() {
        navController.navigate(Destinations.Calibration.route)
    }

    override fun navigateToCalibrationResult(quality: String) {
        navController.navigate("${Destinations.CalibrationResult.route}?quality=$quality") {
            popUpTo(Destinations.Calibration.route) { inclusive = true }
        }
    }

    override fun navigateToStatistics() {
        navController.navigate(Destinations.Statistics.route)
    }

    override fun navigateToHelp() {
        navController.navigate(Destinations.Help.route)
    }

    override fun navigateToAbout() {
        navController.navigate(Destinations.About.route)
    }

    override fun navigateToProfiles() {
        navController.navigate(Destinations.Profiles.route)
    }

    override fun navigateToTouchpad() {
        navController.navigate(Destinations.Touchpad.route)
    }

    override fun navigateToGestureStudio() {
        navController.navigate(Destinations.GestureStudio.route)
    }

    override fun navigateToNetworkDiscovery() {
        navController.navigate(Destinations.NetworkDiscovery.route)
    }

    override fun navigateToProximity() {
        navController.navigate(Destinations.Proximity.route)
    }

    override fun navigateToVoiceCommands() {
        navController.navigate(Destinations.VoiceCommands.route)
    }

    override fun navigateToEdgeGestures() {
        navController.navigate(Destinations.EdgeGestures.route)
    }

    override fun navigateToSensorVisualizer() {
        navController.navigate(Destinations.SensorVisualizer.route)
    }

    override fun navigateToServerLogs() {
        navController.navigate(Destinations.ServerLogs.route)
    }

    override fun navigateToThemes() {
        navController.navigate(Destinations.Themes.route)
    }

    override fun navigateToBattery() {
        navController.navigate(Destinations.Battery.route)
    }

    override fun navigateToAccessibility() {
        navController.navigate(Destinations.Accessibility.route)
    }

    override fun navigateToOnboarding() {
        navController.navigate(Destinations.Onboarding.route)
    }

    override fun navigateToTouchpadSettings() {
        navController.navigate(Destinations.TouchpadSettings.route)
    }
}