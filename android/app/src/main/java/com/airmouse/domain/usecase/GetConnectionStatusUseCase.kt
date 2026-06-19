// app/src/main/java/com/airmouse/domain/usecase/GetConnectionStatusUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting connection status
 */
class GetConnectionStatusUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Get current connection status
     */
    suspend operator fun invoke(): ConnectionStatus {
        return connectionRepository.getConnectionStatus()
    }

    /**
     * Observe connection status
     */
    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return connectionRepository.observeConnectionStatus()
    }

    /**
     * Check if connected
     */
    suspend fun isConnected(): Boolean {
        return connectionRepository.getConnectionStatus() == ConnectionStatus.CONNECTED
    }

    /**
     * Get connection quality
     */
    suspend fun getConnectionQuality(): ConnectionQuality {
        return connectionRepository.getConnectionQuality()
    }

    /**
     * Observe connection quality
     */
    fun observeConnectionQuality(): Flow<ConnectionQuality> {
        return connectionRepository.observeConnectionQuality()
    }

    /**
     * Get connection config
     */
    suspend fun getConnectionConfig(): ConnectionConfig {
        return connectionRepository.getConnectionConfig()
    }
}
