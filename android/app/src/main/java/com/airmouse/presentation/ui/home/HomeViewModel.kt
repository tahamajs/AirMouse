package com.airmouse.presentation.ui.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Trace
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.usecase.CalibrationUseCase
import com.airmouse.files.FileTransferService
import com.airmouse.files.TransferState
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.PresentationModeService
import com.airmouse.notifications.NotificationManager
import com.airmouse.utils.ConnectedDeviceStore
import com.airmouse.utils.QRScanner
import com.airmouse.utils.PreferencesManager
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.AppPreferences
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationState
import com.airmouse.domain.model.StatisticsSummary
import com.airmouse.domain.model.ConnectionQuality
import com.airmouse.domain.model.CalibrationQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

/**
 * ViewModel for the Home screen.
 * Manages connection, sensor processing, gesture detection, calibration, and UI state.
 */
@HiltViewModel
class HomeViewModel @Inject constructor(
    @param:ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    val connectionManager: ConnectionManager,
    private val presentationModeService: PresentationModeService,
    private val fileTransferService: FileTransferService,
    private val calibrationUseCase: CalibrationUseCase,
    private val notificationManager: NotificationManager
) : ViewModel() {

    // ============================================================
    // UI State
    // ============================================================

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    val presentationState: StateFlow<PresentationModeService.PresentationState> = presentationModeService.state
    val transferState: StateFlow<TransferState> = fileTransferService.state

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _approvalCountdownMs = MutableStateFlow(0L)
    val approvalCountdownMs: StateFlow<Long> = _approvalCountdownMs.asStateFlow()

    private val _calibrationStatus = MutableStateFlow<CalibrationQuality>(CalibrationQuality.UNKNOWN)
    val calibrationStatus: StateFlow<CalibrationQuality> = _calibrationStatus.asStateFlow()

    // ============================================================
    // Sensor Components
    // ============================================================

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)
    private val linearAccel: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)

    private var isSensorsActive = false
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f
    private var filteredDx = 0f
    private var filteredDy = 0f
    private var stationarySamples = 0
    private var lastMoveSentAt = 0L

    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    private var lastClickTime = 0L
    private var lastDoubleClickTime = 0L
    private var lastScrollTime = 0L
    private var lastRightClickTime = 0L
    private val scrollCooldown = 300L
    private val doubleClickMaxInterval = 400L
    private val maxLogs = 50

    private var samplesProcessed = 0
    private var lastSampleTime = 0L
    private var currentFps = 0
    private var approvalTimerJob: Job? = null

    private val motionHistory = mutableListOf<MotionSample>()
    private val maxMotionHistory = 30

    // Control mode (GYRO, ACCEL, HYBRID) – cached from preferences
    private var controlMode: ControlMode = prefs.getControlMode().toControlMode()
    private var connectionConfig: ConnectionConfig = ConnectionConfig()
    private var mouseProfile: MovementProfile = MovementProfile()
    private var appPreferences: AppPreferences = AppPreferences()
    private var userPreferences: UserPreferences = UserPreferences()
    private var mouseStatistics: MouseStatistics = MouseStatistics()
    private var calibrationData: CalibrationData? = null
    private var calibrationState: CalibrationState = CalibrationState()
    private var latestAccelX = 0f
    private var latestAccelY = 0f
    private var latestAccelZ = 0f
    private var prevAccelX = 0f
    private var prevAccelY = 0f
    private var prevAccelZ = 0f

    // ============================================================
    // Throttling Constants for Optimized Sensor Processing
    // ============================================================

    private var lastProcessTime = 0L
    private var lastUiUpdateTime = 0L
    private var lastGestureCheckTime = 0L
    private val PROCESS_INTERVAL_MS = 10L
    private val UI_UPDATE_INTERVAL_MS = 33L
    private val MOVE_SEND_INTERVAL_MS = 16L
    private val GESTURE_CHECK_INTERVAL_MS = 50L
    private val sensorProcessingScope = CoroutineScope(Dispatchers.Default + SupervisorJob())

    // ============================================================
    // Optimized Sensor Listener (Background Processing + Throttled UI Updates)
    // ============================================================

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!_uiState.value.isActive) return

            val now = System.currentTimeMillis()
            if (now - lastProcessTime < PROCESS_INTERVAL_MS) return
            lastProcessTime = now

            val dt = if (lastSampleTime > 0) (now - lastSampleTime) / 1000f else 0.016f
            lastSampleTime = now

            sensorProcessingScope.launch {
                Trace.beginSection("AirMouse#onSensorChanged")
                try {
                    when (event.sensor.type) {
                        Sensor.TYPE_GYROSCOPE -> {
                            val gyroData = processGyroscopeOffThread(event.values, dt)

                            withContext(Dispatchers.Main) {
                                updateSensorSeries(
                                    gyroHistory = listOf(
                                        hypot(event.values[0].toDouble(), event.values[1].toDouble()).toFloat(),
                                        abs(event.values[2])
                                    ),
                                    accelHistory = null,
                                    magHistory = null
                                )
                            }

                            val shouldUpdateUi = now - lastUiUpdateTime >= UI_UPDATE_INTERVAL_MS
                            if (shouldUpdateUi) {
                                lastUiUpdateTime = now
                                withContext(Dispatchers.Main) {
                                    _sensorState.update {
                                        it.copy(
                                            lastDx = gyroData.first,
                                            lastDy = gyroData.second,
                                            currentSpeed = gyroData.third
                                        )
                                    }
                                }
                            }

                            val shouldSendMove = now - lastMoveSentAt >= MOVE_SEND_INTERVAL_MS
                            if (shouldSendMove && (gyroData.first != 0f || gyroData.second != 0f)) {
                                lastMoveSentAt = now
                                withContext(Dispatchers.IO) {
                                    val sent = connectionManager.sendMove(gyroData.first, gyroData.second)
                                    if (!sent) {
                                        withContext(Dispatchers.Main) {
                                            addLogMessage("Move packet dropped")
                                        }
                                    }
                                }
                                withContext(Dispatchers.Main) {
                                    addMotionSample(gyroData.first, gyroData.second, dt)
                                }
                            }

                            if (gyroData.third > 0.1f) {
                                val magnitude = gyroData.third
                                withContext(Dispatchers.Main) {
                                    detectGesture(magnitude, now)
                                }
                            }
                        }

                        Sensor.TYPE_ACCELEROMETER -> {
                            latestAccelX = event.values[0]
                            latestAccelY = event.values[1]
                            latestAccelZ = event.values[2]
                            System.arraycopy(event.values, 0, gravity, 0, 3)

                            withContext(Dispatchers.Main) {
                                updateSensorSeries(
                                    gyroHistory = null,
                                    accelHistory = listOf(
                                        hypot(
                                            hypot(event.values[0].toDouble(), event.values[1].toDouble()),
                                            event.values[2].toDouble()
                                        ).toFloat()
                                    ),
                                    magHistory = null
                                )
                                updateOrientationFromAccelMag()
                            }
                        }

                        Sensor.TYPE_MAGNETIC_FIELD -> {
                            System.arraycopy(event.values, 0, magnetic, 0, 3)
                            withContext(Dispatchers.Main) {
                                updateSensorSeries(
                                    gyroHistory = null,
                                    accelHistory = null,
                                    magHistory = listOf(
                                        hypot(
                                            hypot(event.values[0].toDouble(), event.values[1].toDouble()),
                                            event.values[2].toDouble()
                                        ).toFloat()
                                    )
                                )
                                updateOrientationFromAccelMag()
                            }
                        }

                        Sensor.TYPE_ROTATION_VECTOR -> {
                            withContext(Dispatchers.Main) {
                                processRotationVector(event.values)
                            }
                        }

                        Sensor.TYPE_LINEAR_ACCELERATION -> {
                            val linY = event.values[1]
                            withContext(Dispatchers.Main) {
                                detectLinearScroll(linY, now)
                            }
                        }
                    }

                    samplesProcessed++
                    if (samplesProcessed % 60 == 0) {
                        val elapsedMs = (System.currentTimeMillis() - lastSampleTime).coerceAtLeast(1L)
                        val fps = ((samplesProcessed * 1000L) / elapsedMs).coerceAtMost(120L).toInt()
                        withContext(Dispatchers.Main) {
                            _sensorState.update { it.copy(fps = fps) }
                        }
                        samplesProcessed = 0
                    }
                } catch (e: Exception) {
                    Log.e("HomeViewModel", "Sensor processing error", e)
                } finally {
                    Trace.endSection()
                }
            }
        }

        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) {
            // ignored
        }
    }

    // ============================================================
    // OPTIMIZED GYROSCOPE PROCESSING (Off-Thread)
    // ============================================================

    private suspend fun processGyroscopeOffThread(
        values: FloatArray,
        dt: Float
    ): Triple<Float, Float, Float> = withContext(Dispatchers.Default) {
        val sensitivity = prefs.getSensitivity()
        val invertX = prefs.getBoolean("invert_x", false)
        val invertY = prefs.getBoolean("invert_y", false)
        val smoothingEnabled = prefs.getBoolean("smoothing_enabled", true)
        val smoothingFactor = prefs.getFloat("smoothing_factor", 0.7f).coerceIn(0.05f, 0.95f)
        val moveDeadzone = max(0.25f, prefs.getFloat("move_deadzone", 0.3f))
        val maxMoveDelta = prefs.getFloat("max_move_delta", 8f).coerceIn(2f, 30f)
        val stationaryBiasAlpha = prefs.getFloat("stationary_bias_alpha", 0.01f).coerceIn(0.001f, 0.05f)
        val stationaryThreshold = prefs.getFloat("stationary_threshold", 0.12f)

        var dx = (values[2] - gyroOffsetZ) * sensitivity
        var dy = (values[0] - gyroOffsetX) * sensitivity

        if (invertX) dx = -dx
        if (invertY) dy = -dy

        val rawMagnitude = hypot(dx.toDouble(), dy.toDouble()).toFloat()

        if (rawMagnitude < stationaryThreshold) {
            stationarySamples++
            if (stationarySamples > 8) {
                gyroOffsetX = gyroOffsetX * (1 - stationaryBiasAlpha) + values[0] * stationaryBiasAlpha
                gyroOffsetY = gyroOffsetY * (1 - stationaryBiasAlpha) + values[1] * stationaryBiasAlpha
                gyroOffsetZ = gyroOffsetZ * (1 - stationaryBiasAlpha) + values[2] * stationaryBiasAlpha
            }
        } else {
            stationarySamples = 0
        }

        if (smoothingEnabled) {
            filteredDx = filteredDx * (1 - smoothingFactor) + dx * smoothingFactor
            filteredDy = filteredDy * (1 - smoothingFactor) + dy * smoothingFactor
            dx = filteredDx
            dy = filteredDy
        } else {
            filteredDx = dx
            filteredDy = dy
        }

        if (abs(dx) < moveDeadzone) dx = 0f
        if (abs(dy) < moveDeadzone) dy = 0f
        dx = dx.coerceIn(-maxMoveDelta, maxMoveDelta)
        dy = dy.coerceIn(-maxMoveDelta, maxMoveDelta)

        val mode = prefs.getControlMode().toControlMode()
        when (mode) {
            ControlMode.ACCEL -> {
                val accelDx = (latestAccelX - prevAccelX) * sensitivity * 0.5f
                val accelDy = (latestAccelY - prevAccelY) * sensitivity * 0.5f
                prevAccelX = latestAccelX
                prevAccelY = latestAccelY
                dx = accelDx
                dy = accelDy
            }
            ControlMode.HYBRID -> {
                val alpha = 0.5f
                val accelDx = (latestAccelX - prevAccelX) * sensitivity * 0.5f
                val accelDy = (latestAccelY - prevAccelY) * sensitivity * 0.5f
                prevAccelX = latestAccelX
                prevAccelY = latestAccelY
                dx = alpha * dx + (1 - alpha) * accelDx
                dy = alpha * dy + (1 - alpha) * accelDy
            }
            ControlMode.GYRO -> { /* already gyro‑based */ }
        }

        val magnitude = sqrt(dx * dx + dy * dy)
        return@withContext Triple(dx, dy, magnitude)
    }

    // ============================================================
    // SINGLE detectGesture (No duplicates)
    // ============================================================

    private fun detectGesture(magnitude: Float, now: Long = System.currentTimeMillis()) {
        if (now - lastGestureCheckTime < GESTURE_CHECK_INTERVAL_MS) return
        lastGestureCheckTime = now

        Trace.beginSection("AirMouse#detectGesture")
        try {
            if (now - lastScrollTime < scrollCooldown) return

            val clickThreshold = max(3.5f, prefs.getFloat("click_threshold", 4f))
            val scrollThreshold = max(3f, prefs.getFloat("scroll_threshold", 4f))
            val rightClickTilt = prefs.getFloat("right_click_tilt", 45f)
            val axisDeadzone = max(0.08f, prefs.getFloat("move_deadzone", 0.14f))
            val clickAxisThreshold = max(1.5f, clickThreshold * 0.20f)
            val scrollAxisThreshold = max(1.0f, scrollThreshold * 0.18f)
            val doubleClickInterval = prefs.getLong("double_click_interval", doubleClickMaxInterval)

            if (magnitude < axisDeadzone) return

            val yawDelta = abs(yaw - motionHistory.lastOrNull()?.dx.orZero())
            val pitchDelta = abs(pitch - motionHistory.lastOrNull()?.dy.orZero())
            val rollDelta = abs(roll)

            val clickStrength = abs(roll)

            if (clickStrength < axisDeadzone) return

            if (clickStrength >= clickThreshold) {
                val timeSinceLastClick = now - lastClickTime
                if (timeSinceLastClick < doubleClickInterval && timeSinceLastClick > 100) {
                    viewModelScope.launch(Dispatchers.IO) {
                        connectionManager.sendDoubleClick()
                    }
                    updateGestureStats("double_click")
                    addLogMessage("Double click")
                    _uiState.update { it.copy(lastGesture = "Double Click") }
                    lastDoubleClickTime = now
                    lastClickTime = 0
                } else {
                    viewModelScope.launch(Dispatchers.IO) {
                        connectionManager.sendClick("left")
                    }
                    updateGestureStats("click")
                    addLogMessage("Click")
                    _uiState.update { it.copy(lastGesture = "Click") }
                    lastClickTime = now
                }
            }

            if (abs(roll) > rightClickTilt && now - lastRightClickTime > 2000) {
                viewModelScope.launch(Dispatchers.IO) {
                    connectionManager.sendRightClick()
                }
                updateGestureStats("right_click")
                addLogMessage("Right click")
                _uiState.update { it.copy(lastGesture = "Right Click") }
                lastRightClickTime = now
            }
        } finally {
            Trace.endSection()
        }
    }

    private fun detectLinearScroll(linY: Float, now: Long) {
        if (now - lastScrollTime < scrollCooldown) return

        val scrollLinearThreshold = max(2.5f, prefs.getFloat("scroll_threshold", 4f) * 0.7f)
        
        if (abs(linY) > scrollLinearThreshold) {
            val scrollDelta = when {
                abs(linY) > scrollLinearThreshold * 1.5f -> 3
                abs(linY) > scrollLinearThreshold * 1.2f -> 2
                else -> 1
            }
            val direction = if (linY > 0) 1 else -1

            viewModelScope.launch(Dispatchers.IO) {
                connectionManager.sendScroll(scrollDelta * direction)
            }
            updateGestureStats("scroll")
            addLogMessage("Scroll $scrollDelta")
            _uiState.update { it.copy(lastGesture = "Scroll") }
            lastScrollTime = now
        }
    }

    private fun Float?.orZero(): Float = this ?: 0f

    private fun updateGestureStats(gesture: String) {
        when (gesture) {
            "click" -> prefs.putInt("stat_clicks", prefs.getInt("stat_clicks", 0) + 1)
            "double_click" -> prefs.putInt("stat_double_clicks", prefs.getInt("stat_double_clicks", 0) + 1)
            "right_click" -> prefs.putInt("stat_right_clicks", prefs.getInt("stat_right_clicks", 0) + 1)
            "scroll" -> prefs.putInt("stat_scrolls", prefs.getInt("stat_scrolls", 0) + 1)
        }
        prefs.putInt("stat_gestures", prefs.getInt("stat_gestures", 0) + 1)
        loadGestureStats()
    }

    private fun loadGestureStats() {
        _uiState.update {
            it.copy(
                gestureStats = StatisticsSummary(
                    totalClicks = prefs.getInt("stat_clicks", 0),
                    totalDoubleClicks = prefs.getInt("stat_double_clicks", 0),
                    totalRightClicks = prefs.getInt("stat_right_clicks", 0),
                    totalScrolls = prefs.getInt("stat_scrolls", 0),
                    totalMovements = prefs.getInt("stat_gestures", 0)
                )
            )
        }
    }

    private fun String.toControlMode(): ControlMode {
        return when (lowercase(Locale.ROOT)) {
            "accel", "accelerometer" -> ControlMode.ACCEL
            "hybrid" -> ControlMode.HYBRID
            else -> ControlMode.GYRO
        }
    }

    // ============================================================
    // Initialisation
    // ============================================================

    init {
        loadSettingsAndCalibration()
        observeConnection()
        startBatteryMonitoring()
        loadGestureStats()
        startPerformanceMonitor()
        loadCalibrationStatus()
        controlMode = prefs.getControlMode().toControlMode()

        if (_uiState.value.isActive) {
            startSensors()
        }
    }

    // ============================================================
    // Calibration Loading
    // ============================================================

    private fun loadCalibrationStatus() {
        viewModelScope.launch {
            try {
                val data = calibrationUseCase.getCalibrationData()
                calibrationData = data
                _calibrationStatus.value = data.quality
                _uiState.update {
                    it.copy(
                        isCalibrated = data.isCalibrated,
                        calibrationQuality = data.quality.name,
                        calibrationState = calibrationState
                    )
                }
                if (data.isCalibrated) {
                    prefs.putBoolean("calibration_complete", true)
                    prefs.putBoolean("is_calibrated", true)
                    calibrationState = CalibrationState(
                        gyroCalibrated = data.gyroBias.offsetX != 0f || data.gyroBias.offsetY != 0f || data.gyroBias.offsetZ != 0f,
                        accelCalibrated = data.accelOffset.offsetX != 0f || data.accelOffset.offsetY != 0f || data.accelOffset.offsetZ != 0f,
                        magCalibrated = data.magOffset.offsetX != 0f || data.magOffset.offsetY != 0f || data.magOffset.offsetZ != 0f,
                        lastCalibrationDate = data.timestamp,
                        calibrationQuality = when (data.quality) {
                            CalibrationQuality.EXCELLENT -> 1f
                            CalibrationQuality.GOOD -> 0.85f
                            CalibrationQuality.FAIR -> 0.65f
                            CalibrationQuality.POOR -> 0.35f
                            CalibrationQuality.UNKNOWN -> 0f
                        },
                        needsRecalibration = data.quality == CalibrationQuality.POOR || data.quality == CalibrationQuality.UNKNOWN,
                        recommendedActions = if (data.quality == CalibrationQuality.POOR) listOf("Recalibrate before demo") else emptyList()
                    )
                    addLogMessage("Calibration loaded: ${data.quality.name}")
                }
            } catch (e: Exception) {
                // ignore
            }
        }

        viewModelScope.launch {
            calibrationUseCase.observeCalibrationQuality().collect { quality ->
                _calibrationStatus.value = quality
                _uiState.update {
                    it.copy(calibrationQuality = quality.name)
                }
            }
        }
    }

    // ============================================================
    // Settings & Preferences Loading
    // ============================================================

    private fun loadSettingsAndCalibration() {
        val autoConnectEnabled = prefs.getBoolean("auto_connect_enabled", prefs.isAutoConnect())
        prefs.putBoolean("auto_connect_enabled", autoConnectEnabled)
        prefs.setAutoConnect(autoConnectEnabled)

        connectionConfig = ConnectionConfig(
            ip = prefs.getString("last_ip", ""),
            port = prefs.getInt("last_port", 0),
            protocol = try {
                ConnectionProtocol.valueOf(
                    (prefs.getString("last_protocol", prefs.getString("connection_protocol", "WEBSOCKET"))
                        ?: "WEBSOCKET").uppercase()
                )
            } catch (_: Exception) {
                ConnectionProtocol.WEBSOCKET
            },
            autoReconnect = autoConnectEnabled,
            timeoutMs = prefs.getInt("connection_timeout", 5000).toLong()
        ).normalized()
        mouseProfile = MovementProfile(
            sensitivity = prefs.getSensitivity(),
            smoothingEnabled = prefs.getBoolean("smoothing_enabled", true),
            accelerationEnabled = prefs.getBoolean("acceleration_enabled", true),
            invertX = prefs.getBoolean("invert_x", false),
            invertY = prefs.getBoolean("invert_y", false),
            swapAxes = prefs.getBoolean("swap_axes", false),
            deadband = prefs.getFloat("deadband", 0.5f),
            maxSpeed = prefs.getFloat("max_speed", 100f),
            minSpeed = prefs.getFloat("min_speed", 0.5f),
            predictiveBlend = prefs.getFloat("predictive_blend", 0.6f),
            smoothingAlpha = prefs.getFloat("smoothing_alpha", 0.3f)
        )
        appPreferences = AppPreferences(
            theme = prefs.getString("theme", "dark"),
            language = prefs.getLanguage(),
            autoStart = prefs.getBoolean("auto_start_server", false),
            showTrayIcon = true,
            soundEnabled = prefs.isSoundEnabled(),
            notificationsEnabled = prefs.getBoolean("notifications_enabled", true),
            analyticsEnabled = prefs.getBoolean("analytics_enabled", true),
            crashReportingEnabled = prefs.getBoolean("crash_reporting", true)
        )
        userPreferences = UserPreferences(
            username = prefs.getUserName(),
            serverName = prefs.getString("server_name", "Air Mouse Pro"),
            serverIp = connectionConfig.ip,
            serverPort = connectionConfig.port,
            autoConnect = connectionConfig.autoReconnect,
            rememberCredentials = prefs.getBoolean("remember_credentials", true)
        )
        mouseStatistics = MouseStatistics(
            totalClicks = prefs.getInt("stat_clicks", 0),
            totalDoubleClicks = prefs.getInt("stat_double_clicks", 0),
            totalRightClicks = prefs.getInt("stat_right_clicks", 0),
            totalScrolls = prefs.getInt("stat_scrolls", 0),
            totalMovement = prefs.getFloat("movement_distance", 0f),
            movementCount = prefs.getInt("movement_samples", 0),
            averageSpeed = prefs.getFloat("movement_average_speed", 0f),
            lastUpdated = prefs.getLong("movement_last_updated", System.currentTimeMillis())
        )
        _uiState.update {
            it.copy(
                isAutoConnectEnabled = autoConnectEnabled,
                serverIp = connectionConfig.ip,
                serverPort = if (connectionConfig.port > 0) connectionConfig.port else ConnectionConfig.DEFAULT_WEBSOCKET_PORT,
                selectedProtocol = connectionConfig.protocol,
                controlMode = prefs.getString("control_mode", "motion"),
                isCalibrated = prefs.getBoolean("calibration_complete", false) ||
                        prefs.getBoolean("is_calibrated", false),
                aiSmoothingEnabled = prefs.getBoolean("ai_smoothing_enabled", false),
                predictiveEnabled = prefs.getBoolean("predictive_enabled", true),
                userName = prefs.getUserName(),
                theme = appPreferences.theme,
                calibrationState = calibrationState,
                connectionConfig = connectionConfig,
                mouseProfile = mouseProfile,
                appPreferences = appPreferences,
                userPreferences = userPreferences,
                mouseStatistics = mouseStatistics
            )
        }

        gyroOffsetX = prefs.getFloat("gyro_offset_x", prefs.getFloat("gyro_bias_x", 0f))
        gyroOffsetY = prefs.getFloat("gyro_offset_y", prefs.getFloat("gyro_bias_y", 0f))
        gyroOffsetZ = prefs.getFloat("gyro_offset_z", prefs.getFloat("gyro_bias_z", 0f))

        val gyroOk = gyroOffsetX != 0f || gyroOffsetY != 0f || gyroOffsetZ != 0f
        val accelOk = prefs.getFloat("accel_offset_x", 0f) != 0f ||
                prefs.getFloat("accel_offset_y", 0f) != 0f ||
                prefs.getFloat("accel_offset_z", 0f) != 0f
        val magOk = prefs.getFloat("mag_offset_x", 0f) != 0f ||
                prefs.getFloat("mag_offset_y", 0f) != 0f ||
                prefs.getFloat("mag_offset_z", 0f) != 0f

        val calibratedCount = listOf(gyroOk, accelOk, magOk).count { it }
        _uiState.update {
            it.copy(
                sensorsCalibrated = calibratedCount,
                calibrationProgress = percentageOf(calibratedCount, it.totalSensors)
            )
        }
    }

    // ============================================================
    // Connection Observer
    // ============================================================

    private fun observeConnection() {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                val mapped = when (status) {
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> ConnectionStatus.DISCONNECTED
                    ConnectionManager.ConnectionStatus.CONNECTING -> ConnectionStatus.CONNECTING
                    ConnectionManager.ConnectionStatus.CONNECTED -> ConnectionStatus.CONNECTED
                    ConnectionManager.ConnectionStatus.ERROR -> ConnectionStatus.ERROR
                    ConnectionManager.ConnectionStatus.RECONNECTING -> ConnectionStatus.CONNECTING
                    else -> ConnectionStatus.DISCONNECTED
                }

                _uiState.update {
                    it.copy(
                        connectionStatus = mapped,
                        isConnecting = status == ConnectionManager.ConnectionStatus.CONNECTING ||
                                status == ConnectionManager.ConnectionStatus.RECONNECTING
                    )
                }

                when (status) {
                    ConnectionManager.ConnectionStatus.CONNECTING -> {
                        addLogMessage("Server is approving the connection...")
                        startApprovalCountdown()
                        notificationManager.showConnectionPendingNotification(
                            connectionManager.serverName.value.ifBlank { connectionManager.currentIp.value }
                        )
                    }
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        stopApprovalCountdown()
                        addLogMessage("Approved")
                        addLogMessage("Connected to ${connectionManager.currentIp.value}")
                        notificationManager.showConnectedNotification(
                            connectionManager.serverName.value.ifBlank { connectionManager.currentIp.value }
                        )
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isActive = true,
                                connectionStatus = ConnectionStatus.CONNECTED
                            )
                        }
                        updateConnectionQuality()
                        startSensors()
                        addLogMessage("Control enabled")
                        syncCalibrationToServer()
                    }
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> {
                        stopApprovalCountdown()
                        addLogMessage("Disconnected")
                        notificationManager.showDisconnectedNotification()
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isActive = false,
                                connectionStatus = ConnectionStatus.DISCONNECTED
                            )
                        }
                        stopSensors()
                    }
                    ConnectionManager.ConnectionStatus.ERROR -> {
                        stopApprovalCountdown()
                        addLogMessage(connectionManager.lastError.value ?: "Connection error")
                        notificationManager.showConnectionErrorNotification(
                            connectionManager.lastError.value ?: "Connection error"
                        )
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isActive = false,
                                connectionStatus = ConnectionStatus.ERROR
                            )
                        }
                        stopSensors()
                    }
                    else -> {}
                }
            }
        }

        viewModelScope.launch {
            connectionManager.connectionQuality.collect { quality ->
                val mapped = when {
                    quality.ping < 30 -> ConnectionQuality.EXCELLENT
                    quality.ping < 60 -> ConnectionQuality.GOOD
                    quality.ping < 100 -> ConnectionQuality.FAIR
                    quality.ping < 200 -> ConnectionQuality.POOR
                    else -> ConnectionQuality.UNKNOWN
                }
                _connectionQuality.value = mapped
                _uiState.update { it.copy(connectionQuality = mapped) }
            }
        }
    }

    // ============================================================
    // Approval Countdown
    // ============================================================

    private fun startApprovalCountdown() {
        approvalTimerJob?.cancel()
        approvalTimerJob = viewModelScope.launch {
            var remaining = 30_000L
            _approvalCountdownMs.value = remaining
            while (remaining > 0 && _uiState.value.isConnecting) {
                delay(1_000)
                remaining -= 1_000
                _approvalCountdownMs.value = remaining.coerceAtLeast(0L)
            }
        }
    }

    private fun stopApprovalCountdown() {
        approvalTimerJob?.cancel()
        approvalTimerJob = null
        _approvalCountdownMs.value = 0L
    }

    private fun updateConnectionQuality() {
        // handled by the flow above
    }

    // ============================================================
    // Calibration Sync
    // ============================================================

    private fun syncCalibrationToServer() {
        viewModelScope.launch {
            try {
                val data = calibrationUseCase.getCalibrationData()
                if (data.isCalibrated) {
                    val message = buildCalibrationMessage(data)
                    connectionManager.send(message)
                    addLogMessage("Calibration synced to server")
                }
            } catch (e: Exception) {
                addLogMessage("Failed to sync calibration: ${e.message}")
            }
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

    // ============================================================
    // Orientation & Sensor Series
    // ============================================================

    private fun processRotationVector(values: FloatArray) {
        SensorManager.getRotationMatrixFromVector(rotationMatrix, values)
        SensorManager.getOrientation(rotationMatrix, orientation)
        yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
        pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
        roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
        updateOrientationDisplay()
    }

    private fun updateOrientationFromAccelMag() {
        val success = SensorManager.getRotationMatrix(rotationMatrix, null, gravity, magnetic)
        if (success) {
            SensorManager.getOrientation(rotationMatrix, orientation)
            yaw = Math.toDegrees(orientation[0].toDouble()).toFloat()
            pitch = Math.toDegrees(orientation[1].toDouble()).toFloat()
            roll = Math.toDegrees(orientation[2].toDouble()).toFloat()
            updateOrientationDisplay()
        }
    }

    private fun updateOrientationDisplay() {
        _uiState.update {
            it.copy(
                orientationYaw = yaw,
                orientationPitch = pitch,
                orientationRoll = roll
            )
        }
    }

    private fun updateSensorSeries(
        gyroHistory: List<Float>? = null,
        accelHistory: List<Float>? = null,
        magHistory: List<Float>? = null
    ) {
        val maxPoints = 24
        _sensorState.update { state ->
            state.copy(
                gyroSeries = gyroHistory?.let { trimToSeries(it, state.gyroSeries, maxPoints) } ?: state.gyroSeries,
                accelSeries = accelHistory?.let { trimToSeries(it, state.accelSeries, maxPoints) } ?: state.accelSeries,
                magSeries = magHistory?.let { trimToSeries(it, state.magSeries, maxPoints) } ?: state.magSeries
            )
        }
    }

    private fun trimToSeries(values: List<Float>, current: List<Float>, maxPoints: Int): List<Float> {
        val combined = (current + values).takeLast(maxPoints)
        return combined.map { it.coerceIn(-50f, 50f) }
    }

    private fun addMotionSample(dx: Float, dy: Float, dt: Float) {
        val speed = sqrt(dx * dx + dy * dy)
        val sample = MotionSample(
            timestamp = System.currentTimeMillis(),
            dx = dx,
            dy = dy,
            speed = speed,
            dt = dt
        )
        motionHistory.add(sample)
        if (motionHistory.size > maxMotionHistory) {
            motionHistory.removeAt(0)
        }

        _sensorState.update {
            it.copy(
                currentSpeed = speed,
                sampleCount = it.sampleCount + 1
            )
        }
    }

    // ============================================================
    // Air Mouse Activation / Deactivation
    // ============================================================

    fun startAirMouse() {
        if (!_uiState.value.isCalibrated) {
            addLogMessage("Please calibrate sensors first")
            return
        }
        if (_uiState.value.connectionStatus != ConnectionStatus.CONNECTED) {
            addLogMessage("Please connect to a server first")
            return
        }
        if (!_uiState.value.isActive) {
            _uiState.update { it.copy(isActive = true) }
            startSensors()
            addLogMessage("Air Mouse activated")
        }
    }

    fun stopAirMouse() {
        if (_uiState.value.isActive) {
            _uiState.update { it.copy(isActive = false) }
            stopSensors()
            addLogMessage("Air Mouse deactivated")
        }
    }

    fun toggleAirMouse() {
        if (_uiState.value.isActive) {
            stopAirMouse()
        } else {
            startAirMouse()
        }
    }

    fun isAirMouseActive(): Boolean = _uiState.value.isActive

    // ============================================================
    // Sensor Control
    // ============================================================

    fun startSensors() {
        if (isSensorsActive) return
        if (!_uiState.value.isCalibrated) {
            addLogMessage("Calibration missing; starting fallback motion mode")
        }

        isSensorsActive = true
        filteredDx = 0f
        filteredDy = 0f
        stationarySamples = 0
        val sensorDelay = SensorManager.SENSOR_DELAY_GAME

        gyroscope?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }
        accelerometer?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }
        magnetometer?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }
        rotationVector?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }
        linearAccel?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }

        _sensorState.update { it.copy(isActive = true) }
        addLogMessage("Sensors activated")
    }

    fun stopSensors() {
        if (!isSensorsActive) return
        isSensorsActive = false
        sensorManager.unregisterListener(sensorListener)
        _sensorState.update { it.copy(isActive = false) }
        addLogMessage("Sensors stopped")
    }

    // ============================================================
    // Connection Actions
    // ============================================================

    fun toggleAutoConnect() {
        val current = _uiState.value.isAutoConnectEnabled
        val newValue = !current
        prefs.putBoolean("auto_connect_enabled", newValue)
        prefs.setAutoConnect(newValue)
        _uiState.update { it.copy(isAutoConnectEnabled = newValue) }
        addLogMessage("Auto-reconnect ${if (newValue) "enabled" else "disabled"}")
    }

    fun updateIp(ip: String) {
        prefs.putString("last_ip", ip)
        _uiState.update {
            it.copy(
                serverIp = ip,
                connectionConfig = it.connectionConfig.copy(ip = ip)
            )
        }
    }

    fun updatePort(port: Int) {
        prefs.putInt("last_port", port)
        _uiState.update {
            it.copy(
                serverPort = port,
                connectionConfig = it.connectionConfig.copy(port = port)
            )
        }
    }

    fun updateConnectionProtocol(protocol: ConnectionProtocol) {
        val normalizedPort = ConnectionConfig(
            ip = _uiState.value.serverIp,
            port = _uiState.value.serverPort,
            protocol = protocol
        ).normalized().port
        prefs.putString("last_protocol", protocol.name)
        prefs.putString("connection_protocol", protocol.name)
        _uiState.update {
            it.copy(
                selectedProtocol = protocol,
                serverPort = normalizedPort,
                connectionConfig = it.connectionConfig.copy(protocol = protocol, port = normalizedPort)
            )
        }
        addLogMessage("Connection type set to ${protocol.name.lowercase(Locale.US)}")
    }

    fun applyScannedConnection(data: QRScanner.ConnectionData) {
        val protocol = runCatching { ConnectionProtocol.valueOf(data.protocol.uppercase(Locale.US)) }
            .getOrDefault(ConnectionProtocol.WEBSOCKET)
        val normalized = ConnectionConfig(
            ip = data.ip,
            port = data.port,
            protocol = protocol
        ).normalized()
        _uiState.update {
            it.copy(
                serverIp = data.ip,
                serverPort = normalized.port,
                selectedProtocol = protocol,
                connectionConfig = it.connectionConfig.copy(
                    ip = normalized.ip,
                    port = normalized.port,
                    protocol = protocol
                ),
                isConnecting = true
            )
        }
        prefs.putString("last_ip", data.ip)
        prefs.putString("last_protocol", protocol.name)
        prefs.putString("connection_protocol", protocol.name)
        data.token?.let { prefs.putString("auth_token", it) }
        viewModelScope.launch {
            prefs.putInt("last_port", normalized.port)
            connectionManager.setProtocol(
                when (protocol) {
                    ConnectionProtocol.TCP -> ConnectionManager.ConnectionProtocol.TCP
                    ConnectionProtocol.UDP -> ConnectionManager.ConnectionProtocol.UDP
                    ConnectionProtocol.WEBSOCKET -> ConnectionManager.ConnectionProtocol.WEBSOCKET
                }
            )
            val success = connectionManager.connect(normalized.ip, normalized.port)
            if (success) {
                ConnectedDeviceStore.rememberConnection(
                    prefs = prefs,
                    serverName = connectionManager.serverName.value.ifBlank { data.name },
                    ip = normalized.ip,
                    port = normalized.port,
                    protocol = protocol.name,
                    version = connectionManager.serverVersion.value.ifBlank { "3.0.0" }
                )
                addLogMessage("Connected to ${data.name}")
                _uiState.update { it.copy(isConnecting = false) }
            } else {
                connectionManager.lastError.value?.let { addLogMessage(it) }
                _uiState.update { it.copy(isConnecting = false) }
            }
        }
    }

    fun connect() {
        Trace.beginSection("AirMouse#connect")
        try {
            val ip = _uiState.value.serverIp
            if (ip.isBlank()) {
                addLogMessage("Please enter server IP")
                return
            }

            val protocol = _uiState.value.selectedProtocol
            val normalized = ConnectionConfig(
                ip = ip,
                port = _uiState.value.serverPort,
                protocol = protocol
            ).normalized()

            _uiState.update {
                it.copy(
                    serverPort = normalized.port,
                    connectionConfig = it.connectionConfig.copy(
                        ip = normalized.ip,
                        port = normalized.port,
                        protocol = protocol
                    ),
                    isConnecting = true
                )
            }
            prefs.putString("last_protocol", protocol.name)
            prefs.putString("connection_protocol", protocol.name)
            prefs.putInt("last_port", normalized.port)
            connectionManager.setProtocol(
                when (protocol) {
                    ConnectionProtocol.TCP -> ConnectionManager.ConnectionProtocol.TCP
                    ConnectionProtocol.UDP -> ConnectionManager.ConnectionProtocol.UDP
                    ConnectionProtocol.WEBSOCKET -> ConnectionManager.ConnectionProtocol.WEBSOCKET
                }
            )

            addLogMessage("Waiting for server approval...")
            _approvalCountdownMs.value = 30_000L

            viewModelScope.launch {
                val success = connectionManager.connect(normalized.ip, normalized.port)
                if (!success) {
                    connectionManager.lastError.value?.let { addLogMessage(it) }
                    _uiState.update { it.copy(isConnecting = false) }
                    stopApprovalCountdown()
                } else {
                    ConnectedDeviceStore.rememberConnection(
                        prefs = prefs,
                        serverName = connectionManager.serverName.value.ifBlank { ip },
                        ip = normalized.ip,
                        port = normalized.port,
                        protocol = protocol.name,
                        version = connectionManager.serverVersion.value.ifBlank { "3.0.0" }
                    )
                }
            }
        } finally {
            Trace.endSection()
        }
    }

    fun disconnect() {
        val shouldAutoReconnect = _uiState.value.isAutoConnectEnabled
        connectionManager.disconnect()
        stopApprovalCountdown()
        _uiState.update {
            it.copy(
                isActive = false,
                isConnecting = false,
                connectionStatus = ConnectionStatus.DISCONNECTED
            )
        }
        stopSensors()
        addLogMessage(
            if (shouldAutoReconnect) {
                "Disconnected. Auto-reconnect is ON, reconnecting shortly..."
            } else {
                "Disconnected. Auto-reconnect is OFF."
            }
        )
        if (shouldAutoReconnect) {
            viewModelScope.launch {
                delay(1500)
                if (_uiState.value.connectionStatus == ConnectionStatus.DISCONNECTED &&
                    _uiState.value.isAutoConnectEnabled
                ) {
                    connect()
                }
            }
        }
    }

    fun reconnect() {
        disconnect()
        viewModelScope.launch {
            delay(500)
            connect()
        }
    }

    // ============================================================
    // Settings & Preferences Updaters
    // ============================================================

    fun setControlMode(mode: String) {
        prefs.setControlMode(mode)
        _uiState.update { it.copy(controlMode = mode) }
        controlMode = mode.toControlMode()
        addLogMessage("Control mode changed to $mode")
    }

    fun toggleAiSmoothing() {
        val current = _uiState.value.aiSmoothingEnabled
        prefs.putBoolean("ai_smoothing_enabled", !current)
        _uiState.update { it.copy(aiSmoothingEnabled = !current) }
        addLogMessage("AI Smoothing ${if (!current) "enabled" else "disabled"}")
    }

    fun togglePredictive() {
        val current = _uiState.value.predictiveEnabled
        prefs.putBoolean("predictive_enabled", !current)
        _uiState.update { it.copy(predictiveEnabled = !current) }
        addLogMessage("Predictive movement ${if (!current) "enabled" else "disabled"}")
    }

    fun updateTheme(theme: String) {
        prefs.putString("theme", theme)
        _uiState.update { it.copy(theme = theme) }
        addLogMessage("Theme changed to $theme")
    }

    fun resetGestureStats() {
        prefs.putInt("stat_clicks", 0)
        prefs.putInt("stat_double_clicks", 0)
        prefs.putInt("stat_right_clicks", 0)
        prefs.putInt("stat_scrolls", 0)
        prefs.putInt("stat_gestures", 0)
        loadGestureStats()
        addLogMessage("Gesture statistics reset")
    }

    // ============================================================
    // Logging
    // ============================================================

    fun addLogMessage(message: String) {
        val timestamp = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date())
        val newLogs = (listOf("[$timestamp] $message") + _uiState.value.logMessages).take(maxLogs)
        _uiState.update { it.copy(logMessages = newLogs) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logMessages = emptyList()) }
    }

    fun clearRecentGestures() {
        _uiState.update { it.copy(lastGesture = "") }
        addLogMessage("Recent gestures cleared")
    }

    // ============================================================
    // Presentation Mode
    // ============================================================

    fun togglePresentationMode() {
        if (presentationState.value.isActive) {
            presentationModeService.disablePresentationMode()
            addLogMessage("Presentation mode disabled")
        } else {
            presentationModeService.enablePresentationMode()
            addLogMessage("Presentation mode enabled")
        }
    }

    fun clearPresentationOverlay() {
        presentationModeService.clearScreenOverlay()
        addLogMessage("Presentation overlay cleared")
    }

    // ============================================================
    // File Transfer
    // ============================================================

    fun clearCompletedTransfers() {
        fileTransferService.clearCompletedTransfers()
        addLogMessage("Completed file transfers cleared")
    }

    fun getTransferFolderPath(): String = fileTransferService.getTransferFolderPath() // FIXED: used correct method

    // ============================================================
    // Background Monitoring
    // ============================================================

    private fun startBatteryMonitoring() {
        viewModelScope.launch {
            while (true) {
                try {
                    val batteryIntent = context.registerReceiver(
                        null,
                        IntentFilter(Intent.ACTION_BATTERY_CHANGED)
                    )
                    val level = batteryIntent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                    val scale = batteryIntent?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                    val batteryLevel = if (level >= 0 && scale > 0) {
                        percentageOf(level, scale)
                    } else 100
                    _uiState.update { it.copy(batteryLevel = batteryLevel) }
                } catch (_: Exception) {
                    // ignore
                }
                delay(60000)
            }
        }
    }

    private fun startPerformanceMonitor() {
        viewModelScope.launch {
            while (true) {
                delay(2000)
                val runtime = Runtime.getRuntime()
                val usedMemory = (runtime.totalMemory() - runtime.freeMemory()) / 1024 / 1024
                val totalMemory = runtime.totalMemory() / 1024 / 1024
                _sensorState.update {
                    it.copy(
                        memoryUsage = usedMemory,
                        totalMemory = totalMemory,
                        cpuUsage = 0f
                    )
                }
            }
        }
    }

    // ============================================================
    // User Info
    // ============================================================

    fun getUserName(): String = prefs.getUserName()
    fun hasRegisteredUser(): Boolean = !prefs.isFirstLaunch() && prefs.getUserName().isNotBlank()

    fun saveUserName(name: String) {
        val cleanedName = name.trim()
        if (cleanedName.isBlank()) return
        prefs.setUserName(cleanedName)
        prefs.setFirstLaunchComplete()
        _uiState.update { it.copy(userName = cleanedName) }
        addLogMessage("Welcome, $cleanedName")
    }

    fun getServerName(): String = prefs.getString("server_name", "Air Mouse Pro")

    // ============================================================
    // Statistics / History
    // ============================================================

    fun getMotionHistory(): List<MotionSample> = motionHistory.toList()
    fun getAverageSpeed(): Float = motionHistory.map { it.speed }.average().toFloat()
    fun getMaxSpeed(): Float = motionHistory.maxByOrNull { it.speed }?.speed ?: 0f
    fun getSessionDuration(): Long =
        if (motionHistory.isNotEmpty()) motionHistory.last().timestamp - motionHistory.first().timestamp else 0L
    fun getAverageLatency(): Long =
        if (motionHistory.isNotEmpty()) motionHistory.map { (it.dt * 1000f).toLong() }.average().toLong() else 0L

    fun getGestureStats(): StatisticsSummary = _uiState.value.gestureStats
    fun getCalibrationQuality(): String = _uiState.value.calibrationQuality
    fun isCalibrated(): Boolean = _uiState.value.isCalibrated

    private fun percentageOf(part: Int, total: Int): Int {
        if (total <= 0) return 0
        return (part * 100 / total).coerceIn(0, 100)
    }

    // ============================================================
    // Cleanup
    // ============================================================

    override fun onCleared() {
        super.onCleared()
        stopSensors()
        stopApprovalCountdown()
        addLogMessage("ViewModel cleared")
    }

    // ============================================================
    // Inner Classes
    // ============================================================

    data class MotionSample(
        val timestamp: Long,
        val dx: Float,
        val dy: Float,
        val speed: Float,
        val dt: Float
    )

    data class SensorState(
        val isActive: Boolean = false,
        val currentSpeed: Float = 0f,
        val lastDx: Float = 0f,
        val lastDy: Float = 0f,
        val sampleCount: Int = 0,
        val fps: Int = 0,
        val memoryUsage: Long = 0,
        val totalMemory: Long = 0,
        val cpuUsage: Float = 0f,
        val gyroSeries: List<Float> = emptyList(),
        val accelSeries: List<Float> = emptyList(),
        val magSeries: List<Float> = emptyList()
    )

    data class HomeUiState(
        val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
        val serverIp: String = "",
        val serverPort: Int = ConnectionConfig.DEFAULT_WEBSOCKET_PORT,
        val selectedProtocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET,
        val calibrationProgress: Int = 0,
        val isAutoConnectEnabled: Boolean = true,
        val sensorsCalibrated: Int = 0,
        val totalSensors: Int = 3,
        val remainingAttempts: Int = 5,
        val gestureStats: StatisticsSummary = StatisticsSummary(),
        val orientationYaw: Float = 0f,
        val orientationPitch: Float = 0f,
        val orientationRoll: Float = 0f,
        val controlMode: String = "motion",
        val isActive: Boolean = false,
        val aiSmoothingEnabled: Boolean = false,
        val predictiveEnabled: Boolean = false,
        val logMessages: List<String> = emptyList(),
        val batteryLevel: Int = 100,
        val connectionQuality: ConnectionQuality = ConnectionQuality.UNKNOWN,
        val lastGesture: String = "",
        val isCalibrated: Boolean = false,
        val isConnecting: Boolean = false,
        val userName: String = "",
        val theme: String = "dark",
        val calibrationQuality: String = "UNKNOWN",
        val calibrationState: CalibrationState = CalibrationState(),
        val connectionConfig: ConnectionConfig = ConnectionConfig(),
        val mouseProfile: MovementProfile = MovementProfile(),
        val appPreferences: AppPreferences = AppPreferences(),
        val userPreferences: UserPreferences = UserPreferences(),
        val mouseStatistics: MouseStatistics = MouseStatistics()
    )

    enum class ControlMode {
        GYRO,
        ACCEL,
        HYBRID
    }
}
