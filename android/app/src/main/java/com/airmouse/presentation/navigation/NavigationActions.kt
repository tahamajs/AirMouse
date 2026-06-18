package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navOptions

/**
 * Central navigation actions handler.
 * Provides type-safe navigation to all destinations.
 */
class NavigationActions(private val navController: NavController) {

    // ==================== BOTTOM NAVIGATION ====================
    fun navigateToHome() = navigateTo(Destinations.Home)
    fun navigateToStatistics() = navigateTo(Destinations.Statistics)
    fun navigateToGestureStudio() = navigateTo(Destinations.GestureStudio)
    fun navigateToSettings() = navigateTo(Destinations.Settings)

    // ==================== HELP & INFO ====================
    fun navigateToHelp() = navigateTo(Destinations.Help)
    fun navigateToAbout() = navigateTo(Destinations.About)

    // ==================== ADVANCED FEATURES ====================
    fun navigateToNetworkDiscovery() = navigateTo(Destinations.NetworkDiscovery)
    fun navigateToProfiles() = navigateTo(Destinations.Profiles)
    fun navigateToEdgeGestures() = navigateTo(Destinations.EdgeGestures)
    fun navigateToVoiceCommands() = navigateTo(Destinations.VoiceCommands)
    fun navigateToThemes() = navigateTo(Destinations.Themes)
    fun navigateToServerLogs() = navigateTo(Destinations.ServerLogs)
    fun navigateToBattery() = navigateTo(Destinations.Battery)
    fun navigateToAccessibility() = navigateTo(Destinations.Accessibility)
    fun navigateToProximity() = navigateTo(Destinations.Proximity)
    fun navigateToSensorVisualizer() = navigateTo(Destinations.SensorVisualizer)
    fun navigateToCalibration() = navigateTo(Destinations.Calibration)
    fun navigateToTouchpad() = navigateTo(Destinations.Touchpad)

    // ==================== ONBOARDING ====================
    fun navigateToOnboarding() = navigateTo(Destinations.Onboarding)

    // ==================== PARAMETERISED ROUTES ====================
    fun navigateToCalibrationStep(step: Int) {
        navigateToRoute(Destinations.CalibrationWithStep.createRoute(step))
    }

    fun navigateToGestureDetail(gestureName: String) {
        navigateToRoute(Destinations.GestureDetail.createRoute(gestureName))
    }

    fun navigateToProfileDetail(profileName: String) {
        navigateToRoute(Destinations.ProfileDetail.createRoute(profileName))
    }

    // ==================== GENERIC NAVIGATION ====================
    fun navigateTo(destination: Destinations) {
        navigateToRoute(destination.route)
    }

    fun navigateToRoute(route: String) {
        navController.navigate(route) {
            // Clear back stack to the start destination when going to bottom nav screens
            if (Destinations.bottomNavDestinations.any { route.startsWith(it.route) }) {
                popUpTo(navController.graph.findStartDestination().id) {
                    saveState = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }

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
        navController.popBackStack(0, false)
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