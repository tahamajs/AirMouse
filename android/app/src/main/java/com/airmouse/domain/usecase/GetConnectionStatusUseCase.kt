package com.airmouse.domain.usecase

import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.repository.IConnectionRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case to observe the current connection status.
 */
class GetConnectionStatusUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {
    operator fun invoke(): Flow<ConnectionStatus> =
        connectionRepository.connectionStatus()
}