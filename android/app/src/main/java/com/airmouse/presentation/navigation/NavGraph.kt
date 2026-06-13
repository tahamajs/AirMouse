package com.airmouse.presentation.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
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
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.presentation.ui.statistics.StatisticsScreen
import com.airmouse.presentation.ui.themes.ThemesScreen
import com.airmouse.presentation.ui.voice.VoiceCommandsScreen

sealed class Screen(val route: String) {
    object Home : Screen("home")
    object Calibration : Screen("calibration")
    object GestureStudio : Screen("gesture_studio")
    object Settings : Screen("settings")
    object Proximity : Screen("proximity")
    object VoiceCommands : Screen("voice_commands")
    object EdgeGestures : Screen("edge_gestures")
    object Themes : Screen("themes")
    object Profiles : Screen("profiles")
    object NetworkDiscovery : Screen("network_discovery")
    object ServerLogs : Screen("server_logs")
    object Battery : Screen("battery")
    object Accessibility : Screen("accessibility")
    object Statistics : Screen("statistics")
    object About : Screen("about")
    object Help : Screen("help")
    object Touchpad : Screen("touchpad")
}

@Composable
fun NavGraph(modifier: Modifier = Modifier) {
    val navController = rememberNavController()
    val navigationActions = NavigationActions(navController)
    NavHost(
        navController = navController,
        startDestination = Screen.Home.route,
        modifier = modifier
    ) {
        composable(Screen.Home.route) { HomeScreen() }
        composable(Screen.Calibration.route) { CalibrationScreen(onComplete = { navController.popBackStack() }) }
        composable(Screen.GestureStudio.route) { GestureStudioScreen() }
        composable(Screen.Settings.route) { SettingsScreen() }
        composable(Screen.Proximity.route) { ProximityScreen() }
        composable(Screen.VoiceCommands.route) { VoiceCommandsScreen() }
        composable(Screen.EdgeGestures.route) { EdgeGesturesScreen(navigationActions) }
        composable(Screen.Themes.route) { ThemesScreen() }
        composable(Screen.Profiles.route) { ProfilesScreen() }
        composable(Screen.NetworkDiscovery.route) { NetworkDiscoveryScreen() }
        composable(Screen.ServerLogs.route) { ServerLogsScreen(navigationActions) }
        composable(Screen.Battery.route) { BatteryScreen(navigationActions) }
        composable(Screen.Accessibility.route) { AccessibilityScreen() }
        composable(Screen.Statistics.route) { StatisticsScreen() }
        composable(Screen.About.route) { AboutScreen(navigationActions) }
        composable(Screen.Help.route) { HelpScreen() }
    }
}
