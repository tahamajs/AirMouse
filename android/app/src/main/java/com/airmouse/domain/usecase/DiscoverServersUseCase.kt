
package com.airmouse.domain.usecase

import com.airmouse.domain.model.DiscoveredServer
import com.airmouse.domain.repository.IConnectionRepository
import javax.inject.Inject

class DiscoverServersUseCase @Inject constructor(
    private val connectionRepository: IConnectionRepository
) {
    private var discovering = false

    suspend operator fun invoke(): List<DiscoveredServer> {
        return connectionRepository.discoverServers()
    }

    suspend fun startDiscovery(onServerFound: (DiscoveredServer) -> Unit): Result<Unit> {
        return try {
            discovering = true
            connectionRepository.startDiscovery { server ->
                onServerFound(server)
            }
            Result.success(Unit)
        } catch (e: Exception) {
            discovering = false
            Result.failure(e)
        }
    }

    suspend fun stopDiscovery(): Result<Unit> {
        return try {
            connectionRepository.stopDiscovery()
            discovering = false
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getDiscoveredServers(): List<DiscoveredServer> {
        return connectionRepository.discoverServers()
    }

    suspend fun isDiscovering(): Boolean {
        return discovering
    }
}
