// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt
package com.airmouse.presentation.ui.calibration

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val dataSource: ICalibrationDataSource,
    private val connectionRepository: IConnectionRepository,
    private val calibrationRepository: ICalibrationRepository,
    private val calibrationUseCase: CalibrationUseCase,
    private val prefs: PreferencesManager
) : ViewModel() {

    companion object {
        private const val TAG = "CalibrationViewModel"
        // Timing constants for 50Hz (20ms delay)
        private const val GYRO_SAMPLES_NEEDED = 250        // 5 seconds
        private const val MAG_SAMPLES_NEEDED = 500         // 10 seconds
        private const val ACCEL_SAMPLES_PER_POS = 100      // 2 seconds per position
        private const val ACCEL_POSITIONS_NEEDED = 6
        private const val TRANSITION_DELAY = 1000L         // 1s delay to show success
    }

    private val _uiState = MutableStateFlow(CalibrationUiState.initial())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationData = MutableStateFlow<CalibrationData?>(null)
    val calibrationData: StateFlow<CalibrationData?> = _calibrationData.asStateFlow()

    private val _calibrationStatus = MutableStateFlow<CalibrationStatus>(CalibrationStatus.NOT_STARTED)
    val calibrationStatus: StateFlow<CalibrationStatus> = _calibrationStatus.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress.asStateFlow()

    private var calibrationJob: Job? = null
    private var gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private var magSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val accelPositions = mutableMapOf<Int, List<Triple<Float, Float, Float>>>()
    
    private val accelPositionsList = listOf(
        "Flat (Screen Up)", "Flat (Screen Down)", "Left Side Down", 
        "Right Side Down", "Top Edge Down", "Bottom Edge Down"
    )

    var sensorDataProvider: (() -> SensorData)? = null

    init {
        loadExistingCalibration()
        observeStatus()
        observeServerConnection()
    }

    private fun observeStatus() {
        viewModelScope.launch {
            calibrationRepository.observeCalibrationStatus().collect { status ->
                _calibrationStatus.value = status
            }
        }
        viewModelScope.launch {
            calibrationRepository.observeCalibrationProgress().collect { progress ->
                _calibrationProgress.value = progress
            }
        }
    }

    private fun observeServerConnection() {
        viewModelScope.launch {
            connectionRepository.observeConnectionStatus().collect { status ->
                _uiState.update { it.copy(isServerConnected = status == ConnectionStatus.CONNECTED) }
            }
        }
    }

    private fun loadExistingCalibration() {
        viewModelScope.launch {
            try {
                val data = calibrationRepository.getCalibrationData()
                if (data.isCalibrated) {
                    _calibrationData.value = data
                    _uiState.update { it.copy(isComplete = true, progress = 100, calibrationData = data) }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load calibration", e)
            }
        }
    }

    fun startCalibration() {
        if (_isCalibrating.value) return
        resetCalibrationState()
        _uiState.update { it.copy(
            currentStep = 1, 
            statusMessage = "Step 1: Gyroscope",
            stepInstruction = "Place device on a flat surface"
        ) }
        startCurrentStep()
    }

    /**
     * Start/Next Step Controller
     */
    fun onActionClick() {
        val state = _uiState.value
        when {
            state.isComplete -> { /* Done */ }
            !state.isCalibrating && !state.isCollecting -> {
                // If we finished a step but haven't started the next
                if (state.statusMessage.contains("✓") || state.statusMessage.contains("Done")) {
                    nextStep()
                } else {
                    startCurrentStep()
                }
            }
        }
    }

    fun startCurrentStep() {
        when (_uiState.value.currentStep) {
            1 -> startGyroSampling()
            2 -> startMagSampling()
            3 -> startAccelPositionSampling()
        }
    }

    private fun startGyroSampling() {
        _isCalibrating.value = true
        gyroSamples.clear()
        calibrationJob = viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true, isCollecting = true, statusMessage = "Sampling Gyro...") }
            var count = 0
            while (count < GYRO_SAMPLES_NEEDED) {
                val data = getSensorData()
                gyroSamples.add(Triple(data.gyroX, data.gyroY, data.gyroZ))
                count++
                updateStepProgress(count.toFloat() / GYRO_SAMPLES_NEEDED)
                delay(20)
            }
            saveGyroData()
            _isCalibrating.value = false
            _uiState.update { it.copy(isCalibrating = false, isCollecting = false, statusMessage = "Gyroscope Complete ✓", stepInstruction = "Keep device ready for Magnetometer") }
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.GYRO_COMPLETE)
            nextStep()
        }
    }

    private fun saveGyroData() {
        if (gyroSamples.isEmpty()) return
        val avgX = gyroSamples.map { it.first }.average().toFloat()
        val avgY = gyroSamples.map { it.second }.average().toFloat()
        val avgZ = gyroSamples.map { it.third }.average().toFloat()
        prefs.putFloat("gyro_bias_x", avgX)
        prefs.putFloat("gyro_bias_y", avgY)
        prefs.putFloat("gyro_bias_z", avgZ)
        viewModelScope.launch { dataSource.saveGyroBias(avgX, avgY, avgZ) }
    }

    private fun startMagSampling() {
        _isCalibrating.value = true
        magSamples.clear()
        calibrationJob = viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true, isCollecting = true, statusMessage = "Sampling Mag...") }
            var count = 0
            while (count < MAG_SAMPLES_NEEDED) {
                val data = getSensorData()
                magSamples.add(Triple(data.magX, data.magY, data.magZ))
                count++
                updateStepProgress(count.toFloat() / MAG_SAMPLES_NEEDED)
                delay(20)
            }
            saveMagData()
            _isCalibrating.value = false
            _uiState.update { it.copy(isCalibrating = false, isCollecting = false, statusMessage = "Magnetometer Complete ✓", stepInstruction = "Prepare for Accelerometer") }
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.MAG_COMPLETE)
            nextStep()
        }
    }

    private fun saveMagData() {
        if (magSamples.isEmpty()) return
        val offX = (magSamples.minOf { it.first } + magSamples.maxOf { it.first }) / 2f
        prefs.putFloat("mag_offset_x", offX)
        viewModelScope.launch { dataSource.saveMagOffset(offX, 0f, 0f) }
    }

    private fun startAccelPositionSampling() {
        val currentPos = _uiState.value.currentPosition
        if (currentPos >= ACCEL_POSITIONS_NEEDED) return
        
        _isCalibrating.value = true
        calibrationJob = viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true, isCollecting = true, statusMessage = "Sampling: ${accelPositionsList[currentPos]}") }
            val samples = mutableListOf<Triple<Float, Float, Float>>()
            var count = 0
            while (count < ACCEL_SAMPLES_PER_POS) {
                val data = getSensorData()
                samples.add(Triple(data.accelX, data.accelY, data.accelZ))
                count++
                updateStepProgress(count.toFloat() / ACCEL_SAMPLES_PER_POS)
                delay(20)
            }
            accelPositions[currentPos] = samples
            val nextPos = currentPos + 1
            _isCalibrating.value = false
            _uiState.update { it.copy(
                isCalibrating = false, 
                isCollecting = false, 
                currentPosition = nextPos,
                progress = (nextPos * 100 / ACCEL_POSITIONS_NEEDED),
                statusMessage = if (nextPos < ACCEL_POSITIONS_NEEDED) "Pos $nextPos Captured ✓" else "Accelerometer Done ✓",
                stepInstruction = if (nextPos < ACCEL_POSITIONS_NEEDED) "Next: ${accelPositionsList.getOrNull(nextPos)}" else "All orientations completed"
            ) }
            if (nextPos >= ACCEL_POSITIONS_NEEDED) {
                calibrationRepository.updateCalibrationStatus(CalibrationStatus.ACCEL_COMPLETE)
                nextStep()
            } else {
                delay(250)
                startAccelPositionSampling()
            }
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 3) {
            val next = current + 1
            _uiState.update { it.copy(
                currentStep = current + 1, 
                progress = 0, 
                stepProgress = 0f, 
                statusMessage = "Ready for ${if(next==2) "Magnetometer" else "Accelerometer"}",
                stepInstruction = "Tap Begin to start"
            ) }
            _calibrationProgress.value = 0
            startCurrentStep()
        } else if (current == 3 && _uiState.value.currentPosition >= ACCEL_POSITIONS_NEEDED) {
            completeCalibration()
        }
    }

    fun completeCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(statusMessage = "Finalizing...", isCollecting = true) }
            delay(TRANSITION_DELAY)
            val data = CalibrationData(isCalibrated = true, quality = CalibrationQuality.EXCELLENT, timestamp = System.currentTimeMillis())
            calibrationRepository.saveCalibrationData(data)
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.COMPLETED)
            _uiState.update { it.copy(isComplete = true, isCalibrating = false, isCollecting = false, progress = 100, calibrationData = data) }
        }
    }

    fun loadCalibrationData(): CalibrationData? = _calibrationData.value

    fun syncToServer() {
        viewModelScope.launch {
            val data = _calibrationData.value ?: run {
                _uiState.update { it.copy(errorMessage = "No calibration data available") }
                return@launch
            }

            try {
                calibrationRepository.saveCalibrationData(data)
                _uiState.update { it.copy(statusMessage = "Calibration synced to server") }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to sync calibration to server", e)
                _uiState.update { it.copy(errorMessage = e.message ?: "Sync failed") }
            }
        }
    }

    fun skipCalibration() {
        calibrationJob?.cancel()
        _isCalibrating.value = false
        _uiState.update { it.copy(isComplete = true, isSkipped = true) }
        viewModelScope.launch { calibrationRepository.updateCalibrationStatus(CalibrationStatus.SKIPPED) }
    }

    private fun updateStepProgress(value: Float) {
        _uiState.update { it.copy(stepProgress = value) }
    }

    private suspend fun getSensorData(): SensorData = sensorDataProvider?.invoke() ?: SensorData()

    fun resetCalibration() {
        calibrationJob?.cancel()
        resetCalibrationState()
        viewModelScope.launch { calibrationRepository.resetAllCalibration() }
    }

    private fun resetCalibrationState() {
        calibrationJob?.cancel()
        _isCalibrating.value = false
        gyroSamples.clear()
        magSamples.clear()
        accelPositions.clear()
        _uiState.update { CalibrationUiState.initial() }
    }

    override fun onCleared() {
        calibrationJob?.cancel()
    }
}
