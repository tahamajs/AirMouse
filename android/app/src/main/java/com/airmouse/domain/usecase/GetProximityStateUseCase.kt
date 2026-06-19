// app/src/main/java/com/airmouse/domain/usecase/GetProximityStateUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ProximityState
import com.airmouse.domain.repository.IProximityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for getting proximity state
 */
class GetProximityStateUseCase @Inject constructor(
    private val proximityRepository: IProximityRepository
) {

    /**
     * Get current proximity state
     */
    suspend operator fun invoke(): ProximityState {
        return proximityRepository.getProximityState()
    }

    /**
     * Observe proximity state
     */
    fun observeProximityState(): Flow<ProximityState> {
        return proximityRepository.observeProximityState()
    }

    /**
     * Check if device is near
     */
    suspend fun isDeviceNear(): Boolean {
        return proximityRepository.getProximityState().isNear
    }

    /**
     * Get current distance
     */
    suspend fun getCurrentDistance(): Float {
        return proximityRepository.getProximityState().distance
    }

    /**
     * Check if proximity is enabled
     */
    suspend fun isProximityEnabled(): Boolean {
        return proximityRepository.getConfig().enabled
    }

    /**
     * Start monitoring
     */
    suspend fun startMonitoring(): Result<Unit> {
        return try {
            proximityRepository.startMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Stop monitoring
     */
    suspend fun stopMonitoring(): Result<Unit> {
        return try {
            proximityRepository.stopMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}