
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ProximityState
import com.airmouse.domain.repository.IProximityRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetProximityStateUseCase @Inject constructor(
    private val proximityRepository: IProximityRepository
) {

    suspend operator fun invoke(): ProximityState {
        return proximityRepository.getProximityState()
    }

    fun observeProximityState(): Flow<ProximityState> {
        return proximityRepository.observeProximityState()
    }

    suspend fun isDeviceNear(): Boolean {
        return proximityRepository.getProximityState().isNear
    }

    suspend fun getCurrentDistance(): Float {
        return proximityRepository.getProximityState().distance
    }

    suspend fun isProximityEnabled(): Boolean {
        return proximityRepository.getConfig().enabled
    }

    suspend fun startMonitoring(): Result<Unit> {
        return try {
            proximityRepository.startMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun stopMonitoring(): Result<Unit> {
        return try {
            proximityRepository.stopMonitoring()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}