package com.airmouse.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.theme.AirMouseTheme
import com.airmouse.presentation.ui.settings.SettingsScreen
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class SettingsActivity : ComponentActivity() {

    @Inject
    lateinit var prefs: PreferencesManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContent {
            AirMouseTheme(
                darkTheme = prefs.getTheme() == "dark",
                useDynamicColor = prefs.getBoolean("dynamic_colors", true)
            ) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    SettingsScreen(
                        navigationActions = NavigationActions(null),
                        onBack = { finish() }
                    )
                }
            }
        }
    }
}