
@file:Suppress("unused")

package com.airmouse.presentation.ui.home

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.BatteryManager
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Trace
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.domain.model.AppPreferences
import com.airmouse.domain.model.CalibrationState
import com.airmouse.domain.model.ConnectionConfig
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.MouseStatistics
import com.airmouse.domain.model.MovementProfile
import com.airmouse.domain.model.UserPreferences
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.files.FileTransferService
import com.airmouse.presentation.PresentationModeService
import com.airmouse.notifications.NotificationManager
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.ConnectedDeviceStore
import com.airmouse.utils.QRScanner
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    val connectionManager: ConnectionManager,
    private val presentationModeService: PresentationModeService,
    private val fileTransferService: FileTransferService,
    private val calibrationRepository: ICalibrationRepository,
    private val calibrationDataSource: ICalibrationDataSource,
    private val notificationManager: NotificationManager
) : ViewModel() {

    

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    val presentationState: StateFlow<PresentationModeService.PresentationState> = presentationModeService.state
    val transferState: StateFlow<FileTransferService.TransferState> = fileTransferService.state

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _approvalCountdownMs = MutableStateFlow(0L)
    val approvalCountdownMs: StateFlow<Long> = _approvalCountdownMs.asStateFlow()

    private val _calibrationStatus = MutableStateFlow<CalibrationQuality>(CalibrationQuality.UNKNOWN)
    val calibrationStatus: StateFlow<CalibrationQuality> = _calibrationStatus.asStateFlow()

    

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

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

    
    private var calibrationData: com.airmouse.domain.model.CalibrationData? = null
    private var calibrationState: CalibrationState = CalibrationState()
    private var connectionConfig: ConnectionConfig = ConnectionConfig()
    private var mouseProfile: MovementProfile = MovementProfile()
    private var appPreferences: AppPreferences = AppPreferences()
    private var userPreferences: UserPreferences = UserPreferences()
    private var mouseStatistics: MouseStatistics = MouseStatistics()

    

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!_uiState.value.isActive || !_uiState.value.isCalibrated) return

            Trace.beginSection("AirMouse#onSensorChanged")
            try {
                val timestamp = System.currentTimeMillis()
                val dt = if (lastSampleTime > 0) (timestamp - lastSampleTime) / 1000f else 0.016f
                lastSampleTime = timestamp

                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        updateSensorSeries(
                            gyroHistory = listOf(
                                hypot(event.values[0].toDouble(), event.values[1].toDouble()).toFloat(),
                                abs(event.values[2])
                            ),
                            accelHistory = null,
                            magHistory = null
                        )
                        processGyroscope(event.values, dt)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        System.arraycopy(event.values, 0, gravity, 0, 3)
                        updateSensorSeries(
                            gyroHistory = null,
                            accelHistory = listOf(hypot(
                                hypot(event.values[0].toDouble(), event.values[1].toDouble()),
                                event.values[2].toDouble()
                            ).toFloat()),
                            magHistory = null
                        )
                        updateOrientationFromAccelMag()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        System.arraycopy(event.values, 0, magnetic, 0, 3)
                        updateSensorSeries(
                            gyroHistory = null,
                            accelHistory = null,
                            magHistory = listOf(hypot(
                                hypot(event.values[0].toDouble(), event.values[1].toDouble()),
                                event.values[2].toDouble()
                            ).toFloat())
                        )
                        updateOrientationFromAccelMag()
                    }
                    Sensor.TYPE_ROTATION_VECTOR -> processRotationVector(event.values)
                }

                samplesProcessed++
                if (samplesProcessed % 60 == 0) {
                    val elapsedMs = (System.currentTimeMillis() - lastSampleTime).coerceAtLeast(1L)
                    val fps = ((samplesProcessed * 1000L) / elapsedMs).coerceAtMost(120L).toInt()
                    _sensorState.update { it.copy(fps = fps) }
                    samplesProcessed = 0
                }
            } finally {
                Trace.endSection()
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    

    init {
        loadSettingsAndCalibration()
        observeConnection()
        startBatteryMonitoring()
        loadGestureStats()
        startPerformanceMonitor()
        loadCalibrationStatus()

        
        if (_uiState.value.isActive) {
            startSensors()
        }
    }

    

    private fun loadCalibrationStatus() {
        viewModelScope.launch {
            try {
                val data = calibrationRepository.getCalibrationData()
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
                
            }
        }

        
        viewModelScope.launch {
            calibrationRepository.observeCalibrationQuality().collect { quality ->
                _calibrationStatus.value = quality
                _uiState.update {
                    it.copy(calibrationQuality = quality.name)
                }
            }
        }
    }

    

    private fun loadSettingsAndCalibration() {
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
            autoReconnect = prefs.getBoolean("auto_connect", true),
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
                        notificationManager.showConnectionPendingNotification(connectionManager.serverName.value.ifBlank { connectionManager.currentIp.value })
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
                        notificationManager.showConnectionErrorNotification(connectionManager.lastError.value ?: "Connection error")
                        _uiState.update {
                            it.copy(
                                isConnecting = false,
                                isActive = false,
                                connectionStatus = ConnectionStatus.ERROR
                            )
                        }
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
        viewModelScope.launch {
            
        }
    }

    

    private fun syncCalibrationToServer() {
        viewModelScope.launch {
            try {
                val data = calibrationRepository.getCalibrationData()
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

    private fun buildCalibrationMessage(data: com.airmouse.domain.model.CalibrationData): String {
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

    

    private fun processGyroscope(values: FloatArray, dt: Float) {
        Trace.beginSection("AirMouse#processGyroscope")
        try {
        val sensitivity = prefs.getSensitivity()
        val invertX = prefs.getBoolean("invert_x", false)
        val invertY = prefs.getBoolean("invert_y", false)
        val smoothingEnabled = prefs.getBoolean("smoothing_enabled", true)
        val smoothingFactor = prefs.getFloat("smoothing_factor", 0.5f).coerceIn(0.05f, 0.95f)
        val moveDeadzone = max(0.08f, prefs.getFloat("move_deadzone", 0.14f))
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

        
        val now = System.currentTimeMillis()
        if (dx == 0f && dy == 0f) {
            _sensorState.update {
                it.copy(lastDx = 0f, lastDy = 0f)
            }
        } else {
            _sensorState.update {
                it.copy(lastDx = dx, lastDy = dy)
            }
            lastMoveSentAt = now
        }

        if (dx != 0f || dy != 0f) {
            connectionManager.sendMove(dx, dy)
            addMotionSample(dx, dy, dt)
        }

        
        val magnitude = sqrt(
            (values[0] - gyroOffsetX).pow(2) +
                    (values[1] - gyroOffsetY).pow(2) +
                    (values[2] - gyroOffsetZ).pow(2)
        )
        detectGesture(magnitude, now)
        } finally {
            Trace.endSection()
        }
    }

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

    

    private fun detectGesture(magnitude: Float, now: Long = System.currentTimeMillis()) {
        Trace.beginSection("AirMouse#detectGesture")
        try {
        if (now - lastScrollTime < scrollCooldown) return

        val clickThreshold = max(6f, prefs.getFloat("click_threshold", 8f))
        val scrollThreshold = max(4f, prefs.getFloat("scroll_threshold", 5f))
        val rightClickTilt = prefs.getFloat("right_click_tilt", 45f)
        val axisDeadzone = max(0.08f, prefs.getFloat("move_deadzone", 0.14f))
        val clickAxisThreshold = max(1.5f, clickThreshold * 0.20f)
        val scrollAxisThreshold = max(1.0f, scrollThreshold * 0.18f)
        val doubleClickInterval = prefs.getLong("double_click_interval", doubleClickMaxInterval)

        if (magnitude < axisDeadzone) return

        val yawDelta = abs(yaw - motionHistory.lastOrNull()?.dx.orZero())
        val pitchDelta = abs(pitch - motionHistory.lastOrNull()?.dy.orZero())
        val rollDelta = abs(roll)

        val movementStrength = max(yawDelta, pitchDelta)
        val clickStrength = rollDelta

        if (movementStrength < axisDeadzone && clickStrength < axisDeadzone) return

        when {
            clickStrength >= clickThreshold || clickStrength > clickAxisThreshold && movementStrength < scrollAxisThreshold -> {
                val timeSinceLastClick = now - lastClickTime
                if (timeSinceLastClick < doubleClickInterval && timeSinceLastClick > 100) {
                    
                    connectionManager.sendDoubleClick()
                    updateGestureStats("double_click")
                    addLogMessage("Double click")
                    _uiState.update { it.copy(lastGesture = "Double Click") }
                    lastDoubleClickTime = now
                    lastClickTime = 0
                } else {
                    
                    connectionManager.sendClick("left")
                    updateGestureStats("click")
                    addLogMessage("Click")
                    _uiState.update { it.copy(lastGesture = "Click") }
                    lastClickTime = now
                }
                lastScrollTime = now
            }
            movementStrength >= scrollThreshold || movementStrength > scrollAxisThreshold -> {
                val scrollDelta = when {
                    movementStrength > scrollThreshold * 1.5f -> 3
                    movementStrength > scrollThreshold * 1.2f -> 2
                    else -> 1
                }
                val direction = if (pitchDelta >= yawDelta) {
                    if (pitch > 0) 1 else -1
                } else {
                    if (yaw > 0) 1 else -1
                }
                connectionManager.sendScroll(scrollDelta * direction)
                updateGestureStats("scroll")
                addLogMessage("Scroll $scrollDelta")
                _uiState.update { it.copy(lastGesture = "Scroll") }
                lastScrollTime = now
            }
        }

        
        if (abs(roll) > rightClickTilt && now - lastRightClickTime > 2000) {
            connectionManager.sendRightClick()
            updateGestureStats("right_click")
            addLogMessage("Right click")
            _uiState.update { it.copy(lastGesture = "Right Click") }
            lastRightClickTime = now
        }
        } finally {
            Trace.endSection()
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
                gestureStats = GestureStats(
                    clicks = prefs.getInt("stat_clicks", 0),
                    doubleClicks = prefs.getInt("stat_double_clicks", 0),
                    rightClicks = prefs.getInt("stat_right_clicks", 0),
                    scrolls = prefs.getInt("stat_scrolls", 0),
                    gesturesDetected = prefs.getInt("stat_gestures", 0)
                )
            )
        }
    }

    

    fun startSensors() {
        if (isSensorsActive) return
        if (!_uiState.value.isCalibrated) {
            addLogMessage("Please calibrate sensors first")
            return
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
    }

    fun reconnect() {
        disconnect()
        viewModelScope.launch {
            delay(500)
            connect()
        }
    }

    

    fun setControlMode(mode: String) {
        prefs.putString("control_mode", mode)
        _uiState.update { it.copy(controlMode = mode) }
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

    fun clearCompletedTransfers() {
        fileTransferService.clearCompletedTransfers()
        addLogMessage("Completed file transfers cleared")
    }

    fun getTransferFolderPath(): String = fileTransferService.openTransferFolder().absolutePath

    

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

    fun getMotionHistory(): List<MotionSample> = motionHistory.toList()

    fun getAverageSpeed(): Float {
        if (motionHistory.isEmpty()) return 0f
        return motionHistory.map { it.speed }.average().toFloat()
    }

    fun getMaxSpeed(): Float {
        if (motionHistory.isEmpty()) return 0f
        return motionHistory.maxByOrNull { it.speed }?.speed ?: 0f
    }

    fun getSessionDuration(): Long {
        if (motionHistory.isEmpty()) return 0L
        val first = motionHistory.first().timestamp
        val last = motionHistory.last().timestamp
        return last - first
    }

    fun getAverageLatency(): Long {
        if (motionHistory.isEmpty()) return 0L
        return motionHistory.map { (it.dt * 1000f).toLong() }.average().toLong()
    }

    fun getGestureStats(): GestureStats = _uiState.value.gestureStats

    fun getCalibrationQuality(): String = _uiState.value.calibrationQuality

    fun isCalibrated(): Boolean = _uiState.value.isCalibrated

    private fun percentageOf(part: Int, total: Int): Int {
        if (total <= 0) return 0
        return (part * 100 / total).coerceIn(0, 100)
    }

    

    override fun onCleared() {
        super.onCleared()
        stopSensors()
        stopApprovalCountdown()
        addLogMessage("ViewModel cleared")
    }

    

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
        val sensorsCalibrated: Int = 0,
        val totalSensors: Int = 3,
        val remainingAttempts: Int = 5,
        val gestureStats: GestureStats = GestureStats(),
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

    data class GestureStats(
        val clicks: Int = 0,
        val doubleClicks: Int = 0,
        val rightClicks: Int = 0,
        val scrolls: Int = 0,
        val gesturesDetected: Int = 0
    )

    enum class ConnectionQuality(val color: Long, val text: String) {
        EXCELLENT(0xFF4CAF50, "Excellent"),
        GOOD(0xFF8BC34A, "Good"),
        FAIR(0xFFFFC107, "Fair"),
        POOR(0xFFFF5722, "Poor"),
        UNKNOWN(0xFF9E9E9E, "Unknown")
    }
}
