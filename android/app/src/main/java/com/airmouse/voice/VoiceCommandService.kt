package com.airmouse.voice

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.airmouse.R

/**
 * Foreground service shell for the optional voice screen.
 *
 * The offline speech stack is intentionally not bundled in this build, so the
 * service behaves as a lightweight placeholder that can be enabled later without
 * touching the manifest or UI wiring.
 */
class VoiceCommandService : Service() {

    private var isListening = false
    private val binder = LocalBinder()

    companion object {
        private const val NOTIFICATION_ID = 5001
        private const val CHANNEL_ID = "voice_command_channel"
    }

    inner class LocalBinder : Binder() {
        fun getService(): VoiceCommandService = this@VoiceCommandService
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, createNotification())
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            "START_LISTENING" -> startListening()
            "STOP_LISTENING" -> stopListening()
        }
        return START_STICKY
    }

    fun startListening() {
        isListening = true
        updateNotification("Voice commands", "Listening is stubbed in this build")
    }

    fun stopListening() {
        isListening = false
        updateNotification("Voice commands", "Stopped")
    }

    private fun updateNotification(title: String, content: String) {
        getSystemService(NotificationManager::class.java).notify(
            NOTIFICATION_ID,
            createNotification(title, content)
        )
    }

    private fun createNotification(
        title: String = "Voice Commands",
        content: String = "Ready"
    ): Notification {
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
        stopForeground(STOP_FOREGROUND_REMOVE)
    }
}
