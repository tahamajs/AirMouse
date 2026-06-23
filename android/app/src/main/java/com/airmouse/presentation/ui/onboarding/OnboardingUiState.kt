
package com.airmouse.presentation.ui.onboarding

data class OnboardingUiState(
    val currentPage: Int = 0,
    val showWelcomeAnimation: Boolean = true,
    val selectedTheme: String = "dark",
    val hapticEnabled: Boolean = true,
    val autoConnect: Boolean = true,
    val allowAnalytics: Boolean = true,
    val hasCompleted: Boolean = false
)