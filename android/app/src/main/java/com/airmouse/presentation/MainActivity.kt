
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
import androidx.compose.foundation.isSystemInDarkTheme
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
import com.airmouse.presentation.ui.themes.AccentColor
import com.airmouse.presentation.ui.themes.LocalThemeColors
import com.airmouse.presentation.ui.themes.ProvideThemeColors
import com.airmouse.presentation.ui.themes.getThemeColorScheme
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

        
        if (!prefs.isOnboardingCompleted()) {
            keepSplashOnScreen = false
            startOnboarding()
            return
        }

        setupWindow()

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

            
            LaunchedEffect(themeId) {
                updateSystemBarsForTheme(themeId)
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
                        LaunchedEffect(Unit) {
                            delay(500)
                            keepSplashOnScreen = false
                        }

                        MainScreen(startDestination = Destinations.Home.route)
                    }
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
        val themeId = prefs.getString("theme", "system")
        updateSystemBarsForTheme(themeId)
    }

    private fun updateSystemBarsForTheme(themeId: String) {
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

            Log.w("MainActivity", "Onboarding activity is not resolvable; falling back to main screen")
            setupWindow()
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
        } catch (e: Exception) {
            Log.e("MainActivity", "Failed to launch onboarding, continuing into main screen", e)
            setupWindow()
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
    }

    override fun onResume() {
        super.onResume()
        updateSystemBarsColor()
    }

    override fun onConfigurationChanged(newConfig: Configuration) {
        super.onConfigurationChanged(newConfig)
        updateSystemBarsColor()
    }
}

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
