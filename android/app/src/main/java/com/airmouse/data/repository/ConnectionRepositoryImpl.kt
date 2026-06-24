
package com.airmouse.data.repository

import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.network.UdpDiscovery
import com.airmouse.utils.ConnectedDeviceStore
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val udpDiscovery: UdpDiscovery,
    private val prefs: PreferencesManager
) : IConnectionRepository {
    override suspend fun sendKeyPress(keyCode: Int): Boolean {
        return connectionManager.sendKeyPress(keyCode)
    }

    override suspend fun sendWindowCommand(action: String): Boolean {
        return connectionManager.sendWindowCommand(action)
    }

    override suspend fun sendCalibrate(): Boolean {
        return connectionManager.sendControl("calibrate")
    }
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

    private fun loadConfig() {
        val ip = prefs.getString("last_ip", "")
        val port = prefs.getInt("last_port", 8080)
        val protocol = prefs.getString("last_protocol", prefs.getString("connection_protocol", "WEBSOCKET"))
            ?: "WEBSOCKET"
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
                        ConnectionManager.ConnectionQuality.SignalStrength.EXCELLENT ->
                            ConnectionQuality.SignalStrength.EXCELLENT
                        ConnectionManager.ConnectionQuality.SignalStrength.GOOD ->
                            ConnectionQuality.SignalStrength.GOOD
                        ConnectionManager.ConnectionQuality.SignalStrength.FAIR ->
                            ConnectionQuality.SignalStrength.FAIR
                        ConnectionManager.ConnectionQuality.SignalStrength.POOR ->
                            ConnectionQuality.SignalStrength.POOR
                        ConnectionManager.ConnectionQuality.SignalStrength.VERY_POOR ->
                            ConnectionQuality.SignalStrength.VERY_POOR
                        ConnectionManager.ConnectionQuality.SignalStrength.UNKNOWN ->
                            ConnectionQuality.SignalStrength.UNKNOWN
                    }
                )
            }
        }
    }

    override suspend fun connect(config: ConnectionConfig): Boolean {
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
        return success
    }

    override suspend fun disconnect() {
        connectionManager.disconnect()
    }

    override suspend fun reconnect(): Boolean {
        connectionManager.reconnect()
        return true
    }

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

    override suspend fun sendMessage(message: String): Boolean {
        return connectionManager.send(message)
    }

    override suspend fun sendMessage(message: ByteArray): Boolean {
        return connectionManager.sendBinary(message)
    }

    override suspend fun testConnection(ip: String, port: Int): TestResult {
        return try {
            val startTime = System.currentTimeMillis()
            connectionManager.setProtocol(_config.value.protocol.let {
                when (it) {
                    ConnectionProtocol.TCP -> ConnectionManager.ConnectionProtocol.TCP
                    ConnectionProtocol.UDP -> ConnectionManager.ConnectionProtocol.UDP
                    ConnectionProtocol.WEBSOCKET -> ConnectionManager.ConnectionProtocol.WEBSOCKET
                }
            })
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
            TestResult(success = false, message = e.message ?: "Connection failed")
        }
    }

    override suspend fun ping(): Long {
        val startTime = System.currentTimeMillis()
        connectionManager.sendPing()
        
        delay(1000)
        return System.currentTimeMillis() - startTime
    }

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
