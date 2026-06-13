package com.airmouse.presentation.ui.voice

data class VoiceCommandsUiState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val lastCommand: String? = null,
    val lastConfidence: Float = 0f,
    val status: String = "Ready",
    val statusColor: Long = 0xFF4CAF50,
    val availableCommands: List<VoiceCommand> = emptyList(),
    val commandHistory: List<VoiceCommandHistory> = emptyList(),
    val microphonePermissionGranted: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val wakeWord: String = "Hey Air Mouse",
    val sensitivity: Float = 0.5f,
    val language: String = "en-US",
    val continuousListening: Boolean = false,
    val voiceFeedback: Boolean = true,
    val soundEffects: Boolean = true,
    val customCommands: List<CustomVoiceCommand> = emptyList()
)

data class VoiceCommand(
    val keyword: String,
    val action: String,
    val description: String,
    val icon: String = "🎤"
)

data class VoiceCommandHistory(
    val timestamp: Long,
    val command: String,
    val confidence: Float,
    val success: Boolean
)

data class CustomVoiceCommand(
    val id: String,
    var phrase: String,
    var action: String,
    var enabled: Boolean = true
)package com.airmouse.presentation.ui.voice

data class VoiceCommandsUiState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val lastCommand: String? = null,
    val lastConfidence: Float = 0f,
    val status: String = "Ready",
    val statusColor: Long = 0xFF4CAF50,
    val availableCommands: List<VoiceCommand> = emptyList(),
    val commandHistory: List<VoiceCommandHistory> = emptyList(),
    val microphonePermissionGranted: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val wakeWord: String = "Hey Air Mouse",
    val sensitivity: Float = 0.5f,
    val language: String = "en-US",
    val continuousListening: Boolean = false,
    val voiceFeedback: Boolean = true,
    val soundEffects: Boolean = true,
    val customCommands: List<CustomVoiceCommand> = emptyList()
)

data class VoiceCommand(
    val keyword: String,
    val action: String,
    val description: String,
    val icon: String = "🎤"
)

data class VoiceCommandHistory(
    val timestamp: Long,
    val command: String,
    val confidence: Float,
    val success: Boolean
)

data class CustomVoiceCommand(
    val id: String,
    var phrase: String,
    var action: String,
    var enabled: Boolean = true
)