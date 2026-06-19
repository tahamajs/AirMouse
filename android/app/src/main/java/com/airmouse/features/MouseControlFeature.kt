// app/src/main/java/com/airmouse/features/MouseControlFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.MouseButton
import com.airmouse.domain.model.MouseEvent
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.usecase.SendMovementUseCase
import com.airmouse.domain.repository.IMouseRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class MouseControlFeature @Inject constructor(
    private val sendMovementUseCase: SendMovementUseCase,
    private val mouseRepository: IMouseRepository
) {

    private val _isPaused = MutableStateFlow(false)
    val isPaused: StateFlow<Boolean> = _isPaused.asStateFlow()

    private val _currentSpeed = MutableStateFlow(0f)
    val currentSpeed: StateFlow<Float> = _currentSpeed.asStateFlow()

    data class MouseControlState(
        val isPaused: Boolean = false,
        val currentSpeed: Float = 0f,
        val sensitivity: Float = 1.0f,
        val smoothingEnabled: Boolean = true,
        val accelerationEnabled: Boolean = true,
        val totalClicks: Int = 0,
        val totalMovements: Int = 0
    )

    private val _state = MutableStateFlow(MouseControlState())
    val state: StateFlow<MouseControlState> = _state.asStateFlow()

    suspend fun move(dx: Float, dy: Float): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }

        // Update speed
        val speed = kotlin.math.sqrt(dx * dx + dy * dy)
        _currentSpeed.value = speed

        val result = sendMovementUseCase(dx, dy)
        if (result.isSuccess) {
            // Update state
            _state.value = _state.value.copy(
                currentSpeed = speed
            )
        }
        return result
    }

    suspend fun moveSmooth(points: List<Pair<Float, Float>>, durationMs: Int = 100): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }
        return sendMovementUseCase.sendSmoothMovement(points, durationMs)
    }

    suspend fun click(button: MouseButton = MouseButton.LEFT): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }
        return sendMovementUseCase.sendClick(button)
    }

    suspend fun doubleClick(): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }
        return sendMovementUseCase.sendDoubleClick()
    }

    suspend fun rightClick(): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }
        return sendMovementUseCase.sendRightClick()
    }

    suspend fun scroll(delta: Int): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }
        return sendMovementUseCase.sendScroll(delta)
    }

    suspend fun sendGesture(gesture: String, confidence: Float): Result<Boolean> {
        if (_isPaused.value) {
            return Result.success(false)
        }
        return sendMovementUseCase.sendGesture(gesture, confidence)
    }

    suspend fun pauseMovement(): Result<Boolean> {
        val result = sendMovementUseCase.pauseMovement()
        if (result.isSuccess) {
            _isPaused.value = true
            _state.value = _state.value.copy(isPaused = true)
        }
        return result
    }

    suspend fun resumeMovement(): Result<Boolean> {
        val result = sendMovementUseCase.resumeMovement()
        if (result.isSuccess) {
            _isPaused.value = false
            _state.value = _state.value.copy(isPaused = false)
        }
        return result
    }

    suspend fun getMovementProfile(): MovementProfile {
        return mouseRepository.getMovementProfile()
    }

    suspend fun setMovementProfile(profile: MovementProfile) {
        mouseRepository.setMovementProfile(profile)
        _state.value = _state.value.copy(
            sensitivity = profile.sensitivity,
            smoothingEnabled = profile.smoothingEnabled,
            accelerationEnabled = profile.accelerationEnabled
        )
    }

    suspend fun getStatistics(): MouseStatistics {
        return mouseRepository.getStatistics()
    }

    fun observeMouseEvents(): Flow<MouseEvent> {
        return mouseRepository.observeMouseEvents()
    }

    suspend fun resetStatistics() {
        mouseRepository.resetStatistics()
    }

    suspend fun getPosition(): Pair<Int, Int> {
        return mouseRepository.getPosition()
    }

    suspend fun setPosition(x: Int, y: Int): Boolean {
        return mouseRepository.setPosition(x, y)
    }

    fun isPaused(): Boolean = _isPaused.value

    fun getCurrentSpeed(): Float = _currentSpeed.value

    suspend fun togglePause(): Boolean {
        return if (_isPaused.value) {
            resumeMovement().isSuccess
        } else {
            pauseMovement().isSuccess
        }
    }
}
