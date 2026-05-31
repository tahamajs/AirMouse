// VoiceCommandService.kt
package com.airmouse.voice

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.airmouse.R
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.Hypothesis
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import java.io.File
import java.io.IOException

class VoiceCommandService : Service() {

    private lateinit var recognizer: SpeechRecognizer
    private var isListening = false
    private var webSocketManager: WebSocketManager? = null

    companion object {
        private const val NOTIFICATION_ID = 5001
        private const val CHANNEL_ID = "voice_command_channel"
        private const val TAG = "VoiceCommandService"
        private const val KWS_SEARCH = "commands"
        
        // Define the grammar (commands)
        private val COMMANDS = arrayOf(
            "click",
            "double click",
            "right click",
            "scroll up",
            "scroll down",
            "stop listening"
        )
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupRecognizer()
        webSocketManager = WebSocketManager // assume you have a WebSocketManager singleton
    }

    private fun setupRecognizer() {
        try {
            // Copy assets to a writable location
            val assets = Assets(this)
            val assetDir = assets.syncAssets()
            Log.d(TAG, "Assets copied to $assetDir")

            // Create the recognizer
            recognizer = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetDir, "en-us-ptm"))
                .setDictionary(File(assetDir, "cmudict-en-us.dict"))
                .setKeywordThreshold(1e-20f) // low threshold to accept many hypotheses
                .getRecognizer()
            recognizer.addListener(recognizerListener)

            // Build grammar-based search
            val grammar = buildGrammar()
            recognizer.addGrammarSearch(KWS_SEARCH, grammar)

            Log.i(TAG, "Recognizer initialised")
        } catch (e: IOException) {
            Log.e(TAG, "Failed to setup recognizer", e)
            stopSelf()
        }
    }

    private fun buildGrammar(): File {
        // Create a JSGF grammar file dynamically
        val grammarFile = File(cacheDir, "commands.gram")
        grammarFile.writeText(
            """
            #JSGF V1.0;
            grammar commands;
            public <command> = ${COMMANDS.joinToString(" | ") { it.replace(" ", " ") }};
            """.trimIndent()
        )
        return grammarFile
    }

    private val recognizerListener = object : SpeechRecognizer.RecognitionListener {
        override fun onBeginningOfSpeech() {
            Log.d(TAG, "Speech started")
        }

        override fun onEndOfSpeech() {
            Log.d(TAG, "Speech ended")
        }

        override fun onResult(hypothesis: Hypothesis?) {
            val text = hypothesis?.hypstr ?: return
            Log.i(TAG, "Recognised: $text")
            processCommand(text)
        }

        override fun onPartialResult(hypothesis: Hypothesis?) {
            // Optional: handle partial results for real‑time feedback
        }

        override fun onError(error: Exception?) {
            Log.e(TAG, "Recognition error", error)
        }

        override fun onTimeout() {
            Log.d(TAG, "Recognition timeout")
        }
    }

    private fun processCommand(command: String) {
        when {
            command.equals("click", ignoreCase = true) -> {
                webSocketManager?.sendCommand("click")
                speakFeedback("Click")
            }
            command.equals("double click", ignoreCase = true) -> {
                webSocketManager?.sendCommand("doubleclick")
                speakFeedback("Double click")
            }
            command.equals("right click", ignoreCase = true) -> {
                webSocketManager?.sendCommand("rightclick")
                speakFeedback("Right click")
            }
            command.equals("scroll up", ignoreCase = true) -> {
                webSocketManager?.sendCommand("scroll", 1)
                speakFeedback("Scroll up")
            }
            command.equals("scroll down", ignoreCase = true) -> {
                webSocketManager?.sendCommand("scroll", -1)
                speakFeedback("Scroll down")
            }
            command.equals("stop listening", ignoreCase = true) -> {
                stopListening()
                speakFeedback("Stopped listening")
            }
        }
    }

    private fun speakFeedback(message: String) {
        // Optional: Text‑to‑speech feedback (Android TTS)
        // For simplicity, we'll just log
        Log.d(TAG, "Feedback: $message")
    }

    fun startListening() {
        if (!isListening) {
            recognizer.startListening(KWS_SEARCH)
            isListening = true
            updateNotification("Listening for commands", "Say: click, double click, right click, scroll up/down")
        }
    }

    fun stopListening() {
        if (isListening) {
            recognizer.stop()
            isListening = false
            updateNotification("Voice commands stopped", "Tap to start listening")
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LISTENING" -> startListening()
            "STOP_LISTENING" -> stopListening()
        }
        return START_STICKY
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        val manager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(title: String = "Voice Commands", content: String = "Ready"): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Command Service",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.cancel()
        recognizer.shutdown()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}