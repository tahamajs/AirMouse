package com.airmouse.presentation.ui.settings

data class SettingsUiState(
    val sensitivity: Float = 0.5f,
    val clickThreshold: Float = 10f,
    val doubleClickInterval: Long = 300,
    val scrollThreshold: Float = 5f,
    val rightClickTilt: Float = 15f,
    val hapticEnabled: Boolean = true,
    val theme: String = "dark",
    val aiSmoothing: Boolean = false,
    val predictive: Boolean = true,
    val isLoading: Boolean = false,
    val error: String? = null
)