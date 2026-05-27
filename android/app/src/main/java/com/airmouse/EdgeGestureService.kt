package com.airmouse

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import com.airmouse.utils.PreferencesManager

class EdgeGestureService : Service() {

    private lateinit var preferences: PreferencesManager
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isListening) {
            createNotificationChannel()
            startForegroundService()
            isListening = true
        }
        return START_STICKY
    }

    private fun startForegroundService() {
        val notification = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Notification.Builder(this, CHANNEL_ID)
                .setContentTitle("Edge Gestures Active")
                .setContentText("Long press volume keys to control Air Mouse")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        } else {
            @Suppress("DEPRECATION")
            Notification.Builder(this)
                .setContentTitle("Edge Gestures Active")
                .setContentText("Long press volume keys to control Air Mouse")
                .setSmallIcon(android.R.drawable.ic_menu_camera)
                .build()
        }

        // Note: For Android 14+, specific foregroundServiceType might be needed in manifest and here.
        startForeground(NOTIFICATION_ID, notification)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                "Edge Gesture Service Channel",
                NotificationManager.IMPORTANCE_LOW
            )
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(serviceChannel)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val CHANNEL_ID = "edge_gesture_channel"
        private const val NOTIFICATION_ID = 1
    }
}
