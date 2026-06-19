// app/src/main/java/com/airmouse/domain/repository/IConnectionRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.domain.model.TestResult
import kotlinx.coroutines.flow.Flow

interface IConnectionRepository {
    // Connection management
    suspend fun connect(config: ConnectionConfig): Boolean
    suspend fun disconnect()
    suspend fun sendKeyPress(keyCode: Int): Boolean
    suspend fun sendWindowCommand(action: String): Boolean
    suspend fun sendCalibrate(): Boolean

    suspend fun reconnect(): Boolean

    // Status
    suspend fun getConnectionStatus(): ConnectionStatus
    fun observeConnectionStatus(): Flow<ConnectionStatus>

    // Configuration
    suspend fun getConnectionConfig(): ConnectionConfig
    suspend fun saveConnectionConfig(config: ConnectionConfig)

    // Quality
    suspend fun getConnectionQuality(): ConnectionQuality
    fun observeConnectionQuality(): Flow<ConnectionQuality>

    // Discovery
    suspend fun discoverServers(): List<DiscoveredServer>
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit)
    suspend fun stopDiscovery()

    // Messaging
    suspend fun sendMessage(message: String): Boolean
    suspend fun sendMessage(message: ByteArray): Boolean

    // Testing
    suspend fun testConnection(ip: String, port: Int): TestResult
    suspend fun ping(): Long

    // Callbacks
    fun setOnMessageListener(listener: (String) -> Unit)
    fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit)
    fun setOnDisconnectedListener(listener: () -> Unit)
    fun setOnConnectedListener(listener: () -> Unit)
}
