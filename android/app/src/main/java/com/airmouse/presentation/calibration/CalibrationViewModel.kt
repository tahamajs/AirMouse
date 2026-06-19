// CalibrationViewModel.kt
package com.airmouse.presentation.ui.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.presentation.calibration.CalibrationData
import com.airmouse.presentation.calibration.CalibrationUiState
import com.airmouse.presentation.calibration.positions
import com.airmouse.utils.PreferencesManager
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

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null

    private var isCollecting = false
    private var currentCollectStep = 0
    private val gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val magSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val accelPositionSamples = mutableListOf<MutableList<Triple<Float, Float, Float>>>()
    private var currentAccelPos = 0

    // Listeners
    private val gyroListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollecting && currentCollectStep == 0) {
                gyroSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                updateProgress(gyroSamples.size, 500)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val accelListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollecting && currentCollectStep == 1) {
                val lastList = accelPositionSamples.lastOrNull()
                if (lastList == null || lastList.size >= 100) {
                    accelPositionSamples.add(mutableListOf())
                }
                accelPositionSamples.last().add(Triple(event.values[0], event.values[1], event.values[2]))
                updateProgress(accelPositionSamples.last().size, 100)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    private val magListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollecting && currentCollectStep == 2) {
                magSamples.add(Triple(event.values[0], event.values[1], event.values[2]))
                updateProgress(magSamples.size, 1000)
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        loadSavedCalibration()
    }

    private fun loadSavedCalibration() {
        _uiState.update {
            it.copy(
                calibrationData = CalibrationData(
                    gyroOffsetX = prefs.getFloat("gyro_offset_x", 0f),
                    gyroOffsetY = prefs.getFloat("gyro_offset_y", 0f),
                    gyroOffsetZ = prefs.getFloat("gyro_offset_z", 0f),
                    accelOffsetX = prefs.getFloat("accel_offset_x", 0f),
                    accelOffsetY = prefs.getFloat("accel_offset_y", 0f),
                    accelOffsetZ = prefs.getFloat("accel_offset_z", 0f),
                    accelScaleX = prefs.getFloat("accel_scale_x", 1f),
                    accelScaleY = prefs.getFloat("accel_scale_y", 1f),
                    accelScaleZ = prefs.getFloat("accel_scale_z", 1f),
                    magOffsetX = prefs.getFloat("mag_offset_x", 0f),
                    magOffsetY = prefs.getFloat("mag_offset_y", 0f),
                    magOffsetZ = prefs.getFloat("mag_offset_z", 0f),
                    magScaleX = prefs.getFloat("mag_scale_x", 1f),
                    magScaleY = prefs.getFloat("mag_scale_y", 1f),
                    magScaleZ = prefs.getFloat("mag_scale_z", 1f)
                ),
                isComplete = prefs.getBoolean("calibration_complete", false)
            )
        }
    }

    fun startCalibration() {
        if (_uiState.value.isComplete) return
        viewModelScope.launch {
            when (_uiState.value.currentStep) {
                0 -> startGyroCollection()
                1 -> startAccelCollection()
                2 -> startMagCollection()
            }
        }
    }

    private suspend fun startGyroCollection() {
        if (gyroscope == null) {
            _uiState.update { it.copy(errorMessage = "Gyroscope not available") }
            return
        }
        gyroSamples.clear()
        isCollecting = true
        currentCollectStep = 0
        sensorManager.registerListener(gyroListener, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)

        _uiState.update {
            it.copy(
                isCollecting = true,
                statusMessage = "Collecting gyro data... Keep phone still",
                progress = 0,
                samplesCollected = 0,
                totalSamplesNeeded = 500
            )
        }

        while (gyroSamples.size < 500) delay(20)
        sensorManager.unregisterListener(gyroListener)
        isCollecting = false

        val avgX = gyroSamples.map { it.first }.average().toFloat()
        val avgY = gyroSamples.map { it.second }.average().toFloat()
        val avgZ = gyroSamples.map { it.third }.average().toFloat()

        prefs.putFloat("gyro_offset_x", avgX)
        prefs.putFloat("gyro_offset_y", avgY)
        prefs.putFloat("gyro_offset_z", avgZ)

        _uiState.update {
            it.copy(
                calibrationData = it.calibrationData.copy(
                    gyroOffsetX = avgX, gyroOffsetY = avgY, gyroOffsetZ = avgZ
                ),
                statusMessage = "✓ Gyroscope calibrated",
                progress = 100
            )
        }
        delay(1000)
        nextStep()
    }

    private suspend fun startAccelCollection() {
        if (accelerometer == null) {
            _uiState.update { it.copy(errorMessage = "Accelerometer not available") }
            return
        }
        accelPositionSamples.clear()
        currentAccelPos = 0
        isCollecting = true
        currentCollectStep = 1
        sensorManager.registerListener(accelListener, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)

        _uiState.update {
            it.copy(
                isCollecting = true,
                currentPosition = 0,
                statusMessage = "Position 1/6: ${positions[0]}",
                progress = 0,
                samplesCollected = 0,
                totalSamplesNeeded = 100
            )
        }

        while (currentAccelPos < positions.size) {
            if (accelPositionSamples.size > currentAccelPos) {
                currentAccelPos++
                if (currentAccelPos < positions.size) {
                    _uiState.update {
                        it.copy(
                            currentPosition = currentAccelPos,
                            statusMessage = "Position ${currentAccelPos + 1}/${positions.size}: ${positions[currentAccelPos]}",
                            progress = 0
                        )
                    }
                }
            }
            delay(50)
        }

        sensorManager.unregisterListener(accelListener)
        isCollecting = false

        val averages = accelPositionSamples.map { samples ->
            Triple(
                samples.map { it.first }.average().toFloat(),
                samples.map { it.second }.average().toFloat(),
                samples.map { it.third }.average().toFloat()
            )
        }

        val offsetX = (averages[0].first + averages[1].first) / 2f
        val offsetY = (averages[2].second + averages[3].second) / 2f
        val offsetZ = (averages[4].third + averages[5].third) / 2f

        val scaleX = (averages[0].first - averages[1].first) / 19.62f
        val scaleY = (averages[2].second - averages[3].second) / 19.62f
        val scaleZ = (averages[4].third - averages[5].third) / 19.62f

        prefs.putFloat("accel_offset_x", offsetX)
        prefs.putFloat("accel_offset_y", offsetY)
        prefs.putFloat("accel_offset_z", offsetZ)
        prefs.putFloat("accel_scale_x", scaleX)
        prefs.putFloat("accel_scale_y", scaleY)
        prefs.putFloat("accel_scale_z", scaleZ)

        _uiState.update {
            it.copy(
                calibrationData = it.calibrationData.copy(
                    accelOffsetX = offsetX, accelOffsetY = offsetY, accelOffsetZ = offsetZ,
                    accelScaleX = scaleX, accelScaleY = scaleY, accelScaleZ = scaleZ
                ),
                statusMessage = "✓ Accelerometer calibrated",
                progress = 100
            )
        }
        delay(1000)
        nextStep()
    }

    private suspend fun startMagCollection() {
        if (magnetometer == null) {
            _uiState.update {
                it.copy(
                    statusMessage = "Magnetometer skipped (not available)",
                    isComplete = true
                )
            }
            prefs.putBoolean("calibration_complete", true)
            return
        }
        magSamples.clear()
        isCollecting = true
        currentCollectStep = 2
        sensorManager.registerListener(magListener, magnetometer, SensorManager.SENSOR_DELAY_GAME)

        _uiState.update {
            it.copy(
                isCollecting = true,
                statusMessage = "Move phone in a figure‑8 pattern",
                progress = 0,
                samplesCollected = 0,
                totalSamplesNeeded = 1000
            )
        }

        while (magSamples.size < 1000) delay(20)
        sensorManager.unregisterListener(magListener)
        isCollecting = false

        val minX = magSamples.minOf { it.first }
        val maxX = magSamples.maxOf { it.first }
        val minY = magSamples.minOf { it.second }
        val maxY = magSamples.maxOf { it.second }
        val minZ = magSamples.minOf { it.third }
        val maxZ = magSamples.maxOf { it.third }

        val offsetX = (minX + maxX) / 2f
        val offsetY = (minY + maxY) / 2f
        val offsetZ = (minZ + maxZ) / 2f
        val scaleX = (maxX - minX) / 2f
        val scaleY = (maxY - minY) / 2f
        val scaleZ = (maxZ - minZ) / 2f

        prefs.putFloat("mag_offset_x", offsetX)
        prefs.putFloat("mag_offset_y", offsetY)
        prefs.putFloat("mag_offset_z", offsetZ)
        prefs.putFloat("mag_scale_x", scaleX)
        prefs.putFloat("mag_scale_y", scaleY)
        prefs.putFloat("mag_scale_z", scaleZ)

        _uiState.update {
            it.copy(
                calibrationData = it.calibrationData.copy(
                    magOffsetX = offsetX, magOffsetY = offsetY, magOffsetZ = offsetZ,
                    magScaleX = scaleX, magScaleY = scaleY, magScaleZ = scaleZ
                ),
                statusMessage = "✓ Magnetometer calibrated",
                progress = 100
            )
        }
        delay(1000)
        nextStep()
    }

    fun recordPosition() {
        // For accelerometer multi‑position, recording is automatic.
        // This method is kept for compatibility.
    }

    fun nextStep() {
        val step = _uiState.value.currentStep
        if (step < 2) {
            _uiState.update {
                it.copy(
                    currentStep = step + 1,
                    progress = 0,
                    isCollecting = false,
                    errorMessage = null,
                    stepTitle = getStepTitle(step + 1),
                    stepDescription = getStepDescription(step + 1),
                    stepInstruction = getStepInstruction(step + 1)
                )
            }
        } else {
            finishCalibration()
        }
    }

    fun previousStep() {
        val step = _uiState.value.currentStep
        if (step > 0) {
            _uiState.update {
                it.copy(
                    currentStep = step - 1,
                    progress = 0,
                    isCollecting = false,
                    errorMessage = null,
                    stepTitle = getStepTitle(step - 1),
                    stepDescription = getStepDescription(step - 1),
                    stepInstruction = getStepInstruction(step - 1)
                )
            }
        }
    }

    fun abortCalibration() {
        isCollecting = false
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(magListener)
        _uiState.update {
            it.copy(
                isCollecting = false,
                errorMessage = "Calibration aborted",
                currentStep = 0,
                progress = 0
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
        prefs.putFloat("accel_scale_x", 1f)
        prefs.putFloat("accel_scale_y", 1f)
        prefs.putFloat("accel_scale_z", 1f)
        prefs.putFloat("mag_offset_x", 0f)
        prefs.putFloat("mag_offset_y", 0f)
        prefs.putFloat("mag_offset_z", 0f)
        prefs.putFloat("mag_scale_x", 1f)
        prefs.putFloat("mag_scale_y", 1f)
        prefs.putFloat("mag_scale_z", 1f)

        _uiState.update { CalibrationUiState() }
    }

    private fun finishCalibration() {
        prefs.putBoolean("calibration_complete", true)
        _uiState.update {
            it.copy(
                isComplete = true,
                isCollecting = false,
                statusMessage = "Calibration completed successfully!"
            )
        }
    }

    private fun updateProgress(current: Int, target: Int) {
        val percent = (current * 100 / target).coerceIn(0, 100)
        _uiState.update {
            it.copy(
                progress = percent,
                samplesCollected = current
            )
        }
    }

    private fun getStepTitle(step: Int) = when (step) {
        0 -> "Gyroscope Calibration"
        1 -> "Accelerometer Calibration"
        2 -> "Magnetometer Calibration"
        else -> ""
    }
// Add these methods to CalibrationViewModel.kt

    fun nextAccelPosition() {
        val nextPos = _uiState.value.currentPosition + 1
        if (nextPos < positions.size) {
            _uiState.update {
                it.copy(
                    currentPosition = nextPos,
                    progress = 0,
                    samplesCollected = 0,
                    statusMessage = "Position ${nextPos + 1}/${positions.size}: ${positions[nextPos]}"
                )
            }
            startAccelCollectionForPosition(nextPos)
        } else {
            stopCollection()
            nextStep()
        }
    }

    fun jumpToPosition(position: Int) {
        if (position in 0 until positions.size && position <= _uiState.value.currentPosition + 1) {
            _uiState.update {
                it.copy(
                    currentPosition = position,
                    statusMessage = "Position ${position + 1}/${positions.size}: ${positions[position]}"
                )
            }
        }
    }

    fun stopCollection() {
        isCollecting = false
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(magListener)
        _uiState.update {
            it.copy(
                isCollecting = false,
                statusMessage = "Collection stopped"
            )
        }
    }

    fun skipCalibration() {
        prefs.putBoolean("calibration_skipped", true)
        _uiState.update {
            it.copy(
                isSkipped = true,
                isComplete = true,
                statusMessage = "Calibration skipped. You can calibrate later from settings."
            )
        }
    }

    fun openHelp() {
        // Show help dialog or navigate to help screen
    }
    private fun getStepDescription(step: Int) = when (step) {
        0 -> "Remove gyro drift by measuring bias while stationary"
        1 -> "Align gravity vector using 6 positions"
        2 -> "Compensate for magnetic interference with figure‑8 motion"
        else -> ""
    }

    private fun getStepInstruction(step: Int) = when (step) {
        0 -> "Place phone on a flat, stationary surface"
        1 -> "Hold phone in each position until progress reaches 100%"
        2 -> "Move phone in a smooth figure‑8 pattern for 15 seconds"
        else -> ""
    }

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(gyroListener)
        sensorManager.unregisterListener(accelListener)
        sensorManager.unregisterListener(magListener)
    }
}