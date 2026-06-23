
package com.airmouse.domain.usecase

import com.airmouse.domain.model.ProximityConfig
import com.airmouse.domain.repository.IProximityRepository
import javax.inject.Inject

class UpdateProximityConfigUseCase @Inject constructor(
    private val proximityRepository: IProximityRepository
) {

    suspend operator fun invoke(config: ProximityConfig): Result<Unit> {
        return try {
            proximityRepository.updateConfig(config)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun setDeviceAddress(address: String): Result<Unit> {
        return try {
            proximityRepository.setDeviceAddress(address)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateThresholds(near: Float, far: Float): Result<Unit> {
        return try {
            val config = proximityRepository.getConfig()
            proximityRepository.updateConfig(config.copy(
                nearThreshold = near,
                farThreshold = far
            ))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun toggleProximity(enabled: Boolean): Result<Unit> {
        return try {
            val config = proximityRepository.getConfig()
            proximityRepository.updateConfig(config.copy(enabled = enabled))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calibrate(): Result<Boolean> {
        return try {
            val result = proximityRepository.calibrate()
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}