package com.airmouse.data.repository

import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.network.ConnectionQuality as NetworkConnectionQuality
import com.airmouse.network.UdpDiscovery
import com.airmouse.utils.ConnectedDeviceStore
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val udpDiscovery: UdpDiscovery,
    private val prefs: PreferencesManager
) : IConnectionRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun observeConnectionStatus(): Flow<ConnectionStatus> = _status.asStateFlow()

    private val _quality = MutableStateFlow(ConnectionQuality())
    override fun observeConnectionQuality(): Flow<ConnectionQuality> = _quality.asStateFlow()

    private val _config = MutableStateFlow(ConnectionConfig())
    private val _discoveredServers = MutableStateFlow<List<DiscoveredServer>>(emptyList())

    init {
        loadConfig()
        observeConnectionManagerStatus()
    }

    // ============================================================
    // System Commands
    // ============================================================

    override suspend fun sendKeyPress(keyCode: Int): Boolean {
        return try {
            connectionManager.sendKeyPress(keyCode)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send key press")
            false
        }
    }

    override suspend fun sendWindowCommand(action: String): Boolean {
        return try {
            connectionManager.sendWindowCommand(action)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send window command")
            false
        }
    }

    override suspend fun sendCalibrate(): Boolean {
        return try {
            connectionManager.sendControl("calibrate")
        } catch (e: Exception) {
            Timber.e(e, "Failed to send calibrate command")
            false
        }
    }

    // ============================================================
    // Configuration
    // ============================================================

    private fun loadConfig() {
        val ip = prefs.getString("last_ip", "")
        val port = prefs.getInt("last_port", 8080)
        val protocol = prefs.getString("last_protocol", prefs.getString("connection_protocol", "WEBSOCKET"))
        val protocolEnum = try {
            ConnectionProtocol.valueOf(protocol.uppercase())
        } catch (e: IllegalArgumentException) {
            ConnectionProtocol.WEBSOCKET
        }
        _config.value = ConnectionConfig(ip = ip, port = port, protocol = protocolEnum).normalized()
    }

    private fun observeConnectionManagerStatus() {
        scope.launch {
            connectionManager.connectionStatus.collect { status ->
                _status.value = when (status) {
                    ConnectionManager.ConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
                    ConnectionManager.ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                    ConnectionManager.ConnectionStatus.RECONNECTING -> ConnectionStatus.RECONNECTING
                    ConnectionManager.ConnectionStatus.ERROR -> ConnectionStatus.ERROR
                }
            }
        }
        scope.launch {
            connectionManager.connectionQuality.collect { quality ->
                _quality.value = ConnectionQuality(
                    ping = quality.ping,
                    rssi = quality.rssi,
                    jitter = quality.jitter,
                    signalStrength = when (quality.signalStrength) {
                        NetworkConnectionQuality.SignalStrength.EXCELLENT ->
                            ConnectionQuality.SignalStrength.EXCELLENT
                        NetworkConnectionQuality.SignalStrength.GOOD ->
                            ConnectionQuality.SignalStrength.GOOD
                        NetworkConnectionQuality.SignalStrength.FAIR ->
                            ConnectionQuality.SignalStrength.FAIR
                        NetworkConnectionQuality.SignalStrength.POOR ->
                            ConnectionQuality.SignalStrength.POOR
                        NetworkConnectionQuality.SignalStrength.VERY_POOR ->
                            ConnectionQuality.SignalStrength.VERY_POOR
                        NetworkConnectionQuality.SignalStrength.UNKNOWN ->
                            ConnectionQuality.SignalStrength.UNKNOWN
                    }
                )
            }
        }
    }

    // ============================================================
    // Connection Management
    // ============================================================

    override suspend fun connect(config: ConnectionConfig): Boolean {
        if (!prefs.getBoolean("calibration_complete", false)) {
            throw com.airmouse.domain.model.CalibrationRequiredException()
        }
        return try {
            val normalized = config.normalized()
            _config.value = normalized
            saveConnectionConfig(normalized)
            val protocol = normalized.protocol
            connectionManager.setProtocol(
                when (protocol) {
                    ConnectionProtocol.TCP -> ConnectionManager.ConnectionProtocol.TCP
                    ConnectionProtocol.UDP -> ConnectionManager.ConnectionProtocol.UDP
                    ConnectionProtocol.WEBSOCKET -> ConnectionManager.ConnectionProtocol.WEBSOCKET
                }
            )
            val success = connectionManager.connect(normalized.ip, normalized.port)
            if (success) {
                ConnectedDeviceStore.rememberConnection(
                    prefs = prefs,
                    serverName = connectionManager.serverName.value.ifBlank { normalized.ip },
                    ip = normalized.ip,
                    port = normalized.port,
                    protocol = protocol.name,
                    version = connectionManager.serverVersion.value.ifBlank { "3.0.0" }
                )
            }
            success
        } catch (e: Exception) {
            Timber.e(e, "Connection failed")
            false
        }
    }

    override suspend fun disconnect() {
        try {
            connectionManager.disconnect()
        } catch (e: Exception) {
            Timber.e(e, "Disconnect failed")
        }
    }

    override suspend fun reconnect(): Boolean {
        if (!prefs.getBoolean("calibration_complete", false)) {
            throw com.airmouse.domain.model.CalibrationRequiredException()
        }
        return try {
            connectionManager.reconnect()
            true
        } catch (e: Exception) {
            Timber.e(e, "Reconnect failed")
            false
        }
    }

    // ============================================================
    // Status & Config
    // ============================================================

    override suspend fun getConnectionStatus(): ConnectionStatus = _status.value

    override suspend fun getConnectionConfig(): ConnectionConfig = _config.value

    override suspend fun saveConnectionConfig(config: ConnectionConfig) {
        val normalized = config.normalized()
        _config.value = normalized
        prefs.putString("last_ip", normalized.ip)
        prefs.putInt("last_port", normalized.port)
        prefs.putString("last_protocol", normalized.protocol.name)
        prefs.putString("connection_protocol", normalized.protocol.name)
        prefs.putBoolean("use_ssl", normalized.useSSL)
        if (normalized.authToken != null) {
            prefs.putString("auth_token", normalized.authToken)
        }
    }

    override suspend fun getConnectionQuality(): ConnectionQuality = _quality.value

    // ============================================================
    // Discovery
    // ============================================================

    override suspend fun discoverServers(): List<DiscoveredServer> {
        return _discoveredServers.value
    }

    override suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit) {
        udpDiscovery.onServerFound = { ip, port, name, version ->
            val server = DiscoveredServer(ip = ip, port = port, name = name, version = version)
            val currentServers = _discoveredServers.value.toMutableList()
            if (currentServers.none { it.ip == ip && it.port == port }) {
                currentServers.add(server)
                _discoveredServers.value = currentServers
                onServerFound(server)
            }
        }
        udpDiscovery.startDiscovery()
    }

    override suspend fun stopDiscovery() {
        udpDiscovery.stopDiscovery()
    }

    // ============================================================
    // Messaging
    // ============================================================

    override suspend fun sendMessage(message: String): Boolean {
        return try {
            connectionManager.send(message)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send message")
            false
        }
    }

    override suspend fun sendMessage(message: ByteArray): Boolean {
        return try {
            connectionManager.sendBinary(message)
        } catch (e: Exception) {
            Timber.e(e, "Failed to send binary message")
            false
        }
    }

    // ============================================================
    // Testing
    // ============================================================

    override suspend fun testConnection(ip: String, port: Int): TestResult {
        return try {
            val startTime = System.currentTimeMillis()
            val protocol = _config.value.protocol
            connectionManager.setProtocol(
                when (protocol) {
                    ConnectionProtocol.TCP -> ConnectionManager.ConnectionProtocol.TCP
                    ConnectionProtocol.UDP -> ConnectionManager.ConnectionProtocol.UDP
                    ConnectionProtocol.WEBSOCKET -> ConnectionManager.ConnectionProtocol.WEBSOCKET
                }
            )
            val success = connectionManager.connect(ip, port)
            val latency = System.currentTimeMillis() - startTime
            connectionManager.disconnect()
            if (success) {
                TestResult(success = true, message = "Connection successful", latency = latency)
            } else {
                TestResult(
                    success = false,
                    message = connectionManager.lastError.value ?: "Connection failed",
                    latency = latency
                )
            }
        } catch (e: Exception) {
            Timber.e(e, "Connection test failed")
            TestResult(success = false, message = e.message ?: "Connection failed")
        }
    }

    override suspend fun ping(): Long {
        return try {
            val startTime = System.currentTimeMillis()
            connectionManager.sendPing()
            delay(1000)
            System.currentTimeMillis() - startTime
        } catch (e: Exception) {
            Timber.e(e, "Ping failed")
            -1L
        }
    }

    // ============================================================
    // Listeners
    // ============================================================

    override fun setOnMessageListener(listener: (String) -> Unit) {
        connectionManager.onMessage = listener
    }

    override fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit) {
        connectionManager.onBinaryMessage = listener
    }

    override fun setOnDisconnectedListener(listener: () -> Unit) {
        connectionManager.onDisconnected = listener
    }

    override fun setOnConnectedListener(listener: () -> Unit) {
        connectionManager.onConnected = listener
    }
}
