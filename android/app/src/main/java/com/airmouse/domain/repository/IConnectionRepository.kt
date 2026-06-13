// app/src/main/java/com/airmouse/domain/repository/IConnectionRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.MouseEvent
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface IConnectionRepository {
    suspend fun connect(config: ConnectionConfig)
    suspend fun disconnect()
    suspend fun sendEvent(event: MouseEvent)
    fun connectionStatus(): StateFlow<ConnectionStatus>
    suspend fun getLastConfig(): ConnectionConfig?
    suspend fun saveConfig(config: ConnectionConfig)
    fun getConnectionStats(): StateFlow<ConnectionStats>
    suspend fun clearHistory()
    suspend fun testConnection(ip: String, port: Int): TestResult
    suspend fun reconnect()
}

data class ConnectionStats(
    val totalConnections: Int = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val averageLatency: Int = 0,
    val lastConnectionTime: Long = 0,
    val totalDataSent: Long = 0,
    val totalDataReceived: Long = 0
)

data class TestResult(
    val success: Boolean,
    val latency: Int,
    val error: String? = null
)