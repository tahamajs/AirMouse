
package com.airmouse.domain.repository

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.domain.model.TestResult
import kotlinx.coroutines.flow.Flow

interface IConnectionRepository {
    
    suspend fun connect(config: ConnectionConfig): Boolean
    suspend fun disconnect()
    suspend fun sendKeyPress(keyCode: Int): Boolean
    suspend fun sendWindowCommand(action: String): Boolean
    suspend fun sendCalibrate(): Boolean

    suspend fun reconnect(): Boolean

    
    suspend fun getConnectionStatus(): ConnectionStatus
    fun observeConnectionStatus(): Flow<ConnectionStatus>

    
    suspend fun getConnectionConfig(): ConnectionConfig
    suspend fun saveConnectionConfig(config: ConnectionConfig)

    
    suspend fun getConnectionQuality(): ConnectionQuality
    fun observeConnectionQuality(): Flow<ConnectionQuality>

    
    suspend fun discoverServers(): List<DiscoveredServer>
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit)
    suspend fun stopDiscovery()

    
    suspend fun sendMessage(message: String): Boolean
    suspend fun sendMessage(message: ByteArray): Boolean

    
    suspend fun testConnection(ip: String, port: Int): TestResult
    suspend fun ping(): Long

    
    fun setOnMessageListener(listener: (String) -> Unit)
    fun setOnBinaryMessageListener(listener: (ByteArray) -> Unit)
    fun setOnDisconnectedListener(listener: () -> Unit)
    fun setOnConnectedListener(listener: () -> Unit)
}
