package com.airmouse.presentation.ui.proximity

data class ProximityUiState(
    val isEnabled: Boolean = false,
    val nearThreshold: Float = 2.0f,
    val farThreshold: Float = 4.0f,
    val currentDistance: Float? = null,
    val status: String = "Service stopped",
    val isCalibrating: Boolean = false
)