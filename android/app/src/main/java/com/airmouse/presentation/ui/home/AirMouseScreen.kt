package com.airmouse.presentation.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

/**
 * AirMouseScreen: Standalone screen wrapper with isolated high-frequency telemetry.
 * Prevents full-screen recompositions when pitch/roll/yaw stream at 60-120Hz.
 */
@Composable
fun AirMouseScreen(
    navigationActions: NavigationActions,
    onOpenDrawer: (() -> Unit)? = null,
    viewModel: HomeViewModel = hiltViewModel()
) {
    // Delegates to HomeScreen while keeping telemetry decoupled in leaf composables
    HomeScreen(
        navigationActions = navigationActions,
        onOpenDrawer = onOpenDrawer
    )
}

/**
 * Isolated leaf composable for live sensor orientation telemetry.
 * Recomposes independently without triggering parent screen sweeps.
 */
@Composable
fun IsolatedOrientationTelemetry(
    viewModel: HomeViewModel = hiltViewModel(),
    content: @Composable (yaw: Float, pitch: Float, roll: Float) -> Unit
) {
    val telemetry by viewModel.orientationTelemetry.collectAsStateWithLifecycle()
    content(telemetry.yaw, telemetry.pitch, telemetry.roll)
}
