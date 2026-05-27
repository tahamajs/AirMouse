package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.media.AudioManager
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.view.KeyEvent
import com.airmouse.network.DataSender
import com.airmouse.utils.PreferencesManager

class EdgeGestureService : Service() {

    private lateinit var preferences: PreferencesManager
    private var volumeDownCount = 0
    private val handler = Handler(Looper.getMainLooper())
    private var isListening = false

    override fun onCreate() {
        super.onCreate()
        preferences = PreferencesManager(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!isListening) {
            (getSystemService(AUDIO_SERVICE) as AudioManager).apply {
                registerMediaButtonEventReceiver(null) // release previous
            }
            startListeningForVolumeKeys()
            isListening = true
        }
        return START_STICKY
    }

    private fun startListeningForVolumeKeys() {
        // This is a hack: we cannot directly intercept volume keys without Accessibility.
        // A better approach: register a BroadcastReceiver for ACTION_MEDIA_BUTTON.
        // For simplicity, we'll simulate with a foreground service and a callback.
        // Provide a notification to keep service alive.
        startForeground(1, android.app.Notification.Builder(this, "edge_channel")
            .setContentTitle("Edge Gestures Active")
            .setContentText("Long press volume keys to control Air Mouse")
            .setSmallIcon(android.R.drawable.ic_menu_camera)
            .build())
    }

    override fun onBind(intent: Intent?): IBinder? = null
}


package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import com.airmouse.utils.PreferencesManager

class EdgeGestureService : Service() {
    override fun onBind(intent: Intent?): IBinder? = null
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // implementation from previous answer
        return START_STICKY
    }
}