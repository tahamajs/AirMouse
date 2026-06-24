package com.airmouse.presentation

import android.content.Intent
import android.content.res.Configuration
import android.graphics.Color
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.airmouse.presentation.navigation.AirMouseBottomBar
import com.airmouse.presentation.navigation.AirMouseNavHost
import com.airmouse.presentation.navigation.Destinations
import com.airmouse.presentation.navigation.NavigationActionsImpl
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.themes.*
import com.airmouse.ui.onboarding.OnboardingActivity
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

/**
 * Main entry point for the Air Mouse Android app.
 * Handles splash screen, onboarding redirection, theme setup, and navigation.
 */
@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PreferencesManager

    // Keeps the splash screen visible while loading resources.
    private var keepSplashOnScreen = true

    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen and keep it up while we load.
        val splashScreen = installSplashScreen()
        splashScreen.setKeepOnScreenCondition { keepSplashOnScreen }

        super.onCreate(savedInstanceState)

        // If user hasn't completed onboarding, go directly to onboarding flow.
        if (!prefs.isOnboardingCompleted()) {
            keepSplashOnScreen = false
            startOnboarding()
            return
        }

        // Set up system bars and window flags.
        setupWindow()

        // Set the Compose content.
        setContent {
            // Read theme and accent preferences.
            val themeId = prefs.getString("theme", "system")
            val accentName = prefs.getString("accent_color", "ORANGE")
            val accentColor = try {
                AccentColor.valueOf(accentName)
            } catch (e: Exception) {
                AccentColor.ORANGE
            }

            // Determine dark mode based on theme selection.
            val isDarkTheme = when (themeId) {
                "light" -> false
                "high_contrast" -> false
                else -> true
            }

            // Build the color scheme.
            val themeColors = remember(themeId, accentColor) {
                getThemeColorScheme(themeId, accentColor)
            }

            // Update system bars (status & navigation) to match the theme.
            LaunchedEffect(themeId) {
                updateSystemBarsForTheme(themeId)
            }

            // Provide the theme colors to the rest of the app via CompositionLocal.
            ProvideThemeColors(themeColors) {
                AirMouseTheme(
                    darkTheme = isDarkTheme,
                    useDynamicColor = prefs.getBoolean("dynamic_colors", true),
                    themeColors = themeColors
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = themeColors.background
                    ) {
                        // Hide splash screen after a short delay.
                        LaunchedEffect(Unit) {
                            delay(500)
                            keepSplashOnScreen = false
                        }

                        // The main navigation host.
                        MainScreen(startDestination = Destinations.Home.route)
                    }
                }
            }
        }
    }

    /**
     * Configures the window for edge‑to‑edge display and transparent system bars.
     */
    private fun setupWindow() {
        WindowCompat.setDecorFitsSystemWindows(window, false)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            window.statusBarColor = Color.TRANSPARENT
            window.navigationBarColor = Color.TRANSPARENT
        }
        updateSystemBarsColor()
    }

    /**
     * Updates the system bar appearance (light/dark icons) based on the current theme.
     */
    private fun updateSystemBarsColor() {
        val themeId = prefs.getString("theme", "system")
        updateSystemBarsForTheme(themeId)
    }

    /**
     * Updates system bars for a specific theme.
     * @param themeId The theme identifier (e.g., "light", "dark", "system", "high_contrast").
     */
    private fun updateSystemBarsForTheme(themeId: String) {
        // Determine if the status bar should be light (dark icons) or dark.
        val isDark = when (themeId) {
            "light" -> false
            "high_contrast" -> false
            else -> true
        }

        val controller = WindowCompat.getInsetsController(window, window.decorView)
        controller?.isAppearanceLightStatusBars = !isDark
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            controller.isAppearanceLightNavigationBars = !isDark
        }

        // Set navigation bar color to match the theme background.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            val accentName = prefs.getString("accent_color", "ORANGE")
            val accentColor = try {
                AccentColor.valueOf(accentName)
            } catch (e: Exception) {
                AccentColor.ORANGE
            }

            val colorScheme = getThemeColorScheme(themeId, accentColor)
            window.navigationBarColor = colorScheme.background.toArgb()
            window.statusBarColor = Color.TRANSPARENT
        }
    }

    /**
     * Launches the onboarding activity and finishes this activity.
     * If the onboarding activity is not available, falls back to the main screen.
     */
    private fun startOnboarding() {
        try {
            val intent = Intent(this, OnboardingActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
            }
            if (intent.resolveActivity(packageManager) != null) {
                startActivity(intent)
                finish()
                overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out)
                return
            }

            // Fallback: onboarding activity not found – go directly to main screen.
            Log.w("MainActivity", "Onboarding activity is not resolvable; falling back to main screen")
            setupWindow()
            launchMainScreen()
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to launch onboarding, continuing into main screen", e)
            setupWindow()
            launchMainScreen()
        }
    }

    /**
     * Launches the main screen content directly (used as a fallback).
     */
    private fun launchMainScreen() {
        setContent {
            val themeId = prefs.getString("theme", "system")
            val accentName = prefs.getString("accent_color", "ORANGE")
            val accentColor = try {
                AccentColor.valueOf(accentName)
            } catch (e: Exception) {
                AccentColor.ORANGE
            }
            val isDarkTheme = when (themeId) {
                "light" -> false
                "high_contrast" -> false
                else -> true
            }
            val themeColors = remember(themeId, accentColor) {
                getThemeColorScheme(themeId, accentColor)
            }
            ProvideThemeColors(themeColors) {
                AirMouseTheme(
                    darkTheme = isDarkTheme,
                    useDynamicColor = prefs.getBoolean("dynamic_colors", true),
                    themeColors = themeColors
                ) {
                    Surface(
                        modifier = Modifier.fillMaxSize(),
                        color = themeColors.background
                    ) {
                        MainScreen(startDestination = Destinations.Home.route)
                    }
                }
            }
        }
    }

    // ============================================================
    // Lifecycle callbacks to handle system bar updates.
    // ============================================================

    override fun onResume() {
        super.onResume()
        updateSystemBarsColor()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarsColor()
    }
}

// ============================================================
// MainScreen composable – the root navigation host.
// ============================================================

/**
 * The main application screen with a bottom navigation bar.
 * @param startDestination The initial destination route.
 */
@Composable
fun MainScreen(startDestination: String) {
    val navController = rememberNavController()
    val navBackStackEntry = navController.currentBackStackEntryAsState().value
    val currentRoute = navBackStackEntry?.destination?.route
    val navigationActions = NavigationActionsImpl(navController)
    val themeColors = LocalThemeColors.current

    Scaffold(
        bottomBar = {
            if (Destinations.isBottomNavScreen(currentRoute)) {
                AirMouseBottomBar(
                    currentRoute = currentRoute,
                    onItemSelected = { destination ->
                        navigationActions.navigateTo(destination.route)
                    }
                )
            }
        },
        containerColor = themeColors.background
    ) { paddingValues ->
        AirMouseNavHost(
            navController = navController,
            startDestination = startDestination,
            modifier = Modifier.padding(paddingValues)
        )
    }
}