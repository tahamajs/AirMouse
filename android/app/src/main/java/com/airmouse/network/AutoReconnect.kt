package com.airmouse.network

import android.util.Log
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.*

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

    fun start() {
        if (isRunning) return
        isRunning = true
        scope.launch {
            Log.d(TAG, "AutoReconnect started")
            while (isRunning) {
                delay(CHECK_INTERVAL_MS)
                if (!dataSender.isConnected) {
                    Log.w(TAG, "Connection lost, triggering reconnect...")
                    dataSender.stopSending()

                    val ip = prefs.getLastIp()
                    val port = prefs.getLastPort()
                    if (ip.isNotBlank()) {
                        // Create a new DataSender (constructor is now public)
                        val newSender = DataSender(ip, port, prefs)
                        dataSender = newSender
                        onNewSender(newSender)
                        newSender.start()
                        Log.d(TAG, "New DataSender started for $ip:$port")
                    } else {
                        Log.e(TAG, "No stored IP address, cannot reconnect")
                    }
                }
            }
        }
    }

    fun stop() {
        if (!isRunning) return
        isRunning = false
        scope.cancel()
        Log.d(TAG, "AutoReconnect stopped")
    }
}
