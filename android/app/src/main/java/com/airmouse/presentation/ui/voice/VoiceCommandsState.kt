package com.airmouse.presentation.ui.voice

data class VoiceCommandsUiState(
    val isListening: Boolean = false,
    val lastCommand: String? = null,
    val status: String = "Ready"
)