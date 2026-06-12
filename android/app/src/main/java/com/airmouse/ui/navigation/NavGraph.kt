package com.airmouse.presentation.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavType
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.airmouse.presentation.navigation.NavigationActions
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
    navController: NavHostController,
    navigationActions: NavigationActions,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = "home",
        modifier = modifier
    ) {
        composable("home") {
            HomeScreen(navigationActions = navigationActions)
        }
        composable("statistics") {
            StatisticsScreen(navigationActions = navigationActions)
        }
        composable("settings") {
            SettingsScreen(navigationActions = navigationActions)
        }
        composable("help") {
            HelpScreen(navigationActions = navigationActions)
        }
        composable("about") {
            AboutScreen(navigationActions = navigationActions)
        }
        composable("network") {
            NetworkDiscoveryScreen(navigationActions = navigationActions)
        }
        composable("profiles") {
            ProfilesScreen(navigationActions = navigationActions)
        }
        composable("gesture") {
            GestureStudioScreen(navigationActions = navigationActions)
        }
        composable("edge") {
            EdgeGesturesScreen(navigationActions = navigationActions)
        }
        composable("voice") {
            VoiceCommandsScreen(navigationActions = navigationActions)
        }
        composable("themes") {
            ThemesScreen(navigationActions = navigationActions)
        }
        composable("serverlog") {
            ServerLogScreen(navigationActions = navigationActions)
        }
        composable("battery") {
            BatteryScreen(navigationActions = navigationActions)
        }
        composable("accessibility") {
            AccessibilityScreen(navigationActions = navigationActions)
        }
        composable("proximity") {
            ProximityScreen(navigationActions = navigationActions)
        }
        composable("sensor") {
            SensorVisualizerScreen(navigationActions = navigationActions)
        }
        composable("onboarding") {
            OnboardingScreen(navigationActions = navigationActions)
        }
        composable(
            route = "calibration?step={step}",
            arguments = listOf(navArgument("step") { type = NavType.IntType; defaultValue = 0 })
        ) { backStackEntry ->
            val step = backStackEntry.arguments?.getInt("step") ?: 0
            CalibrationScreen(step = step, navigationActions = navigationActions)
        }
        composable("touchpad") {
            TouchpadScreen(navigationActions = navigationActions)
        }
    }
}