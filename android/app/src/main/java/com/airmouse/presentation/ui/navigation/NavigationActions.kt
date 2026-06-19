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
    // ==================== MAIN NAVIGATION ====================

    fun navigateTo(destination: Destinations) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToWithOptions(
        destination: Destinations,
        popUpTo: Destinations? = null,
        inclusive: Boolean = false
    ) {
        navController.navigate(destination.route) {
            popUpTo?.let {
                popUpTo(it.route) {
                    this.inclusive = inclusive
                }
            }
            launchSingleTop = true
        }
    }

    // ==================== BOTTOM NAVIGATION SCREENS ====================

    fun navigateToHome() = navigateTo(Destinations.Home)
    fun navigateToStatistics() = navigateTo(Destinations.Statistics)
    fun navigateToSettings() = navigateTo(Destinations.Settings)
    fun navigateToHelp() = navigateTo(Destinations.Help)

    // ==================== INFO SCREENS ====================

    fun navigateToAbout() = navigateTo(Destinations.About)

    // ==================== CALIBRATION & SENSORS ====================

    fun navigateToCalibration() = navigateTo(Destinations.Calibration)
    fun navigateToSensorVisualizer() = navigateTo(Destinations.SensorVisualizer)

    // ==================== GESTURE & TOUCH ====================

    fun navigateToGestureStudio() = navigateTo(Destinations.GestureStudio)
    fun navigateToEdgeGestures() = navigateTo(Destinations.EdgeGestures)
    fun navigateToTouchpad() = navigateTo(Destinations.Touchpad)

    // ==================== CONNECTIVITY ====================

    fun navigateToNetworkDiscovery() = navigateTo(Destinations.NetworkDiscovery)
    fun navigateToServerLogs() = navigateTo(Destinations.ServerLogs)

    // ==================== SECURITY & PRIVACY ====================

    fun navigateToProximity() = navigateTo(Destinations.Proximity)
    fun navigateToVoiceCommands() = navigateTo(Destinations.VoiceCommands)

    // ==================== CUSTOMIZATION ====================

    fun navigateToProfiles() = navigateTo(Destinations.Profiles)
    fun navigateToThemes() = navigateTo(Destinations.Themes)

    // ==================== SYSTEM ====================

    fun navigateToBattery() = navigateTo(Destinations.Battery)
    fun navigateToAccessibility() = navigateTo(Destinations.Accessibility)
    fun navigateToTouchpadSettings() = navigateTo(Destinations.TouchpadSettings)

    // ==================== ONBOARDING ====================

    fun navigateToOnboarding() = navigateTo(Destinations.Onboarding)

    // ==================== NAVIGATION UTILITIES ====================

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateUp() {
        navController.navigateUp()
    }

    fun clearBackStack() {
        navController.popBackStack(
            navController.graph.findStartDestination().id,
            inclusive = false
        )
    }

    fun navigateToHomeAndClearStack() {
        navController.popBackStack(
            navController.graph.findStartDestination().id,
            inclusive = false
        )
        navigateToHome()
    }

    fun canGoBack(): Boolean = navController.previousBackStackEntry != null

    fun getCurrentRoute(): String? = navController.currentDestination?.route

    fun isCurrentDestination(destination: Destinations): Boolean {
        return navController.currentDestination?.route == destination.route
    }
}

@Composable
fun rememberNavigationActions(
    navController: NavController = rememberNavController()
): NavigationActions {
    return remember(navController) {
        NavigationActions(navController)
    }
}