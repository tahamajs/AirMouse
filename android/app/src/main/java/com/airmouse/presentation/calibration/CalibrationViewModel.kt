// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt
package com.airmouse.presentation.ui.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.features.CalibrationFeature
import com.airmouse.features.SensorFeature
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationFeature: CalibrationFeature,
    private val sensorFeature: SensorFeature
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private val _gyroProgress = MutableStateFlow(0)
    val gyroProgress: StateFlow<Int> = _gyroProgress.asStateFlow()

    private val _magProgress = MutableStateFlow(0)
    val magProgress: StateFlow<Int> = _magProgress.asStateFlow()

    private val _accelProgress = MutableStateFlow(0)
    val accelProgress: StateFlow<Int> = _accelProgress.asStateFlow()

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private var calibrationJob: kotlinx.coroutines.Job? = null

    init {
        observeCalibrationState()
        observeSensorData()
        loadInitialCalibrationStatus()
    }

    private fun observeCalibrationState() {
        viewModelScope.launch {
            calibrationFeature.state.collect { state ->
                _uiState.update { current ->
                    current.copy(
                        progress = state.progress,
                        statusMessage = when (state.status) {
                            CalibrationStatus.NOT_STARTED -> "Ready to calibrate"
                            CalibrationStatus.IN_PROGRESS -> "Calibrating..."
                            CalibrationStatus.GYRO_COMPLETE -> "Gyroscope calibrated ✓"
                            CalibrationStatus.MAG_COMPLETE -> "Magnetometer calibrated ✓"
                            CalibrationStatus.ACCEL_COMPLETE -> "Accelerometer calibrated ✓"
                            CalibrationStatus.COMPLETED -> "Calibration complete!"
                            CalibrationStatus.FAILED -> "Calibration failed"
                            CalibrationStatus.SKIPPED -> "Calibration skipped"
                        },
                        isComplete = state.status == CalibrationStatus.COMPLETED,
                        isCollecting = state.status == CalibrationStatus.IN_PROGRESS,
                        calibrationQuality = state.quality.name,
                        quality = state.quality.name,
                        calibrationData = CalibrationData(
                            gyroBias = Triple(0f, 0f, 0f),
                            accelBias = Triple(0f, 0f, 0f),
                            magBias = Triple(0f, 0f, 0f)
                        ),
                        errorMessage = state.error,
                        currentStep = when (state.status) {
                            CalibrationStatus.GYRO_COMPLETE -> 2
                            CalibrationStatus.MAG_COMPLETE -> 3
                            CalibrationStatus.ACCEL_COMPLETE -> 4
                            CalibrationStatus.COMPLETED -> 4
                            else -> 1
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            calibrationFeature.gyroProgress.collect { progress ->
                _gyroProgress.value = progress
                _uiState.update { it.copy(progress = progress) }
            }
        }

        viewModelScope.launch {
            calibrationFeature.magProgress.collect { progress ->
                _magProgress.value = progress
            }
        }

        viewModelScope.launch {
            calibrationFeature.accelProgress.collect { progress ->
                _accelProgress.value = progress
            }
        }
    }

    private fun observeSensorData() {
        viewModelScope.launch {
            sensorFeature.observeOrientation().collect { orientation ->
                _uiState.update { current ->
                    current.copy(
                        roll = orientation.roll,
                        pitch = orientation.pitch,
                        yaw = orientation.yaw
                    )
                }
            }
        }

        viewModelScope.launch {
            sensorFeature.observeSensorData().collect { data ->
                _uiState.update { current ->
                    current.copy(
                        gyroData = Triple(data.gyroX, data.gyroY, data.gyroZ),
                        accelData = Triple(data.accelX, data.accelY, data.accelZ),
                        magData = Triple(data.magX, data.magY, data.magZ)
                    )
                }
            }
        }
    }

    private fun loadInitialCalibrationStatus() {
        viewModelScope.launch {
            val isCalibrated = calibrationFeature.isCalibrated()
            if (isCalibrated) {
                val quality = calibrationFeature.getCalibrationQuality()
                _uiState.update {
                    it.copy(
                        isComplete = true,
                        calibrationQuality = quality.name,
                        quality = quality.name,
                        statusMessage = "Calibration complete!"
                    )
                }
            }
        }
    }

    fun startFullCalibration() {
        if (_isCalibrating.value) return

        _isCalibrating.value = true
        _uiState.update {
            it.copy(
                isCollecting = true,
                statusMessage = "Starting calibration...",
                errorMessage = null
            )
        }

        calibrationJob = viewModelScope.launch {
            val result = calibrationFeature.startFullCalibration { progress ->
                _uiState.update { current ->
                    current.copy(
                        progress = progress,
                        samplesCollected = (progress * _uiState.value.totalSamplesNeeded / 100)
                    )
                }
            }

            _isCalibrating.value = false

            if (result.isSuccess) {
                val quality = calibrationFeature.getCalibrationQuality()
                _uiState.update {
                    it.copy(
                        isComplete = true,
                        calibrationQuality = quality.name,
                        quality = quality.name,
                        statusMessage = "Calibration complete!",
                        isCollecting = false
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCollecting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Calibration failed"
                    )
                }
            }
        }
    }

    fun startGyroCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _uiState.update { it.copy(isCollecting = true, statusMessage = "Calibrating gyroscope...") }

            val result = calibrationFeature.calibrateGyroscope { progress ->
                _gyroProgress.value = progress
                _uiState.update { current ->
                    current.copy(
                        progress = progress,
                        samplesCollected = (progress * current.totalSamplesNeeded / 100)
                    )
                }
            }

            _isCalibrating.value = false
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Gyroscope calibrated ✓") }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Gyroscope calibration failed",
                        isCollecting = false
                    )
                }
            }
        }
    }

    fun startMagCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _uiState.update { it.copy(isCollecting = true, statusMessage = "Calibrating magnetometer...") }

            val result = calibrationFeature.calibrateMagnetometer { progress ->
                _magProgress.value = progress
                _uiState.update { current ->
                    current.copy(
                        progress = progress,
                        samplesCollected = (progress * current.totalSamplesNeeded / 100)
                    )
                }
            }

            _isCalibrating.value = false
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Magnetometer calibrated ✓") }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Magnetometer calibration failed",
                        isCollecting = false
                    )
                }
            }
        }
    }

    fun startAccelCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _uiState.update { it.copy(isCollecting = true, statusMessage = "Calibrating accelerometer...") }

            val result = calibrationFeature.calibrateAccelerometer { instruction ->
                _uiState.update { it.copy(stepInstruction = instruction) }
            }

            _isCalibrating.value = false
            if (result.isSuccess) {
                _uiState.update { it.copy(statusMessage = "Accelerometer calibrated ✓") }
            } else {
                _uiState.update {
                    it.copy(
                        errorMessage = result.exceptionOrNull()?.message ?: "Accelerometer calibration failed",
                        isCollecting = false
                    )
                }
            }
        }
    }

    fun selectPosition(index: Int) {
        _uiState.update {
            it.copy(
                currentPosition = index,
                statusMessage = "Position ${index + 1} selected"
            )
        }
    }

    fun resetCalibration() {
        viewModelScope.launch {
            calibrationJob?.cancel()
            _isCalibrating.value = false
            calibrationFeature.resetCalibration()
            _uiState.value = CalibrationUiState()
            _gyroProgress.value = 0
            _magProgress.value = 0
            _accelProgress.value = 0
        }
    }

    fun skipCalibration() {
        viewModelScope.launch {
            calibrationJob?.cancel()
            _isCalibrating.value = false
            _uiState.update {
                it.copy(
                    isSkipped = true,
                    isComplete = true,
                    isCollecting = false,
                    statusMessage = "Calibration skipped",
                    calibrationQuality = "SKIPPED"
                )
            }
        }
    }

    fun retryCalibration() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                isCollecting = false,
                isComplete = false
            )
        }
        startFullCalibration()
    }

    override fun onCleared() {
        super.onCleared()
        calibrationJob?.cancel()
    }
}