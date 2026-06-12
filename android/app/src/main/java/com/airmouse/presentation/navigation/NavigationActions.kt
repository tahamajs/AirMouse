// app/src/main/java/com/airmouse/presentation/navigation/NavigationActions.kt
package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.Stable
import androidx.navigation.NavController
import androidx.navigation.NavHostController
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController

@Stable
class NavigationActions(val navController: NavHostController) {

    fun navigateTo(destination: Destinations, clearBackStack: Boolean = false) {
        if (clearBackStack) {
            navController.popBackStack(destination.route, inclusive = false)
        }
        navController.navigate(destination.route) {
            popUpTo(navController.graph.startDestinationId) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    fun navigateToWithArgs(destination: Destinations, args: Map<String, String>) {
        val route = destination.withArgs(*args.map { it.key to it.value }.toTypedArray())
        navController.navigate(route) {
            launchSingleTop = true
        }
    }

    fun navigateBack() {
        navController.popBackStack()
    }

    fun navigateToCalibration() {
        navigateTo(Destinations.Calibration)
    }

    fun navigateToOnboarding() {
        navigateTo(Destinations.Onboarding, clearBackStack = true)
    }

    fun navigateToHome() {
        navigateTo(Destinations.Home, clearBackStack = true)
    }

    fun getCurrentRoute(): String? {
        return navController.currentBackStackEntry?.destination?.route
    }
}

@Composable
fun rememberNavigationActions(
    navController: NavHostController = rememberNavController()
): NavigationActions = NavigationActions(navController)
