package com.airmouse.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController

@Stable
class NavigationActions(private val navController: NavHostController) {

    fun navigateTo(destination: Destinations, clearBackStack: Boolean = false) {
        if (clearBackStack) navController.popBackStack(destination.route, inclusive = false)
        navController.navigate(destination.route) { launchSingleTop = true }
    }

    fun navigateBack() = navController.popBackStack()
    fun navigateToHome() = navigateTo(Destinations.Home, clearBackStack = true)
    fun navigateToCalibration() = navigateTo(Destinations.Calibration)
}

@Composable
fun rememberNavigationActions(navController: NavHostController = rememberNavController()): NavigationActions {
    return NavigationActions(navController)
}
package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavController
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.rememberNavController

class NavigationActions(
    val navController: NavController
) {
    fun navigateTo(destination: Destinations) {
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToHome() = navigateTo(Destinations.Home)
    fun navigateToStatistics() = navigateTo(Destinations.Statistics)
    fun navigateToSettings() = navigateTo(Destinations.Settings)
    fun navigateToHelp() = navigateTo(Destinations.Help)
    fun navigateToAbout() = navigateTo(Destinations.About)
    fun navigateToCalibration() = navigateTo(Destinations.Calibration)
    fun navigateToGestureStudio() = navigateTo(Destinations.GestureStudio)
    fun navigateToProximity() = navigateTo(Destinations.Proximity)
    fun navigateToVoiceCommands() = navigateTo(Destinations.VoiceCommands)
    fun navigateToEdgeGestures() = navigateTo(Destinations.EdgeGestures)
    fun navigateToThemes() = navigateTo(Destinations.Themes)
    fun navigateToProfiles() = navigateTo(Destinations.Profiles)
    fun navigateToNetworkDiscovery() = navigateTo(Destinations.NetworkDiscovery)
    fun navigateToServerLogs() = navigateTo(Destinations.ServerLogs)
    fun navigateToBattery() = navigateTo(Destinations.Battery)
    fun navigateToAccessibility() = navigateTo(Destinations.Accessibility)
    fun navigateToSensorVisualizer() = navigateTo(Destinations.SensorVisualizer)
    fun navigateToTouchpad() = navigateTo(Destinations.Touchpad)
    fun navigateToOnboarding() = navigateTo(Destinations.Onboarding)

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateUp() {
        navController.navigateUp()
    }

    fun clearBackStack() {
        navController.popBackStack(navController.graph.findStartDestination().id, inclusive = false)
    }
}

@Composable
fun rememberNavigationActions(navController: NavController = rememberNavController()): NavigationActions {
    return remember(navController) {
        NavigationActions(navController)
    }
}