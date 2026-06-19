// app/src/main/java/com/airmouse/domain/usecase/DiscoverServersUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.domain.repository.IConnectionRepository
import javax.inject.Inject

/**
 * Use case for discovering servers on the network
 */
class DiscoverServersUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {

    /**
     * Discover servers
     */
    suspend operator fun invoke(): List<DiscoveredServer> {
        return connectionRepository.discoverServers()
    }

    /**
     * Start discovery
     */
    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit): Result<Unit> {
        return try {
            connectionRepository.startDiscovery { server ->
                onServerFound(server)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop discovery
     */
    suspend fun stopDiscovery(): Result<Unit> {
        return try {
            connectionRepository.stopDiscovery()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get discovered servers
     */
    suspend fun getDiscoveredServers(): List<DiscoveredServer> {
        return connectionRepository.discoverServers()
    }

    /**
     * Check if discovery is running
     */
    suspend fun isDiscovering(): Boolean {
        // Would need to track discovery state
        return false
    }
}