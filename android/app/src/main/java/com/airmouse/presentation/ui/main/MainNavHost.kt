package com.airmouse.presentation.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.navigation.rememberNavigationActions
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
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.touchpad.TouchpadScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen

// =======================================================================
// ProfilesScreen Missing Definition Stub
// NOTE: If you create an actual ProfilesScreen, remove this stub.
// =======================================================================
@Composable
fun ProfilesScreen(navigationActions: NavigationActions) {
    // Temporary fallback placeholder ui
}
// =======================================================================

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun MainNavHost(
    // Lint Fix: Fixed structural positional ordering of optional parameter modifiers
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route
) {
    val navigationActions = rememberNavigationActions(navController)

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
        // Main Bottom Navigation Screens
        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Help.route) {
            HelpScreen(navigationActions = navigationActions)
        }

        // Info Screens
        composable(Destinations.About.route) {
            AboutScreen(navigationActions = navigationActions)
        }

        // Calibration & Sensors
        composable(Destinations.Calibration.route) {
            CalibrationScreen(
                navigationActions = navigationActions
                // Fixed: CalibrationScreen does not support an explicit runtime onComplete hook lambda
            )
        }
        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = navigationActions)
        }

        // Gesture & Touch
        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = navigationActions)
        }
        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = navigationActions)
        }

        // Connectivity
        composable(Destinations.NetworkDiscovery.route) {
            // Fixed parameter ambiguity/types by routing strictly via the NavigationActions routing wrapper
            NetworkDiscoveryScreen(
                navigationActions = navigationActions
            )
        }
        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = navigationActions)
        }

        // Security & Privacy
        composable(Destinations.Proximity.route) {
            // Fixed: Solved KSP resolution ambiguity for ProximityScreen by passing exactly the expected base parameters
            ProximityScreen(navigationActions = navigationActions)
        }
        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = navigationActions)
        }

        // Customization
        composable(Destinations.Profiles.route) {
            ProfilesScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = navigationActions)
        }

        // System
        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = navigationActions)
        }
        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = navigationActions)
        }

        // Onboarding
        composable(Destinations.Onboarding.route) {
            // Fixed: OnboardingScreen definition lacks an onComplete hook parameter; relies strictly on internal navigation view model actions
            OnboardingScreen(
                navigationActions = navigationActions
            )
        }
    }
}