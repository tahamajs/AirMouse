// app/src/main/java/com/airmouse/data/repository/ConnectionRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.data.local.PreferencesManager
import com.airmouse.data.remote.TcpClient
import com.airmouse.data.remote.WebSocketManager
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject

class ConnectionRepositoryImpl @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val tcpClient: TcpClient,
    private val prefs: PreferencesManager
) : IConnectionRepository {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun connectionStatus(): Flow<ConnectionStatus> = _status.asStateFlow()

    override suspend fun connect(config: ConnectionConfig) {
        _status.value = ConnectionStatus.CONNECTING
        try {
            when (config.protocol) {
                ConnectionProtocol.WEBSOCKET -> {
                    webSocketManager.connect("ws://${config.serverIp}:${config.serverPort}")
                }
                ConnectionProtocol.TCP -> {
                    tcpClient.connect(config.serverIp, config.serverPort)
                }
                else -> throw UnsupportedOperationException("Protocol not implemented")
            }
            _status.value = ConnectionStatus.CONNECTED
            saveConfig(config)
        } catch (e: Exception) {
            _status.value = ConnectionStatus.ERROR
            throw e
        }
    }

    override suspend fun disconnect() {
        webSocketManager.disconnect()
        tcpClient.disconnect()
        _status.value = ConnectionStatus.DISCONNECTED
    }

    override suspend fun sendEvent(event: MouseEvent) {
        val message = when (event) {
            is MouseEvent.Move -> "{\"type\":\"move\",\"dx\":${event.dx},\"dy\":${event.dy}}"
            MouseEvent.Click -> "{\"type\":\"click\"}"
            MouseEvent.DoubleClick -> "{\"type\":\"doubleclick\"}"
            MouseEvent.RightClick -> "{\"type\":\"rightclick\"}"
            is MouseEvent.Scroll -> "{\"type\":\"scroll\",\"delta\":${event.delta}}"
        }
        webSocketManager.send(message)
        tcpClient.send(message)
    }

    override suspend fun getLastConfig(): ConnectionConfig? {
        val ip = prefs.getLastIp()
        val port = prefs.getLastPort()
        return if (ip.isNotBlank() && port > 0) {
            ConnectionConfig(ip, port, ConnectionProtocol.TCP)
        } else null
    }

    override suspend fun saveConfig(config: ConnectionConfig) {
        prefs.setLastIp(config.serverIp)
        prefs.setLastPort(config.serverPort)
    }
}