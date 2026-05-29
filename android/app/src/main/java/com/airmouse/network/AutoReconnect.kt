package com.airmouse.network

import kotlinx.coroutines.*

class AutoReconnect(
    private var sender: DataSender,
    private val host: String,
    private val port: Int,
    private val retryIntervalMs: Long = 3000L,
    private val onReconnect: ((DataSender) -> Unit)? = null
) {
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    fun start() {
        job = scope.launch {
            while (isActive) {
                delay(retryIntervalMs)
                if (!sender.isConnected) {
                    try {
                        val newSender = DataSender(host, port)
                        newSender.start()
                        sender = newSender
                        onReconnect?.invoke(newSender)
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun stop() { job?.cancel() }
}