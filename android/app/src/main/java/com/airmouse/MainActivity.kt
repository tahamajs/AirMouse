package com.airmouse.presentation

import android.os.Build
import android.os.Bundle
import android.view.WindowManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import androidx.core.view.WindowCompat
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.main.MainScreen
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.delay
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var prefs: PreferencesManager
    
    private var keepSplashOnScreen = true
    
    override fun onCreate(savedInstanceState: Bundle?) {
        // Install splash screen
        installSplashScreen().setKeepOnScreenCondition {
            keepSplashOnScreen
        }
        
        super.onCreate(savedInstanceState)
        
        // Configure window for edge-to-edge display
        setupWindow()
        
        // Set content
        setContent {
            val isDarkTheme = when (prefs.getTheme()) {
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
                    // Check onboarding status
                    val startDestination = if (prefs.isOnboardingCompleted()) {
                        Destinations.Home.route
                    } else {
                        Destinations.Onboarding.route
                    }
                    
                    // Hide splash screen after a short delay
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
        // Make status bar and navigation bar transparent
        WindowCompat.setDecorFitsSystemWindows(window, false)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
            
            // Set status bar and navigation bar to transparent
            window.statusBarColor = Color.Transparent.toArgb()
            window.navigationBarColor = Color.Transparent.toArgb()
            
            // Make icons light or dark based on theme
            updateSystemBarsColor()
        }
    }
    
    private fun updateSystemBarsColor() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val isDarkTheme = when (prefs.getTheme()) {
                "dark" -> true
                "pure_black" -> true
                else -> isSystemInDarkTheme()
            }
            
            if (!isDarkTheme) {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility or
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
            } else {
                window.decorView.systemUiVisibility = window.decorView.systemUiVisibility and
                        android.view.View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR.inv()
            }
        }
    }
    
    override fun onResume() {
        super.onResume()
        updateSystemBarsColor()
    }
    
    override fun onPause() {
        super.onPause()
        // Pause background operations if needed
    }
    
    override fun onDestroy() {
        super.onDestroy()
        // Clean up resources
    }
}

// MainScreen.kt - Use these components:
@Composable
fun MainScreen() {
    // Use: ParticleBackground, AnimatedGradientBackground
    ParticleBackground(particleCount = 30)
    
    // Use: AnimatedConnectionStatus, ConnectionStatusBadge
    ConnectionStatusBadge(connectionManager = connectionManager)
    
    // Use: FloatingActionMenu for quick actions
    FloatingActionMenu(
        items = listOf(
            FABMenuItem("calibrate", "Calibrate", Icons.Default.Build),
            FABMenuItem("network", "Network", Icons.Default.Wifi),
            FABMenuItem("gesture", "Gesture", Icons.Default.Gesture)
        ),
        onItemClick = { /* handle click */ }
    )
}