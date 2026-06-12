package com.airmouse.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.airmouse.presentation.navigation.NavigationActions
import androidx.navigation.navArgument
import com.airmouse.presentation.ui.about.AboutScreen
import com.airmouse.presentation.ui.accessibility.AccessibilityScreen
import com.airmouse.presentation.ui.battery.BatteryScreen
import com.airmouse.presentation.ui.calibration.CalibrationScreen
import com.airmouse.presentation.ui.edge.EdgeGesturesScreen
import com.airmouse.presentation.ui.gesture.GestureStudioScreen
import com.airmouse.presentation.ui.help.HelpScreen
import com.airmouse.presentation.ui.home.HomeScreen
import com.airmouse.presentation.ui.logs.ServerLogScreen
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
fun NavGraph(
    startDestination: Destinations = Destinations.Home,
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController()
) {
    val navigationActions = rememberNavigationActions(navController)
    NavHost(navController = navController, startDestination = startDestination.route, modifier = modifier) {
        composable(Destinations.Home.route) { HomeScreen(navigationActions) }
        composable(Destinations.Statistics.route) { StatisticsScreen(navigationActions) }
        composable(Destinations.Settings.route) { SettingsScreen(navigationActions) }
        composable(Destinations.Help.route) { HelpScreen(navigationActions) }
        composable(Destinations.About.route) { AboutScreen(navigationActions) }
        composable(Destinations.NetworkDiscovery.route) { NetworkDiscoveryScreen(navigationActions) }
        composable(Destinations.Profiles.route) { ProfilesScreen(navigationActions) }
        composable(Destinations.GestureStudio.route) { GestureStudioScreen(navigationActions) }
        composable(Destinations.EdgeGestures.route) { EdgeGesturesScreen(navigationActions) }
        composable(Destinations.VoiceCommands.route) { VoiceCommandsScreen(navigationActions) }
        composable(Destinations.Themes.route) { ThemesScreen(navigationActions) }
        composable(Destinations.ServerLogs.route) { ServerLogScreen(navigationActions) }
        composable(Destinations.Battery.route) { BatteryScreen(navigationActions) }
        composable(Destinations.Accessibility.route) { AccessibilityScreen(navigationActions) }
        composable(Destinations.Proximity.route) { ProximityScreen(navigationActions) }
        composable(Destinations.SensorVisualizer.route) { SensorVisualizerScreen() }
        composable(Destinations.Calibration.route) { CalibrationScreen(onComplete = { navigationActions.navigateToHome() }) }
        composable(Destinations.Onboarding.route) { OnboardingScreen(navigationActions) }
    }
}
