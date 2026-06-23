
package com.airmouse.domain.usecase

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.repository.IGestureRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class ManageGestureTemplatesUseCase @Inject constructor(
    private val gestureRepository: IGestureRepository
) {

    suspend operator fun invoke(template: CustomGestureTemplate): Result<String> {
        return try {
            val id = gestureRepository.addCustomGesture(template)
            Result.success(id)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun updateTemplate(template: CustomGestureTemplate): Result<Unit> {
        return try {
            gestureRepository.updateCustomGesture(template)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun deleteTemplate(id: String): Result<Unit> {
        return try {
            gestureRepository.deleteCustomGesture(id)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getAllTemplates(): List<CustomGestureTemplate> {
        return gestureRepository.getAllCustomGestures()
    }

    suspend fun getTemplate(id: String): CustomGestureTemplate? {
        return gestureRepository.getCustomGesture(id)
    }

    fun observeTemplates(): Flow<List<CustomGestureTemplate>> {
        return gestureRepository.observeCustomGestures()
    }

    suspend fun toggleTemplate(id: String, enabled: Boolean): Result<Unit> {
        return try {
            val template = gestureRepository.getCustomGesture(id) ?: return Result.failure(Exception("Template not found"))
            gestureRepository.updateCustomGesture(template.copy(isEnabled = enabled))
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun trainTemplate(gestureName: String, samples: List<FloatArray>): Result<Boolean> {
        return try {
            val result = gestureRepository.trainGesture(gestureName, samples)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getGestureStats() = gestureRepository.getGestureStats()

    suspend fun getConfidenceThreshold(): Float = gestureRepository.getConfidenceThreshold()

    suspend fun setConfidenceThreshold(threshold: Float) {
        gestureRepository.setConfidenceThreshold(threshold)
    }

    suspend fun getCooldownMs(): Long = gestureRepository.getCooldownMs()

    suspend fun setCooldownMs(cooldown: Long) {
        gestureRepository.setCooldownMs(cooldown)
    }
}
