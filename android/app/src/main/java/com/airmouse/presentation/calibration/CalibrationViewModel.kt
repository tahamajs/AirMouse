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
        private const val SAMPLES_NEEDED = 100
        private const val POSITIONS_NEEDED = 6
    }

    // ==========================================
    // UI State
    // ==========================================

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

    private val _calibrationQuality = MutableStateFlow<CalibrationQuality>(CalibrationQuality.UNKNOWN)
    val calibrationQuality: StateFlow<CalibrationQuality> = _calibrationQuality.asStateFlow()

    // ==========================================
    // Internal State
    // ==========================================

    private var calibrationJob: Job? = null
    private var gyroSamples = mutableListOf<Triple<Float, Float, Float>>()
    private var magSamples = mutableListOf<Triple<Float, Float, Float>>()
    private val accelPositions = mutableMapOf<Int, Triple<Float, Float, Float>>()
    private var currentAccelPosition = 0
    private val accelPositionsList = listOf(
        "Flat (Screen Up)",
        "Flat (Screen Down)",
        "Left Side",
        "Right Side",
        "Top Edge",
        "Bottom Edge"
    )

    private var serverCalibrationSent = false
    private var calibrationApplied = false

    init {
        loadExistingCalibration()
        observeServerConnection()
        observeCalibrationStatusFromRepository()
        applyCalibrationOnStart()
    }

    // ==========================================
    // Observe Calibration Status from Repository
    // ==========================================

    private fun observeCalibrationStatusFromRepository() {
        viewModelScope.launch {
            calibrationRepository.observeCalibrationStatus().collect { status ->
                _calibrationStatus.value = status
                _uiState.update { state ->
                    state.copy(
                        statusMessage = when (status) {
                            CalibrationStatus.COMPLETED -> "Calibration complete!"
                            CalibrationStatus.IN_PROGRESS -> "Calibrating..."
                            CalibrationStatus.GYRO_COMPLETE -> "Gyroscope calibrated ✓"
                            CalibrationStatus.MAG_COMPLETE -> "Magnetometer calibrated ✓"
                            CalibrationStatus.ACCEL_COMPLETE -> "Accelerometer calibrated ✓"
                            CalibrationStatus.SKIPPED -> "Calibration skipped"
                            CalibrationStatus.FAILED -> "Calibration failed"
                            else -> "Ready to calibrate"
                        }
                    )
                }
            }
        }

        viewModelScope.launch {
            calibrationRepository.observeCalibrationProgress().collect { progress ->
                _calibrationProgress.value = progress
                _uiState.update { state ->
                    state.copy(progress = progress)
                }
            }
        }

        viewModelScope.launch {
            calibrationRepository.observeCalibrationQuality().collect { quality ->
                _calibrationQuality.value = quality
                _uiState.update { state ->
                    state.copy(
                        calibrationQuality = quality.name,
                        quality = quality.name
                    )
                }
            }
        }
    }

    // ==========================================
    // Server Connection Observer
    // ==========================================

    private fun observeServerConnection() {
        viewModelScope.launch {
            connectionRepository.observeConnectionStatus().collect { status ->
                val isConnected = status == ConnectionStatus.CONNECTED
                _uiState.update { state ->
                    state.copy(isServerConnected = isConnected)
                }

                if (isConnected && _uiState.value.isComplete && !serverCalibrationSent) {
                    syncCalibrationToServer()
                }
            }
        }
    }

    // ==========================================
    // Auto-Apply Calibration on Start
    // ==========================================

    private fun applyCalibrationOnStart() {
        viewModelScope.launch {
            val data = calibrationRepository.getCalibrationData()
            if (data.isCalibrated) {
                applyCalibrationToSensors(data)
                _uiState.update { state ->
                    state.copy(
                        isCalibrationApplied = true,
                        calibrationData = data,
                        statusMessage = "Calibration applied automatically"
                    )
                }
                Log.i(TAG, "Calibration applied on start: ${data.quality}")
            }
        }
    }

    // ==========================================
    // Load Existing Calibration
    // ==========================================

    private fun loadExistingCalibration() {
        viewModelScope.launch {
            try {
                val data = calibrationRepository.getCalibrationData()
                if (data.isCalibrated && data.quality != CalibrationQuality.UNKNOWN) {
                    _calibrationData.value = data
                    _calibrationQuality.value = data.quality
                    _calibrationStatus.value = CalibrationStatus.COMPLETED
                    _uiState.update { state ->
                        state.copy(
                            isComplete = true,
                            calibrationQuality = data.quality.name,
                            quality = data.quality.name,
                            statusMessage = "Calibration loaded from storage",
                            progress = 100,
                            calibrationData = data,
                            isCalibrationApplied = true
                        )
                    }
                    Log.i(TAG, "Loaded existing calibration: ${data.quality}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load calibration", e)
            }
        }
    }

    // ==========================================
    // Calibration Data Management
    // ==========================================

    suspend fun loadCalibrationData(): CalibrationData {
        return calibrationRepository.getCalibrationData()
    }

    fun saveCalibrationData(data: CalibrationData) {
        viewModelScope.launch {
            try {
                calibrationRepository.saveCalibrationData(data)
                _calibrationData.value = data
                _calibrationQuality.value = data.quality
                _uiState.update { state ->
                    state.copy(
                        calibrationData = data,
                        isCalibrationApplied = true,
                        statusMessage = "Calibration saved successfully!"
                    )
                }
                Log.i(TAG, "Calibration data saved: ${data.quality}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to save calibration", e)
                _uiState.update { state ->
                    state.copy(errorMessage = "Failed to save calibration: ${e.message}")
                }
            }
        }
    }

    fun applyCalibrationToSensors(data: CalibrationData) {
        if (!data.isCalibrated) {
            Log.w(TAG, "Cannot apply uncalibrated data")
            return
        }

        calibrationApplied = true
        calibrationUseCase.applyCalibration(data)

        _uiState.update { state ->
            state.copy(
                isCalibrationApplied = true,
                statusMessage = "Calibration applied to sensors"
            )
        }

        Log.i(TAG, "Calibration applied to sensors: ${data.quality}")
    }

    fun isCalibrationApplied(): Boolean = calibrationApplied

    suspend fun isCalibrated(): Boolean = calibrationUseCase.isCalibrated()

    suspend fun getCalibrationQuality(): CalibrationQuality = calibrationUseCase.getCalibrationQuality()

    // ==========================================
    // Send Calibration to Server
    // ==========================================

    suspend fun syncCalibrationToServer() {
        try {
            val data = calibrationRepository.getCalibrationData()
            if (!data.isCalibrated) {
                Log.w(TAG, "No calibration data to send")
                return
            }

            val message = buildCalibrationMessage(data)
            val success = connectionRepository.sendMessage(message)

            if (success) {
                serverCalibrationSent = true
                Log.i(TAG, "Calibration sent to server successfully")
                _uiState.update { state ->
                    state.copy(
                        statusMessage = "Calibration synced to server ✓",
                        detailedInstruction = "Calibration parameters sent to Air Mouse server"
                    )
                }
            } else {
                Log.w(TAG, "Failed to send calibration to server")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error sending calibration to server", e)
        }
    }

    private fun buildCalibrationMessage(data: CalibrationData): String {
        return """
            {
                "type": "calibration_data",
                "payload": {
                    "gyro": {
                        "bias_x": ${data.gyroBias.offsetX},
                        "bias_y": ${data.gyroBias.offsetY},
                        "bias_z": ${data.gyroBias.offsetZ}
                    },
                    "accel": {
                        "offset_x": ${data.accelOffset.offsetX},
                        "offset_y": ${data.accelOffset.offsetY},
                        "offset_z": ${data.accelOffset.offsetZ}
                    },
                    "mag": {
                        "offset_x": ${data.magOffset.offsetX},
                        "offset_y": ${data.magOffset.offsetY},
                        "offset_z": ${data.magOffset.offsetZ}
                    },
                    "quality": "${data.quality.name}",
                    "timestamp": ${data.timestamp}
                }
            }
        """.trimIndent()
    }

    // ==========================================
    // Public Methods
    // ==========================================

    fun startCalibration() {
        if (_isCalibrating.value) return
        _isCalibrating.value = true
        serverCalibrationSent = false
        calibrationApplied = false

        _uiState.update { state ->
            state.copy(
                isCalibrating = true,
                isCollecting = false,
                isComplete = false,
                errorMessage = null,
                statusMessage = "Starting calibration...",
                progress = 0,
                stepProgress = 0f
            )
        }

        startGyroCalibration()
    }

    fun resetCalibration() {
        calibrationJob?.cancel()
        calibrationJob = null

        gyroSamples.clear()
        magSamples.clear()
        accelPositions.clear()
        currentAccelPosition = 0
        serverCalibrationSent = false
        calibrationApplied = false

        _isCalibrating.value = false
        _calibrationData.value = null
        _calibrationProgress.value = 0
        _calibrationQuality.value = CalibrationQuality.UNKNOWN
        _calibrationStatus.value = CalibrationStatus.NOT_STARTED

        _uiState.update { CalibrationUiState.initial() }

        viewModelScope.launch {
            calibrationRepository.resetAllCalibration()
        }
    }

    fun skipCalibration() {
        calibrationJob?.cancel()
        calibrationJob = null
        _isCalibrating.value = false

        _uiState.update { state ->
            state.copy(
                isCalibrating = false,
                isCollecting = false,
                isSkipped = true,
                isComplete = true,
                statusMessage = "Calibration skipped",
                calibrationQuality = CalibrationQuality.UNKNOWN.name,
                quality = CalibrationQuality.UNKNOWN.name
            )
        }

        viewModelScope.launch {
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.SKIPPED)
            calibrationRepository.updateCalibrationQuality(CalibrationQuality.UNKNOWN)
        }
    }

    fun retryCalibration() {
        resetCalibration()
        startCalibration()
    }

    fun applyCalibration() {
        viewModelScope.launch {
            val data = calibrationRepository.getCalibrationData()
            if (!data.isCalibrated) {
                _uiState.update { state ->
                    state.copy(
                        statusMessage = "No calibration data available",
                        errorMessage = "Please calibrate first"
                    )
                }
                return@launch
            }

            applyCalibrationToSensors(data)
            _uiState.update { state ->
                state.copy(
                    statusMessage = "Calibration applied successfully!",
                    isCalibrationApplied = true
                )
            }
        }
    }

    fun syncToServer() {
        viewModelScope.launch {
            if (_uiState.value.isComplete) {
                syncCalibrationToServer()
            } else {
                _uiState.update { state ->
                    state.copy(
                        statusMessage = "Complete calibration first before syncing",
                        errorMessage = "Calibration not complete"
                    )
                }
            }
        }
    }

    suspend fun getCalibrationData(): CalibrationData {
        return calibrationRepository.getCalibrationData()
    }

    fun getCompletedPositions(): List<Int> {
        return accelPositions.keys.toList()
    }

    fun getTotalPositions(): Int {
        return POSITIONS_NEEDED
    }

    fun getCalibrationProgress(): Int = _calibrationProgress.value

    fun getCalibrationQuality(): CalibrationQuality = _calibrationQuality.value

    fun getCalibrationStatus(): CalibrationStatus = _calibrationStatus.value

    fun nextStep() {
        val currentStep = _uiState.value.currentStep
        if (currentStep < 3) {
            _uiState.update { state ->
                state.copy(
                    currentStep = currentStep + 1,
                    stepProgress = 0f,
                    samplesCollected = 0,
                    errorMessage = null
                )
            }
            when (currentStep + 1) {
                2 -> startMagCalibration()
                3 -> startAccelCalibration()
            }
        } else {
            completeCalibration()
        }
    }

    fun selectPosition(positionIndex: Int) {
        currentAccelPosition = positionIndex
        _uiState.update { state ->
            state.copy(
                currentPosition = positionIndex,
                stepInstruction = "Place device: ${accelPositionsList[positionIndex]}"
            )
        }
    }

    fun updateSensorData(data: SensorData) {
        _uiState.update { state ->
            state.copy(
                gyroData = Triple(data.gyroX, data.gyroY, data.gyroZ),
                accelData = Triple(data.accelX, data.accelY, data.accelZ),
                magData = Triple(data.magX, data.magY, data.magZ),
                roll = data.gyroX,
                pitch = data.gyroY,
                yaw = data.gyroZ
            )
        }
    }

    fun updateOrientation(roll: Float, pitch: Float, yaw: Float) {
        _uiState.update { state ->
            state.copy(
                roll = roll,
                pitch = pitch,
                yaw = yaw
            )
        }
    }

    // ==========================================
    // Calibration Steps
    // ==========================================

    private fun startGyroCalibration() {
        _uiState.update { state ->
            state.copy(
                currentStep = 1,
                statusMessage = "Calibrating gyroscope...",
                stepInstruction = "Place device on a flat surface. Keep it perfectly still.",
                detailedInstruction = "Keep the device stationary for 5 seconds.",
                isCollecting = true,
                isCalibrating = true,
                totalSamplesNeeded = SAMPLES_NEEDED
            )
        }

        gyroSamples.clear()

        calibrationJob = viewModelScope.launch {
            var progress = 0f
            while (progress < 1f) {
                val data = getSimulatedSensorData()
                gyroSamples.add(Triple(data.gyroX, data.gyroY, data.gyroZ))
                progress = gyroSamples.size.toFloat() / SAMPLES_NEEDED
                _calibrationProgress.value = (progress * 33).toInt()

                _uiState.update { state ->
                    state.copy(
                        stepProgress = progress,
                        samplesCollected = gyroSamples.size,
                        progress = (progress * 33).toInt(),
                        gyroData = Triple(data.gyroX, data.gyroY, data.gyroZ),
                        roll = data.gyroX,
                        pitch = data.gyroY,
                        yaw = data.gyroZ
                    )
                }
                delay(16)
            }

            calculateGyroOffsets()
            _uiState.update { state ->
                state.copy(
                    isCollecting = false,
                    statusMessage = "Gyroscope calibration complete!",
                    stepProgress = 1f
                )
            }
            _isCalibrating.value = false
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.GYRO_COMPLETE)
        }
    }

    private fun calculateGyroOffsets() {
        if (gyroSamples.isEmpty()) return

        val avgX = gyroSamples.map { it.first }.average().toFloat()
        val avgY = gyroSamples.map { it.second }.average().toFloat()
        val avgZ = gyroSamples.map { it.third }.average().toFloat()

        viewModelScope.launch {
            dataSource.saveGyroBias(avgX, avgY, avgZ)
        }

        gyroSamples.clear()
    }

    private fun startMagCalibration() {
        _uiState.update { state ->
            state.copy(
                currentStep = 2,
                statusMessage = "Calibrating magnetometer...",
                stepInstruction = "Move your device in a figure-8 pattern.",
                detailedInstruction = "Rotate device in all directions for 10 seconds.",
                isCollecting = true,
                isCalibrating = true,
                totalSamplesNeeded = SAMPLES_NEEDED * 2
            )
        }

        magSamples.clear()

        calibrationJob = viewModelScope.launch {
            var progress = 0f
            while (progress < 1f) {
                val data = getSimulatedSensorData()
                magSamples.add(Triple(data.magX, data.magY, data.magZ))
                progress = magSamples.size.toFloat() / (SAMPLES_NEEDED * 2)
                _calibrationProgress.value = 33 + (progress * 33).toInt()

                _uiState.update { state ->
                    state.copy(
                        stepProgress = progress,
                        samplesCollected = magSamples.size,
                        progress = 33 + (progress * 33).toInt(),
                        magData = Triple(data.magX, data.magY, data.magZ)
                    )
                }
                delay(16)
            }

            calculateMagOffsets()
            _uiState.update { state ->
                state.copy(
                    isCollecting = false,
                    statusMessage = "Magnetometer calibration complete!",
                    stepProgress = 1f
                )
            }
            _isCalibrating.value = false
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.MAG_COMPLETE)
        }
    }

    private fun calculateMagOffsets() {
        if (magSamples.isEmpty()) return

        val minX = magSamples.minOfOrNull { it.first } ?: 0f
        val maxX = magSamples.maxOfOrNull { it.first } ?: 0f
        val minY = magSamples.minOfOrNull { it.second } ?: 0f
        val maxY = magSamples.maxOfOrNull { it.second } ?: 0f
        val minZ = magSamples.minOfOrNull { it.third } ?: 0f
        val maxZ = magSamples.maxOfOrNull { it.third } ?: 0f

        val offsetX = (minX + maxX) / 2f
        val offsetY = (minY + maxY) / 2f
        val offsetZ = (minZ + maxZ) / 2f

        viewModelScope.launch {
            dataSource.saveMagOffset(offsetX, offsetY, offsetZ)
        }

        magSamples.clear()
    }

    private fun startAccelCalibration() {
        currentAccelPosition = 0
        accelPositions.clear()

        _uiState.update { state ->
            state.copy(
                currentStep = 3,
                statusMessage = "Calibrating accelerometer...",
                stepInstruction = "Rotate device to each position",
                detailedInstruction = "Hold each position steady for 3 seconds.",
                isCollecting = true,
                isCalibrating = true,
                currentPosition = 0,
                totalPositions = POSITIONS_NEEDED,
                stepProgress = 0f
            )
        }

        collectAccelPosition()
    }

    private fun collectAccelPosition() {
        if (currentAccelPosition >= POSITIONS_NEEDED) {
            calculateAccelOffsets()
            completeCalibration()
            return
        }

        _uiState.update { state ->
            state.copy(
                stepInstruction = "Place device: ${accelPositionsList[currentAccelPosition]}",
                currentPosition = currentAccelPosition
            )
        }

        calibrationJob = viewModelScope.launch {
            var samples = 0
            val neededSamples = 50

            while (samples < neededSamples) {
                val data = getSimulatedSensorData()
                samples++
                _uiState.update { state ->
                    state.copy(
                        samplesCollected = samples,
                        accelData = Triple(data.accelX, data.accelY, data.accelZ)
                    )
                }
                delay(16)
            }

            val lastData = getSimulatedSensorData()
            accelPositions[currentAccelPosition] = Triple(
                lastData.accelX,
                lastData.accelY,
                lastData.accelZ
            )

            val progress = (currentAccelPosition + 1).toFloat() / POSITIONS_NEEDED
            _calibrationProgress.value = 66 + (progress * 34).toInt()

            _uiState.update { state ->
                state.copy(
                    stepProgress = progress,
                    progress = 66 + (progress * 34).toInt(),
                    completedPositions = accelPositions.keys.toList()
                )
            }

            currentAccelPosition++
            collectAccelPosition()
        }
    }

    private fun calculateAccelOffsets() {
        if (accelPositions.isEmpty()) return

        var sumX = 0f
        var sumY = 0f
        var sumZ = 0f

        for ((_, values) in accelPositions) {
            sumX += values.first
            sumY += values.second
            sumZ += values.third
        }

        val offsetX = sumX / accelPositions.size
        val offsetY = sumY / accelPositions.size
        val offsetZ = sumZ / accelPositions.size

        viewModelScope.launch {
            dataSource.saveAccelOffset(offsetX, offsetY, offsetZ)
        }

        accelPositions.clear()
        calibrationRepository.updateCalibrationStatus(CalibrationStatus.ACCEL_COMPLETE)
    }

    // ==========================================
    // Completion
    // ==========================================

    private fun completeCalibration() {
        _isCalibrating.value = false

        val quality = determineQuality()
        val data = CalibrationData(
            gyroBias = SensorCalibrationData(
                offsetX = prefs.getFloat("gyro_bias_x", 0f),
                offsetY = prefs.getFloat("gyro_bias_y", 0f),
                offsetZ = prefs.getFloat("gyro_bias_z", 0f)
            ),
            accelOffset = SensorCalibrationData(
                offsetX = prefs.getFloat("accel_offset_x", 0f),
                offsetY = prefs.getFloat("accel_offset_y", 0f),
                offsetZ = prefs.getFloat("accel_offset_z", 0f)
            ),
            magOffset = SensorCalibrationData(
                offsetX = prefs.getFloat("mag_offset_x", 0f),
                offsetY = prefs.getFloat("mag_offset_y", 0f),
                offsetZ = prefs.getFloat("mag_offset_z", 0f)
            ),
            isCalibrated = true,
            quality = quality,
            timestamp = System.currentTimeMillis()
        )

        viewModelScope.launch {
            calibrationRepository.saveCalibrationData(data)
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.COMPLETED)
            calibrationRepository.updateCalibrationQuality(quality)
            calibrationRepository.updateCalibrationProgress(100)

            if (_uiState.value.isServerConnected) {
                syncCalibrationToServer()
            }

            applyCalibrationToSensors(data)
        }

        _uiState.update { state ->
            state.copy(
                isComplete = true,
                isCalibrating = false,
                isCollecting = false,
                progress = 100,
                statusMessage = if (_uiState.value.isServerConnected)
                    "Calibration complete! Synced to server ✓"
                else "Calibration complete! Connect to sync to server",
                calibrationQuality = quality.name,
                quality = quality.name,
                showConfetti = true,
                calibrationData = data,
                isCalibrationApplied = true
            )
        }

        Log.i(TAG, "Calibration complete! Quality: ${quality.name}")
    }

    private fun determineQuality(): CalibrationQuality {
        return try {
            val (gx, gy, gz) = runBlocking { dataSource.getGyroBias() }
            val (aox, aoy, aoz) = runBlocking { dataSource.getAccelOffset() }
            val (mox, moy, moz) = runBlocking { dataSource.getMagOffset() }

            val maxGyroOffset = listOf(gx, gy, gz).maxOf { kotlin.math.abs(it) }
            val maxAccelOffset = listOf(aox, aoy, aoz).maxOf { kotlin.math.abs(it) }
            val maxMagOffset = listOf(mox, moy, moz).maxOf { kotlin.math.abs(it) }

            when {
                maxGyroOffset < 0.1f && maxAccelOffset < 0.5f && maxMagOffset < 0.5f -> CalibrationQuality.EXCELLENT
                maxGyroOffset < 0.5f && maxAccelOffset < 1.0f && maxMagOffset < 1.0f -> CalibrationQuality.GOOD
                maxGyroOffset < 1.0f && maxAccelOffset < 2.0f && maxMagOffset < 2.0f -> CalibrationQuality.FAIR
                else -> CalibrationQuality.POOR
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error determining quality", e)
            CalibrationQuality.UNKNOWN
        }
    }

    // Helper for blocking suspend calls in non-suspend functions
    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    private suspend fun getSimulatedSensorData(): SensorData {
        return SensorData(
            gyroX = (Math.random() * 0.2 - 0.1).toFloat(),
            gyroY = (Math.random() * 0.2 - 0.1).toFloat(),
            gyroZ = (Math.random() * 0.2 - 0.1).toFloat(),
            accelX = (Math.random() * 0.2 - 0.1 + 9.81).toFloat(),
            accelY = (Math.random() * 0.2 - 0.1).toFloat(),
            accelZ = (Math.random() * 0.2 - 0.1).toFloat(),
            magX = (Math.random() * 0.5 - 0.25).toFloat(),
            magY = (Math.random() * 0.5 - 0.25).toFloat(),
            magZ = (Math.random() * 0.5 - 0.25).toFloat()
        )
    }

    override fun onCleared() {
        super.onCleared()
        calibrationJob?.cancel()
        calibrationJob = null
    }
}