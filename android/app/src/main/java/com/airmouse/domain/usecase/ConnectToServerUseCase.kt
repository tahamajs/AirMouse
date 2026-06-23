
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ConnectToServerUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    suspend operator fun invoke(config: ConnectionConfig): Result<Boolean> {
        return try {
            val result = connectionRepository.connect(config)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun connect(
        ip: String,
        port: Int = ConnectionConfig.DEFAULT_WEBSOCKET_PORT,
        protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET
    ): Result<Boolean> {
        val config = ConnectionConfig(
            ip = ip,
            port = port,
            protocol = protocol
        ).normalized()
        return invoke(config)
    }

    suspend fun connectToLastServer(): Result<Boolean> {
        return try {
            val config = connectionRepository.getConnectionConfig()
            val result = connectionRepository.connect(config)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return connectionRepository.observeConnectionStatus()
    }

    suspend fun getConnectionStatus(): ConnectionStatus {
        return connectionRepository.getConnectionStatus()
    }

    suspend fun disconnect() {
        connectionRepository.disconnect()
    }

    suspend fun reconnect(): Result<Boolean> {
        return try {
            val result = connectionRepository.reconnect()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isConnected(): Boolean {
        return connectionRepository.getConnectionStatus() == ConnectionStatus.CONNECTED
    }
}
