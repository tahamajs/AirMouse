package com.airmouse.presentation.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val calibrationHelper: CalibrationHelper
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()
    
    private val _gyroProgress = MutableStateFlow(0)
    val gyroProgress: StateFlow<Int> = _gyroProgress.asStateFlow()
    
    private val _magProgress = MutableStateFlow(0)
    val magProgress: StateFlow<Int> = _magProgress.asStateFlow()
    
    private val _accelStep = MutableStateFlow(0)
    val accelStep: StateFlow<Int> = _accelStep.asStateFlow()
    
    private val accelMeasurements = mutableListOf<FloatArray>()
    
    data class CalibrationUiState(
        val currentStep: CalibrationStep = CalibrationStep.GYROSCOPE,
        val isCalibrating: Boolean = false,
        val instruction: String = "",
        val gyroComplete: Boolean = false,
        val magComplete: Boolean = false,
        val accelComplete: Boolean = false,
        val allComplete: Boolean = false,
        val quality: String = "UNKNOWN"
    )
    
    enum class CalibrationStep { GYROSCOPE, MAGNETOMETER, ACCELEROMETER, COMPLETE }

    init {
        observeCalibrationState()
    }

    private fun observeCalibrationState() {
        viewModelScope.launch {
            calibrationHelper.state.collect { state ->
                _uiState.update { it.copy(isCalibrating = state == CalibrationHelper.CalibrationState.CALIBRATING_GYRO ||
                    state == CalibrationHelper.CalibrationState.CALIBRATING_MAG ||
                    state == CalibrationHelper.CalibrationState.CALIBRATING_ACCEL) }
            }
        }
        
        viewModelScope.launch {
            calibrationHelper.message.collect { message ->
                _uiState.update { it.copy(instruction = message) }
            }
        }
        
        viewModelScope.launch {
            calibrationHelper.quality.collect { quality ->
                _uiState.update { it.copy(quality = quality.name) }
            }
        }
    }

    fun startGyroCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(currentStep = CalibrationStep.GYROSCOPE, isCalibrating = true) }
            val success = calibrationHelper.calibrateGyroscope { progress ->
                _gyroProgress.value = progress
            }
            if (success) {
                _uiState.update { it.copy(gyroComplete = true, currentStep = CalibrationStep.MAGNETOMETER) }
                prefs.putBoolean("gyro_calibrated", true)
            }
        }
    }

    fun startMagCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(currentStep = CalibrationStep.MAGNETOMETER, isCalibrating = true) }
            val success = calibrationHelper.calibrateMagnetometer { progress ->
                _magProgress.value = progress
            }
            if (success) {
                _uiState.update { it.copy(magComplete = true, currentStep = CalibrationStep.ACCELEROMETER) }
                prefs.putBoolean("mag_calibrated", true)
            }
        }
    }

    suspend fun calibrateAccelerometerStep(step: Int): Boolean {
        val success = calibrationHelper.calibrateAccelerometer(step) { instruction ->
            _uiState.update { it.copy(instruction = instruction) }
        }
        
        if (success) {
            // In production, collect actual measurements here
            _accelStep.value = step + 1
        }
        return success
    }

    fun completeAccelerometerCalibration() {
        calibrationHelper.finishAccelerometerCalibration(accelMeasurements)
        _uiState.update { 
            it.copy(
                accelComplete = true,
                allComplete = true,
                currentStep = CalibrationStep.COMPLETE,
                isCalibrating = false
            )
        }
        prefs.putBoolean("calibration_complete", true)
    }

    fun skipCalibration() {
        prefs.putBoolean("calibration_skipped", true)
        _uiState.update { it.copy(allComplete = true, currentStep = CalibrationStep.COMPLETE) }
    }

    fun resetAndRecalibrate() {
        calibrationHelper.reset()
        _uiState.value = CalibrationUiState()
        _gyroProgress.value = 0
        _magProgress.value = 0
        _accelStep.value = 0
        accelMeasurements.clear()
    }
}