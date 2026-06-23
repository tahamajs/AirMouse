package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class EdgeGestureService : Service() {
    private var enabled = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        enabled = intent?.getBooleanExtra(EXTRA_ENABLED, true) ?: true
        return START_STICKY
    }

    fun isEnabled(): Boolean = enabled

    fun setEnabled(value: Boolean) {
        enabled = value
    }

    companion object {
        private const val EXTRA_ENABLED = "enabled"
    }
}
