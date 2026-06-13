package com.airmouse.presentation.ui.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    private var sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private var gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private var accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private var magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private var gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private var accelSamples = mutableListOf<Triple<Float, Float, Float>>()
    private var magSamples = mutableListOf<Triple<Float, Float, Float>>()
    private var isCollectingData = false

    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollectingData && _uiState.value.currentStep == 0) {
                gyroSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                updateProgress(gyroSamples.size, 500)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollectingData && _uiState.value.currentStep == 1) {
                accelSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                updateProgress(accelSamples.size, 100)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val magListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollectingData && _uiState.value.currentStep == 2) {
                magSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                updateProgress(magSamples.size, 500)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    val currentStep: Int
        get() = _uiState.value.currentStep

    fun nextStep() {
        if (_uiState.value.currentStep < 2) {
            _uiState.update { state ->
                state.copy(
                    currentStep = state.currentStep + 1,
                    progress = (state.currentStep + 1) * 33
                )
            }
            updateStepContent()
        } else if (_uiState.value.currentStep == 2) {
            completeCalibration()
        }
    }

    fun previousStep() {
        if (_uiState.value.currentStep > 0) {
            _uiState.update { state ->
                state.copy(
                    currentStep = state.currentStep - 1,
                    progress = (state.currentStep - 1) * 33
                )
            }
            updateStepContent()
        }
    }

    fun startCalibration() {
        viewModelScope.launch {
            _uiState.update { it.copy(isCollecting = true, errorMessage = null) }

            when (_uiState.value.currentStep) {
                0 -> calibrateGyroscope()
                1 -> calibrateAccelerometer()
                2 -> calibrateMagnetometer()
            }
        }
    }

    private suspend fun calibrateGyroscope() {
        if (gyroscope == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Gyroscope sensor not available on this device",
                    isCollecting = false
                )
            }
            return
        }

        gyroSamples.clear()
        isCollectingData = true

        sensorManager.registerListener(
            gyroListener,
            gyroscope,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        _uiState.update {
            it.copy(
                statusMessage = "Keep phone perfectly still... Collecting data",
                progress = 0
            )
        }

        // Collect 500 samples or timeout after 10 seconds
        var elapsed = 0
        while (gyroSamples.size < 500 && elapsed < 10000) {
            delay(20)
            elapsed += 20
        }

        sensorManager.unregisterListener(gyroListener)
        isCollectingData = false

        if (gyroSamples.size >= 100) {
            // Calculate bias offsets
            val avgX = gyroSamples.map { it.first }.average().toFloat()
            val avgY = gyroSamples.map { it.second }.average().toFloat()
            val avgZ = gyroSamples.map { it.third }.average().toFloat()

            prefs.putFloat("gyro_offset_x", avgX)
            prefs.putFloat("gyro_offset_y", avgY)
            prefs.putFloat("gyro_offset_z", avgZ)

            _uiState.update {
                it.copy(
                    statusMessage = "✓ Gyroscope calibrated successfully!",
                    isCollecting = false
                )
            }
            delay(1000)
            nextStep()
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "Failed to collect enough gyroscope data. Please try again.",
                    isCollecting = false
                )
            }
        }
    }

    private suspend fun calibrateAccelerometer() {
        if (accelerometer == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Accelerometer sensor not available on this device",
                    isCollecting = false
                )
            }
            return
        }

        accelSamples.clear()
        isCollectingData = true

        sensorManager.registerListener(
            accelListener,
            accelerometer,
            SensorManager.SENSOR_DELAY_FASTEST
        )

        _uiState.update {
            it.copy(
                statusMessage = "Place phone flat on a table... Collecting data",
                progress = 0
            )
        }

        var elapsed = 0
        while (accelSamples.size < 100 && elapsed < 5000) {
            delay(20)
            elapsed += 20
        }

        sensorManager.unregisterListener(accelListener)
        isCollectingData = false

        if (accelSamples.isNotEmpty()) {
            // Calculate average accelerometer values
            val avgX = accelSamples.map { it.first }.average().toFloat()
            val avgY = accelSamples.map { it.second }.average().toFloat()
            val avgZ = accelSamples.map { it.third }.average().toFloat()

            prefs.putFloat("accel_offset_x", avgX)
            prefs.putFloat("accel_offset_y", avgY)
            prefs.putFloat("accel_offset_z", avgZ)

            _uiState.update {
                it.copy(
                    statusMessage = "✓ Accelerometer calibrated successfully!",
                    isCollecting = false
                )
            }
            delay(1000)
            nextStep()
        } else {
            _uiState.update {
                it.copy(
                    errorMessage = "Failed to collect accelerometer data. Please try again.",
                    isCollecting = false
                )
            }
        }
    }

    private suspend fun calibrateMagnetometer() {
        if (magnetometer == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "Magnetometer not available, skipping...",
                    isCollecting = false
                )
            }
            delay(1000)
            nextStep()
            return
        }

        magSamples.clear()
        isCollectingData = true

        sensorManager.registerListener(
            magListener,
            magnetometer,
            SensorManager.SENSOR_DELAY_GAME
        )

        _uiState.update {
            it.copy(
                statusMessage = "Move phone in a figure-8 pattern... Collecting data",
                progress = 0
            )
        }

        var elapsed = 0
        while (magSamples.size < 500 && elapsed < 15000) {
            delay(20)
            elapsed += 20
        }

        sensorManager.unregisterListener(magListener)
        isCollectingData = false

        if (magSamples.isNotEmpty()) {
            // Calculate min/max for hard iron calibration
            val minX = magSamples.minOf { it.first }
            val maxX = magSamples.maxOf { it.first }
            val minY = magSamples.minOf { it.second }
            val maxY = magSamples.maxOf { it.second }
            val minZ = magSamples.minOf { it.third }
            val maxZ = magSamples.maxOf { it.third }

            val offsetX = (minX + maxX) / 2
            val offsetY = (minY + maxY) / 2
            val offsetZ = (minZ + maxZ) / 2

            prefs.putFloat("mag_offset_x", offsetX)
            prefs.putFloat("mag_offset_y", offsetY)
            prefs.putFloat("mag_offset_z", offsetZ)

            _uiState.update {
                it.copy(
                    statusMessage = "✓ Magnetometer calibrated successfully!",
                    isCollecting = false
                )
            }
            delay(1000)
            nextStep()
        } else {
            _uiState.update {
                it.copy(
                    statusMessage = "Magnetometer calibration skipped (insufficient data)",
                    isCollecting = false
                )
            }
            delay(1000)
            nextStep()
        }
    }

    fun abortCalibration() {
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(magListener)
        isCollectingData = false

        _uiState.update {
            CalibrationUiState(
                currentStep = 0,
                progress = 0,
                statusMessage = "Calibration aborted",
                stepTitle = "Gyroscope Calibration",
                stepDescription = "Keep the phone perfectly still while we measure the gyroscope bias."
            )
        }
    }

    private fun updateProgress(current: Int, target: Int) {
        val progress = (current * 100 / target).coerceIn(0, 100)
        _uiState.update { it.copy(progress = progress) }
    }

    private fun updateStepContent() {
        when (_uiState.value.currentStep) {
            0 -> {
                _uiState.update {
                    it.copy(
                        stepTitle = "Gyroscope Calibration",
                        stepDescription = "Keep the phone perfectly still on a flat surface while we measure the gyroscope bias.",
                        statusMessage = "Ready to calibrate gyroscope",
                        isCollecting = false,
                        errorMessage = null
                    )
                }
            }
            1 -> {
                _uiState.update {
                    it.copy(
                        stepTitle = "Accelerometer Calibration",
                        stepDescription = "Place the phone flat on a table. Keep it still while we measure gravity.",
                        statusMessage = "Ready to calibrate accelerometer",
                        isCollecting = false,
                        errorMessage = null
                    )
                }
            }
            2 -> {
                _uiState.update {
                    it.copy(
                        stepTitle = "Magnetometer Calibration",
                        stepDescription = "Move the phone in a figure-8 pattern until calibration completes.",
                        statusMessage = "Ready to calibrate magnetometer",
                        isCollecting = false,
                        errorMessage = null
                    )
                }
            }
        }
    }

    private fun completeCalibration() {
        prefs.putBoolean("calibration_complete", true)
        _uiState.update {
            it.copy(
                isComplete = true,
                statusMessage = "Calibration completed successfully!",
                progress = 100
            )
        }
    }

    fun resetCalibration() {
        prefs.putBoolean("calibration_complete", false)
        prefs.putFloat("gyro_offset_x", 0f)
        prefs.putFloat("gyro_offset_y", 0f)
        prefs.putFloat("gyro_offset_z", 0f)
        prefs.putFloat("accel_offset_x", 0f)
        prefs.putFloat("accel_offset_y", 0f)
        prefs.putFloat("accel_offset_z", 0f)
        prefs.putFloat("mag_offset_x", 0f)
        prefs.putFloat("mag_offset_y", 0f)
        prefs.putFloat("mag_offset_z", 0f)

        _uiState.update {
            CalibrationUiState(
                currentStep = 0,
                progress = 0,
                statusMessage = "Calibration reset. Ready to start over.",
                stepTitle = "Gyroscope Calibration",
                stepDescription = "Keep the phone perfectly still while we measure the gyroscope bias."
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(magListener)
    }
}

data class CalibrationUiState(
    val currentStep: Int = 0,
    val progress: Int = 0,
    val stepTitle: String = "Gyroscope Calibration",
    val stepDescription: String = "Keep the phone perfectly still on a flat surface while we measure the gyroscope bias.",
    val statusMessage: String = "Ready to calibrate",
    val errorMessage: String? = null,
    val isCollecting: Boolean = false,
    val isComplete: Boolean = false
)