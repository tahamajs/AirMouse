// app/src/main/java/com/airmouse/features/ConnectionFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.domain.model.TestResult
import com.airmouse.domain.usecase.ConnectToServerUseCase
import com.airmouse.domain.usecase.DiscoverServersUseCase
import com.airmouse.domain.usecase.GetConnectionStatusUseCase
import com.airmouse.domain.usecase.TestConnectionUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ConnectionFeature @Inject constructor(
    private val connectToServerUseCase: ConnectToServerUseCase,
    private val discoverServersUseCase: DiscoverServersUseCase,
    private val getConnectionStatusUseCase: GetConnectionStatusUseCase,
    private val testConnectionUseCase: TestConnectionUseCase
) {

    private val _isAutoConnectEnabled = MutableStateFlow(true)
    val isAutoConnectEnabled: StateFlow<Boolean> = _isAutoConnectEnabled.asStateFlow()

    private val _lastUsedServer = MutableStateFlow<ConnectionConfig?>(null)
    val lastUsedServer: StateFlow<ConnectionConfig?> = _lastUsedServer.asStateFlow()

    // Combined state
    data class ConnectionFeatureState(
        val status: ConnectionStatus = ConnectionStatus.DISCONNECTED,
        val quality: ConnectionQuality = ConnectionQuality(),
        val isConnected: Boolean = false,
        val autoConnect: Boolean = true,
        val lastServer: ConnectionConfig? = null,
        val discoveredServers: List<DiscoveredServer> = emptyList()
    )

    private val _state = MutableStateFlow(ConnectionFeatureState())
    val state: StateFlow<ConnectionFeatureState> = _state.asStateFlow()

    init {
        observeStatus()
        observeQuality()
        loadLastServer()
    }

    private fun observeStatus() {
        // Combine status and other flows
        // In production, would use combine operator
    }

    private fun observeQuality() {
        // Observe quality changes
    }

    private fun loadLastServer() {
        // Load last used server from preferences
    }

    suspend fun connect(ip: String, port: Int = 8080, protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET): Result<Boolean> {
        val result = connectToServerUseCase.connect(ip, port, protocol)
        if (result.isSuccess) {
            _lastUsedServer.value = ConnectionConfig(ip, port, protocol)
        }
        return result
    }

    suspend fun connectToLastServer(): Result<Boolean> {
        val lastServer = _lastUsedServer.value
        return if (lastServer != null) {
            connectToServerUseCase.connectToLastServer()
        } else {
            Result.failure(Exception("No last server found"))
        }
    }

    suspend fun disconnect() {
        connectToServerUseCase.disconnect()
    }

    suspend fun reconnect(): Result<Boolean> {
        return connectToServerUseCase.reconnect()
    }

    suspend fun getStatus(): ConnectionStatus {
        return getConnectionStatusUseCase()
    }

    suspend fun getConnectionQuality(): ConnectionQuality {
        return getConnectionStatusUseCase.getConnectionQuality()
    }

    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return getConnectionStatusUseCase.observeConnectionStatus()
    }

    fun observeConnectionQuality(): Flow<ConnectionQuality> {
        return getConnectionStatusUseCase.observeConnectionQuality()
    }

    suspend fun isConnected(): Boolean {
        return getConnectionStatusUseCase.isConnected()
    }

    suspend fun discoverServers(): List<DiscoveredServer> {
        return discoverServersUseCase()
    }

    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit): Result<Unit> {
        return discoverServersUseCase.startDiscovery(onServerFound)
    }

    suspend fun stopDiscovery(): Result<Unit> {
        return discoverServersUseCase.stopDiscovery()
    }

    suspend fun testConnection(ip: String, port: Int = 8080): Result<TestResult> {
        return testConnectionUseCase(ip, port)
    }

    suspend fun ping(): Result<Long> {
        return testConnectionUseCase.ping()
    }

    suspend fun getConnectionConfig(): ConnectionConfig {
        return getConnectionStatusUseCase.getConnectionConfig()
    }

    fun setAutoConnect(enabled: Boolean) {
        _isAutoConnectEnabled.value = enabled
    }

    suspend fun testCurrentConnection(): Result<TestResult> {
        return testConnectionUseCase.testCurrentConnection()
    }

    fun getConnectionFeatureState(): ConnectionFeatureState = _state.value
}