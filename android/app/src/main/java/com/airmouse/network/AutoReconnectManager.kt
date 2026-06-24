
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
    private var lastStatus: ConnectionManager.ConnectionStatus = ConnectionManager.ConnectionStatus.DISCONNECTED

    fun start(autoConnect: Boolean = true) {
        if (!autoConnect) return

        scope.launch {
            connectionManager.connectionStatus.collect { status ->
                val previousStatus = lastStatus
                lastStatus = status
                when (status) {
                    ConnectionManager.ConnectionStatus.ERROR -> {
                        val shouldReconnect =
                            previousStatus == ConnectionManager.ConnectionStatus.CONNECTED ||
                                previousStatus == ConnectionManager.ConnectionStatus.RECONNECTING
                        if (shouldReconnect && !_isReconnecting.value) {
                            scheduleReconnect()
                        }
                    }
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        resetReconnectAttempts()
                    }
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> {
                        if (previousStatus == ConnectionManager.ConnectionStatus.CONNECTED ||
                            previousStatus == ConnectionManager.ConnectionStatus.RECONNECTING) {
                            resetReconnectAttempts()
                        }
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
        lastStatus = ConnectionManager.ConnectionStatus.CONNECTED
        reconnectJob?.cancel()
    }

    fun cancel() {
        resetReconnectAttempts()
        lastStatus = ConnectionManager.ConnectionStatus.DISCONNECTED
        scope.cancel()
    }
}
