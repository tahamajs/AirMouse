package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class GestureRecorderService : Service() {
    private var recording = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        recording = intent?.getBooleanExtra(EXTRA_RECORDING, true) ?: true
        return START_STICKY
    }

    fun isRecording(): Boolean = recording

    fun startRecording() {
        recording = true
    }

    fun stopRecording() {
        recording = false
    }

    companion object {
        private const val EXTRA_RECORDING = "recording"
    }
}
