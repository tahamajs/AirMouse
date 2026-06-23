package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class BluetoothHidService : Service() {
    private var connected = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        connected = intent?.getBooleanExtra(EXTRA_CONNECTED, false) ?: false
        return START_STICKY
    }

    fun isConnected(): Boolean = connected

    fun connect() {
        connected = true
    }

    fun disconnect() {
        connected = false
    }

    companion object {
        private const val EXTRA_CONNECTED = "connected"
    }
}
