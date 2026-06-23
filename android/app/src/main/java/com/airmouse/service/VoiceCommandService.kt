package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class VoiceCommandService : Service() {
    private var lastCommand: String? = null
    private var listening = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        listening = intent?.getBooleanExtra(EXTRA_LISTENING, true) ?: true
        lastCommand = intent?.getStringExtra(EXTRA_COMMAND)
        return START_STICKY
    }

    fun isListening(): Boolean = listening
    fun getLastCommand(): String? = lastCommand

    fun handleCommand(command: String) {
        lastCommand = command
    }

    fun startListening() {
        listening = true
    }

    fun stopListening() {
        listening = false
    }

    companion object {
        private const val EXTRA_LISTENING = "listening"
        private const val EXTRA_COMMAND = "command"
    }
}
