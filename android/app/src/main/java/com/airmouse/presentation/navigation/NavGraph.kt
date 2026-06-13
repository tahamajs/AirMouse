package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
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
import com.airmouse.presentation.ui.profiles.ProfilesScreen
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen

@Composable
fun NavGraph(
    navController: NavHostController = rememberNavController(),
    modifier: Modifier = Modifier
) {
    val navigationActions = rememberNavigationActions(navController)

    NavHost(
        navController = navController,
        startDestination = Destinations.Home.route,
        modifier = modifier
    ) {
        composable(Destinations.Home.route) {
            HomeScreen()
        }

        composable(Destinations.Calibration.route) {
            CalibrationScreen(
                onComplete = { navController.popBackStack() }
            )
        }

        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen()
        }

        composable(Destinations.Settings.route) {
            SettingsScreen()
        }

        composable(Destinations.Proximity.route) {
            ProximityScreen()
        }

        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen()
        }

        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Themes.route) {
            ThemesScreen()
        }

        composable(Destinations.Profiles.route) {
            ProfilesScreen()
        }

        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen()
        }

        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Statistics.route) {
            StatisticsScreen()
        }

        composable(Destinations.About.route) {
            AboutScreen(navigationActions = navigationActions)
        }

        composable(Destinations.Help.route) {
            HelpScreen()
        }

        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen()
        }
        composable("calibration") {
            CalibrationScreen(
                navigationActions = navigationActions,
                onComplete = { navController.popBackStack() }
            )
        }
    }
}
