// app/src/main/java/com/airmouse/data/datasource/remote/ConnectionDataSourceImpl.kt
package com.airmouse.data.datasource.remote

import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.network.ConnectionManager
import com.airmouse.network.UdpDiscovery
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionDataSourceImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val udpDiscovery: UdpDiscovery
) : IConnectionDataSource {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _quality = MutableStateFlow(ConnectionQuality())
    override fun observeConnectionQuality(): Flow<ConnectionQuality> = _quality.asStateFlow()

    private val _discoveredServers = mutableListOf<DiscoveredServer>()

    init {
        observeConnectionManagerQuality()
    }

    private fun observeConnectionManagerQuality() {
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
                        else -> ConnectionQuality.SignalStrength.UNKNOWN
                    }
                )
            }
        }
    }

    override suspend fun connect(ip: String, port: Int, useSSL: Boolean): Boolean {
        return connectionManager.connect(ip, port)
    }

    override suspend fun disconnect() {
        connectionManager.disconnect()
    }

    override suspend fun reconnect(): Boolean {
        return connectionManager.reconnect()
    }

    override suspend fun isConnected(): Boolean {
        return connectionManager.isConnected()
    }

    override suspend fun sendMessage(message: String): Boolean {
        return connectionManager.send(message)
    }

    override suspend fun sendMessage(message: ByteArray): Boolean {
        return connectionManager.sendBinary(message)
    }

    override suspend fun sendMove(dx: Float, dy: Float): Boolean {
        return connectionManager.sendMove(dx, dy)
    }

    override suspend fun sendClick(button: String): Boolean {
        return connectionManager.sendClick(button)
    }

    override suspend fun sendDoubleClick(): Boolean {
        return connectionManager.sendDoubleClick()
    }

    override suspend fun sendRightClick(): Boolean {
        return connectionManager.sendRightClick()
    }

    override suspend fun sendScroll(delta: Int): Boolean {
        return connectionManager.sendScroll(delta)
    }

    override suspend fun sendGesture(gesture: String, confidence: Float): Boolean {
        return connectionManager.sendGesture(gesture, confidence)
    }

    override suspend fun sendProximity(isNear: Boolean, distance: Float): Boolean {
        return connectionManager.sendProximity(isNear, distance)
    }

    override suspend fun sendControl(command: String): Boolean {
        return connectionManager.sendControl(command)
    }

    override suspend fun sendHello(name: String, version: String): Boolean {
        return connectionManager.sendHello(name, version)
    }

    override suspend fun sendPing(): Boolean {
        return connectionManager.sendPing()
    }

    override suspend fun sendPong(): Boolean {
        return connectionManager.sendPong()
    }

    override suspend fun discoverServers(): List<DiscoveredServer> {
        return _discoveredServers.toList()
    }

    override suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit) {
        udpDiscovery.onServerFound = { ip, port, name, version ->
            val server = DiscoveredServer(
                ip = ip,
                port = port,
                name = name,
                version = version,
                lastSeen = System.currentTimeMillis()
            )
            if (_discoveredServers.none { it.ip == ip && it.port == port }) {
                _discoveredServers.add(server)
                onServerFound(server)
            }
        }
        udpDiscovery.startDiscovery()
    }

    override suspend fun stopDiscovery() {
        udpDiscovery.stopDiscovery()
        _discoveredServers.clear()
    }

    override suspend fun getConnectionQuality(): ConnectionQuality {
        return _quality.value
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
