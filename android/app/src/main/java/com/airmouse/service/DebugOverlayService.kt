package com.airmouse.service

import android.app.Service
import android.content.Intent
import android.os.IBinder

class DebugOverlayService : Service() {
    private var overlayVisible = false

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        overlayVisible = intent?.getBooleanExtra(EXTRA_VISIBLE, true) ?: true
        return START_STICKY
    }

    fun isOverlayVisible(): Boolean = overlayVisible

    fun showOverlay() {
        overlayVisible = true
    }

    fun hideOverlay() {
        overlayVisible = false
    }

    companion object {
        private const val EXTRA_VISIBLE = "visible"

        fun start(context: android.content.Context, visible: Boolean = true) {
            val intent = Intent(context, DebugOverlayService::class.java)
                .putExtra(EXTRA_VISIBLE, visible)
            context.startService(intent)
        }

        fun stop(context: android.content.Context) {
            context.stopService(Intent(context, DebugOverlayService::class.java))
        }
    }
}
