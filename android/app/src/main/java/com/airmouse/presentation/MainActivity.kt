package com.airmouse.presentation

import android.content.Intent
import android.graphics.Color
import android.os.Build
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airmouse.AirMouseApplication
import com.airmouse.presentation.navigation.AirMouseBottomBar
import com.airmouse.presentation.navigation.AirMouseNavHost
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.rememberNavigationActions
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.onboarding.OnboardingActivity
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
        // Install splash screen
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition {
            keepSplashOnScreen
        }

        super.onCreate(savedInstanceState)

        // Check if we need to show onboarding
        if (!prefs.isOnboardingCompleted()) {
            keepSplashOnScreen = false
            startOnboarding()
            return
        }

        // Setup window for edge-to-edge display
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
                    // Dismiss splash screen after a short delay
                    LaunchedEffect(Unit) {
                        delay(500)
                        keepSplashOnScreen = false
                    }

                    MainAppContent()
                }
            }
        }
    }

    private fun setupWindow() {
        // Enable edge-to-edge display
        WindowCompat.setDecorFitsSystemWindows(window, false)

        // Make status and navigation bars transparent
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }

        // Update system bar colors based on theme
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
        windowInsetsController?.let {
            it.isAppearanceLightStatusBars = !isDarkTheme
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                it.isAppearanceLightNavigationBars = !isDarkTheme
            }
        }
    }

    private fun startOnboarding() {
        val intent = Intent(this, OnboardingActivity::class.java)
        intent.flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        startActivity(intent)
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
    }

    override fun onResume() {
        super.onResume()
        updateSystemBarsColor()
    }
}

@Composable
fun MainAppContent() {
    val context = LocalContext.current
    val application = context.applicationContext as? AirMouseApplication
    val prefs = application?.prefsManager

    val startDestination = if (prefs?.isOnboardingCompleted() == true) {
        Destinations.Home.route
    } else {
        Destinations.Onboarding.route
    }

    MainScreen(startDestination = startDestination)
}

@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val navigationActions = rememberNavigationActions(navController)

    Scaffold(
        bottomBar = {
            if (currentRoute != null && Destinations.isBottomNavScreen(currentRoute)) {
                AirMouseBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { destination ->
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