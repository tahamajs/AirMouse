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

    // ==========================================
    // STATE FLOWS
    // ==========================================

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

    // Accelerometer positions
    private val accelPositions = listOf(
        "Flat facing UP",
        "Flat facing DOWN",
        "Left side",
        "Right side",
        "Top edge",
        "Bottom edge"
    )

    // ==========================================
    // INIT
    // ==========================================

    init {
        observeCalibrationState()
        observeSensorData()
        loadInitialCalibrationStatus()
    }

    // ==========================================
    // OBSERVERS
    // ==========================================

    private fun observeCalibrationState() {
        viewModelScope.launch {
            calibrationFeature.state.collect { state ->
                val statusMessage = when (state.status) {
                    CalibrationStatus.NOT_STARTED -> "Ready to calibrate"
                    CalibrationStatus.IN_PROGRESS -> "Calibrating..."
                    CalibrationStatus.GYRO_COMPLETE -> "Gyroscope calibrated ✓"
                    CalibrationStatus.MAG_COMPLETE -> "Magnetometer calibrated ✓"
                    CalibrationStatus.ACCEL_COMPLETE -> "Accelerometer calibrated ✓"
                    CalibrationStatus.COMPLETED -> "Calibration complete!"
                    CalibrationStatus.FAILED -> "Calibration failed"
                    CalibrationStatus.SKIPPED -> "Calibration skipped"
                    CalibrationStatus.IDLE -> "Ready to calibrate"
                }

                val currentStep = when (state.status) {
                    CalibrationStatus.NOT_STARTED -> 1
                    CalibrationStatus.IDLE -> 1
                    CalibrationStatus.IN_PROGRESS -> 1 // will be updated separately
                    CalibrationStatus.GYRO_COMPLETE -> 2
                    CalibrationStatus.MAG_COMPLETE -> 3
                    CalibrationStatus.ACCEL_COMPLETE -> 4
                    CalibrationStatus.COMPLETED -> 4
                    CalibrationStatus.FAILED -> 1
                    CalibrationStatus.SKIPPED -> 4
                }

                _uiState.update { current ->
                    current.copy(
                        progress = state.progress,
                        statusMessage = statusMessage,
                        isComplete = state.status == CalibrationStatus.COMPLETED,
                        isCollecting = state.status == CalibrationStatus.IN_PROGRESS,
                        calibrationQuality = state.quality.name,
                        quality = state.quality.name,
                        errorMessage = state.error,
                        currentStep = currentStep,
                        isSkipped = state.status == CalibrationStatus.SKIPPED
                    )
                }
            }
        }

        viewModelScope.launch {
            calibrationFeature.gyroProgress.collect { progress ->
                _gyroProgress.value = progress
                // Update total progress (gyro part = 33%)
                val totalProgress = (progress * 33) / 100
                _uiState.update {
                    it.copy(
                        progress = totalProgress,
                        samplesCollected = (progress * 100 / 500) // assuming 500 samples
                    )
                }
            }
        }

        viewModelScope.launch {
            calibrationFeature.magProgress.collect { progress ->
                _magProgress.value = progress
                val totalProgress = 33 + (progress * 33) / 100
                _uiState.update {
                    it.copy(
                        progress = totalProgress,
                        samplesCollected = (progress * 100 / 300) // assuming 300 samples
                    )
                }
            }
        }

        viewModelScope.launch {
            calibrationFeature.accelProgress.collect { progress ->
                _accelProgress.value = progress
                val totalProgress = 66 + (progress * 34) / 100
                _uiState.update {
                    it.copy(
                        progress = totalProgress,
                        currentPosition = (progress * 6 / 100) // 6 positions
                    )
                }
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
                        statusMessage = "Calibration complete!",
                        currentStep = 4
                    )
                }
            }
        }
    }

    // ==========================================
    // PUBLIC ACTIONS
    // ==========================================

    fun startFullCalibration() {
        if (_isCalibrating.value) return

        _isCalibrating.value = true
        _uiState.update {
            it.copy(
                isCollecting = true,
                statusMessage = "Starting calibration...",
                errorMessage = null,
                isComplete = false,
                isSkipped = false,
                currentStep = 1,
                progress = 0
            )
        }

        calibrationJob = viewModelScope.launch {
            val result = calibrationFeature.startFullCalibration { progress ->
                // progress is overall 0-100
                _uiState.update { current ->
                    current.copy(
                        progress = progress,
                        statusMessage = when {
                            progress < 33 -> "Calibrating gyroscope..."
                            progress < 66 -> "Calibrating magnetometer..."
                            progress < 100 -> "Calibrating accelerometer..."
                            else -> "Finalizing..."
                        }
                    )
                }
            }

            _isCalibrating.value = false

            if (result.isSuccess) {
                val quality = calibrationFeature.getCalibrationQuality()
                val data = calibrationFeature.getCalibrationData()
                _uiState.update {
                    it.copy(
                        isComplete = true,
                        isCollecting = false,
                        calibrationQuality = quality.name,
                        quality = quality.name,
                        calibrationData = data,
                        statusMessage = "Calibration complete!",
                        progress = 100,
                        currentStep = 4
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCollecting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Calibration failed",
                        statusMessage = "Calibration failed"
                    )
                }
            }
        }
    }

    fun startGyroCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _uiState.update {
                it.copy(
                    isCollecting = true,
                    statusMessage = "Calibrating gyroscope...",
                    errorMessage = null,
                    currentStep = 1,
                    totalSamplesNeeded = 500,
                    samplesCollected = 0
                )
            }

            val result = calibrationFeature.calibrateGyroscope { progress ->
                _gyroProgress.value = progress
                _uiState.update { current ->
                    current.copy(
                        progress = (progress * 33 / 100),
                        samplesCollected = (progress * 500 / 100)
                    )
                }
            }

            _isCalibrating.value = false
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        statusMessage = "Gyroscope calibrated ✓",
                        currentStep = 2
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCollecting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Gyroscope calibration failed",
                        statusMessage = "Gyroscope calibration failed"
                    )
                }
            }
        }
    }

    fun startMagCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _uiState.update {
                it.copy(
                    isCollecting = true,
                    statusMessage = "Calibrating magnetometer...",
                    errorMessage = null,
                    currentStep = 2,
                    totalSamplesNeeded = 300,
                    samplesCollected = 0
                )
            }

            val result = calibrationFeature.calibrateMagnetometer { progress ->
                _magProgress.value = progress
                _uiState.update { current ->
                    current.copy(
                        progress = 33 + (progress * 33 / 100),
                        samplesCollected = (progress * 300 / 100)
                    )
                }
            }

            _isCalibrating.value = false
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        statusMessage = "Magnetometer calibrated ✓",
                        currentStep = 3
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCollecting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Magnetometer calibration failed",
                        statusMessage = "Magnetometer calibration failed"
                    )
                }
            }
        }
    }

    fun startAccelCalibration() {
        viewModelScope.launch {
            _isCalibrating.value = true
            _uiState.update {
                it.copy(
                    isCollecting = true,
                    statusMessage = "Calibrating accelerometer...",
                    errorMessage = null,
                    currentStep = 3,
                    totalSamplesNeeded = 6,
                    samplesCollected = 0,
                    currentPosition = 0
                )
            }

            val result = calibrationFeature.calibrateAccelerometer { instruction ->
                // instruction is the position name
                val positionIndex = accelPositions.indexOf(instruction)
                _uiState.update { current ->
                    current.copy(
                        stepInstruction = instruction,
                        currentPosition = if (positionIndex >= 0) positionIndex else current.currentPosition,
                        samplesCollected = current.samplesCollected + 1
                    )
                }
                // Update progress (each step = ~16.6%)
                val stepProgress = ((_uiState.value.samplesCollected.toFloat() / 6) * 100).toInt()
                _uiState.update { current ->
                    current.copy(
                        progress = 66 + (stepProgress * 34 / 100)
                    )
                }
            }

            _isCalibrating.value = false
            if (result.isSuccess) {
                _uiState.update {
                    it.copy(
                        statusMessage = "Accelerometer calibrated ✓",
                        currentStep = 4,
                        isComplete = true,
                        isCollecting = false,
                        progress = 100
                    )
                }
                // Save final calibration data
                val quality = calibrationFeature.getCalibrationQuality()
                val data = calibrationFeature.getCalibrationData()
                _uiState.update {
                    it.copy(
                        calibrationQuality = quality.name,
                        quality = quality.name,
                        calibrationData = data,
                        statusMessage = "Calibration complete!"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCollecting = false,
                        errorMessage = result.exceptionOrNull()?.message ?: "Accelerometer calibration failed",
                        statusMessage = "Accelerometer calibration failed"
                    )
                }
            }
        }
    }

    fun selectPosition(index: Int) {
        if (index in accelPositions.indices) {
            _uiState.update {
                it.copy(
                    currentPosition = index,
                    stepInstruction = accelPositions[index],
                    statusMessage = "Position ${index + 1} of ${accelPositions.size}"
                )
            }
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
                    calibrationQuality = "SKIPPED",
                    quality = "SKIPPED",
                    progress = 100,
                    currentStep = 4
                )
            }
        }
    }

    fun retryCalibration() {
        _uiState.update {
            it.copy(
                errorMessage = null,
                isCollecting = false,
                isComplete = false,
                isSkipped = false,
                statusMessage = "Retrying calibration...",
                progress = 0,
                currentStep = 1
            )
        }
        startFullCalibration()
    }

    override fun onCleared() {
        super.onCleared()
        calibrationJob?.cancel()
    }
}