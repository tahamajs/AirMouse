package com.airmouse.presentation.ui.calibration

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.usecase.CalibrationUseCase
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val calibrationUseCase: CalibrationUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    fun startCalibration() {
        _uiState.update { it.copy(isCollecting = true, progress = 0, errorMessage = null) }
        // Simulate step-by-step – in real app, interact with sensors
    }

    fun recordPosition() {
        // Called when user holds phone in a specific orientation
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < _uiState.value.totalSteps - 1) {
            _uiState.update {
                it.copy(
                    currentStep = current + 1,
                    progress = 0,
                    isCollecting = false,
                    stepTitle = getStepTitle(current + 1),
                    stepDescription = getStepDescription(current + 1)
                )
            }
        } else {
            finishCalibration()
        }
    }

    fun previousStep() {
        val current = _uiState.value.currentStep
        if (current > 0) {
            _uiState.update {
                it.copy(
                    currentStep = current - 1,
                    progress = 0,
                    isCollecting = false,
                    stepTitle = getStepTitle(current - 1),
                    stepDescription = getStepDescription(current - 1)
                )
            }
        }
    }

    fun abortCalibration() {
        _uiState.update { it.copy(isComplete = false, isCollecting = false, errorMessage = "Calibration aborted") }
    }

    private fun finishCalibration() {
        viewModelScope.launch {
            calibrationUseCase.isCalibrationComplete()
            _uiState.update { it.copy(isComplete = true, isCollecting = false) }
        }
    }

    private fun getStepTitle(step: Int): String = when (step) {
        0 -> "Gyroscope Calibration"
        1 -> "Accelerometer Calibration"
        2 -> "Magnetometer Calibration"
        else -> "Calibration"
    }

    private fun getStepDescription(step: Int): String = when (step) {
        0 -> "Place phone on a flat, stationary surface"
        1 -> "Hold phone in the 6 positions as shown"
        2 -> "Move phone in a figure‑8 pattern"
        else -> ""
    }
}