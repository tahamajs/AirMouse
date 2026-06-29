// app/src/main/java/com/airmouse/presentation/navigation/AirMouseNavHost.kt
package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController

// Import your screen composables (adjust package names as needed)
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.help.HelpScreen
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.calibration.CalibrationScreen
import com.airmouse.presentation.ui.calibration.CalibrationProcessScreen
import com.airmouse.presentation.ui.calibration.CalibrationResultScreen
import com.airmouse.presentation.ui.sensor.SensorVisualizerScreen
import com.airmouse.presentation.ui.gesture.GestureStudioScreen
import com.airmouse.presentation.ui.edge.EdgeGesturesScreen
import com.airmouse.presentation.ui.gaming.GamingModeScreen
import com.airmouse.presentation.ui.touchpad.TouchpadScreen
import com.airmouse.presentation.ui.touchpad.TouchpadSettingsScreen
import com.airmouse.presentation.ui.network.NetworkDiscoveryScreen
import com.airmouse.presentation.ui.logs.ServerLogsScreen
import com.airmouse.presentation.ui.proximity.ProximityScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen
import com.airmouse.presentation.ui.profiles.ProfilesScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.onboarding.OnboardingScreen

/**
 * Main navigation host for the Air Mouse app.
 *
 * @param navController The NavHostController (defaults to rememberNavController).
 * @param startDestination The starting route (defaults to Destinations.Home.route).
 * @param modifier Modifier for the NavHost.
 */
@Composable
fun AirMouseNavHost(
    navController: NavHostController = rememberNavController(),
    startDestination: String = Destinations.Home.route,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = startDestination,
        modifier = modifier
    ) {
        // Bottom navigation screens
        composable(Destinations.Home.route) {
            HomeScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Statistics.route) {
            StatisticsScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Settings.route) {
            SettingsScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Help.route) {
            HelpScreen(navigationActions = NavigationActionsImpl(navController))
        }

        // Other screens
        composable(Destinations.About.route) {
            AboutScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Calibration.route) {
            CalibrationScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.CalibrationProcess.route) {
            CalibrationProcessScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.CalibrationResult.route) {
            CalibrationResultScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.SensorVisualizer.route) {
            SensorVisualizerScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.GestureStudio.route) {
            GestureStudioScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.EdgeGestures.route) {
            EdgeGesturesScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Touchpad.route) {
            TouchpadScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.GamingMode.route) {
            GamingModeScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.TouchpadSettings.route) {
            TouchpadSettingsScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.NetworkDiscovery.route) {
            NetworkDiscoveryScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.ServerLogs.route) {
            ServerLogsScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Proximity.route) {
            ProximityScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.VoiceCommands.route) {
            VoiceCommandsScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Profiles.route) {
            ProfilesScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Themes.route) {
            ThemesScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Battery.route) {
            BatteryScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Accessibility.route) {
            AccessibilityScreen(navigationActions = NavigationActionsImpl(navController))
        }
        composable(Destinations.Onboarding.route) {
            OnboardingScreen(navigationActions = NavigationActionsImpl(navController))
        }
    }
}
