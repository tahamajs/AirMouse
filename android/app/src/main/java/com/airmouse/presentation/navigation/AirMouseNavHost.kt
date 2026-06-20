// app/src/main/java/com/airmouse/presentation/navigation/AirMouseNavHost.kt
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
import com.airmouse.presentation.ui.calibration.CalibrationProcessScreen
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
    modifier: Modifier = Modifier
) {
    val navActions = rememberNavigationActions(navController)

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
        // ==================== MAIN SCREENS ====================

        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = navActions)
        }

        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions = navActions)
        }

        composable(Destinations.Settings.route) {
            SettingsScreen(
                navigationActions = navActions,
                onBack = { navActions.navigateBack() }
            )
        }

        composable(Destinations.Profiles.route) {
            ProfilesScreen(
                navigationActions = navActions,
                onNavigateBack = { navActions.navigateBack() }
            )
        }

        // ==================== CALIBRATION (Process + Result) ====================

        // Calibration Process Screen - the step-by-step guide
        composable(Destinations.Calibration.route) {
            CalibrationProcessScreen(
                navigationActions = navActions,
                onCalibrationComplete = { resultData ->
                    // Navigate to results screen with quality parameter
                    navController.navigate(
                        Destinations.CalibrationResult.createRoute(resultData.quality)
                    ) {
                        popUpTo(Destinations.Calibration.route) { inclusive = true }
                    }
                }
            )
        }

        // Calibration Results Screen - shows the final quality
        composable(
            route = Destinations.CalibrationResult.route,
            arguments = Destinations.CalibrationResult.arguments
        ) { backStackEntry ->
            val quality = backStackEntry.arguments?.getString("quality") ?: "GOOD"
            CalibrationScreen(
                navigationActions = navActions,
                quality = quality,
                onContinue = { navController.popBackStack() },
                onRecalibrate = {
                    navController.navigate(Destinations.Calibration.route) {
                        popUpTo(Destinations.CalibrationResult.route) { inclusive = true }
                    }
                },
                onSkip = { navController.popBackStack() },
                onComplete = { navController.popBackStack() }
            )
        }

        // ==================== OTHER SCREENS ====================

        composable(Destinations.Help.route) {
            HelpScreen(navigationActions = navActions)
        }

        composable(Destinations.About.route) {
            AboutScreen(navigationActions = navActions)
        }

        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = navActions)
        }

        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = navActions)
        }

        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = navActions)
        }

        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = navActions)
        }

        composable(Destinations.TouchpadSettings.route) {
            TouchpadScreen(navigationActions = navActions)
        }

        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions = navActions)
        }

        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = navActions)
        }

        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions = navActions)
        }

        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = navActions)
        }

        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = navActions)
        }

        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = navActions)
        }

        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = navActions)
        }

        // ==================== ONBOARDING ====================

        composable(Destinations.Onboarding.route) {
            OnboardingScreen(
                navigationActions = navActions,
                onComplete = {
                    navController.navigate(Destinations.Home.route) {
                        popUpTo(Destinations.Onboarding.route) { inclusive = true }
                    }
                },
                onSkip = {
                    navController.navigate(Destinations.Home.route) {
                        popUpTo(Destinations.Onboarding.route) { inclusive = true }
                    }
                }
            )
        }
    }
}