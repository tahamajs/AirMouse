// app/src/main/java/com/airmouse/presentation/navigation/AirMouseNavHost.kt
package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.airmouse.presentation.about.AboutScreen
import com.airmouse.presentation.accessibility.AccessibilityScreen
import com.airmouse.presentation.battery.BatteryScreen
import com.airmouse.presentation.calibration.CalibrationScreen
import com.airmouse.presentation.edge.EdgeGesturesScreen
import com.airmouse.presentation.gesture.GestureStudioScreen
import com.airmouse.presentation.help.HelpScreen
import com.airmouse.presentation.home.HomeScreen
import com.airmouse.presentation.network.NetworkDiscoveryScreen
import com.airmouse.presentation.onboarding.OnboardingScreen
import com.airmouse.presentation.profiles.ProfilesScreen
import com.airmouse.presentation.proximity.ProximityScreen
import com.airmouse.presentation.serverlog.ServerLogScreen
import com.airmouse.presentation.settings.SettingsScreen
import com.airmouse.presentation.statistics.StatisticsScreen
import com.airmouse.presentation.themes.ThemesScreen
import com.airmouse.presentation.voice.VoiceCommandsScreen
import com.airmouse.presentation.sensor.SensorVisualizerScreen

@Composable
fun AirMouseNavHost(
    startDestination: Destinations = Destinations.Home,
    modifier: Modifier = Modifier
) {
    val navController = rememberNavigationActions().navController
    val navigationActions = rememberNavigationActions(navController)

    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        // Main Screens
        composable(Destinations.Home.route) {
            HomeScreen(navigationActions)
        }
        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions)
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(navigationActions)
        }
        composable(Destinations.Help.route) {
            HelpScreen(navigationActions)
        }
        composable(Destinations.About.route) {
            AboutScreen(navigationActions)
        }

        // Advanced Features
        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions)
        }
        composable(Destinations.Profiles.route) {
            ProfilesScreen(navigationActions)
        }
        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions)
        }
        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions)
        }
        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions)
        }
        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions)
        }
        composable(Destinations.ServerLogs.route) {
            ServerLogScreen(navigationActions)
        }
        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions)
        }
        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions)
        }
        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions)
        }
        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions)
        }

        // Calibration Flow (with optional step argument)
        composable(
            route = Destinations.Calibration.route + "?step={step}",
            arguments = listOf(
                navArgument("step") {
                    type = NavType.IntType
                    defaultValue = 0
                }
            )
        ) { backStackEntry ->
            val step = backStackEntry.arguments?.getInt("step") ?: 0
            CalibrationScreen(navigationActions, step)
        }

        // Onboarding Flow
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions)
        }
    }
}