package com.airmouse.presentation.ui.gesture

import com.airmouse.domain.model.CustomGestureTemplate

data class GestureStudioUiState(
    val gestureName: String = "",
    val isRecording: Boolean = false,
    val status: String = "Ready",
    val savedGestures: List<CustomGestureTemplate> = emptyList()
)