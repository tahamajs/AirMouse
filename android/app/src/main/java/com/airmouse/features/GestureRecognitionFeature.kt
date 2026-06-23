
package com.airmouse.features

import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureEvent
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType
import com.airmouse.domain.usecase.DetectGestureUseCase
import com.airmouse.domain.usecase.ManageGestureTemplatesUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GestureRecognitionFeature @Inject constructor(
    private val detectGestureUseCase: DetectGestureUseCase,
    private val manageGestureTemplatesUseCase: ManageGestureTemplatesUseCase
) {

    data class GestureRecognitionState(
        val currentGesture: GestureEvent? = null,
        val confidence: Float = 0f,
        val isDetecting: Boolean = false,
        val templates: List<CustomGestureTemplate> = emptyList(),
        val stats: GestureTrainingStats = GestureTrainingStats(),
        val totalDetections: Int = 0
    )

    private val _state = MutableStateFlow(GestureRecognitionState())
    val state: StateFlow<GestureRecognitionState> = _state.asStateFlow()

    private val _lastGesture = MutableStateFlow<GestureEvent?>(null)
    val lastGesture: StateFlow<GestureEvent?> = _lastGesture.asStateFlow()

    private val _gestureHistory = MutableStateFlow<List<GestureEvent>>(emptyList())
    val gestureHistory: StateFlow<List<GestureEvent>> = _gestureHistory.asStateFlow()

    suspend fun detectGesture(sensorData: FloatArray): Result<GestureEvent> {
        _state.value = _state.value.copy(isDetecting = true)

        val result = detectGestureUseCase(sensorData)

        if (result.isSuccess) {
            val gesture = result.getOrNull()
            if (gesture != null && gesture.type != GestureType.NONE) {
                _lastGesture.value = gesture
                _state.value = _state.value.copy(
                    currentGesture = gesture,
                    confidence = gesture.confidence,
                    totalDetections = _state.value.totalDetections + 1
                )
                addToHistory(gesture)
            }
        }

        _state.value = _state.value.copy(isDetecting = false)
        return result
    }

    suspend fun detectFromMotion(dx: Float, dy: Float): GestureType {
        return detectGestureUseCase.detectFromMotion(dx, dy)
    }

    suspend fun getCurrentGesture(): GestureEvent? {
        return _lastGesture.value
    }

    suspend fun getGestureStats(): GestureTrainingStats {
        return manageGestureTemplatesUseCase.getGestureStats()
    }

    suspend fun addTemplate(template: CustomGestureTemplate): Result<String> {
        val result = manageGestureTemplatesUseCase(template)
        if (result.isSuccess) {
            refreshTemplates()
        }
        return result
    }

    suspend fun updateTemplate(template: CustomGestureTemplate): Result<Unit> {
        val result = manageGestureTemplatesUseCase.updateTemplate(template)
        if (result.isSuccess) {
            refreshTemplates()
        }
        return result
    }

    suspend fun deleteTemplate(id: String): Result<Unit> {
        val result = manageGestureTemplatesUseCase.deleteTemplate(id)
        if (result.isSuccess) {
            refreshTemplates()
        }
        return result
    }

    suspend fun getAllTemplates(): List<CustomGestureTemplate> {
        return manageGestureTemplatesUseCase.getAllTemplates()
    }

    fun observeTemplates(): Flow<List<CustomGestureTemplate>> {
        return manageGestureTemplatesUseCase.observeTemplates()
    }

    suspend fun toggleTemplate(id: String, enabled: Boolean): Result<Unit> {
        return manageGestureTemplatesUseCase.toggleTemplate(id, enabled)
    }

    suspend fun trainTemplate(gestureName: String, samples: List<FloatArray>): Result<Boolean> {
        return manageGestureTemplatesUseCase.trainTemplate(gestureName, samples)
    }

    suspend fun resetTraining() {
        
    }

    private suspend fun refreshTemplates() {
        val templates = manageGestureTemplatesUseCase.getAllTemplates()
        _state.value = _state.value.copy(templates = templates)
    }

    private fun addToHistory(gesture: GestureEvent) {
        val history = _gestureHistory.value.toMutableList()
        history.add(gesture)
        if (history.size > 50) {
            history.removeAt(0)
        }
        _gestureHistory.value = history
    }

    suspend fun clearHistory() {
        _gestureHistory.value = emptyList()
    }

    suspend fun getConfidenceThreshold(): Float {
        return manageGestureTemplatesUseCase.getConfidenceThreshold()
    }

    suspend fun setConfidenceThreshold(threshold: Float) {
        manageGestureTemplatesUseCase.setConfidenceThreshold(threshold)
    }

    suspend fun getCooldownMs(): Long {
        return manageGestureTemplatesUseCase.getCooldownMs()
    }

    suspend fun setCooldownMs(cooldown: Long) {
        manageGestureTemplatesUseCase.setCooldownMs(cooldown)
    }
}