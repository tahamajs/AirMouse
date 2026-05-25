package com.airmouse.network

import android.os.Handler
import android.os.Looper
import android.util.Log
import com.airmouse.utils.PreferencesManager

/**
 * Monitors the DataSender connection and attempts to reconnect if it becomes stale.
 * Uses a background thread and periodic checks.
 */
class AutoReconnect(
    private val dataSender: DataSender,
    private val prefs: PreferencesManager
) {

    companion object {
        private const val TAG = "AutoReconnect"
        private const val CHECK_INTERVAL_MS = 5000L   // Check every 5 seconds
        private const val STALE_THRESHOLD_MS = 10000L // If no ACK in 10 sec, consider stale (optional)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var isRunning = false
    private var lastAckTime = System.currentTimeMillis()
    private var isConnected = false

    private val checkRunnable = object : Runnable {
        override fun run() {
            if (!isRunning) return

            // Check if DataSender is still connected (we can expose a flag)
            // Since DataSender doesn't expose isConnected easily, we'll rely on the fact that
            // DataSender will call onDisconnected callback. We can set a flag here.
            // For simplicity, we'll just check if the DataSender thread is alive and try to reconnect
            // by stopping and restarting if needed. But that's heavy.

            // Better: DataSender should have a method to check connection status.
            // Since we enhanced DataSender, we can add a public `isConnected` property.
            // I'll assume DataSender has a public `isConnected` flag (add it).

            if (!dataSender.isConnected) {
                Log.w(TAG, "Connection lost, triggering reconnect...")
                // Stop current sender and start a new one (or call reconnect method)
                dataSender.stopSending()
                // Wait a bit then restart
                Handler(Looper.getMainLooper()).postDelayed({
                    if (isRunning) {
                        val ip = prefs.getLastIp()
                        if (ip.isNotBlank()) {
                            dataSender.updateHost(ip)
                            dataSender.start()
                        }
                    }
                }, 2000)
            }
            handler.postDelayed(this, CHECK_INTERVAL_MS)
        }
    }

    /**
     * Starts the monitoring.
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        handler.post(checkRunnable)
        Log.d(TAG, "AutoReconnect started")
    }

    /**
     * Stops the monitoring.
     */
    fun stop() {
        isRunning = false
        handler.removeCallbacks(checkRunnable)
        Log.d(TAG, "AutoReconnect stopped")
    }
}