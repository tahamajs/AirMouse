// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt
package com.airmouse.presentation.ui.calibration

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import android.os.Trace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.IConnectionRepository
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.utils.PreferencesManager
import com.airmouse.presentation.ui.calibration.CalibrationPhase
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Job
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume
import kotlin.math.abs
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
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
        private const val SENSOR_TIMEOUT_EXTRA_MS = 2500L
        private const val GRAVITY_EARTH = 9.80665f
    }

    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

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
    private var preparationJob: Job? = null
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
                val data = withTimeoutOrNull(2000L) {
                    calibrationRepository.getCalibrationData()
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
                            statusMessage = if (data == null) {
                                "No saved calibration found"
                            } else {
                                "Saved calibration is incomplete"
                            },
                            stepInstruction = "Tap Start to begin calibration",
                            calibrationData = null
                        )
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to load calibration", e)
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

    fun startCalibration() {
        if (_isCalibrating.value) return
        viewModelScope.launch {
            Log.d(TAG, "Starting fresh calibration run")
            calibrationRepository.resetAllCalibration()
            resetCalibrationState()
            _uiState.update { it.copy(
                currentStep = 1,
                calibrationPhase = CalibrationPhase.INTRO,
                statusMessage = "Step 1: Gyroscope",
                stepInstruction = "Watch the motion preview, then tap Start to begin sampling"
            ) }
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

// In CalibrationViewModel.kt – REPLACE startGyroSampling:

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
                ) { sample, count ->
                    gyroSamples.add(sample)
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
                calibrationRepository.updateCalibrationStatus(CalibrationStatus.GYRO_COMPLETE)
                calibrationRepository.updateCalibrationProgress(33)

                // Auto-advance to next step
                nextStep()
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun saveGyroData() {
        if (gyroSamples.isEmpty()) return
        val avgX = gyroSamples.map { it.first }.average().toFloat()
        val avgY = gyroSamples.map { it.second }.average().toFloat()
        val avgZ = gyroSamples.map { it.third }.average().toFloat()
        val varX = gyroSamples.variance { it.first }
        val varY = gyroSamples.variance { it.second }
        val varZ = gyroSamples.variance { it.third }
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
                ) { sample, count ->
                    magSamples.add(sample)
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
                calibrationRepository.updateCalibrationStatus(CalibrationStatus.MAG_COMPLETE)
                calibrationRepository.updateCalibrationProgress(66)
                nextStep()
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun saveMagData() {
        if (magSamples.isEmpty()) return
        val minX = magSamples.minOf { it.first }
        val maxX = magSamples.maxOf { it.first }
        val minY = magSamples.minOf { it.second }
        val maxY = magSamples.maxOf { it.second }
        val minZ = magSamples.minOf { it.third }
        val maxZ = magSamples.maxOf { it.third }
        val offX = (minX + maxX) / 2f
        val offY = (minY + maxY) / 2f
        val offZ = (minZ + maxZ) / 2f
        val rangeX = (maxX - minX).coerceAtLeast(0.0001f)
        val rangeY = (maxY - minY).coerceAtLeast(0.0001f)
        val rangeZ = (maxZ - minZ).coerceAtLeast(0.0001f)
        val avgRange = (rangeX + rangeY + rangeZ) / 3f
        dataSource.saveMagOffset(offX, offY, offZ)
        dataSource.saveMagScale(avgRange / rangeX, avgRange / rangeY, avgRange / rangeZ)
        dataSource.saveMagSampleCount(magSamples.size)
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
            ) { sample, count ->
                updateStepProgress(count.toFloat() / ACCEL_SAMPLES_PER_POS)
            }
            if (samples.size < ACCEL_SAMPLES_PER_POS) {
                failCurrentStep("Accelerometer data did not arrive. Keep the phone awake and try again.")
                return@launch
            }
            accelPositions[currentPos] = samples
            saveAccelPosition(currentPos, samples)
            val nextPos = currentPos + 1
            _isCalibrating.value = false
            _uiState.update { it.copy(
                isCalibrating = false, 
                isCollecting = false, 
                currentPosition = nextPos,
                completedPositions = (it.completedPositions + currentPos).distinct(),
                progress = (nextPos * 100 / ACCEL_POSITIONS_NEEDED),
                statusMessage = if (nextPos < ACCEL_POSITIONS_NEEDED) "Position ${nextPos} captured" else "Accelerometer complete",
                calibrationPhase = if (nextPos < ACCEL_POSITIONS_NEEDED) CalibrationPhase.COUNTDOWN else CalibrationPhase.INTRO,
                stepInstruction = if (nextPos < ACCEL_POSITIONS_NEEDED) "Next: ${accelPositionsList.getOrNull(nextPos)}" else "All orientations completed",
                samplesCollected = samples.size,
                totalSamplesNeeded = ACCEL_SAMPLES_PER_POS
            ) }
            if (nextPos >= ACCEL_POSITIONS_NEEDED) {
                saveAccelCalibration()
                calibrationRepository.updateCalibrationStatus(CalibrationStatus.ACCEL_COMPLETE)
                calibrationRepository.updateCalibrationProgress(100)
                nextStep()
            } else {
                delay(250)
                startAccelPositionSampling()
            }
            } finally {
                Trace.endSection()
            }
        }
    }

    private suspend fun saveAccelPosition(position: Int, samples: List<Triple<Float, Float, Float>>) {
        if (samples.isEmpty()) return
        val average = samples.averageTriple()
        dataSource.saveAccelPosition(position, average)
        dataSource.saveAccelPositionsCompleted((position + 1).coerceAtMost(ACCEL_POSITIONS_NEEDED))
    }

    private suspend fun saveAccelCalibration() {
        if (accelPositions.size < ACCEL_POSITIONS_NEEDED) return
        val measurements = (0 until ACCEL_POSITIONS_NEEDED).mapNotNull { accelPositions[it]?.averageTriple() }
        if (measurements.size < ACCEL_POSITIONS_NEEDED) return

        val offsetX = measurements.map { it.first }.average().toFloat()
        val offsetY = measurements.map { it.second }.average().toFloat()
        val offsetZ = measurements.map { it.third }.average().toFloat()

        val scaleX = calculateAxisScale(
            positive = measurements[3].first,
            negative = measurements[2].first
        )
        val scaleY = calculateAxisScale(
            positive = measurements[4].second,
            negative = measurements[5].second
        )
        val scaleZ = calculateAxisScale(
            positive = measurements[0].third,
            negative = measurements[1].third
        )

        dataSource.saveAccelOffset(offsetX, offsetY, offsetZ)
        dataSource.saveAccelScale(scaleX, scaleY, scaleZ)
    }

// In CalibrationViewModel.kt – REPLACE nextStep:

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
            // All steps complete – finalize
            completeCalibration()
        }
    }
// In CalibrationViewModel.kt – REPLACE completeCalibration:

    fun completeCalibration() {
        viewModelScope.launch {
            Trace.beginSection("Calibration#complete")
            try {
                _uiState.update { it.copy(statusMessage = "Finalizing...", isCollecting = true) }
                delay(TRANSITION_DELAY)

                // Get collected data
                val gyro = dataSource.getGyroBias()
                val accelOffset = dataSource.getAccelOffset()
                val accelScale = dataSource.getAccelScale()
                val magOffset = dataSource.getMagOffset()
                val magScale = dataSource.getMagScale()

                // Calculate quality based on gyro variance
                val quality = calculateCalibrationQuality()

                val data = CalibrationData(
                    gyroBias = SensorCalibrationData(
                        offsetX = gyro.first,
                        offsetY = gyro.second,
                        offsetZ = gyro.third
                    ),
                    accelOffset = SensorCalibrationData(
                        offsetX = accelOffset.first,
                        offsetY = accelOffset.second,
                        offsetZ = accelOffset.third,
                        scaleX = accelScale.first,
                        scaleY = accelScale.second,
                        scaleZ = accelScale.third
                    ),
                    magOffset = SensorCalibrationData(
                        offsetX = magOffset.first,
                        offsetY = magOffset.second,
                        offsetZ = magOffset.third,
                        scaleX = magScale.first,
                        scaleY = magScale.second,
                        scaleZ = magScale.third
                    ),
                    isCalibrated = true,
                    quality = quality,
                    timestamp = System.currentTimeMillis()
                )

                // Save to repository
                calibrationRepository.saveCalibrationData(data)
                prefs.setCalibrated(true)
                prefs.setCalibrationTimestamp(data.timestamp)
                calibrationRepository.updateCalibrationStatus(CalibrationStatus.COMPLETED)
                calibrationRepository.updateCalibrationQuality(quality)
                calibrationRepository.updateCalibrationProgress(100)

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

                Log.d(TAG, "Calibration completed successfully with quality: ${quality.name}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to complete calibration", e)
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
        val clamped = value.coerceIn(0f, 1f)
        _uiState.update {
            it.copy(
                stepProgress = clamped,
                progress = (clamped * 100).toInt().coerceIn(0, 100),
                samplesCollected = if (it.totalSamplesNeeded > 0) {
                    (clamped * it.totalSamplesNeeded).toInt().coerceIn(0, it.totalSamplesNeeded)
                } else {
                    it.samplesCollected
                }
            )
        }
    }

    private suspend fun getSensorData(): SensorData = sensorDataProvider?.invoke() ?: SensorData()

// In CalibrationViewModel.kt – REPLACE the existing collectSensorSamples with this:

    private suspend fun collectSensorSamples(
        sensorType: Int,
        samplesNeeded: Int,
        timeoutMs: Long,
        onSample: (Triple<Float, Float, Float>, Int) -> Unit
    ): List<Triple<Float, Float, Float>> {
        val sensor = sensorManager.getDefaultSensor(sensorType) ?: return emptyList()
        val samples = mutableListOf<Triple<Float, Float, Float>>()

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
                    val sample = Triple(event.values[0], event.values[1], event.values[2])
                    samples.add(sample)
                    onSample(sample, samples.size)

                    // Update UI progress
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

            // Timeout
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
        calibrationRepository.updateCalibrationStatus(CalibrationStatus.FAILED)
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

    private fun calculateAxisScale(positive: Float, negative: Float): Float {
        val measuredRange = abs(positive - negative)
        return if (measuredRange > 0.0001f) (2f * GRAVITY_EARTH) / measuredRange else 1f
    }

    private fun calculateCalibrationQuality(): CalibrationQuality {
        val gyroVariance = gyroSamples.variance { it.first } +
            gyroSamples.variance { it.second } +
            gyroSamples.variance { it.third }
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

    private fun List<Triple<Float, Float, Float>>.averageTriple(): Triple<Float, Float, Float> {
        if (isEmpty()) return Triple(0f, 0f, 0f)
        return Triple(
            map { it.first }.average().toFloat(),
            map { it.second }.average().toFloat(),
            map { it.third }.average().toFloat()
        )
    }

    private inline fun List<Triple<Float, Float, Float>>.variance(selector: (Triple<Float, Float, Float>) -> Float): Float {
        if (isEmpty()) return 0f
        val mean = map(selector).average().toFloat()
        return map { value ->
            val delta = selector(value) - mean
            delta * delta
        }.average().toFloat()
    }

    fun resetCalibration() {
        calibrationJob?.cancel()
        resetCalibrationState()
        viewModelScope.launch { calibrationRepository.resetAllCalibration() }
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

    override fun onCleared() {
        calibrationJob?.cancel()
        preparationJob?.cancel()
    }
}
