// app/src/main/java/com/airmouse/presentation/navigation/AirMouseNavHost.kt
package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.calibration.CalibrationScreen
import com.airmouse.presentation.ui.edge.EdgeGesturesScreen
import com.airmouse.presentation.ui.gesture.GestureStudioScreen
import com.airmouse.presentation.ui.help.HelpScreen
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.network.NetworkDiscoveryScreen
import com.airmouse.presentation.ui.onboarding.OnboardingScreen
import com.airmouse.presentation.ui.profiles.ProfilesScreen
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.logs.ServerLogsScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen

@Composable
fun AirMouseNavHost(
    startDestination: Destinations = Destinations.Home,
    modifier: Modifier = Modifier
) {
    val navController = androidx.navigation.compose.rememberNavController()
    val navigationActions = rememberNavigationActions(navController)

    NavHost(
        navController = navController,
        startDestination = startDestination.route,
        modifier = modifier
    ) {
        // Main Screens
        composable(Destinations.Home.route) {
            HomeScreen()
        }
        composable(Destinations.Statistics.route) {
            StatisticsScreen()
        }
        composable(Destinations.Settings.route) {
            SettingsScreen()
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
            ServerLogsScreen(navigationActions)
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
            SensorVisualizerScreen()
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
