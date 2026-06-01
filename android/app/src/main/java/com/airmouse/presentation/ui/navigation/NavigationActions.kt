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