package com.airmouse.service

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioManager
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import androidx.core.content.ContextCompat
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class VoiceCommandService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: ConnectionManager,
    private val prefs: PreferencesManager
) {

    private var speechRecognizer: SpeechRecognizer? = null
    private var isListening = false
    private var isWakeWordActive = false
    private var wakeWordDetected = false
    private var currentVolume = 0
    private var mediaPlayer: MediaPlayer? = null
    private val mainHandler = Handler(Looper.getMainLooper())
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    // Audio level monitoring
    private var audioLevelJob: Job? = null
    private var currentAudioLevel = 0f

    // Wake word
    private var wakeWord = "hey air mouse"
    private var wakeWordConfidence = 0.7f

    // Command history
    private val commandHistory = mutableListOf<VoiceCommandRecord>()
    private val maxHistorySize = 50

    companion object {
        private const val TAG = "VoiceCommandService"
        private const val SILENCE_TIMEOUT_MS = 2000L
        private const val WAKE_WORD_TIMEOUT_MS = 10000L

        // Available commands with aliases
        private val COMMANDS = mapOf(
            "click" to listOf("click", "tap", "press", "select"),
            "double click" to listOf("double click", "double tap", "two clicks"),
            "right click" to listOf("right click", "context menu", "secondary click"),
            "scroll up" to listOf("scroll up", "up", "move up"),
            "scroll down" to listOf("scroll down", "down", "move down"),
            "stop listening" to listOf("stop listening", "stop", "disable voice"),
            "start listening" to listOf("start listening", "resume", "enable voice"),
            "calibrate" to listOf("calibrate", "calibration", "recalibrate"),
            "connect" to listOf("connect", "pair", "link"),
            "disconnect" to listOf("disconnect", "unpair", "unlink"),
            "home" to listOf("home", "go home", "desktop"),
            "back" to listOf("back", "go back", "previous"),
            "volume up" to listOf("volume up", "louder", "increase volume"),
            "volume down" to listOf("volume down", "quieter", "decrease volume"),
            "mute" to listOf("mute", "silence", "quiet"),
            "maximize" to listOf("maximize", "full screen", "enlarge"),
            "minimize" to listOf("minimize", "hide", "shrink"),
            "close" to listOf("close", "exit", "quit"),
            "open settings" to listOf("open settings", "settings", "preferences"),
            "show stats" to listOf("show stats", "statistics", "stats"),
            "help" to listOf("help", "what can I say", "commands")
        )

        val supportedCommands: List<String> = COMMANDS.keys.toList()
    }

    init {
        initializeSpeechRecognizer()
        loadSettings()
        setupAudioManager()
    }

    private fun loadSettings() {
        wakeWord = prefs.getString("voice_wake_word", "hey air mouse").lowercase()
        wakeWordConfidence = prefs.getFloat("voice_wake_word_confidence", 0.7f)
    }

    private fun setupAudioManager() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        currentVolume = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
    }

    private fun initializeSpeechRecognizer() {
        if (!hasMicrophonePermission()) {
            Log.w(TAG, "Microphone permission not granted")
            return
        }

        try {
            speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)
            speechRecognizer?.setRecognitionListener(recognitionListener)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize speech recognizer", e)
        }
    }

    private val recognitionListener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {
            Log.d(TAG, "Ready for speech")
            startAudioLevelMonitoring()
            onStatusChanged("Listening...", true)
        }

        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech began")
            onStatusChanged("Processing...", true)
        }

        override fun onRmsChanged(rmsdB: Float) {
            currentAudioLevel = rmsdB
            onAudioLevelChanged(rmsdB)
        }

        override fun onBufferReceived(buffer: ByteArray?) = Unit

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
            stopAudioLevelMonitoring()
        }

        override fun onError(error: Int) {
            val errorMessage = getErrorMessage(error)
            Log.e(TAG, "Speech recognition error: $errorMessage")
            onStatusChanged("Error: $errorMessage", false)

            // Retry after error
            if (isListening && !wakeWordDetected) {
                mainHandler.postDelayed({ startListening() }, 1000)
            }
        }

        override fun onResults(results: Bundle?) {
            val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            if (!matches.isNullOrEmpty()) {
                val topResult = matches[0]
                val confidence = getConfidence(results)
                processVoiceInput(topResult, confidence)
            }

            // Continue listening if in continuous mode
            if (isListening && !wakeWordDetected) {
                startListening()
            } else if (wakeWordDetected) {
                // Stop after wake word detection timeout
                mainHandler.postDelayed({
                    if (wakeWordDetected) {
                        wakeWordDetected = false
                        onStatusChanged("Wake word timeout", false)
                    }
                }, WAKE_WORD_TIMEOUT_MS)
            }
        }

        override fun onPartialResults(partialResults: Bundle?) {
            val matches = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
            matches?.firstOrNull()?.let { partial ->
                onPartialResult(partial)
            }
        }

        override fun onEvent(eventType: Int, params: Bundle?) = Unit
    }

    private fun getConfidence(results: Bundle?): Float {
        // Get confidence scores if available
        val confidence = results?.getFloatArray(SpeechRecognizer.CONFIDENCE_SCORES)
        return confidence?.firstOrNull() ?: 0.8f
    }

    private fun getErrorMessage(errorCode: Int): String {
        return when (errorCode) {
            SpeechRecognizer.ERROR_AUDIO -> "Audio recording error"
            SpeechRecognizer.ERROR_CLIENT -> "Client error"
            SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Insufficient permissions"
            SpeechRecognizer.ERROR_NETWORK -> "Network error"
            SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Network timeout"
            SpeechRecognizer.ERROR_NO_MATCH -> "No match found"
            SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "Recognizer busy"
            SpeechRecognizer.ERROR_SERVER -> "Server error"
            SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "Speech timeout"
            else -> "Unknown error"
        }
    }

    private fun processVoiceInput(text: String, confidence: Float) {
        val lowerText = text.lowercase().trim()
        Log.i(TAG, "Recognized: '$text' (confidence: $confidence)")

        // Check for wake word first
        if (!wakeWordDetected && isWakeWordActive) {
            if (lowerText.contains(wakeWord) || lowerText.contains(wakeWord.replace("hey ", ""))) {
                if (confidence >= wakeWordConfidence) {
                    wakeWordDetected = true
                    onStatusChanged("Wake word detected! Say a command...", true)
                    playBeepSound()
                    vibrate(100)
                    return
                }
            }
            return
        }

        // Process actual command
        var commandMatched = false
        for ((command, aliases) in COMMANDS) {
            if (aliases.any { lowerText.contains(it) } || lowerText == command) {
                executeCommand(command)
                commandMatched = true

                // Add to history
                addToHistory(command, text, confidence, true)

                onStatusChanged("Executed: $command", true)
                playConfirmationSound()
                vibrate(50)
                break
            }
        }

        if (!commandMatched) {
            onStatusChanged("Command not recognized: $text", false)
            addToHistory("unknown", text, confidence, false)
        }

        // Reset wake word detection after command
        wakeWordDetected = false
    }

    private fun executeCommand(command: String) {
        when (command) {
            "click" -> connectionManager.sendClick()
            "double click" -> connectionManager.sendDoubleClick()
            "right click" -> connectionManager.sendRightClick()
            "scroll up" -> connectionManager.sendScroll(3)
            "scroll down" -> connectionManager.sendScroll(-3)
            "stop listening" -> stopListening()
            "start listening" -> startListening()
            "calibrate" -> connectionManager.sendCalibrate()
            "connect" -> connectionManager.reconnect()
            "disconnect" -> connectionManager.disconnect()
            "home" -> connectionManager.sendKeyPress(android.view.KeyEvent.KEYCODE_HOME)
            "back" -> connectionManager.sendKeyPress(android.view.KeyEvent.KEYCODE_BACK)
            "volume up" -> adjustSystemVolume(1)
            "volume down" -> adjustSystemVolume(-1)
            "mute" -> muteSystemVolume()
            "maximize" -> connectionManager.sendWindowCommand("maximize")
            "minimize" -> connectionManager.sendWindowCommand("minimize")
            "close" -> connectionManager.sendWindowCommand("close")
            "open settings" -> openSettings()
            "show stats" -> onShowStatsRequested()
            "help" -> onHelpRequested()
        }
    }

    private fun adjustSystemVolume(delta: Int) {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val newVolume = (currentVolume + delta).coerceIn(0, audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC))
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, newVolume, 0)
        currentVolume = newVolume
        onStatusChanged("Volume: $currentVolume", true)
    }

    private fun muteSystemVolume() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        audioManager.setStreamVolume(AudioManager.STREAM_MUSIC, 0, 0)
        currentVolume = 0
        onStatusChanged("Muted", true)
    }

    private fun openSettings() {
        val intent = android.content.Intent(android.provider.Settings.ACTION_SETTINGS)
        intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
        context.startActivity(intent)
    }

    private fun playBeepSound() {
        try {
            val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
            val toneGenerator = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 100)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP2, 200)
            toneGenerator.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play beep", e)
        }
    }

    private fun playConfirmationSound() {
        try {
            val toneGenerator = android.media.ToneGenerator(AudioManager.STREAM_MUSIC, 80)
            toneGenerator.startTone(android.media.ToneGenerator.TONE_PROP_BEEP, 150)
            toneGenerator.release()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to play confirmation", e)
        }
    }

    private fun vibrate(duration: Long) {
        if (!prefs.getBoolean("voice_haptic_feedback", true)) return

        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(duration, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(duration)
        }
    }

    private fun addToHistory(command: String, spoken: String, confidence: Float, success: Boolean) {
        val record = VoiceCommandRecord(
            timestamp = System.currentTimeMillis(),
            command = command,
            spoken = spoken,
            confidence = confidence,
            success = success
        )
        commandHistory.add(0, record)
        while (commandHistory.size > maxHistorySize) {
            commandHistory.removeAt(commandHistory.lastIndex)
        }
        saveHistory()
    }

    private fun saveHistory() {
        // Save to preferences or database
        val historyJson = commandHistory.joinToString("|") { record ->
            "${record.timestamp},${record.command},${record.spoken},${record.confidence},${record.success}"
        }
        prefs.putString("voice_command_history", historyJson)
    }

    fun getCommandHistory(): List<VoiceCommandRecord> = commandHistory.toList()

    fun clearHistory() {
        commandHistory.clear()
        prefs.putString("voice_command_history", "")
    }

    fun startListening() {
        if (isListening) return

        if (!hasMicrophonePermission()) {
            onStatusChanged("Microphone permission required", false)
            return
        }

        if (speechRecognizer == null) {
            initializeSpeechRecognizer()
        }

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
            putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 5)
            putExtra(RecognizerIntent.EXTRA_SPEECH_INPUT_COMPLETE_SILENCE_LENGTH_MILLIS, SILENCE_TIMEOUT_MS)
        }

        speechRecognizer?.startListening(intent)
        isListening = true
        isWakeWordActive = prefs.getBoolean("voice_wake_word_enabled", true)
        wakeWordDetected = !isWakeWordActive

        onStatusChanged("Listening...", true)
        Log.i(TAG, "Voice listening started")
    }

    fun stopListening() {
        if (!isListening) return

        speechRecognizer?.stopListening()
        isListening = false
        wakeWordDetected = false
        stopAudioLevelMonitoring()

        onStatusChanged("Stopped", false)
        Log.i(TAG, "Voice listening stopped")
    }

    private fun startAudioLevelMonitoring() {
        audioLevelJob = serviceScope.launch {
            while (isListening) {
                delay(100)
                // Audio level is updated via onRmsChanged callback
            }
        }
    }

    private fun stopAudioLevelMonitoring() {
        audioLevelJob?.cancel()
        audioLevelJob = null
        currentAudioLevel = 0f
    }

    private fun hasMicrophonePermission(): Boolean {
        return ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    }

    fun isListening(): Boolean = isListening

    fun getCurrentAudioLevel(): Float = currentAudioLevel

    fun setWakeWord(word: String) {
        wakeWord = word.lowercase()
        prefs.putString("voice_wake_word", wakeWord)
    }

    fun setWakeWordConfidence(confidence: Float) {
        wakeWordConfidence = confidence.coerceIn(0.5f, 0.95f)
        prefs.putFloat("voice_wake_word_confidence", wakeWordConfidence)
    }

    fun enableWakeWord(enabled: Boolean) {
        prefs.putBoolean("voice_wake_word_enabled", enabled)
        isWakeWordActive = enabled
        if (!enabled) wakeWordDetected = true
    }

    fun supportedCommands(): List<String> = supportedCommands

    // Callbacks for UI
    var onStatusChanged: (String, Boolean) -> Unit = { _, _ -> }
    var onAudioLevelChanged: (Float) -> Unit = { }
    var onPartialResult: (String) -> Unit = { }
    var onShowStatsRequested: () -> Unit = { }
    var onHelpRequested: () -> Unit = { }

    fun destroy() {
        stopListening()
        speechRecognizer?.destroy()
        speechRecognizer = null
        serviceScope.cancel()
    }
}

data class VoiceCommandRecord(
    val timestamp: Long,
    val command: String,
    val spoken: String,
    val confidence: Float,
    val success: Boolean
)