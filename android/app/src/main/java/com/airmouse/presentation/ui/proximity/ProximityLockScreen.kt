package com.airmouse.presentation.ui.proximity

import androidx.compose.runtime.Composable
import androidx.hilt.navigation.compose.hiltViewModel
import com.airmouse.presentation.navigation.NavigationActions

/**
 * ProximityLockScreen provides proximity auto-lock controls based on Bluetooth signal strength.
 * Aliases and delegates directly to ProximityScreen.
 */
@Composable
fun ProximityLockScreen(
    navigationActions: NavigationActions,
    viewModel: ProximityViewModel = hiltViewModel()
) {
    ProximityScreen(navigationActions = navigationActions, viewModel = viewModel)
}
