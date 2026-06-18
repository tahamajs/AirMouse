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

@Composable
fun AirMouseNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier,
    onServerSelected: (String, Int) -> Unit = { _, _ -> }
) {
    val navActions = rememberNavigationActions(navController)

    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier,
        enterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { it }
        },
        exitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { -it }
        },
        popEnterTransition = {
            fadeIn(animationSpec = tween(300)) + slideInHorizontally(animationSpec = tween(300)) { -it }
        },
        popExitTransition = {
            fadeOut(animationSpec = tween(300)) + slideOutHorizontally(animationSpec = tween(300)) { it }
        }
    ) {
        // Bottom Navigation Screens
        composable(Destinations.Home.route) { HomeScreen(navigationActions = navActions) }
        composable(Destinations.Statistics.route) { StatisticsScreen(navigationActions = navActions) }
        composable(Destinations.Settings.route) { SettingsScreen(navigationActions = navActions) }
        composable(Destinations.Help.route) { HelpScreen(navigationActions = navActions) }

        // Info Screens
        composable(Destinations.About.route) { AboutScreen(navigationActions = navActions) }

        // Calibration & Sensors
        composable(Destinations.Calibration.route) {
            CalibrationScreen(
                navigationActions = navActions,
                onComplete = { navController.popBackStack() }
            )
        }
        composable(Destinations.SensorVisualizer.route) { SensorVisualizerScreen(navigationActions = navActions) }

        // Gesture & Touch
        composable(Destinations.GestureStudio.route) { GestureStudioScreen(navigationActions = navActions) }
        composable(Destinations.EdgeGestures.route) { EdgeGesturesScreen(navigationActions = navActions) }
        composable(Destinations.Touchpad.route) { TouchpadScreen(navigationActions = navActions) }

        // Connectivity
        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(
                navigationActions = navActions,
                onServerSelected = { ip, port ->
                    onServerSelected(ip, port)
                    navController.popBackStack()
                    navActions.navigateToHome()
                }
            )
        }
        composable(Destinations.ServerLogs.route) { ServerLogsScreen(navigationActions = navActions) }

        // Security & Privacy
        composable(Destinations.Proximity.route) { ProximityScreen(navigationActions = navActions) }
        composable(Destinations.VoiceCommands.route) { VoiceCommandsScreen(navigationActions = navActions) }

        // Customization
        composable(Destinations.Profiles.route) { ProfilesScreen(navigationActions = navActions) }
        composable(Destinations.Themes.route) { ThemesScreen(navigationActions = navActions) }

        // System
        composable(Destinations.Battery.route) { BatteryScreen(navigationActions = navActions) }
        composable(Destinations.Accessibility.route) { AccessibilityScreen(navigationActions = navActions) }
        composable(Destinations.TouchpadSettings.route) { TouchpadScreen(navigationActions = navActions) }

        // Onboarding
        composable(Destinations.Onboarding.route) {
            // FIXED: Removed invalid onComplete trailing lambda to perfectly match your implementation signature
            OnboardingScreen(navigationActions = navActions)
        }
    }
}