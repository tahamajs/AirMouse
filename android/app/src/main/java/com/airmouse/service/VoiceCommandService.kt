// app/src/main/java/com/airmouse/service/VoiceCommandService.kt
package com.airmouse.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.airmouse.R
import com.airmouse.network.ConnectionManager
import dagger.hilt.android.AndroidEntryPoint
import edu.cmu.pocketsphinx.Assets
import edu.cmu.pocketsphinx.SpeechRecognizer
import edu.cmu.pocketsphinx.SpeechRecognizerSetup
import kotlinx.coroutines.*
import java.io.File
import java.io.IOException
import javax.inject.Inject

@AndroidEntryPoint
class VoiceCommandService : Service() {

    private lateinit var recognizer: SpeechRecognizer
    private var isListening = false
    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val COMMAND_GRAMMAR = "commands"

    companion object {
        private const val NOTIFICATION_ID = 1004
        private const val CHANNEL_ID = "voice_command_channel"
        private val COMMANDS = arrayOf(
            "click", "double click", "right click",
            "scroll up", "scroll down", "stop listening"
        )

        fun start(context: Context) {
            val intent = Intent(context, VoiceCommandService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            context.stopService(Intent(context, VoiceCommandService::class.java))
        }
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
        setupRecognizer()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Voice Commands",
                NotificationManager.IMPORTANCE_LOW
            )
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    private fun createNotification(): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Voice Commands")
            .setContentText("Listening for commands...")
            .setSmallIcon(R.drawable.ic_mic)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .build()
    }

    private fun setupRecognizer() {
        try {
            val assets = Assets(this)
            val assetDir = assets.syncAssets()
            val config = SpeechRecognizerSetup.defaultSetup()
                .setAcousticModel(File(assetDir, "en-us-ptm"))
                .setDictionary(File(assetDir, "cmudict-en-us.dict"))
                .setKeywordThreshold(1e-20f)
                .getRecognizer()
            recognizer = config
            recognizer.addListener(recognizerListener)

            val grammar = buildGrammar()
            recognizer.addGrammarSearch(COMMAND_GRAMMAR, grammar)
        } catch (e: IOException) {
            e.printStackTrace()
            stopSelf()
        }
    }

    private fun buildGrammar(): File {
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
        override fun onBeginningOfSpeech() = Unit
        override fun onEndOfSpeech() = Unit
        override fun onResult(hypothesis: edu.cmu.pocketsphinx.Hypothesis?) {
            val text = hypothesis?.hypstr ?: return
            processCommand(text)
        }
        override fun onPartialResult(hypothesis: edu.cmu.pocketsphinx.Hypothesis?) = Unit
        override fun onError(error: Exception?) = Unit
        override fun onTimeout() = Unit
    }

    private fun processCommand(command: String) {
        when {
            command.equals("click", ignoreCase = true) -> ConnectionManager.sendClick()
            command.equals("double click", ignoreCase = true) -> ConnectionManager.sendDoubleClick()
            command.equals("right click", ignoreCase = true) -> ConnectionManager.sendRightClick()
            command.equals("scroll up", ignoreCase = true) -> ConnectionManager.sendScroll(1)
            command.equals("scroll down", ignoreCase = true) -> ConnectionManager.sendScroll(-1)
            command.equals("stop listening", ignoreCase = true) -> stopListening()
        }
    }

    fun startListening() {
        if (!isListening) {
            recognizer.startListening(COMMAND_GRAMMAR)
            isListening = true
            updateNotification("Listening...", "Say a command")
        }
    }

    fun stopListening() {
        if (isListening) {
            recognizer.stop()
            isListening = false
            updateNotification("Stopped", "Voice commands off")
        }
    }

    private fun updateNotification(title: String, content: String) {
        val notification = createNotification(title, content)
        getSystemService(NotificationManager::class.java).notify(NOTIFICATION_ID, notification)
    }

    private fun createNotification(title: String, content: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_mic)
            .build()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LISTENING" -> startListening()
            "STOP_LISTENING" -> stopListening()
        }
        return START_STICKY
    }

    override fun onDestroy() {
        super.onDestroy()
        recognizer.cancel()
        recognizer.shutdown()
        serviceScope.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}