
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetConnectionStatusUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    suspend operator fun invoke(): ConnectionStatus {
        return connectionRepository.getConnectionStatus()
    }

    fun observeConnectionStatus(): Flow<ConnectionStatus> {
        return connectionRepository.observeConnectionStatus()
    }

    suspend fun isConnected(): Boolean {
        return connectionRepository.getConnectionStatus() == ConnectionStatus.CONNECTED
    }

    suspend fun getConnectionQuality(): ConnectionQuality {
        return connectionRepository.getConnectionQuality()
    }

    fun observeConnectionQuality(): Flow<ConnectionQuality> {
        return connectionRepository.observeConnectionQuality()
    }

    suspend fun getConnectionConfig(): ConnectionConfig {
        return connectionRepository.getConnectionConfig()
    }
}
