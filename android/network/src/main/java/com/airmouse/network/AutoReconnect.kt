package com.airmouse.network

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class AutoReconnect(
    private var sender: DataSender,
    private val store: ConnectionStore,
    private val onReconnect: (DataSender) -> Unit,
    private val retryDelayMs: Long = 3_000L
) {
    private val scope = CoroutineScope(Dispatchers.IO + Job())
    private var job: Job? = null

    fun start() {
        if (job?.isActive == true) return
        job = scope.launch {
            while (isActive) {
                if (sender.isConnected) {
                    delay(retryDelayMs)
                    continue
                }

                val next = DataSender.getInstance(store.getLastIp(), store.getLastPort(), store)
                    ?: sender
                sender = next
                next.start()
                delay(500)
                if (next.isConnected) {
                    onReconnect(next)
                    return@launch
                }
                delay(retryDelayMs)
            }
        }
    }

    fun stop() {
        job?.cancel()
        scope.cancel()
    }
}