package com.airmouse.network

import kotlinx.coroutines.*
import com.airmouse.utils.LogManager

class AutoReconnect(
    private val ip: String,
    private val port: Int,
    private val onReconnect: () -> Unit
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        job = scope.launch {
            while (isActive) {
                LogManager.add("AutoReconnect: trying to reconnect...")
                try {
                    DataSender.getInstance(ip, port, null)?.start()
                    onReconnect()   // success – stop reconnecting
                    break
                } catch (e: Exception) {
                    LogManager.add("Reconnect failed: ${e.message}")
                    delay(3000)
                }
            }
        }
    }

    fun stop() {
        job?.cancel()
    }
}