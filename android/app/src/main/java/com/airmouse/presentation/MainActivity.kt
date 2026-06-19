// app/src/main/java/com/airmouse/presentation/MainActivity.kt
package com.airmouse.presentation

import android.content.Intent
import android.content.res.Configuration
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
import com.airmouse.ui.onboarding.OnboardingActivity
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
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)

        // Route directly to native onboarding activity if not completed
        if (!prefs.isOnboardingCompleted()) {
            keepSplashOnScreen = false
            startOnboarding()
            return
        }

        setupWindow()

        setContent {
            val isDarkTheme = when (prefs.getString("theme", "system")) {
                "dark", "pure_black" -> true
                "light", "high_contrast" -> false
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
                    LaunchedEffect(Unit) {
                        delay(500)
                        keepSplashOnScreen = false
                    }

                    // Onboarding is guaranteed to be complete here, route straight home
                    MainScreen(startDestination = Destinations.Home.route)
                }
            }
        }
    }

    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        updateSystemBarsColor()
    }

    private fun updateSystemBarsColor() {
        val isDarkTheme = when (prefs.getString("theme", "system")) {
            "dark", "pure_black" -> true
            "light", "high_contrast" -> false
            else -> (resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.isAppearanceLightStatusBars = !isDarkTheme
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = !isDarkTheme
        }
    }

    private fun startOnboarding() {
        startActivity(Intent(this, OnboardingActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        })
        finish()
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
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