// app/src/main/java/com/airmouse/presentation/navigation/AirMouseNavHost.kt
package com.airmouse.presentation.navigation

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.calibration.CalibrationGuideDialog
import com.airmouse.presentation.ui.calibration.CalibrationScreen
import com.airmouse.presentation.ui.calibration.CalibrationViewModel
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
import androidx.hilt.navigation.compose.hiltViewModel

@Composable
fun AirMouseNavHost(
    navController: NavHostController,
    startDestination: String,
    modifier: Modifier = Modifier
) {
    val navActions = NavigationActionsImpl(navController)

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

        // ==================== CALIBRATION ====================

        // Calibration Guide Dialog - step-by-step instructions
        composable(Destinations.Calibration.route) {
            CalibrationGuideDialog(
                step = 0,
                onDismiss = { navActions.navigateBack() },
                onNextStep = {
                    // Navigate to calibration process
                    navActions.navigateTo(Destinations.CalibrationProcess.route)
                }
            )
        }

        // Calibration Process Screen - actual calibration
        composable(Destinations.CalibrationProcess.route) {
            val viewModel: CalibrationViewModel = hiltViewModel()
            CalibrationProcessScreen(
                viewModel = viewModel,
                navigationActions = navActions,
                onCalibrationComplete = { quality ->
                    // Navigate to results screen with quality parameter
                    navController.navigate(
                        Destinations.CalibrationResult.createRoute("quality" to quality)
                    ) {
                        popUpTo(Destinations.Calibration.route) { inclusive = true }
                    }
                }
            )
        }

        // Calibration Results Screen - shows the final quality
        composable(
            route = Destinations.CalibrationResult.route,
            arguments = listOf(
                navArgument("quality") { defaultValue = "GOOD" }
            )
        ) { backStackEntry ->
            val quality = backStackEntry.arguments?.getString("quality") ?: "GOOD"
            CalibrationScreen(
                navigationActions = navActions,
                onContinue = { navActions.navigateToHome() },
                onRecalibrate = {
                    navController.navigate(Destinations.Calibration.route) {
                        popUpTo(Destinations.CalibrationResult.route) { inclusive = true }
                    }
                },
                onSkip = { navActions.navigateToHome() },
                onComplete = { navActions.navigateToHome() }
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
            TouchpadSettingsScreen(navigationActions = navActions)
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