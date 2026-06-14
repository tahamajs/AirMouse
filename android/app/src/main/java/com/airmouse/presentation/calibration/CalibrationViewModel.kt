package com.airmouse.presentation.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
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
    
    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress.asStateFlow()
    
    private val _currentOrientation = MutableStateFlow(Orientation(0f, 0f, 0f))
    val currentOrientation: StateFlow<Orientation> = _currentOrientation.asStateFlow()
    
    fun startGyroCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true, currentStep = CalibrationStep.GYROSCOPE) }
            
            // Show instruction
            _uiState.update { it.copy(instruction = "Place device on a flat, stationary surface") }
            
            // Simulate progress
            for (i in 0..100 step 5) {
                _calibrationProgress.value = i
                delay(50)
            }
            
            // Perform actual calibration
            val success = calibrationHelper.startGyroCalibration { progress ->
                _calibrationProgress.value = progress
            }
            
            if (success) {
                _uiState.update { 
                    it.copy(
                        isCalibrating = false,
                        currentStep = CalibrationStep.MAGNETOMETER,
                        gyroComplete = true
                    )
                }
            }
        }
    }
    
    fun startMagnetometerCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true, currentStep = CalibrationStep.MAGNETOMETER) }
            _uiState.update { it.copy(instruction = "Move device in a figure-8 pattern for 10 seconds") }
            
            calibrationHelper.startMagnetometerCalibration { progress ->
                _calibrationProgress.value = progress
            }
            
            _uiState.update { 
                it.copy(
                    isCalibrating = false,
                    currentStep = CalibrationStep.ACCELEROMETER,
                    magnetometerComplete = true
                )
            }
        }
    }
    
    fun startAccelerometerCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCalibrating = true, currentStep = CalibrationStep.ACCELEROMETER) }
            
            val orientations = listOf(
                "Place device flat facing UP" to Orientation(0f, 0f, 9.81f),
                "Place device flat facing DOWN" to Orientation(0f, 0f, -9.81f),
                "Place device on LEFT side" to Orientation(9.81f, 0f, 0f),
                "Place device on RIGHT side" to Orientation(-9.81f, 0f, 0f),
                "Place device standing UP" to Orientation(0f, 9.81f, 0f),
                "Place device standing DOWN" to Orientation(0f, -9.81f, 0f)
            )
            
            var step = 0
            for ((instruction, expected) in orientations) {
                _uiState.update { 
                    it.copy(
                        instruction = instruction,
                        accelerometerStep = step,
                        totalAccelerometerSteps = orientations.size
                    )
                }
                _calibrationProgress.value = (step * 100 / orientations.size)
                delay(3000)
                step++
            }
            
            calibrationHelper.startAccelerometerCalibration { progress, instruction ->
                _calibrationProgress.value = progress
                _uiState.update { it.copy(instruction = instruction) }
            }
            
            prefs.putBoolean("calibration_complete", true)
            
            _uiState.update { 
                it.copy(
                    isCalibrating = false,
                    currentStep = CalibrationStep.COMPLETE,
                    accelerometerComplete = true,
                    allComplete = true
                )
            }
        }
    }
    
    fun updateOrientation(roll: Float, pitch: Float, yaw: Float) {
        _currentOrientation.value = Orientation(roll, pitch, yaw)
    }
    
    fun skipCalibration() {
        viewModelScope.launch {
            prefs.putBoolean("calibration_skipped", true)
            _uiState.update { it.copy(skipRequested = true) }
        }
    }
    
    data class CalibrationUiState(
        val isCalibrating: Boolean = false,
        val currentStep: CalibrationStep = CalibrationStep.GYROSCOPE,
        val instruction: String = "",
        val gyroComplete: Boolean = false,
        val magnetometerComplete: Boolean = false,
        val accelerometerComplete: Boolean = false,
        val accelerometerStep: Int = 0,
        val totalAccelerometerSteps: Int = 6,
        val allComplete: Boolean = false,
        val skipRequested: Boolean = false
    )
    
    enum class CalibrationStep {
        GYROSCOPE, MAGNETOMETER, ACCELEROMETER, COMPLETE
    }
    
    data class Orientation(
        val roll: Float,
        val pitch: Float,
        val yaw: Float
    )
}