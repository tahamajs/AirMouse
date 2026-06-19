// app/src/main/java/com/airmouse/domain/usecase/ConnectToServerUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for connecting to the Air Mouse server
 */
class ConnectToServerUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Connect to server with configuration
     */
    suspend operator fun invoke(config: ConnectionConfig): Result<Boolean> {
        return try {
            val result = connectionRepository.connect(config)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Connect to server with IP and port
     */
    suspend fun connect(ip: String, port: Int = 8080, protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET): Result<Boolean> {
        val config = ConnectionConfig(
            ip = ip,
            port = port,
            protocol = protocol
        )
        return invoke(config)
    }

    /**
     * Connect to last used server
     */
    suspend fun connectToLastServer(): Result<Boolean> {
        return try {
            val config = connectionRepository.getConnectionConfig()
            val result = connectionRepository.connect(config)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Observe connection status
     */
    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return connectionRepository.observeConnectionStatus()
    }

    /**
     * Get current connection status
     */
    suspend fun getConnectionStatus(): ConnectionStatus {
        return connectionRepository.getConnectionStatus()
    }

    /**
     * Disconnect from server
     */
    suspend fun disconnect() {
        connectionRepository.disconnect()
    }

    /**
     * Reconnect to server
     */
    suspend fun reconnect(): Result<Boolean> {
        return try {
            val result = connectionRepository.reconnect()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if connected
     */
    suspend fun isConnected(): Boolean {
        return connectionRepository.getConnectionStatus() == ConnectionStatus.CONNECTED
    }
}