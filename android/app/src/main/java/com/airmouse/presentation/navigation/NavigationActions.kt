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
