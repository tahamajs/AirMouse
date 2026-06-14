package com.airmouse

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.airmouse.PreferencesManager  // Update with your actual package path
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airmouse.presentation.navigation.NavGraph
import com.airmouse.presentation.theme.AirMouseTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)

        setContent {
            AirMouseTheme(
                theme = prefs.getTheme(),      // "light", "dark", "pure_black", "high_contrast", "system"
                useDynamicColor = true,        // Enable Material You colors on Android 12+
                darkTheme = isSystemInDarkTheme()  // Auto-detect system theme
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NavGraph()
                }
            }
        }
    }
}


package com.airmouse.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.main.MainScreen
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    
    @Inject lateinit var prefs: PreferencesManager
    
    override fun onCreate(savedInstanceState: Bundle?) {
        installSplashScreen()
        super.onCreate(savedInstanceState)
        
        setContent {
            AirMouseTheme(
                darkTheme = prefs.getTheme() == "dark" || prefs.getTheme() == "system"
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    val startDestination = if (prefs.isOnboardingCompleted()) {
                        "home"
                    } else {
                        "onboarding"
                    }
                    MainScreen(startDestination = startDestination)
                }
            }
        }
    }
}