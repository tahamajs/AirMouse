package com.airmouse.presentation.ui.voice

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.*
import javax.inject.Inject

@HiltViewModel
class VoiceCommandsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceCommandsUiState())
    val uiState: StateFlow<VoiceCommandsUiState> = _uiState.asStateFlow()

    private val availableCommands = listOf(
        VoiceCommand("click", "CLICK", "Perform left click", "👆"),
        VoiceCommand("double click", "DOUBLE_CLICK", "Perform double click", "👆👆"),
        VoiceCommand("right click", "RIGHT_CLICK", "Perform right click", "👉"),
        VoiceCommand("scroll up", "SCROLL_UP", "Scroll up", "⬆️"),
        VoiceCommand("scroll down", "SCROLL_DOWN", "Scroll down", "⬇️"),
        VoiceCommand("left click", "CLICK", "Perform left click", "👈"),
        VoiceCommand("select", "CLICK", "Select item", "✅"),
        VoiceCommand("back", "BACK", "Go back", "🔙"),
        VoiceCommand("home", "HOME", "Go to desktop", "🏠"),
        VoiceCommand("calibrate", "CALIBRATE", "Start calibration", "🔧"),
        VoiceCommand("connect", "CONNECT", "Connect to server", "🔌"),
        VoiceCommand("disconnect", "DISCONNECT", "Disconnect from server", "🔌❌"),
        VoiceCommand("stop", "STOP", "Stop Air Mouse", "⏹️"),
        VoiceCommand("start", "START", "Start Air Mouse", "▶️")
    )

    init {
        loadSettings()
        loadCustomCommands()
        checkMicrophonePermission()
        _uiState.update { it.copy(availableCommands = availableCommands) }
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                wakeWordEnabled = prefs.getBoolean("voice_wake_word_enabled", false),
                wakeWord = prefs.getString("voice_wake_word", "Hey Air Mouse"),
                sensitivity = prefs.getFloat("voice_sensitivity", 0.5f),
                language = prefs.getString("voice_language", "en-US"),
                continuousListening = prefs.getBoolean("voice_continuous", false),
                voiceFeedback = prefs.getBoolean("voice_feedback", true),
                soundEffects = prefs.getBoolean("voice_sound_effects", true)
            )
        }
    }

    private fun loadCustomCommands() {
        val savedCommands = prefs.getString("voice_custom_commands", "")
        if (savedCommands.isNotEmpty()) {
            try {
                val commands = mutableListOf<CustomVoiceCommand>()
                savedCommands.split(";").forEach { cmdStr ->
                    val parts = cmdStr.split("|")
                    if (parts.size >= 3) {
                        commands.add(
                            CustomVoiceCommand(
                                id = parts[0],
                                phrase = parts[1],
                                action = parts[2],
                                enabled = parts.getOrElse(3) { "true" }.toBoolean()
                            )
                        )
                    }
                }
                _uiState.update { it.copy(customCommands = commands) }
            } catch (e: Exception) {
                // Ignore parsing errors
            }
        }
    }

    private fun saveCustomCommands() {
        val commandsStr = _uiState.value.customCommands.joinToString(";") { cmd ->
            "${cmd.id}|${cmd.phrase}|${cmd.action}|${cmd.enabled}"
        }
        prefs.putString("voice_custom_commands", commandsStr)
    }

    private fun checkMicrophonePermission() {
        val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
        } else true
        _uiState.update { it.copy(microphonePermissionGranted = hasPermission) }

        if (!hasPermission) {
            _uiState.update {
                it.copy(
                    status = "Microphone permission required",
                    statusColor = 0xFFF44336
                )
            }
        }
    }

    fun requestMicrophonePermission() {
        // This should be handled in the activity
        _uiState.update { it.copy(status = "Please grant microphone permission in settings") }
    }

    fun startListening() {
        if (!_uiState.value.microphonePermissionGranted) {
            _uiState.update {
                it.copy(
                    status = "Please grant microphone permission first",
                    statusColor = 0xFFF44336
                )
            }
            return
        }

        _uiState.update {
            it.copy(
                isListening = true,
                isProcessing = false,
                status = "Listening... Speak now",
                statusColor = 0xFF4CAF50
            )
        }

        if (_uiState.value.soundEffects) {
            playBeepSound()
        }

        // Simulate speech recognition (in real app, integrate with SpeechRecognizer)
        viewModelScope.launch {
            delay(5000) // Simulate listening for 5 seconds
            if (_uiState.value.isListening) {
                // Simulate a command for demo
                processVoiceInput("click", 0.9f)
                if (!_uiState.value.continuousListening) {
                    stopListening()
                }
            }
        }
    }

    fun stopListening() {
        _uiState.update {
            it.copy(
                isListening = false,
                isProcessing = false,
                status = "Stopped",
                statusColor = 0xFF9E9E9E
            )
        }

        if (_uiState.value.soundEffects) {
            playStopSound()
        }
    }

    fun processVoiceInput(text: String, confidence: Float = 0.9f) {
        if (!_uiState.value.isListening) return

        _uiState.update { it.copy(isProcessing = true) }

        val lowerText = text.lowercase().trim()
        var matchedCommand: VoiceCommand? = null

        // Match built-in commands
        matchedCommand = availableCommands.find {
            lowerText.contains(it.keyword) || it.keyword.contains(lowerText)
        }

        // Check custom commands
        if (matchedCommand == null) {
            val customMatch = _uiState.value.customCommands.find {
                it.enabled && lowerText.contains(it.phrase.lowercase())
            }
            if (customMatch != null) {
                executeCustomAction(customMatch.action)
                addToHistory(customMatch.phrase, confidence, true)
                _uiState.update {
                    it.copy(
                        lastCommand = customMatch.phrase,
                        lastConfidence = confidence,
                        status = "Command executed: ${customMatch.phrase}",
                        statusColor = 0xFF4CAF50,
                        isProcessing = false
                    )
                }
                if (_uiState.value.voiceFeedback) speakResponse("Command executed")
                return
            }
        }

        if (matchedCommand != null) {
            executeCommand(matchedCommand)
            addToHistory(matchedCommand.keyword, confidence, true)
            _uiState.update {
                it.copy(
                    lastCommand = matchedCommand.keyword,
                    lastConfidence = confidence,
                    status = "Recognized: ${matchedCommand.keyword}",
                    statusColor = 0xFF4CAF50,
                    isProcessing = false
                )
            }
            if (_uiState.value.voiceFeedback) speakResponse(matchedCommand.keyword)
        } else {
            _uiState.update {
                it.copy(
                    status = "Command not recognized: $text",
                    statusColor = 0xFFFF9800,
                    isProcessing = false
                )
            }
            addToHistory(text, confidence, false)
            if (_uiState.value.voiceFeedback) speakResponse("Command not recognized")
        }

        // Auto-stop if not in continuous mode
        if (!_uiState.value.continuousListening) {
            stopListening()
        }
    }

    private fun executeCommand(command: VoiceCommand) {
        when (command.action) {
            "CLICK" -> connectionManager.sendClick("left")
            "DOUBLE_CLICK" -> connectionManager.sendDoubleClick()
            "RIGHT_CLICK" -> connectionManager.sendClick("right")
            "SCROLL_UP" -> connectionManager.sendScroll(3)
            "SCROLL_DOWN" -> connectionManager.sendScroll(-3)
            "CALIBRATE" -> connectionManager.sendControl("calibrate")
            "CONNECT" -> connectionManager.connect("", 0) // This would need proper parameters
            "DISCONNECT" -> connectionManager.disconnect()
            "STOP" -> connectionManager.sendControl("stop")
            "START" -> connectionManager.sendControl("start")
            "BACK" -> connectionManager.sendControl("back")
            "HOME" -> connectionManager.sendControl("home")
        }
        vibrate(30)
    }

    private fun executeCustomAction(action: String) {
        when {
            action.startsWith("move:") -> {
                val parts = action.split(":")
                if (parts.size >= 3) {
                    connectionManager.sendMove(parts[1].toIntOrNull() ?: 0, parts[2].toIntOrNull() ?: 0)
                }
            }
            action.startsWith("scroll:") -> {
                val delta = action.substringAfter(":").toIntOrNull() ?: 1
                connectionManager.sendScroll(delta)
            }
            action.startsWith("click:") -> {
                val button = action.substringAfter(":")
                connectionManager.sendClick(button)
            }
            else -> {
                connectionManager.sendControl(action)
            }
        }
        vibrate(30)
    }

    private fun addToHistory(command: String, confidence: Float, success: Boolean) {
        val history = listOf(
            VoiceCommandHistory(
                timestamp = System.currentTimeMillis(),
                command = command,
                confidence = confidence,
                success = success
            )
        ) + _uiState.value.commandHistory

        _uiState.update { it.copy(commandHistory = history.take(50)) }
        saveHistory()
    }

    private fun saveHistory() {
        val historyStr = _uiState.value.commandHistory.joinToString("|") { history ->
            "${history.timestamp},${history.command},${history.confidence},${history.success}"
        }
        prefs.putString("voice_command_history", historyStr)
    }

    fun clearHistory() {
        _uiState.update { it.copy(commandHistory = emptyList()) }
        prefs.putString("voice_command_history", "")
        vibrate(20)
    }

    fun addCustomCommand(phrase: String, action: String) {
        val newCommand = CustomVoiceCommand(
            id = UUID.randomUUID().toString(),
            phrase = phrase,
            action = action,
            enabled = true
        )
        _uiState.update {
            it.copy(customCommands = it.customCommands + newCommand)
        }
        saveCustomCommands()
        vibrate(30)
    }

    fun removeCustomCommand(commandId: String) {
        _uiState.update {
            it.copy(customCommands = it.customCommands.filter { it.id != commandId })
        }
        saveCustomCommands()
        vibrate(20)
    }

    fun updateCustomCommand(commandId: String, enabled: Boolean) {
        _uiState.update { state ->
            val updatedCommands = state.customCommands.map {
                if (it.id == commandId) it.copy(enabled = enabled)
                else it
            }
            state.copy(customCommands = updatedCommands)
        }
        saveCustomCommands()
    }

    fun updateSensitivity(value: Float) {
        _uiState.update { it.copy(sensitivity = value) }
        prefs.putFloat("voice_sensitivity", value)
    }

    fun updateLanguage(language: String) {
        _uiState.update { it.copy(language = language) }
        prefs.putString("voice_language", language)
    }

    fun updateWakeWordEnabled(enabled: Boolean) {
        _uiState.update { it.copy(wakeWordEnabled = enabled) }
        prefs.putBoolean("voice_wake_word_enabled", enabled)
    }

    fun updateWakeWord(wakeWord: String) {
        _uiState.update { it.copy(wakeWord = wakeWord) }
        prefs.putString("voice_wake_word", wakeWord)
    }

    fun updateContinuousListening(enabled: Boolean) {
        _uiState.update { it.copy(continuousListening = enabled) }
        prefs.putBoolean("voice_continuous", enabled)
    }

    fun updateVoiceFeedback(enabled: Boolean) {
        _uiState.update { it.copy(voiceFeedback = enabled) }
        prefs.putBoolean("voice_feedback", enabled)
    }

    fun updateSoundEffects(enabled: Boolean) {
        _uiState.update { it.copy(soundEffects = enabled) }
        prefs.putBoolean("voice_sound_effects", enabled)
    }

    private fun playBeepSound() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val volume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
            if (volume > 0) {
                val toneGenerator = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 100)
                toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 200)
                toneGenerator.release()
            }
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun playStopSound() {
        try {
            val toneGenerator = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
            toneGenerator.release()
        } catch (e: Exception) {
            // Ignore
        }
    }

    private fun speakResponse(text: String) {
        if (!_uiState.value.voiceFeedback) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                val tts = android.speech.tts.TextToSpeech(context) { status ->
                    if (status == android.speech.tts.TextToSpeech.SUCCESS) {
                        tts.language = Locale.US
                        tts.speak(text, android.speech.tts.TextToSpeech.QUEUE_FLUSH, null, null)
                    }
                }
            }
        } catch (e: Exception) {
            // TTS not available
        }
    }

    private fun vibrate(duration: Long) {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    override fun onCleared() {
        super.onCleared()
        if (_uiState.value.isListening) {
            stopListening()
        }
    }
}