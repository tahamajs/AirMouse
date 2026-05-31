package com.airmouse.presentation.ui.edge

data class EdgeGesturesUiState(
    val isEnabled: Boolean = false,
    val volumeUpAction: String = "Click",
    val volumeDownAction: String = "Scroll Up"
)