// app/src/main/java/com/airmouse/data/repository/ConnectionRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.model.TestResult
import com.airmouse.domain.repository.ConnectionStats
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.network.WebSocketManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.net.InetSocketAddress
import java.net.Socket
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IConnectionRepository {

    private val _connectionStatus = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun connectionStatus(): StateFlow<ConnectionStatus> = _connectionStatus.asStateFlow()

    private val _connectionStats = MutableStateFlow(ConnectionStats())
    override fun getConnectionStats(): StateFlow<ConnectionStats> = _connectionStats.asStateFlow()

    private var currentConfig: ConnectionConfig? = null
    private var reconnectAttempts = 0
    private val maxReconnectAttempts = 5
    private val reconnectDelayMs = 2000L

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    init {
        setupWebSocketCallbacks()
        loadConnectionStats()
    }

    private fun setupWebSocketCallbacks() {
        WebSocketManager.onConnected = {
            scope.launch {
                _connectionStatus.value = ConnectionStatus.CONNECTED
                reconnectAttempts = 0
                updateConnectionStats(success = true)
                saveCurrentConfig()
            }
        }

        WebSocketManager.onDisconnected = {
            scope.launch {
                _connectionStatus.value = ConnectionStatus.DISCONNECTED
                if (currentConfig?.autoReconnect == true && reconnectAttempts < maxReconnectAttempts) {
                    startReconnection()
                }
            }
        }

        WebSocketManager.onError = { error ->
            scope.launch {
                _connectionStatus.value = ConnectionStatus.ERROR
                updateConnectionStats(success = false)
            }
        }
    }

    private fun startReconnection() {
        scope.launch {
            _connectionStatus.value = ConnectionStatus.RECONNECTING
            reconnectAttempts++
            delay(reconnectDelayMs * reconnectAttempts)
            currentConfig?.let { connect(it) }
        }
    }

    override suspend fun connect(config: ConnectionConfig) {
        currentConfig = config
        _connectionStatus.value = ConnectionStatus.CONNECTING

        val url = when (config.protocol) {
            ConnectionProtocol.WEBSOCKET -> "ws://${config.serverIp}:${config.serverPort}/ws"
            else -> "ws://${config.serverIp}:${config.serverPort}/ws"
        }

        WebSocketManager.connect(url)
        saveConfig(config)
    }

    override suspend fun disconnect() {
        reconnectAttempts = maxReconnectAttempts // Disable auto-reconnect
        WebSocketManager.disconnect()
        _connectionStatus.value = ConnectionStatus.DISCONNECTED
        currentConfig = null
    }

    override suspend fun sendEvent(event: MouseEvent) {
        if (_connectionStatus.value != ConnectionStatus.CONNECTED) return

        when (event) {
            is MouseEvent.Move -> WebSocketManager.sendMove(event.dx.toFloat(), event.dy.toFloat())
            is MouseEvent.Click -> WebSocketManager.sendClick(event.button)
            is MouseEvent.DoubleClick -> WebSocketManager.sendDoubleClick()
            is MouseEvent.RightClick -> WebSocketManager.sendRightClick()
            is MouseEvent.Scroll -> WebSocketManager.sendScroll(event.delta)
            is MouseEvent.Gesture -> WebSocketManager.sendGesture(event.name, event.confidence)
            is MouseEvent.Proximity -> WebSocketManager.sendProximity(event.isNear, event.distance)
            is MouseEvent.Control -> WebSocketManager.send(event.command)
        }

        _connectionStats.update { stats ->
            stats.copy(totalDataSent = stats.totalDataSent + 1)
        }
        saveConnectionStats()
    }

    override suspend fun getLastConfig(): ConnectionConfig? {
        val lastIp = prefs.getString("last_ip", "")
        if (lastIp.isEmpty()) return null

        return ConnectionConfig(
            serverIp = lastIp,
            serverPort = prefs.getInt("last_port", 8080),
            protocol = ConnectionProtocol.values().getOrElse(prefs.getInt("last_protocol", 0)) { ConnectionProtocol.TCP },
            useAuth = prefs.getBoolean("use_auth", false),
            authToken = prefs.getString("auth_token", null),
            timeoutMs = prefs.getInt("connection_timeout", 5000),
            autoReconnect = prefs.getBoolean("auto_reconnect", true),
            lastConnected = prefs.getLong("last_connected", 0)
        )
    }

    override suspend fun saveConfig(config: ConnectionConfig) {
        prefs.putString("last_ip", config.serverIp)
        prefs.putInt("last_port", config.serverPort)
        prefs.putInt("last_protocol", config.protocol.ordinal)
        prefs.putBoolean("use_auth", config.useAuth)
        config.authToken?.let { prefs.putString("auth_token", it) }
        prefs.putInt("connection_timeout", config.timeoutMs)
        prefs.putBoolean("auto_reconnect", config.autoReconnect)
    }

    private suspend fun saveCurrentConfig() {
        currentConfig?.let { config ->
            prefs.putLong("last_connected", System.currentTimeMillis())
            config.copy(lastConnected = System.currentTimeMillis()).let { saveConfig(it) }
        }
    }

    private fun updateConnectionStats(success: Boolean) {
        _connectionStats.update { stats ->
            stats.copy(
                totalConnections = stats.totalConnections + 1,
                successfulConnections = if (success) stats.successfulConnections + 1 else stats.successfulConnections,
                failedConnections = if (!success) stats.failedConnections + 1 else stats.failedConnections,
                lastConnectionTime = System.currentTimeMillis()
            )
        }
        saveConnectionStats()
    }

    private fun loadConnectionStats() {
        _connectionStats.update {
            ConnectionStats(
                totalConnections = prefs.getInt("stat_total_connections", 0),
                successfulConnections = prefs.getInt("stat_successful_connections", 0),
                failedConnections = prefs.getInt("stat_failed_connections", 0),
                averageLatency = prefs.getInt("stat_avg_latency", 0),
                lastConnectionTime = prefs.getLong("stat_last_connection", 0),
                totalDataSent = prefs.getLong("stat_data_sent", 0),
                totalDataReceived = prefs.getLong("stat_data_received", 0)
            )
        }
    }

    private fun saveConnectionStats() {
        val stats = _connectionStats.value
        prefs.putInt("stat_total_connections", stats.totalConnections)
        prefs.putInt("stat_successful_connections", stats.successfulConnections)
        prefs.putInt("stat_failed_connections", stats.failedConnections)
        prefs.putInt("stat_avg_latency", stats.averageLatency)
        prefs.putLong("stat_last_connection", stats.lastConnectionTime)
        prefs.putLong("stat_data_sent", stats.totalDataSent)
        prefs.putLong("stat_data_received", stats.totalDataReceived)
    }

    override suspend fun clearHistory() {
        _connectionStats.update { ConnectionStats() }
        saveConnectionStats()
    }

    override suspend fun testConnection(ip: String, port: Int): TestResult {
        return try {
            val startTime = System.currentTimeMillis()
            val socket = Socket()
            socket.connect(InetSocketAddress(ip, port), 3000)
            val latency = (System.currentTimeMillis() - startTime).toInt()
            socket.close()
            TestResult(success = true, latency = latency)
        } catch (e: Exception) {
            TestResult(success = false, latency = -1, error = e.message)
        }
    }

    override suspend fun reconnect() {
        if (currentConfig != null) {
            disconnect()
            delay(500)
            connect(currentConfig!!)
        } else {
            val lastConfig = getLastConfig()
            if (lastConfig != null) {
                connect(lastConfig)
            }
        }
    }

    fun destroy() {
        scope.cancel()
    }
}