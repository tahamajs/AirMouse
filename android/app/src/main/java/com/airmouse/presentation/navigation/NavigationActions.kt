package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavOptions
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlin.reflect.KProperty

@Stable
class NavigationActions(
    val navController: NavHostController
) {
    private val _currentDestination = MutableStateFlow<String?>(null)
    val currentDestination: StateFlow<String?> = _currentDestination.asStateFlow()
    
    init {
        // Track current destination
        navController.addOnDestinationChangedListener { _, destination, _ ->
            _currentDestination.value = destination.route
        }
    }
    fun navigateToHome() {
        navController.popBackStack(Destinations.Home.route, inclusive = false)
    }

    /**
     * Navigate to a destination
     */
    fun navigateTo(
        destination: Destinations,
        clearBackStack: Boolean = false,
        launchSingleTop: Boolean = true,
        popUpToStart: Boolean = false
    ) {
        navController.navigate(destination.route) {
            if (clearBackStack || popUpToStart) {
                popUpTo(
                    if (popUpToStart) navController.graph.findStartDestination().id else navController.graph.startDestinationId
                ) {
                    inclusive = clearBackStack
                    saveState = true
                }
            }
            this.launchSingleTop = launchSingleTop
            restoreState = true
        }
    }
    
    /**
     * Navigate to a destination with arguments
     */
    fun navigateToRoute(
        route: String,
        clearBackStack: Boolean = false
    ) {
        navController.navigate(route) {
            if (clearBackStack) {
                popUpTo(navController.graph.startDestinationId) {
                    inclusive = true
                }
            }
            launchSingleTop = true
            restoreState = true
        }
    }
    
    /**
     * Navigate to calibration with optional step
     */
    fun navigateToCalibration(step: Int = 0) {
        if (step > 0) {
            navController.navigate(Destinations.CalibrationWithStep.createRoute(step)) {
                launchSingleTop = true
            }
        } else {
            navigateTo(Destinations.Calibration)
        }
    }
    
    /**
     * Navigate back
     */
    fun navigateBack() {
        if (!navController.popBackStack()) {
            navigateTo(Destinations.Home, clearBackStack = true)
        }
    }
    
    /**
     * Navigate up
     */
    fun navigateUp() {
        navController.navigateUp()
    }
    
    /**
     * Check if current screen is home
     */
    fun isOnHomeScreen(): Boolean {
        return navController.currentBackStackEntry?.destination?.route == Destinations.Home.route
    }
}

/**
 * Composable function to remember navigation actions
 */
@Composable
fun rememberNavigationActions(
    navController: NavHostController = rememberNavController()
): NavigationActions {
    return remember(navController) {
        NavigationActions(navController)
    }
}

/**
 * Extension function to check if a destination is currently active
 */
@Composable
fun NavigationActions.isCurrentDestination(destination: Destinations): Boolean {
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val currentDestination = navBackStackEntry?.destination
    return currentDestination?.hierarchy?.any { it.route == destination.route } == true
}
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