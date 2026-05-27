package com.airmouse.network

import android.util.Log
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*

/**
 * Monitors the DataSender connection and attempts to reconnect if it becomes stale.
 * Uses coroutines for efficient, non‑blocking checks.
 */
class AutoReconnect(
    private var dataSender: DataSender,
    private val prefs: PreferencesManager,
    private val onNewSender: (DataSender) -> Unit
) {

    companion object {
        private const val TAG = "AutoReconnect"
        private const val CHECK_INTERVAL_MS = 5000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var isRunning = false

    /**
     * Starts monitoring the connection.
     */
    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            Log.d(TAG, "AutoReconnect started")
            while (isRunning) {
                delay(CHECK_INTERVAL_MS)
                if (!dataSender.isConnected) {
                    Log.w(TAG, "Connection lost, triggering reconnect...")

                    // Stop the old sender
                    dataSender.stopSending()

                    val ip = prefs.getLastIp()
                    if (ip.isNotBlank()) {
                        // Create a new DataSender instance because the old one cannot be restarted
                        val newSender = DataSender(ip, 8080, prefs)
                        dataSender = newSender
                        onNewSender(newSender)
                        newSender.start()
                        Log.d(TAG, "New DataSender started for $ip")
                    } else {
                        Log.e(TAG, "No stored IP address, cannot reconnect")
                    }
                }
            }
        }
    }

    /**
     * Stops monitoring and cancels all reconnect attempts.
     */
    fun stop() {
        if (!isRunning) return
        isRunning = false
        scope.cancel()
        Log.d(TAG, "AutoReconnect stopped")
    }
}