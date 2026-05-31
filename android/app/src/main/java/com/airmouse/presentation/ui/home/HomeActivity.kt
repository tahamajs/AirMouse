// app/src/main/java/com/airmouse/presentation/ui/home/HomeActivity.kt
package com.airmouse.presentation.ui.home

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import dagger.hilt.android.AndroidEntryPoint
import com.airmouse.presentation.theme.AirMouseTheme

@AndroidEntryPoint
class HomeActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            AirMouseTheme {
                HomeScreen()
            }
        }
    }
}