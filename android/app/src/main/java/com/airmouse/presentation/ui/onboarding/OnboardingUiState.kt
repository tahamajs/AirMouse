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
    val isAnimating: Boolean = false,
    val selectedFeatures: List<String> = emptyList(),
    val skipPressed: Boolean = false,
    val errorMessage: String? = null
) {
    val canGoNext: Boolean = currentPage < totalPages - 1
    val canGoPrevious: Boolean = currentPage > 0
    val isLastPage: Boolean = currentPage == totalPages - 1
    val progress: Float = (currentPage + 1).toFloat() / totalPages
    val progressPercent: Int = (progress * 100).toInt()

    val isFirstPage: Boolean = currentPage == 0
    val isWelcomePage: Boolean = currentPage == 0 && showWelcomeAnimation

    val pageTitle: String
        get() = when (currentPage) {
            0 -> "Welcome"
            1 -> "Gestures"
            2 -> "Connect"
            3 -> "Voice"
            4 -> "Proximity"
            5 -> "Ready"
            else -> ""
        }

    val pageDescription: String
        get() = when (currentPage) {
            0 -> "Turn your phone into a wireless mouse"
            1 -> "Control with natural hand movements"
            2 -> "WiFi, Bluetooth, or USB"
            3 -> "Hands-free commands"
            4 -> "Auto-lock when you walk away"
            5 -> "You're all set!"
            else -> ""
        }

    val isDarkTheme: Boolean
        get() = when (selectedTheme) {
            "dark" -> true
            "light" -> false
            "pure_black" -> true
            "high_contrast" -> false
            else -> true // system default
        }

    val shouldShowSkip: Boolean
        get() = !isLastPage && !isCompleted

    val shouldShowBack: Boolean
        get() = currentPage > 0 && !isCompleted

    val shouldShowNext: Boolean
        get() = !isLastPage && !isCompleted

    val shouldShowGetStarted: Boolean
        get() = isLastPage && !isCompleted

    val isUserReady: Boolean
        get() = userName.isNotEmpty() && isCompleted

    fun withPage(page: Int): OnboardingUiState {
        return copy(
            currentPage = page.coerceIn(0, totalPages - 1)
        )
    }

    fun nextPage(): OnboardingUiState {
        return if (canGoNext) {
            copy(
                currentPage = currentPage + 1,
                showWelcomeAnimation = currentPage == 0
            )
        } else this
    }

    fun previousPage(): OnboardingUiState {
        return if (canGoPrevious) {
            copy(
                currentPage = currentPage - 1,
                showWelcomeAnimation = false
            )
        } else this
    }

    fun complete(): OnboardingUiState {
        return copy(
            isCompleted = true,
            showWelcomeAnimation = false,
            isAnimating = false
        )
    }

    fun skip(): OnboardingUiState {
        return copy(
            skipPressed = true,
            isCompleted = true,
            showWelcomeAnimation = false
        )
    }

    fun setUserName(name: String): OnboardingUiState {
        return copy(userName = name)
    }

    fun setTheme(theme: String): OnboardingUiState {
        return copy(selectedTheme = theme)
    }

    fun setHaptic(enabled: Boolean): OnboardingUiState {
        return copy(hapticEnabled = enabled)
    }

    fun setAutoConnect(enabled: Boolean): OnboardingUiState {
        return copy(autoConnect = enabled)
    }

    fun setAnalytics(enabled: Boolean): OnboardingUiState {
        return copy(allowAnalytics = enabled)
    }

    fun setError(message: String?): OnboardingUiState {
        return copy(errorMessage = message)
    }

    fun clearError(): OnboardingUiState {
        return copy(errorMessage = null)
    }

    fun toggleFeature(feature: String): OnboardingUiState {
        return if (selectedFeatures.contains(feature)) {
            copy(selectedFeatures = selectedFeatures - feature)
        } else {
            copy(selectedFeatures = selectedFeatures + feature)
        }
    }

    fun isFeatureSelected(feature: String): Boolean {
        return selectedFeatures.contains(feature)
    }
}

/**
 * Factory function to create a default onboarding state
 */
fun defaultOnboardingState(): OnboardingUiState {
    return OnboardingUiState(
        currentPage = 0,
        totalPages = 6,
        isCompleted = false,
        userName = "",
        selectedTheme = "system",
        hapticEnabled = true,
        autoConnect = true,
        allowAnalytics = true,
        showWelcomeAnimation = true,
        isAnimating = false,
        selectedFeatures = listOf(
            "Motion Control",
            "Gesture Recognition",
            "Voice Commands"
        )
    )
}

/**
 * Extension function to check if onboarding should be shown
 */
fun OnboardingUiState.shouldShowOnboarding(): Boolean {
    return !isCompleted && !skipPressed
}

/**
 * Extension function to get the current page indicator text
 */
fun OnboardingUiState.getPageIndicator(): String {
    return "${currentPage + 1} / $totalPages"
}

/**
 * Extension function to get the button text for the current page
 */
fun OnboardingUiState.getButtonText(): String {
    return when {
        isLastPage -> "Get Started"
        currentPage == 0 -> "Explore"
        else -> "Next"
    }
}