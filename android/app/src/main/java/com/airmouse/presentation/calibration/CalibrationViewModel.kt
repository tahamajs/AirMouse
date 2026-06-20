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
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

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
        private const val SAMPLES_NEEDED = 100           // for gyro bias
        private const val MAG_SAMPLES_NEEDED = 200       // for magnetometer (figure‑8)
        private const val ACCEL_SAMPLES_PER_POS = 50     // for each accelerometer position
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

    // Collect samples for gyro bias (stationary)
    private var gyroSamples = mutableListOf<Triple<Float, Float, Float>>()

    // Collect samples for magnetometer (figure‑8)
    private var magSamples = mutableListOf<Triple<Float, Float, Float>>()

    // Collect samples for accelerometer (6 positions)
    private val accelPositions = mutableMapOf<Int, List<Triple<Float, Float, Float>>>()
    private var currentAccelPosition = 0

    // Position labels for UI
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

    // Real sensor data provider (injected or passed from activity)
    var sensorDataProvider: (() -> SensorData)? = null

    init {
        loadExistingCalibration()
        observeServerConnection()
        observeCalibrationStatusFromRepository()
        applyCalibrationOnStart()
    }

    // ==========================================
    // Observers
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
                _uiState.update { state -> state.copy(progress = progress) }
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

    private fun observeServerConnection() {
        viewModelScope.launch {
            connectionRepository.observeConnectionStatus().collect { status ->
                val isConnected = status == ConnectionStatus.CONNECTED
                _uiState.update { state -> state.copy(isServerConnected = isConnected) }
                if (isConnected && _uiState.value.isComplete && !serverCalibrationSent) {
                    syncCalibrationToServer()
                }
            }
        }
    }

    // ==========================================
    // Load / Apply Existing Calibration
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
    // Core Calibration Logic
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

        // Start with gyroscope
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

    // ==========================================
    // Individual Calibration Steps
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
                val data = getSensorData()
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
                delay(20)  // sample at ~50 Hz
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
            // Automatically proceed to next step after a short delay
            delay(500)
            nextStep()
        }
    }

    private fun calculateGyroOffsets() {
        if (gyroSamples.isEmpty()) return
        val avgX = gyroSamples.map { it.first }.average().toFloat()
        val avgY = gyroSamples.map { it.second }.average().toFloat()
        val avgZ = gyroSamples.map { it.third }.average().toFloat()

        viewModelScope.launch {
            dataSource.saveGyroBias(avgX, avgY, avgZ)
            // also store sample count
            prefs.putInt("gyro_samples", gyroSamples.size)
        }
        gyroSamples.clear()
    }

    private fun startMagCalibration() {
        _uiState.update { state ->
            state.copy(
                currentStep = 2,
                statusMessage = "Calibrating magnetometer...",
                stepInstruction = "Move your device in a figure‑8 pattern.",
                detailedInstruction = "Rotate device in all directions for 10 seconds.",
                isCollecting = true,
                isCalibrating = true,
                totalSamplesNeeded = MAG_SAMPLES_NEEDED
            )
        }

        magSamples.clear()

        calibrationJob = viewModelScope.launch {
            var progress = 0f
            while (progress < 1f) {
                val data = getSensorData()
                magSamples.add(Triple(data.magX, data.magY, data.magZ))
                progress = magSamples.size.toFloat() / MAG_SAMPLES_NEEDED
                _calibrationProgress.value = 33 + (progress * 33).toInt()

                _uiState.update { state ->
                    state.copy(
                        stepProgress = progress,
                        samplesCollected = magSamples.size,
                        progress = 33 + (progress * 33).toInt(),
                        magData = Triple(data.magX, data.magY, data.magZ)
                    )
                }
                delay(20)
            }

            calculateMagOffsetsAndScale()
            _uiState.update { state ->
                state.copy(
                    isCollecting = false,
                    statusMessage = "Magnetometer calibration complete!",
                    stepProgress = 1f
                )
            }
            _isCalibrating.value = false
            calibrationRepository.updateCalibrationStatus(CalibrationStatus.MAG_COMPLETE)
            delay(500)
            nextStep()
        }
    }

    private fun calculateMagOffsetsAndScale() {
        if (magSamples.isEmpty()) return

        // Compute min and max for each axis
        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        for (sample in magSamples) {
            if (sample.first < minX) minX = sample.first
            if (sample.first > maxX) maxX = sample.first
            if (sample.second < minY) minY = sample.second
            if (sample.second > maxY) maxY = sample.second
            if (sample.third < minZ) minZ = sample.third
            if (sample.third > maxZ) maxZ = sample.third
        }

        // Offset = (min + max) / 2
        val offsetX = (minX + maxX) / 2f
        val offsetY = (minY + maxY) / 2f
        val offsetZ = (minZ + maxZ) / 2f

        // Scale = (range) / 2  (or range / desired_range, but we keep it as range/2 for simplicity)
        // In many implementations scale = (max - min) / 2  (for ellipsoid fitting)
        // But to be safe, we use (max - min) / 2  and later apply (raw - offset) / scale
        val scaleX = (maxX - minX) / 2f
        val scaleY = (maxY - minY) / 2f
        val scaleZ = (maxZ - minZ) / 2f

        viewModelScope.launch {
            dataSource.saveMagOffset(offsetX, offsetY, offsetZ)
            dataSource.saveMagScale(scaleX, scaleY, scaleZ)
            prefs.putInt("mag_samples", magSamples.size)
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
                stepProgress = 0f,
                completedPositions = emptyList()
            )
        }

        collectAccelPosition()
    }

    private fun collectAccelPosition() {
        if (currentAccelPosition >= POSITIONS_NEEDED) {
            calculateAccelOffsetsAndScale()
            completeCalibration()
            return
        }

        val posIndex = currentAccelPosition
        _uiState.update { state ->
            state.copy(
                stepInstruction = "Place device: ${accelPositionsList[posIndex]}",
                currentPosition = posIndex
            )
        }

        calibrationJob = viewModelScope.launch {
            val samples = mutableListOf<Triple<Float, Float, Float>>()
            var count = 0
            while (count < ACCEL_SAMPLES_PER_POS) {
                val data = getSensorData()
                samples.add(Triple(data.accelX, data.accelY, data.accelZ))
                count++
                _uiState.update { state ->
                    state.copy(
                        samplesCollected = count,
                        accelData = Triple(data.accelX, data.accelY, data.accelZ)
                    )
                }
                delay(20)
            }

            // Store samples for this position
            accelPositions[posIndex] = samples

            val progress = (posIndex + 1).toFloat() / POSITIONS_NEEDED
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

    private fun calculateAccelOffsetsAndScale() {
        if (accelPositions.isEmpty()) return

        // We have 6 positions. For each axis, compute offset and scale using linear regression
        // or by averaging the min and max values.
        // A simple method: compute min and max for each axis across all positions, then offset = (min+max)/2, scale = (max-min)/2
        // But a better approach: use the fact that gravity magnitude should be ~9.81 m/s².
        // For each axis, we can compute the average of all samples for that axis.
        // However, the true offset and scale can be computed from the min/max of each axis.
        // Since we have 6 positions, we can compute min/max per axis.

        var minX = Float.MAX_VALUE
        var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE
        var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE
        var maxZ = Float.MIN_VALUE

        for ((_, samples) in accelPositions) {
            for (sample in samples) {
                if (sample.first < minX) minX = sample.first
                if (sample.first > maxX) maxX = sample.first
                if (sample.second < minY) minY = sample.second
                if (sample.second > maxY) maxY = sample.second
                if (sample.third < minZ) minZ = sample.third
                if (sample.third > maxZ) maxZ = sample.third
            }
        }

        val offsetX = (minX + maxX) / 2f
        val offsetY = (minY + maxY) / 2f
        val offsetZ = (minZ + maxZ) / 2f

        // Scale: we want the range to be 2*9.81 = 19.62 (since gravity can be positive or negative)
        // But we can compute scale as (max - min) / 2  (same as before)
        val scaleX = (maxX - minX) / 2f
        val scaleY = (maxY - minY) / 2f
        val scaleZ = (maxZ - minZ) / 2f

        // If scale is too small or zero, set to 1 to avoid division by zero
        val safeScaleX = if (abs(scaleX) < 0.01f) 1f else scaleX
        val safeScaleY = if (abs(scaleY) < 0.01f) 1f else scaleY
        val safeScaleZ = if (abs(scaleZ) < 0.01f) 1f else scaleZ

        viewModelScope.launch {
            dataSource.saveAccelOffset(offsetX, offsetY, offsetZ)
            dataSource.saveAccelScale(safeScaleX, safeScaleY, safeScaleZ)
            prefs.putInt("accel_positions", accelPositions.size)
        }
        accelPositions.clear()
        calibrationRepository.updateCalibrationStatus(CalibrationStatus.ACCEL_COMPLETE)
    }

    // ==========================================
    // Completion & Quality
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
                offsetZ = prefs.getFloat("accel_offset_z", 0f),
                scaleX = prefs.getFloat("accel_scale_x", 1f),
                scaleY = prefs.getFloat("accel_scale_y", 1f),
                scaleZ = prefs.getFloat("accel_scale_z", 1f)
            ),
            magOffset = SensorCalibrationData(
                offsetX = prefs.getFloat("mag_offset_x", 0f),
                offsetY = prefs.getFloat("mag_offset_y", 0f),
                offsetZ = prefs.getFloat("mag_offset_z", 0f),
                scaleX = prefs.getFloat("mag_scale_x", 1f),
                scaleY = prefs.getFloat("mag_scale_y", 1f),
                scaleZ = prefs.getFloat("mag_scale_z", 1f)
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

            val maxGyroOffset = listOf(gx, gy, gz).maxOf { abs(it) }
            val maxAccelOffset = listOf(aox, aoy, aoz).maxOf { abs(it) }
            val maxMagOffset = listOf(mox, moy, moz).maxOf { abs(it) }

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

    // ==========================================
    // Sync to Server
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
                        "offset_z": ${data.accelOffset.offsetZ},
                        "scale_x": ${data.accelOffset.scaleX},
                        "scale_y": ${data.accelOffset.scaleY},
                        "scale_z": ${data.accelOffset.scaleZ}
                    },
                    "mag": {
                        "offset_x": ${data.magOffset.offsetX},
                        "offset_y": ${data.magOffset.offsetY},
                        "offset_z": ${data.magOffset.offsetZ},
                        "scale_x": ${data.magOffset.scaleX},
                        "scale_y": ${data.magOffset.scaleY},
                        "scale_z": ${data.magOffset.scaleZ}
                    },
                    "quality": "${data.quality.name}",
                    "timestamp": ${data.timestamp}
                }
            }
        """.trimIndent()
    }

    // ==========================================
    // Apply Calibration to Sensors
    // ==========================================

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
    // Public Getters
    // ==========================================

    fun getCompletedPositions(): List<Int> = accelPositions.keys.toList()

    fun getTotalPositions(): Int = POSITIONS_NEEDED

    fun getCalibrationProgress(): Int = _calibrationProgress.value

    fun getCalibrationQuality(): CalibrationQuality = _calibrationQuality.value

    fun getCalibrationStatus(): CalibrationStatus = _calibrationStatus.value

    suspend fun loadCalibrationData(): CalibrationData = calibrationRepository.getCalibrationData()

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
    // Helper: Get real sensor data
    // ==========================================

    private suspend fun getSensorData(): SensorData {
        // If a provider is set, use it; otherwise fall back to simulated data
        return sensorDataProvider?.invoke() ?: getSimulatedSensorData()
    }

    private suspend fun getSimulatedSensorData(): SensorData {
        // Simulate sensor data with noise – for testing only
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

    // Helper for blocking suspend calls (only for determineQuality)
    private fun <T> runBlocking(block: suspend () -> T): T {
        return kotlinx.coroutines.runBlocking { block() }
    }

    override fun onCleared() {
        super.onCleared()
        calibrationJob?.cancel()
        calibrationJob = null
    }
}