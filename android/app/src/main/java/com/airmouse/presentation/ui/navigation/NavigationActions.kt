package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController

/**
 * Navigation actions handler - manages all screen transitions
 */
class NavigationActions(
    val navController: NavController
) {
    // Main navigation
    fun navigateTo(destination: Destinations) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    // Bottom navigation screens
    fun navigateToHome() = navigateTo(Destinations.Home)
    fun navigateToStatistics() = navigateTo(Destinations.Statistics)
    fun navigateToSettings() = navigateTo(Destinations.Settings)
    fun navigateToHelp() = navigateTo(Destinations.Help)

    // Info screens
    fun navigateToAbout() = navigateTo(Destinations.About)

    // Calibration & Sensors
    fun navigateToCalibration() = navigateTo(Destinations.Calibration)
    fun navigateToSensorVisualizer() = navigateTo(Destinations.SensorVisualizer)

    // Gesture & Touch
    fun navigateToGestureStudio() = navigateTo(Destinations.GestureStudio)
    fun navigateToEdgeGestures() = navigateTo(Destinations.EdgeGestures)
    fun navigateToTouchpad() = navigateTo(Destinations.Touchpad)

    // Connectivity
    fun navigateToNetworkDiscovery() = navigateTo(Destinations.NetworkDiscovery)
    fun navigateToServerLogs() = navigateTo(Destinations.ServerLogs)

    // Security & Privacy
    fun navigateToProximity() = navigateTo(Destinations.Proximity)
    fun navigateToVoiceCommands() = navigateTo(Destinations.VoiceCommands)

    // Customization
    fun navigateToProfiles() = navigateTo(Destinations.Profiles)
    fun navigateToThemes() = navigateTo(Destinations.Themes)

    // System
    fun navigateToBattery() = navigateTo(Destinations.Battery)
    fun navigateToAccessibility() = navigateTo(Destinations.Accessibility)
    fun navigateToTouchpadSettings() = navigateTo(Destinations.TouchpadSettings)

    // Onboarding
    fun navigateToOnboarding() = navigateTo(Destinations.Onboarding)

    // Navigation utilities
    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateUp() {
        navController.navigateUp()
    }

    fun clearBackStack() {
        navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false)
    }

    fun navigateToHomeAndClearStack() {
        navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false)
        navigateToHome()
    }

    fun canGoBack(): Boolean = navController.previousBackStackEntry != null
}

@Composable
fun rememberNavigationActions(navController: NavController = rememberNavController()): NavigationActions {
    return remember(navController) {
        NavigationActions(navController)
    }
}