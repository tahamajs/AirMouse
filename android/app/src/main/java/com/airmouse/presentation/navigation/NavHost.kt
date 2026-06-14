package com.airmouse.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.calibration.CalibrationScreen
import com.airmouse.presentation.ui.edge.EdgeGesturesScreen
import com.airmouse.presentation.ui.gesture.GestureStudioScreen
import com.airmouse.presentation.ui.help.HelpScreen
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.logs.ServerLogsScreen
import com.airmouse.presentation.ui.network.NetworkDiscoveryScreen
import com.airmouse.presentation.ui.onboarding.OnboardingScreen
import com.airmouse.presentation.ui.profiles.ProfilesScreen
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.touchpad.TouchpadScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun AirMouseNavHost(
    navController: NavHostController,
    startDestination: String = Destinations.Home.route,
    modifier: Modifier = Modifier,
    onServerSelected: (String, Int) -> Unit = { _, _ -> }
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { it }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { -it }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) +
                    slideInHorizontally(animationSpec = tween(300)) { -it }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) +
                    slideOutHorizontally(animationSpec = tween(300)) { it }
        }
    ) {
        // Bottom Navigation Screens
        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.Help.route) {
            HelpScreen(navigationActions = NavigationActions(navController))
        }
        
        // Info Screens
        composable(Destinations.About.route) {
            AboutScreen(navigationActions = NavigationActions(navController))
        }
        
        // Calibration & Sensors
        composable(Destinations.Calibration.route) {
            CalibrationScreen(
                navigationActions = NavigationActions(navController),
                onComplete = { navController.popBackStack() }
            )
        }
        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = NavigationActions(navController))
        }
        
        // Gesture & Touch
        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = NavigationActions(navController))
        }
        
        // Connectivity
        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(
                navigationActions = NavigationActions(navController),
                onServerSelected = { ip, port ->
                    navController.popBackStack()
                    NavigationActions(navController).navigateToHome()
                }
            )
        }
        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = NavigationActions(navController))
        }
        
        // Security & Privacy
        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = NavigationActions(navController))
        }
        
        // Customization
        composable(Destinations.Profiles.route) {
            ProfilesScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = NavigationActions(navController))
        }
        
        // System
        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = NavigationActions(navController))
        }
        composable(Destinations.TouchpadSettings.route) {
            TouchpadScreen(navigationActions = NavigationActions(navController))
        }
        
        // Onboarding
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(
                navigationActions = NavigationActions(navController),
                onComplete = { navController.popBackStack() }
            )
        }
    }
}