// app/src/main/java/com/airmouse/network/AutoReconnectManager.kt
package com.airmouse.network

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AutoReconnectManager @Inject constructor(
    private val connectionManager: ConnectionManager
) {
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 10
    private val baseDelayMs = 3000L
    private val maxDelayMs = 60000L

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private var reconnectJob: Job? = null

    fun start(autoConnect: Boolean = true) {
        if (!autoConnect) return

        scope.launch {
            connectionManager.connectionStatus.collect { status ->
                when (status) {
                    ConnectionManager.ConnectionStatus.DISCONNECTED,
                    ConnectionManager.ConnectionStatus.ERROR -> {
                        if (!_isReconnecting.value) {
                            scheduleReconnect()
                        }
                    }
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        resetReconnectAttempts()
                    }
                    else -> {}
                }
            }
        }
    }

    private fun scheduleReconnect() {
        if (reconnectAttempts >= maxReconnectAttempts) {
            _isReconnecting.value = false
            return
        }

        _isReconnecting.value = true
        reconnectAttempts++

        val delay = calculateDelay()
        reconnectJob = scope.launch {
            delay(delay)
            connectionManager.reconnect()
            _isReconnecting.value = false
        }
    }

    private fun calculateDelay(): Long {
        val exponential = baseDelayMs * (1L shl reconnectAttempts.coerceAtMost(4))
        return exponential.coerceAtMost(maxDelayMs)
    }

    fun resetReconnectAttempts() {
        reconnectAttempts = 0
        _isReconnecting.value = false
        reconnectJob?.cancel()
    }

    fun cancel() {
        resetReconnectAttempts()
        scope.cancel()
    }
}