package com.airmouse.presentation.ui.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Trace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlinx.coroutines.withContext
import timber.log.Timber
import kotlin.coroutines.resume
import kotlin.math.abs
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val connectionRepository: IConnectionRepository,
    private val calibrationUseCase: CalibrationUseCase,
    private val prefs: PreferencesManager,
    private val dataSource: ICalibrationDataSource
) : ViewModel() {

    companion object {
        private const val TAG = "CalibrationViewModel"
        private const val GYRO_SAMPLES_NEEDED = 250
        private const val MAG_SAMPLES_NEEDED = 500
        private const val ACCEL_SAMPLES_PER_POS = 100
        private const val ACCEL_POSITIONS_NEEDED = 6
        private const val TRANSITION_DELAY = 1000L
        private const val SENSOR_TIMEOUT_EXTRA_MS = 2500L
        private const val GRAVITY_EARTH = 9.80665f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    // ============================================================
    // UI State
    // ============================================================

    private val _uiState = MutableStateFlow(CalibrationUiState.initial())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()

    // ============================================================
    // Calibration State
    // ============================================================

    private val _isCalibrating = MutableStateFlow(false)
    val isCalibrating: StateFlow<Boolean> = _isCalibrating.asStateFlow()

    private val _calibrationData = MutableStateFlow<CalibrationData?>(null)
    val calibrationData: StateFlow<CalibrationData?> = _calibrationData.asStateFlow()

    private val _calibrationStatus = MutableStateFlow(CalibrationStatus.NOT_STARTED)
    val calibrationStatus: StateFlow<CalibrationStatus> = _calibrationStatus.asStateFlow()

    private val _calibrationQuality = MutableStateFlow(CalibrationQuality.UNKNOWN)
    val calibrationQuality: StateFlow<CalibrationQuality> = _calibrationQuality.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0)
    val calibrationProgress: StateFlow<Int> = _calibrationProgress.asStateFlow()

    // ============================================================
    // Calibration Data Collection
    // ============================================================

    private var calibrationJob: Job? = null
    private var preparationJob: Job? = null
    private var gyroSamples = mutableListOf<SensorSample>()
    private var magSamples = mutableListOf<SensorSample>()
    private val accelPositions = mutableMapOf<Int, List<SensorSample>>()

    private val accelPositionsList = listOf(
        "Flat (Screen Up)", "Flat (Screen Down)", "Left Side Down",
        "Right Side Down", "Top Edge Down", "Bottom Edge Down"
    )

    init {
        loadExistingCalibration()
        observeStatus()
        observeServerConnection()
    }

    // ============================================================
    // Observers
    // ============================================================

    private fun observeStatus() {
        viewModelScope.launch {
            calibrationUseCase.observeCalibrationStatus().collect { status ->
                _calibrationStatus.value = status
            }
        }
        viewModelScope.launch {
            calibrationUseCase.observeCalibrationQuality().collect { quality ->
                _calibrationQuality.value = quality
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

    // ============================================================
    // Load Existing Calibration
    // ============================================================

    private fun loadExistingCalibration() {
        viewModelScope.launch {
            try {
                val data = withTimeoutOrNull(2000L) {
                    calibrationUseCase.getCalibrationData()
                }

                if (data?.isCalibrated == true) {
                    _calibrationData.value = data
                    _uiState.update {
                        it.copy(
                            currentStep = 3,
                            calibrationPhase = CalibrationPhase.INTRO,
                            isComplete = true,
                            isCalibrating = false,
                            isCollecting = false,
                            progress = 100,
                            statusMessage = "Calibration already saved",
                            stepInstruction = "You can continue to the app or recalibrate if needed",
                            calibrationData = data
                        )
                    }
                    prefs.setCalibrated(true)
                    prefs.setCalibrationTimestamp(data.timestamp)
                } else {
                    _calibrationData.value = null
                    _uiState.update {
                        it.copy(
                            currentStep = 0,
                            calibrationPhase = CalibrationPhase.INTRO,
                            isComplete = false,
                            isCalibrating = false,
                            isCollecting = false,
                            progress = 0,
                            statusMessage = "No saved calibration found",
                            stepInstruction = "Tap Start to begin calibration",
                            calibrationData = null
                        )
                    }
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to load calibration")
                _calibrationData.value = null
                _uiState.update {
                    it.copy(
                        currentStep = 0,
                        calibrationPhase = CalibrationPhase.INTRO,
                        isComplete = false,
                        isCalibrating = false,
                        isCollecting = false,
                        progress = 0,
                        statusMessage = "Calibration check failed",
                        stepInstruction = "Tap Start to begin calibration",
                        errorMessage = e.message ?: "Unable to read saved calibration",
                        calibrationData = null
                    )
                }
            }
        }
    }

    // ============================================================
    // Public API
    // ============================================================

    fun startCalibration() {
        if (_isCalibrating.value) return
        viewModelScope.launch {
            Timber.tag(TAG).d("Starting fresh calibration run")
            withContext(Dispatchers.IO) {
                calibrationUseCase.resetAllCalibration()
            }
            resetCalibrationState()
            _uiState.update {
                it.copy(
                    currentStep = 1,
                    calibrationPhase = CalibrationPhase.INTRO,
                    statusMessage = "Step 1: Gyroscope",
                    stepInstruction = "Watch the motion preview, then tap Start to begin sampling"
                )
            }
        }
    }

    fun beginCurrentStep() {
        if (_isCalibrating.value) return
        if (_uiState.value.calibrationPhase == CalibrationPhase.SAMPLING) return

        preparationJob?.cancel()
        _uiState.update {
            it.copy(
                calibrationPhase = CalibrationPhase.COUNTDOWN,
                statusMessage = "Preparing ${stepLabel(_uiState.value.currentStep)}...",
                stepInstruction = "Hold the phone as shown. Sampling starts automatically in a moment."
            )
        }

        preparationJob = viewModelScope.launch {
            delay(1400)
            if (_uiState.value.isComplete) return@launch
            _uiState.update {
                it.copy(
                    calibrationPhase = CalibrationPhase.SAMPLING,
                    statusMessage = "Sampling ${stepLabel(_uiState.value.currentStep)}...",
                    stepInstruction = when (_uiState.value.currentStep) {
                        1 -> "Keep the phone flat and still while we sample gyro bias."
                        2 -> "Keep the phone steady through the magnetometer sweep."
                        else -> "Rotate through each orientation and hold each pose still."
                    }
                )
            }
            startCurrentStep()
        }
    }

    fun startCurrentStep() {
        when (_uiState.value.currentStep) {
            1 -> startGyroSampling()
            2 -> startMagSampling()
            3 -> startAccelPositionSampling()
        }
    }

    fun nextStep() {
        val current = _uiState.value.currentStep
        if (current < 3) {
            val next = current + 1
            _uiState.update {
                it.copy(
                    currentStep = next,
                    progress = 0,
                    stepProgress = 0f,
                    calibrationPhase = CalibrationPhase.INTRO,
                    statusMessage = when (next) {
                        2 -> "Ready for Magnetometer"
                        3 -> "Ready for Accelerometer"
                        else -> "Ready for next step"
                    },
                    stepInstruction = "Review the instruction, then tap Start to begin sampling.",
                    samplesCollected = 0,
                    totalSamplesNeeded = 0
                )
            }
            _calibrationProgress.value = 0
        } else if (current == 3 && _uiState.value.currentPosition >= ACCEL_POSITIONS_NEEDED) {
            completeCalibration()
        }
    }

    fun completeCalibration() {
        viewModelScope.launch {
            Trace.beginSection("Calibration#complete")
            try {
                _uiState.update { it.copy(statusMessage = "Finalizing...", isCollecting = true) }
                delay(TRANSITION_DELAY)

                val (data, quality) = withContext(Dispatchers.IO) {
                    val computedQuality = calculateCalibrationQuality()
                    val averageGyro = gyroSamples.average()
                    val measurements = (0 until ACCEL_POSITIONS_NEEDED).mapNotNull { accelPositions[it]?.average() }
                    val averageAccelX = measurements.map { it.x }.average().toFloat()
                    val averageAccelY = measurements.map { it.y }.average().toFloat()
                    val averageAccelZ = measurements.map { it.z }.average().toFloat()

                    val scaleX = calculateAxisScale(
                        positive = measurements.getOrNull(3)?.x ?: 0f,
                        negative = measurements.getOrNull(2)?.x ?: 0f
                    )
                    val scaleY = calculateAxisScale(
                        positive = measurements.getOrNull(5)?.y ?: 0f,
                        negative = measurements.getOrNull(4)?.y ?: 0f
                    )
                    val scaleZ = calculateAxisScale(
                        positive = measurements.getOrNull(0)?.z ?: 0f,
                        negative = measurements.getOrNull(1)?.z ?: 0f
                    )

                    val minX = magSamples.minOfOrNull { it.x } ?: 0f
                    val maxX = magSamples.maxOfOrNull { it.x } ?: 0f
                    val minY = magSamples.minOfOrNull { it.y } ?: 0f
                    val maxY = magSamples.maxOfOrNull { it.y } ?: 0f
                    val minZ = magSamples.minOfOrNull { it.z } ?: 0f
                    val maxZ = magSamples.maxOfOrNull { it.z } ?: 0f
                    val rangeX = (maxX - minX).coerceAtLeast(0.0001f)
                    val rangeY = (maxY - minY).coerceAtLeast(0.0001f)
                    val rangeZ = (maxZ - minZ).coerceAtLeast(0.0001f)
                    val avgRange = (rangeX + rangeY + rangeZ) / 3f

                    val calibration = CalibrationData(
                        gyroBias = SensorCalibrationData(averageGyro.x, averageGyro.y, averageGyro.z),
                        accelOffset = SensorCalibrationData(
                            offsetX = averageAccelX,
                            offsetY = averageAccelY,
                            offsetZ = averageAccelZ,
                            scaleX = scaleX,
                            scaleY = scaleY,
                            scaleZ = scaleZ
                        ),
                        magOffset = SensorCalibrationData(
                            offsetX = (minX + maxX) / 2f,
                            offsetY = (minY + maxY) / 2f,
                            offsetZ = (minZ + maxZ) / 2f,
                            scaleX = avgRange / rangeX,
                            scaleY = avgRange / rangeY,
                            scaleZ = avgRange / rangeZ
                        ),
                        isCalibrated = true,
                        quality = computedQuality,
                        timestamp = System.currentTimeMillis()
                    )

                    calibrationUseCase.saveCalibrationData(calibration)
                    calibrationUseCase.applyCalibration(calibration)
                    prefs.setCalibrated(true)
                    prefs.setCalibrationTimestamp(calibration.timestamp)
                    calibration to computedQuality
                }

                _calibrationData.value = data

                _uiState.update {
                    it.copy(
                        isComplete = true,
                        isCalibrating = false,
                        isCollecting = false,
                        progress = 100,
                        stepProgress = 1f,
                        statusMessage = "✅ Calibration complete!",
                        stepInstruction = "Sensor offsets and scale factors are saved.",
                        calibrationData = data,
                        calibrationQuality = quality.name,
                        quality = quality.name,
                        isCalibrationApplied = true
                    )
                }

                Timber.tag(TAG).d("Calibration completed successfully with quality: %s", quality.name)
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Failed to complete calibration")
                _uiState.update {
                    it.copy(
                        isCalibrating = false,
                        isCollecting = false,
                        errorMessage = "Failed to complete calibration: ${e.message}"
                    )
                }
            } finally {
                Trace.endSection()
            }
        }
    }

    fun resetCalibration() {
        calibrationJob?.cancel()
        resetCalibrationState()
        viewModelScope.launch {
            withContext(Dispatchers.IO) {
                calibrationUseCase.resetAllCalibration()
            }
        }
    }

    @Suppress("unused")
    fun skipCalibration() {
        calibrationJob?.cancel()
        _isCalibrating.value = false
        _uiState.update { it.copy(isComplete = true, isSkipped = true) }
        viewModelScope.launch { calibrationUseCase.saveCalibrationData(CalibrationData(isCalibrated = false, quality = CalibrationQuality.UNKNOWN, timestamp = System.currentTimeMillis())) }
    }

    // ============================================================
    // Private Methods
    // ============================================================

    private fun startGyroSampling() {
        _isCalibrating.value = true
        gyroSamples.clear()
        calibrationJob = viewModelScope.launch {
            Trace.beginSection("Calibration#gyroSampling")
            try {
                _uiState.update {
                    it.copy(
                        isCalibrating = true,
                        isCollecting = true,
                        calibrationPhase = CalibrationPhase.SAMPLING,
                        statusMessage = "Sampling gyroscope...",
                        stepInstruction = "Keep the phone still on a flat surface.",
                        samplesCollected = 0,
                        totalSamplesNeeded = GYRO_SAMPLES_NEEDED,
                        errorMessage = null
                    )
                }

                val samples = collectSensorSamples(
                    sensorType = Sensor.TYPE_GYROSCOPE,
                    samplesNeeded = GYRO_SAMPLES_NEEDED,
                    timeoutMs = GYRO_SAMPLES_NEEDED * 25L + SENSOR_TIMEOUT_EXTRA_MS
                ) { _, count ->
                    updateStepProgress(count.toFloat() / GYRO_SAMPLES_NEEDED)
                }

                if (samples.size < GYRO_SAMPLES_NEEDED) {
                    failCurrentStep("Gyroscope data did not arrive. Make sure your device has a gyroscope.")
                    return@launch
                }

                saveGyroData()
                _isCalibrating.value = false

                _uiState.update {
                    it.copy(
                        isCalibrating = false,
                        isCollecting = false,
                        calibrationPhase = CalibrationPhase.INTRO,
                        statusMessage = "✓ Gyroscope calibrated",
                        stepInstruction = "Next: Magnetometer",
                        samplesCollected = GYRO_SAMPLES_NEEDED,
                        totalSamplesNeeded = GYRO_SAMPLES_NEEDED,
                        progress = 33
                    )
                }
                nextStep()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Gyro sampling error")
                failCurrentStep("Gyroscope sampling error: ${e.message}")
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun saveGyroData() {
        if (gyroSamples.isEmpty()) return
        val avgX = gyroSamples.map { it.x }.average().toFloat()
        val avgY = gyroSamples.map { it.y }.average().toFloat()
        val avgZ = gyroSamples.map { it.z }.average().toFloat()
        val varX = gyroSamples.variance { it.x }
        val varY = gyroSamples.variance { it.y }
        val varZ = gyroSamples.variance { it.z }
        prefs.putFloat("gyro_bias_x", avgX)
        prefs.putFloat("gyro_bias_y", avgY)
        prefs.putFloat("gyro_bias_z", avgZ)
        dataSource.saveGyroBias(avgX, avgY, avgZ)
        dataSource.saveGyroVariance(varX, varY, varZ)
        dataSource.saveGyroSampleCount(gyroSamples.size)
    }

    private fun startMagSampling() {
        _isCalibrating.value = true
        magSamples.clear()
        calibrationJob = viewModelScope.launch {
            Trace.beginSection("Calibration#magSampling")
            try {
                _uiState.update {
                    it.copy(
                        isCalibrating = true,
                        isCollecting = true,
                        calibrationPhase = CalibrationPhase.SAMPLING,
                        statusMessage = "Sampling magnetometer...",
                        stepInstruction = "Move in a slow figure-eight away from metal and magnets.",
                        samplesCollected = 0,
                        totalSamplesNeeded = MAG_SAMPLES_NEEDED,
                        errorMessage = null
                    )
                }
                val samples = collectSensorSamples(
                    sensorType = Sensor.TYPE_MAGNETIC_FIELD,
                    samplesNeeded = MAG_SAMPLES_NEEDED,
                    timeoutMs = MAG_SAMPLES_NEEDED * 25L + SENSOR_TIMEOUT_EXTRA_MS
                ) { _, count ->
                    updateStepProgress(count.toFloat() / MAG_SAMPLES_NEEDED)
                }
                if (samples.size < MAG_SAMPLES_NEEDED) {
                    failCurrentStep("Magnetometer data did not arrive. This device may not have a compass sensor.")
                    return@launch
                }
                saveMagData()
                _isCalibrating.value = false
                _uiState.update {
                    it.copy(
                        isCalibrating = false,
                        isCollecting = false,
                        calibrationPhase = CalibrationPhase.INTRO,
                        statusMessage = "Magnetometer complete",
                        stepInstruction = "Next up: accelerometer. Watch the next animation before starting.",
                        samplesCollected = MAG_SAMPLES_NEEDED,
                        totalSamplesNeeded = MAG_SAMPLES_NEEDED
                    )
                }
                nextStep()
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Mag sampling error")
                failCurrentStep("Magnetometer sampling error: ${e.message}")
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun saveMagData() {
        // Data is processed and saved in completeCalibration via CalibrationData domain model.
    }

    private fun startAccelPositionSampling() {
        val currentPos = _uiState.value.currentPosition
        if (currentPos >= ACCEL_POSITIONS_NEEDED) return

        _isCalibrating.value = true
        calibrationJob = viewModelScope.launch {
            Trace.beginSection("Calibration#accelerometerSampling")
            try {
                _uiState.update {
                    it.copy(
                        isCalibrating = true,
                        isCollecting = false,
                        calibrationPhase = CalibrationPhase.COUNTDOWN,
                        statusMessage = "Prepare: ${accelPositionsList[currentPos]}",
                        stepInstruction = "Hold the phone in position and wait for sampling to begin."
                    )
                }
                delay(900)
                _uiState.update {
                    it.copy(
                        isCollecting = true,
                        calibrationPhase = CalibrationPhase.SAMPLING,
                        statusMessage = "Sampling: ${accelPositionsList[currentPos]}",
                        stepInstruction = "Hold this orientation still until the progress finishes.",
                        samplesCollected = 0,
                        totalSamplesNeeded = ACCEL_SAMPLES_PER_POS,
                        errorMessage = null
                    )
                }
                val samples = collectSensorSamples(
                    sensorType = Sensor.TYPE_ACCELEROMETER,
                    samplesNeeded = ACCEL_SAMPLES_PER_POS,
                    timeoutMs = ACCEL_SAMPLES_PER_POS * 25L + SENSOR_TIMEOUT_EXTRA_MS
                ) { _, count ->
                    updateStepProgress(count.toFloat() / ACCEL_SAMPLES_PER_POS)
                }
                if (samples.size < ACCEL_SAMPLES_PER_POS) {
                    failCurrentStep("Accelerometer data did not arrive. Keep the phone awake and try again.")
                    return@launch
                }
                accelPositions[currentPos] = samples
                val nextPos = currentPos + 1
                _isCalibrating.value = false
                _uiState.update {
                    it.copy(
                        isCalibrating = false,
                        isCollecting = false,
                        currentPosition = nextPos,
                        completedPositions = (it.completedPositions + currentPos).distinct(),
                        progress = (nextPos * 100 / ACCEL_POSITIONS_NEEDED),
                        statusMessage = if (nextPos < ACCEL_POSITIONS_NEEDED) "Position ${nextPos + 1} captured" else "Accelerometer complete",
                        calibrationPhase = if (nextPos < ACCEL_POSITIONS_NEEDED) CalibrationPhase.COUNTDOWN else CalibrationPhase.INTRO,
                        stepInstruction = if (nextPos < ACCEL_POSITIONS_NEEDED) "Next: ${accelPositionsList.getOrNull(nextPos)}" else "All orientations completed",
                        samplesCollected = samples.size,
                        totalSamplesNeeded = ACCEL_SAMPLES_PER_POS
                    )
                }
                if (nextPos >= ACCEL_POSITIONS_NEEDED) {
                    nextStep()
                } else {
                    delay(250)
                    startAccelPositionSampling()
                }
            } catch (e: Exception) {
                Timber.tag(TAG).e(e, "Accel sampling error")
                failCurrentStep("Accelerometer sampling error: ${e.message}")
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun collectSensorSamples(
        sensorType: Int,
        samplesNeeded: Int,
        timeoutMs: Long,
        onSample: (SensorSample, Int) -> Unit
    ): List<SensorSample> {
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return emptyList()
        val samples = mutableListOf<SensorSample>()

        return suspendCancellableCoroutine { continuation ->
            val listener = object : SensorEventListener {
                override fun onSensorChanged(event: SensorEvent) {
                    if (samples.size >= samplesNeeded) {
                        sensorManager.unregisterListener(this)
                        if (continuation.isActive) {
                            continuation.resume(samples)
                        }
                        return
                    }
                    val sample = SensorSample(event.values[0], event.values[1], event.values[2])
                    samples.add(sample)
                    onSample(sample, samples.size)

                    _uiState.update {
                        it.copy(
                            samplesCollected = samples.size,
                            totalSamplesNeeded = samplesNeeded,
                            progress = (samples.size * 100 / samplesNeeded).coerceAtMost(100)
                        )
                    }
                }
                override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {}
            }

            val registered = sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_GAME)
            if (!registered) {
                if (continuation.isActive) {
                    continuation.resume(emptyList())
                }
                return@suspendCancellableCoroutine
            }

            continuation.invokeOnCancellation {
                sensorManager.unregisterListener(listener)
            }

            viewModelScope.launch {
                delay(timeoutMs)
                if (samples.size < samplesNeeded) {
                    sensorManager.unregisterListener(listener)
                    if (continuation.isActive) {
                        continuation.resume(samples)
                    }
                }
            }
        }
    }

    private suspend fun failCurrentStep(message: String) {
        _isCalibrating.value = false
        _uiState.update {
            it.copy(
                isCalibrating = false,
                isCollecting = false,
                calibrationPhase = CalibrationPhase.INTRO,
                statusMessage = "Calibration paused",
                stepInstruction = "Fix the issue and tap Start to retry this step.",
                errorMessage = message
            )
        }
    }

    private fun updateStepProgress(value: Float) {
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                stepProgress = clamped,
                progress = (clamped * 100).toInt().coerceIn(0, 100),
                samplesCollected = if (_uiState.value.totalSamplesNeeded > 0) {
                    (clamped * _uiState.value.totalSamplesNeeded).toInt().coerceIn(0, _uiState.value.totalSamplesNeeded)
                } else {
                    _uiState.value.samplesCollected
                }
            )
        }
    }

    private fun calculateAxisScale(positive: Float, negative: Float): Float {
        val measuredRange = abs(positive - negative)
        return if (measuredRange > 0.0001f) (2f * GRAVITY_EARTH) / measuredRange else 1f
    }

    private fun calculateCalibrationQuality(): CalibrationQuality {
        val gyroVariance = gyroSamples.variance { it.x } +
                gyroSamples.variance { it.y } +
                gyroSamples.variance { it.z }
        val hasAllSamples = gyroSamples.size >= GYRO_SAMPLES_NEEDED &&
                magSamples.size >= MAG_SAMPLES_NEEDED &&
                accelPositions.size >= ACCEL_POSITIONS_NEEDED
        return when {
            !hasAllSamples -> CalibrationQuality.FAIR
            gyroVariance < 0.001f -> CalibrationQuality.EXCELLENT
            gyroVariance < 0.01f -> CalibrationQuality.GOOD
            else -> CalibrationQuality.FAIR
        }
    }

    private fun resetCalibrationState() {
        calibrationJob?.cancel()
        preparationJob?.cancel()
        _isCalibrating.value = false
        _calibrationData.value = null
        _calibrationStatus.value = CalibrationStatus.NOT_STARTED
        _calibrationProgress.value = 0
        gyroSamples.clear()
        magSamples.clear()
        accelPositions.clear()
        _uiState.update { CalibrationUiState.initial() }
    }

    private fun stepLabel(step: Int): String {
        return when (step) {
            1 -> "gyroscope"
            2 -> "magnetometer"
            3 -> "accelerometer"
            else -> "calibration"
        }
    }

    // ============================================================
    // Data Classes
    // ============================================================

    data class SensorSample(
        val x: Float,
        val y: Float,
        val z: Float
    )

    // ============================================================
    // Extension Functions
    // ============================================================

    private fun List<SensorSample>.average(): SensorSample {
        if (isEmpty()) return SensorSample(0f, 0f, 0f)
        return SensorSample(
            map { it.x }.average().toFloat(),
            map { it.y }.average().toFloat(),
            map { it.z }.average().toFloat()
        )
    }

    private inline fun List<SensorSample>.variance(selector: (SensorSample) -> Float): Float {
        if (isEmpty()) return 0f
        val mean = map(selector).average().toFloat()
        return map { value ->
            val delta = selector(value) - mean
            delta * delta
        }.average().toFloat()
    }

    fun syncToServer() {
        val data = _calibrationData.value ?: return
        viewModelScope.launch {
            try {
                val payload = org.json.JSONObject().apply {
                    put("type", "control")
                    put("command", "calibrate")
                    put("payload", org.json.JSONObject().apply {
                        put("gyro_bias", org.json.JSONArray(doubleArrayOf(
                            data.gyroBias.offsetX.toDouble(),
                            data.gyroBias.offsetY.toDouble(),
                            data.gyroBias.offsetZ.toDouble()
                        )))
                        put("accel_offset", org.json.JSONArray(doubleArrayOf(
                            data.accelOffset.offsetX.toDouble(),
                            data.accelOffset.offsetY.toDouble(),
                            data.accelOffset.offsetZ.toDouble()
                        )))
                        put("mag_offset", org.json.JSONArray(doubleArrayOf(
                            data.magOffset.offsetX.toDouble(),
                            data.magOffset.offsetY.toDouble(),
                            data.magOffset.offsetZ.toDouble()
                        )))
                        put("quality", data.quality.name)
                    })
                }.toString()
                
                val success = connectionRepository.sendMessage(payload)
                if (success) {
                    _uiState.update { it.copy(statusMessage = "Calibration successfully synchronized to PC") }
                } else {
                    _uiState.update { it.copy(errorMessage = "Failed to send calibration to PC server") }
                }
            } catch (e: Exception) {
                _uiState.update { it.copy(errorMessage = "Sync error: ${e.message}") }
            }
        }
    }

    override fun onCleared() {
        calibrationJob?.cancel()
        preparationJob?.cancel()
    }
}
