// app/src/main/java/com/airmouse/data/repository/ConnectionRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.data.local.PreferencesManager
import com.airmouse.data.remote.TcpClient
import com.airmouse.data.remote.WebSocketManager
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.data.model.NetworkMessage
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionRepositoryImpl @Inject constructor(
    private val webSocketManager: WebSocketManager,
    private val tcpClient: TcpClient,
    private val prefs: PreferencesManager
) : IConnectionRepository {

    private val _status = MutableStateFlow(ConnectionStatus.DISCONNECTED)
    override fun connectionStatus(): Flow<ConnectionStatus> = _status.asStateFlow()
    private var activeProtocol: ConnectionProtocol = ConnectionProtocol.TCP

    override suspend fun connect(config: ConnectionConfig) {
        _status.value = ConnectionStatus.CONNECTING
        try {
            webSocketManager.disconnect()
            tcpClient.disconnect()
            activeProtocol = config.protocol
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
        activeProtocol = ConnectionProtocol.TCP
        _status.value = ConnectionStatus.DISCONNECTED
    }

    override suspend fun sendEvent(event: MouseEvent) {
        val message = when (event) {
            is MouseEvent.Move -> NetworkMessage.toJson(NetworkMessage.Move(event.dx, event.dy))
            MouseEvent.Click -> NetworkMessage.toJson(NetworkMessage.Click())
            MouseEvent.DoubleClick -> NetworkMessage.toJson(NetworkMessage.DoubleClick)
            MouseEvent.RightClick -> NetworkMessage.toJson(NetworkMessage.RightClick)
            is MouseEvent.Scroll -> NetworkMessage.toJson(NetworkMessage.Scroll(event.delta))
        }
        when (activeProtocol) {
            ConnectionProtocol.WEBSOCKET -> webSocketManager.send(message)
            ConnectionProtocol.TCP -> tcpClient.send(message)
            else -> webSocketManager.send(message)
        }
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