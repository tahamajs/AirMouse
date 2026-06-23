
package com.airmouse.features

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.usecase.CalibrationUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationFeature @Inject constructor(
    private val calibrationUseCase: CalibrationUseCase
) {

    data class CalibrationFeatureState(
        val status: CalibrationStatus = CalibrationStatus.NOT_STARTED,
        val progress: Int = 0,
        val quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
        val isCalibrated: Boolean = false,
        val currentStep: String = "",
        val error: String? = null
    )

    private val _state = MutableStateFlow(CalibrationFeatureState())
    val state: StateFlow<CalibrationFeatureState> = _state.asStateFlow()

    private val _gyroProgress = MutableStateFlow(0)
    val gyroProgress: StateFlow<Int> = _gyroProgress.asStateFlow()

    private val _magProgress = MutableStateFlow(0)
    val magProgress: StateFlow<Int> = _magProgress.asStateFlow()

    private val _accelProgress = MutableStateFlow(0)
    val accelProgress: StateFlow<Int> = _accelProgress.asStateFlow()

    init {
        observeStatus()
    }

    private fun observeStatus() {
        
    }

    suspend fun startFullCalibration(onProgress: (Int) -> Unit): Result<Boolean> {
        _state.value = _state.value.copy(status = CalibrationStatus.IN_PROGRESS)

        val result = calibrationUseCase.startFullCalibration { progress ->
            onProgress(progress)
            _state.value = _state.value.copy(
                progress = progress,
                status = when {
                    progress < 33 -> CalibrationStatus.GYRO_COMPLETE
                    progress < 66 -> CalibrationStatus.MAG_COMPLETE
                    progress < 100 -> CalibrationStatus.ACCEL_COMPLETE
                    else -> CalibrationStatus.COMPLETED
                }
            )
        }

        if (result.isSuccess) {
            _state.value = _state.value.copy(
                isCalibrated = true,
                status = CalibrationStatus.COMPLETED
            )
        } else {
            _state.value = _state.value.copy(
                status = CalibrationStatus.FAILED,
                error = result.exceptionOrNull()?.message
            )
        }

        return result
    }

    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Result<Boolean> {
        _state.value = _state.value.copy(status = CalibrationStatus.IN_PROGRESS)

        val result = calibrationUseCase.calibrateGyroscope { progress ->
            onProgress(progress)
            _gyroProgress.value = progress
        }

        if (result.isSuccess) {
            _state.value = _state.value.copy(status = CalibrationStatus.GYRO_COMPLETE)
        }
        return result
    }

    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Result<Boolean> {
        _state.value = _state.value.copy(status = CalibrationStatus.IN_PROGRESS)

        val result = calibrationUseCase.calibrateMagnetometer { progress ->
            onProgress(progress)
            _magProgress.value = progress
        }

        if (result.isSuccess) {
            _state.value = _state.value.copy(status = CalibrationStatus.MAG_COMPLETE)
        }
        return result
    }

    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Result<Boolean> {
        _state.value = _state.value.copy(status = CalibrationStatus.IN_PROGRESS)

        val result = calibrationUseCase.calibrateAccelerometer { instruction ->
            onInstruction(instruction)
            _state.value = _state.value.copy(currentStep = instruction)
        }

        if (result.isSuccess) {
            _state.value = _state.value.copy(status = CalibrationStatus.ACCEL_COMPLETE)
        }
        return result
    }

    suspend fun getCalibrationStatus(): CalibrationStatus {
        return calibrationUseCase.getCalibrationStatus()
    }

    fun observeCalibrationStatus(): Flow<CalibrationStatus> {
        return calibrationUseCase.observeCalibrationStatus()
    }

    suspend fun getCalibrationQuality(): CalibrationQuality {
        return calibrationUseCase.getCalibrationQuality()
    }

    fun observeCalibrationQuality(): Flow<CalibrationQuality> {
        return calibrationUseCase.observeCalibrationQuality()
    }

    suspend fun getCalibrationData(): CalibrationData {
        return calibrationUseCase.getCalibrationData()
    }

    suspend fun resetCalibration(): Result<Unit> {
        val result = calibrationUseCase.resetCalibration()
        if (result.isSuccess) {
            _state.value = CalibrationFeatureState()
            _gyroProgress.value = 0
            _magProgress.value = 0
            _accelProgress.value = 0
        }
        return result
    }

    suspend fun isCalibrated(): Boolean {
        return calibrationUseCase.isCalibrated()
    }

    suspend fun getProgress(): Int {
        return _state.value.progress
    }

    suspend fun getCurrentStep(): String {
        return _state.value.currentStep
    }

    fun getCalibrationFeatureState(): CalibrationFeatureState = _state.value
}
