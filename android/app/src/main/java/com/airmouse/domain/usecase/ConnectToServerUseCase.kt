// app/src/main/java/com/airmouse/domain/usecase/ConnectToServerUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

class ConnectToServerUseCase @Inject constructor(
    private val connectionRepo: IConnectionRepository
) {

    /**
     * Connect to server with specified parameters
     */
    suspend operator fun invoke(
        ip: String,
        port: Int,
        protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET,
        useEncryption: Boolean = false,
        authToken: String? = null
    ): Result<Unit> {
        return try {
            val config = ConnectionConfig(
                serverIp = ip,
                serverPort = port,
                protocol = protocol,
                useEncryption = useEncryption,
                authenticationToken = authToken
            )
            connectionRepo.connect(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Disconnect from current server
     */
    suspend fun disconnect(): Result<Unit> {
        return try {
            connectionRepo.disconnect()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Reconnect to last used server
     */
    suspend fun reconnect(): Result<Unit> {
        return try {
            val lastConfig = connectionRepo.getLastConfig()
            if (lastConfig != null) {
                connectionRepo.connect(lastConfig)
                Result.success(Unit)
            } else {
                Result.failure(Exception("No previous connection found"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current connection status
     */
    suspend fun getConnectionStatus(): ConnectionStatus {
        return connectionRepo.connectionStatus().first()
    }

    /**
     * Check if currently connected
     */
    suspend fun isConnected(): Boolean {
        return connectionRepo.connectionStatus().first() == ConnectionStatus.CONNECTED
    }

    /**
     * Test connection without persisting
     */
    suspend fun testConnection(ip: String, port: Int): Result<Int> {
        return try {
            val result = connectionRepo.testConnection(ip, port)
            if (result.success) {
                Result.success(result.latency)
            } else {
                Result.failure(Exception(result.error ?: "Connection failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Auto-connect to last used server if enabled
     */
    suspend fun autoConnectIfEnabled(): Boolean {
        val prefs = connectionRepo.getLastConfig()
        return if (prefs != null && prefs.autoReconnect) {
            connect(prefs.serverIp, prefs.serverPort, prefs.protocol).isSuccess
        } else false
    }
}