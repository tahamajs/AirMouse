package com.airmouse.network

import android.util.Log
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages automatic reconnection to the Air Mouse server.
 *
 * Listens to the ConnectionManager's status and triggers reconnection with
 * exponential backoff when the connection is lost.
 */
@Singleton
class AutoReconnectManager @Inject constructor(
    private val connectionManager: ConnectionManager
) {
    companion object {
        private const val TAG = "AutoReconnectManager"
        private const val MAX_RECONNECT_ATTEMPTS = 10
        private const val BASE_DELAY_MS = 3000L
        private const val MAX_DELAY_MS = 60000L
    }

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var collectorJob: Job? = null
    private var reconnectJob: Job? = null
    private var isStarted = false

    private var reconnectAttempts = 0

    private val _isReconnecting = MutableStateFlow(false)
    val isReconnecting: StateFlow<Boolean> = _isReconnecting.asStateFlow()

    private var lastStatus: ConnectionManager.ConnectionStatus =
        ConnectionManager.ConnectionStatus.DISCONNECTED

    /**
     * Start monitoring the connection status and automatically reconnect if needed.
     *
     * @param autoConnect If true, start monitoring immediately. Default is true.
     */
    fun start(autoConnect: Boolean = true) {
        if (!autoConnect) return
        if (isStarted) {
            Log.d(TAG, "Already started")
            return
        }

        isStarted = true
        Log.i(TAG, "Auto-reconnect manager started")

        // Cancel any existing collector
        collectorJob?.cancel()
        collectorJob = scope.launch {
            connectionManager.connectionStatus.collect { status ->
                val previousStatus = lastStatus
                lastStatus = status
                Log.d(TAG, "Status changed: $previousStatus -> $status")

                when (status) {
                    ConnectionManager.ConnectionStatus.ERROR -> {
                        val shouldReconnect =
                            previousStatus == ConnectionManager.ConnectionStatus.CONNECTED ||
                                    previousStatus == ConnectionManager.ConnectionStatus.RECONNECTING
                        if (shouldReconnect && !_isReconnecting.value) {
                            Log.w(TAG, "Connection error detected, scheduling reconnect")
                            scheduleReconnect()
                        }
                    }
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        Log.i(TAG, "Connected, resetting reconnect attempts")
                        resetReconnectAttempts()
                    }
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> {
                        if (previousStatus == ConnectionManager.ConnectionStatus.CONNECTED ||
                            previousStatus == ConnectionManager.ConnectionStatus.RECONNECTING
                        ) {
                            Log.i(TAG, "Disconnected, resetting reconnect attempts")
                            resetReconnectAttempts()
                        }
                    }
                    else -> { /* ignore other states */ }
                }
            }
        }
    }

    /**
     * Schedule a reconnection attempt with exponential backoff.
     */
    private fun scheduleReconnect() {
        if (reconnectAttempts >= MAX_RECONNECT_ATTEMPTS) {
            Log.w(TAG, "Max reconnect attempts ($MAX_RECONNECT_ATTEMPTS) reached, giving up")
            _isReconnecting.value = false
            return
        }

        _isReconnecting.value = true
        reconnectAttempts++
        val delay = calculateDelay()
        Log.i(TAG, "Reconnect attempt $reconnectAttempts/$MAX_RECONNECT_ATTEMPTS in ${delay}ms")

        // Cancel any pending reconnect job
        reconnectJob?.cancel()
        reconnectJob = scope.launch {
            delay(delay)
            Log.d(TAG, "Attempting reconnect...")
            connectionManager.reconnect()
            _isReconnecting.value = false
        }
    }

    /**
     * Calculate the delay for the next reconnect attempt using exponential backoff.
     */
    private fun calculateDelay(): Long {
        // Shift up to 4 times (max 16x base) then cap
        val exponential = BASE_DELAY_MS * (1L shl reconnectAttempts.coerceAtMost(4))
        return exponential.coerceAtMost(MAX_DELAY_MS)
    }

    /**
     * Reset the reconnect attempt counter and cancel any pending reconnect.
     */
    fun resetReconnectAttempts() {
        reconnectAttempts = 0
        _isReconnecting.value = false
        lastStatus = ConnectionManager.ConnectionStatus.CONNECTED
        reconnectJob?.cancel()
        reconnectJob = null
        Log.d(TAG, "Reconnect attempts reset")
    }

    /**
     * Stop all monitoring and cancel any pending reconnection.
     */
    fun cancel() {
        Log.i(TAG, "Cancelling auto-reconnect manager")
        resetReconnectAttempts()
        isStarted = false
        lastStatus = ConnectionManager.ConnectionStatus.DISCONNECTED
        collectorJob?.cancel()
        collectorJob = null
        scope.cancel()
    }

    /**
     * Check if the manager is currently trying to reconnect.
     */
    fun isReconnecting(): Boolean = _isReconnecting.value

    /**
     * Get the current reconnect attempt count.
     */
    fun getReconnectAttempts(): Int = reconnectAttempts
}