package com.airmouse.presentation

import android.graphics.Color
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airmouse.presentation.navigation.AirMouseBottomBar
import com.airmouse.presentation.navigation.AirMouseNavHost
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.rememberNavigationActions
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PreferencesManager

    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen().setKeepOnScreenCondition {
            keepSplashOnScreen
        }

        super.onCreate(savedInstanceState)
        setupWindow()

        setContent {
            val isDarkTheme = when (prefs.getString("theme", "system")) {
                "dark" -> true
                "light" -> false
                "pure_black" -> true
                "high_contrast" -> false
                else -> isSystemInDarkTheme()
            }

            AirMouseTheme(
                darkTheme = isDarkTheme,
                useDynamicColor = prefs.getBoolean("dynamic_colors", true)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = if (prefs.isOnboardingCompleted()) {
                        Destinations.Home.route
                    } else {
                        Destinations.Onboarding.route
                    }

                    LaunchedEffect(Unit) {
                        delay(500)
                        keepSplashOnScreen = false
                    }

                    MainScreen(startDestination = startDestination)
                }
            }
        }
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
        window.statusBarColor = Color.TRANSPARENT
        window.navigationBarColor = Color.TRANSPARENT
        updateSystemBarsColor()
    }

    private fun updateSystemBarsColor() {
        val isDarkTheme = when (prefs.getString("theme", "system")) {
            "dark" -> true
            "pure_black" -> true
            "light" -> false
            "high_contrast" -> false
            else -> isSystemInDarkTheme()
        }

        val windowInsetsController = WindowCompat.getInsetsController(window, window.decorView)
        windowInsetsController.isAppearanceLightStatusBars = !isDarkTheme
        windowInsetsController.isAppearanceLightNavigationBars = !isDarkTheme
    }

    override fun onResume() {
        super.onResume()
        updateSystemBarsColor()
    }
}

@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val navigationActions = rememberNavigationActions(navController)

    Scaffold(
        bottomBar = {
            if (Destinations.isBottomNavScreen(currentRoute)) {
                AirMouseBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { destination: Destinations ->
                        navigationActions.navigateTo(destination)
                    }
                )
            }
        }
    ) { paddingValues ->
        AirMouseNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        )
    }
}