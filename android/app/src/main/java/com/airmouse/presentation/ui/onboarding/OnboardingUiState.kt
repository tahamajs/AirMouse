package com.airmouse.presentation.ui.onboarding

/**
 * UI state for onboarding screen
 */
data class OnboardingUiState(
    val currentPage: Int = 0,
    val totalPages: Int = 6,
    val isCompleted: Boolean = false,
    val userName: String = "",
    val selectedTheme: String = "system",
    val hapticEnabled: Boolean = true,
    val autoConnect: Boolean = true,
    val allowAnalytics: Boolean = true,
    val showWelcomeAnimation: Boolean = true,
    val isAnimating: Boolean = false
) {
    val canGoNext: Boolean = currentPage < totalPages - 1
    val canGoPrevious: Boolean = currentPage > 0
    val isLastPage: Boolean = currentPage == totalPages - 1
    val progress: Float = (currentPage + 1).toFloat() / totalPages
}