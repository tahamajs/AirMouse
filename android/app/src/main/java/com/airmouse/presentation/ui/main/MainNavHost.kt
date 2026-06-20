// app/src/main/java/com/airmouse/presentation/ui/main/MainNavHost.kt
@file:Suppress("unused")

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
import com.airmouse.presentation.navigation.NavigationActionsImpl
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.calibration.CalibrationResultScreen
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
fun MainNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route
) {
    val navigationActions = NavigationActionsImpl(navController)

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
        // ==========================================
        // MAIN BOTTOM NAVIGATION SCREENS
        // ==========================================

        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Settings.route) {
            SettingsScreen(
                navigationActions = navigationActions,
                onBack = { navigationActions.navigateBack() }
            )
        }

        composable(Destinations.Help.route) {
            HelpScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // INFO SCREENS
        // ==========================================

        composable(Destinations.About.route) {
            AboutScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // CALIBRATION & SENSORS
        // ==========================================

        composable(Destinations.Calibration.route) {
            CalibrationScreen(
                navigationActions = navigationActions,
                onComplete = {
                    navigationActions.navigateToHome()
                }
            )
        }

        composable(Destinations.CalibrationResult.route) {
            CalibrationResultScreen(
                navigationActions = navigationActions,
                onContinue = { navigationActions.navigateToHome() },
                onRecalibrate = { navigationActions.navigateTo(Destinations.Calibration.route) }
            )
        }

        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // GESTURE & TOUCH
        // ==========================================

        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = navigationActions)
        }

        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // CONNECTIVITY
        // ==========================================

        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions = navigationActions)
        }

        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // SECURITY & PRIVACY
        // ==========================================

        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions = navigationActions)
        }

        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // CUSTOMIZATION
        // ==========================================

        composable(Destinations.Profiles.route) {
            ProfilesScreen(
                navigationActions = navigationActions,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // SYSTEM
        // ==========================================

        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = navigationActions)
        }

        // ==========================================
        // ONBOARDING
        // ==========================================

        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions = navigationActions)
        }
    }
}
