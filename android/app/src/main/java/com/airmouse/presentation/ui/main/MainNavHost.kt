
@file:Suppress("unused")

package com.airmouse.presentation.ui.main

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
    startDestination: String = Destinations.Home.route,
    onOpenDrawer: (() -> Unit)? = null
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




        composable(Destinations.Home.route) {
            HomeScreen(
                navigationActions = navigationActions,
                onOpenDrawer = onOpenDrawer
            )
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





        composable(Destinations.About.route) {
            AboutScreen(navigationActions = navigationActions)
        }





        composable(Destinations.Calibration.route) {
            CalibrationScreen(
                navigationActions = navigationActions,
                onComplete = { }
            )
        }

        composable(
            route = "${Destinations.ROUTE_CALIBRATION_RESULT}?quality={quality}",
            arguments = listOf(
                navArgument("quality") {
                    type = NavType.StringType
                    defaultValue = "UNKNOWN"
                    nullable = false
                }
            )
        ) {
            CalibrationResultScreen(
                navigationActions = navigationActions,
                onContinue = { navigationActions.navigateToHome() },
                onRecalibrate = { navigationActions.navigateTo(Destinations.Calibration.route) }
            )
        }

        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = navigationActions)
        }





        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = navigationActions)
        }

        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = navigationActions)
        }





        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions = navigationActions)
        }

        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = navigationActions)
        }





        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions = navigationActions)
        }

        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = navigationActions)
        }





        composable(Destinations.Profiles.route) {
            ProfilesScreen(
                navigationActions = navigationActions,
                onNavigateBack = { navigationActions.navigateBack() }
            )
        }

        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = navigationActions)
        }





        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = navigationActions)
        }





        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions = navigationActions)
        }
    }
}
