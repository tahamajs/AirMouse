// app/src/main/java/com/airmouse/presentation/ui/home/HomeViewModel.kt
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.QRScanner
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
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
    private val calibrationRepository: ICalibrationRepository,
    private val calibrationDataSource: ICalibrationDataSource
) : ViewModel() {

    // ==================== STATE ====================

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private val _sensorState = MutableStateFlow(SensorState())
    val sensorState: StateFlow<SensorState> = _sensorState.asStateFlow()

    private val _connectionQuality = MutableStateFlow(ConnectionQuality.UNKNOWN)
    val connectionQuality: StateFlow<ConnectionQuality> = _connectionQuality.asStateFlow()

    private val _calibrationStatus = MutableStateFlow<CalibrationQuality>(CalibrationQuality.UNKNOWN)
    val calibrationStatus: StateFlow<CalibrationQuality> = _calibrationStatus.asStateFlow()

    // ==================== SENSOR SETUP ====================

    private val sensorManager: SensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroscope: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelerometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
    private val magnetometer: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
    private val rotationVector: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

    private var isSensorsActive = false
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f

    // Orientation tracking
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private val gravity = FloatArray(3)
    private val magnetic = FloatArray(3)
    private val rotationMatrix = FloatArray(9)
    private val orientation = FloatArray(3)

    // Gesture detection state
    private var lastClickTime = 0L
    private var lastDoubleClickTime = 0L
    private var lastScrollTime = 0L
    private var lastRightClickTime = 0L
    private val scrollCooldown = 300L
    private val doubleClickMaxInterval = 400L
    private val maxLogs = 50

    // Performance tracking
    private var samplesProcessed = 0
    private var lastSampleTime = 0L
    private var currentFps = 0

    // Motion history for pattern detection
    private val motionHistory = mutableListOf<MotionSample>()
    private val maxMotionHistory = 30

    // Calibration data
    private var calibrationData: com.airmouse.domain.model.CalibrationData? = null

    // ==================== SENSOR LISTENER ====================

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!_uiState.value.isActive || !_uiState.value.isCalibrated) return

            val timestamp = System.currentTimeMillis()
            val dt = if (lastSampleTime > 0) (timestamp - lastSampleTime) / 1000f else 0.016f
            lastSampleTime = timestamp

            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> processGyroscope(event.values, dt)
                Sensor.TYPE_ACCELEROMETER -> {
                    System.arraycopy(event.values, 0, gravity, 0, 3)
                    updateOrientationFromAccelMag()
                }
                Sensor.TYPE_MAGNETIC_FIELD -> {
                    System.arraycopy(event.values, 0, magnetic, 0, 3)
                    updateOrientationFromAccelMag()
                }
                Sensor.TYPE_ROTATION_VECTOR -> processRotationVector(event.values)
            }

            samplesProcessed++
            if (samplesProcessed % 60 == 0) {
                val fps = ((samplesProcessed * 1000L) / (System.currentTimeMillis() - lastSampleTime)).coerceAtMost(120L).toInt()
                _sensorState.update { it.copy(fps = fps) }
                samplesProcessed = 0
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    // ==================== INITIALIZATION ====================

    init {
        loadSettingsAndCalibration()
        observeConnection()
        startBatteryMonitoring()
        loadGestureStats()
        startPerformanceMonitor()
        loadCalibrationStatus()

        // Auto-connect if enabled
        if (prefs.getBoolean("auto_connect", true)) {
            viewModelScope.launch {
                val ip = _uiState.value.serverIp
                val port = _uiState.value.serverPort
                if (ip.isNotBlank()) {
                    connectionManager.connect(ip, port)
                    addLogMessage("Auto-connecting to $ip:$port")
                }
            }
        }

        // Start sensor listener if already active
        if (_uiState.value.isActive) {
            startSensors()
        }
    }

    // ==================== CALIBRATION LOADING ====================

    private fun loadCalibrationStatus() {
        viewModelScope.launch {
            try {
                val data = calibrationRepository.getCalibrationData()
                calibrationData = data
                _calibrationStatus.value = data.quality
                _uiState.update {
                    it.copy(
                        isCalibrated = data.isCalibrated,
                        calibrationQuality = data.quality.name
                    )
                }
                if (data.isCalibrated) {
                    addLogMessage("Calibration loaded: ${data.quality.name}")
                }
            } catch (e: Exception) {
                // Handle error
            }
        }

        // Also observe calibration quality
        viewModelScope.launch {
            calibrationRepository.observeCalibrationQuality().collect { quality ->
                _calibrationStatus.value = quality
                _uiState.update {
                    it.copy(calibrationQuality = quality.name)
                }
            }
        }
    }

    // ==================== SETTINGS & CALIBRATION ====================

    private fun loadSettingsAndCalibration() {
        _uiState.update {
            it.copy(
                serverIp = prefs.getString("last_ip", ""),
                serverPort = prefs.getInt("last_port", 8080),
                controlMode = prefs.getString("control_mode", "motion"),
                isCalibrated = prefs.getBoolean("calibration_complete", false),
                aiSmoothingEnabled = prefs.getBoolean("ai_smoothing_enabled", false),
                predictiveEnabled = prefs.getBoolean("predictive_enabled", true),
                userName = prefs.getUserName(),
                theme = prefs.getString("theme", "dark")
            )
        }

        // Load gyro offsets
        gyroOffsetX = prefs.getFloat("gyro_offset_x", 0f)
        gyroOffsetY = prefs.getFloat("gyro_offset_y", 0f)
        gyroOffsetZ = prefs.getFloat("gyro_offset_z", 0f)

        // Calculate calibration progress
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
                calibrationProgress = if (it.totalSensors > 0) {
                    (calibratedCount * 100 / it.totalSensors)
                } else 0
            )
        }
    }

    // ==================== CONNECTION OBSERVER ====================

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
                    ConnectionManager.ConnectionStatus.CONNECTED -> {
                        addLogMessage("Connected to ${connectionManager.currentIp.value}")
                        updateConnectionQuality()
                        startSensors()
                        // Sync calibration when connected
                        syncCalibrationToServer()
                    }
                    ConnectionManager.ConnectionStatus.DISCONNECTED -> {
                        addLogMessage("Disconnected")
                        stopSensors()
                        _uiState.update { it.copy(isActive = false) }
                    }
                    ConnectionManager.ConnectionStatus.ERROR -> {
                        addLogMessage("Connection error")
                    }
                    else -> {}
                }
            }
        }

        // Observe connection quality
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

    private fun updateConnectionQuality() {
        viewModelScope.launch {
            // Connection quality is updated via the observer above
        }
    }

    // ==================== CALIBRATION SYNC ====================

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

    // ==================== SENSOR PROCESSING ====================

    private fun processGyroscope(values: FloatArray, dt: Float) {
        val sensitivity = prefs.getSensitivity()
        val invertX = prefs.getBoolean("invert_x", false)
        val invertY = prefs.getBoolean("invert_y", false)
        val smoothingEnabled = prefs.getBoolean("smoothing_enabled", true)

        var dx = (values[2] - gyroOffsetZ) * sensitivity
        var dy = (values[0] - gyroOffsetX) * sensitivity

        if (invertX) dx = -dx
        if (invertY) dy = -dy

        // Apply smoothing (EMA)
        if (smoothingEnabled) {
            val alpha = 0.3f
            val smoothedDx = _sensorState.value.lastDx * (1 - alpha) + dx * alpha
            val smoothedDy = _sensorState.value.lastDy * (1 - alpha) + dy * alpha
            _sensorState.update {
                it.copy(lastDx = smoothedDx, lastDy = smoothedDy)
            }
            dx = smoothedDx
            dy = smoothedDy
        }

        // Deadband
        if (abs(dx) < 0.5f) dx = 0f
        if (abs(dy) < 0.5f) dy = 0f

        if (dx != 0f || dy != 0f) {
            connectionManager.sendMove(dx, dy)
            addMotionSample(dx, dy, dt)
        }

        // Detect gestures
        val magnitude = sqrt(
            (values[0] - gyroOffsetX).pow(2) +
                    (values[1] - gyroOffsetY).pow(2) +
                    (values[2] - gyroOffsetZ).pow(2)
        )
        detectGesture(magnitude)
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

    // ==================== GESTURE DETECTION ====================

    private fun detectGesture(magnitude: Float) {
        val now = System.currentTimeMillis()
        if (now - lastScrollTime < scrollCooldown) return

        val clickThreshold = prefs.getFloat("click_threshold", 8f)
        val scrollThreshold = prefs.getFloat("scroll_threshold", 5f)
        val rightClickTilt = prefs.getFloat("right_click_tilt", 45f)
        val doubleClickInterval = prefs.getLong("double_click_interval", doubleClickMaxInterval)

        when {
            magnitude > clickThreshold -> {
                val timeSinceLastClick = now - lastClickTime
                if (timeSinceLastClick < doubleClickInterval && timeSinceLastClick > 100) {
                    // Double click
                    connectionManager.sendDoubleClick()
                    updateGestureStats("double_click")
                    addLogMessage("Double click")
                    _uiState.update { it.copy(lastGesture = "Double Click") }
                    lastDoubleClickTime = now
                    lastClickTime = 0
                } else {
                    // Single click
                    connectionManager.sendClick("left")
                    updateGestureStats("click")
                    addLogMessage("Click")
                    _uiState.update { it.copy(lastGesture = "Click") }
                    lastClickTime = now
                }
                lastScrollTime = now
            }
            magnitude > scrollThreshold -> {
                val scrollDelta = when {
                    magnitude > 7f -> 3
                    magnitude > 6f -> 2
                    else -> 1
                }
                connectionManager.sendScroll(scrollDelta)
                updateGestureStats("scroll")
                addLogMessage("Scroll $scrollDelta")
                _uiState.update { it.copy(lastGesture = "Scroll") }
                lastScrollTime = now
            }
        }

        // Right click detection (tilt)
        if (abs(roll) > rightClickTilt && now - lastRightClickTime > 2000) {
            connectionManager.sendRightClick()
            updateGestureStats("right_click")
            addLogMessage("Right click")
            _uiState.update { it.copy(lastGesture = "Right Click") }
            lastRightClickTime = now
        }
    }

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

    // ==================== SENSOR CONTROL ====================

    fun startSensors() {
        if (isSensorsActive) return
        if (!_uiState.value.isCalibrated) {
            addLogMessage("Please calibrate sensors first")
            return
        }

        isSensorsActive = true
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

    // ==================== AIR MOUSE CONTROL ====================

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

    // ==================== CONNECTION CONTROL ====================

    fun updateIp(ip: String) {
        _uiState.update { it.copy(serverIp = ip) }
    }

    fun updatePort(port: Int) {
        _uiState.update { it.copy(serverPort = port) }
    }

    fun applyScannedConnection(data: QRScanner.ConnectionData) {
        _uiState.update {
            it.copy(
                serverIp = data.ip,
                serverPort = data.port,
                isConnecting = true
            )
        }
        prefs.putString("last_ip", data.ip)
        prefs.putInt("last_port", data.port)
        prefs.putString("connection_protocol", data.protocol)
        data.token?.let { prefs.putString("auth_token", it) }
        viewModelScope.launch {
            val protocol = when (data.protocol.uppercase(Locale.US)) {
                "TCP" -> ConnectionProtocol.TCP
                else -> ConnectionProtocol.WEBSOCKET
            }
            connectionManager.setProtocol(
                if (protocol == ConnectionProtocol.TCP) {
                    ConnectionManager.ConnectionProtocol.TCP
                } else {
                    ConnectionManager.ConnectionProtocol.WEBSOCKET
                }
            )
            val success = connectionManager.connect(data.ip, data.port)
            if (success) {
                addLogMessage("Connected to ${data.name}")
                _uiState.update { it.copy(isConnecting = false) }
            } else {
                addLogMessage("Connection failed")
                _uiState.update { it.copy(isConnecting = false) }
            }
        }
    }

    fun connect() {
        val ip = _uiState.value.serverIp
        if (ip.isBlank()) {
            addLogMessage("Please enter server IP")
            return
        }

        val protocol = when (prefs.getString("last_protocol", "WEBSOCKET").uppercase(Locale.US)) {
            "TCP" -> ConnectionProtocol.TCP
            else -> ConnectionProtocol.WEBSOCKET
        }
        connectionManager.setProtocol(
            if (protocol == ConnectionProtocol.TCP) {
                ConnectionManager.ConnectionProtocol.TCP
            } else {
                ConnectionManager.ConnectionProtocol.WEBSOCKET
            }
        )

        _uiState.update { it.copy(isConnecting = true) }
        addLogMessage("Connecting to $ip:${_uiState.value.serverPort}...")

        viewModelScope.launch {
            val success = connectionManager.connect(ip, _uiState.value.serverPort)
            if (!success) {
                addLogMessage("Connection failed")
                _uiState.update { it.copy(isConnecting = false) }
            }
        }
    }

    fun disconnect() {
        connectionManager.disconnect()
        _uiState.update { it.copy(isActive = false, isConnecting = false) }
        stopSensors()
    }

    fun reconnect() {
        disconnect()
        viewModelScope.launch {
            delay(500)
            connect()
        }
    }

    // ==================== SETTINGS ====================

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

    // ==================== LOGGING ====================

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

    // ==================== MONITORING ====================

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
                        (level * 100 / scale)
                    } else 100
                    _uiState.update { it.copy(batteryLevel = batteryLevel) }
                } catch (_: Exception) {
                    // Ignore
                }
                delay(60000) // Update every minute
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
                        cpuUsage = 0f // Would need external library for accurate CPU usage
                    )
                }
            }
        }
    }

    // ==================== UTILITY FUNCTIONS ====================

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
        // Calculate average latency from connection
        return 0L // Placeholder
    }

    fun getGestureStats(): GestureStats = _uiState.value.gestureStats

    fun getCalibrationQuality(): String = _uiState.value.calibrationQuality

    fun isCalibrated(): Boolean = _uiState.value.isCalibrated

    // ==================== LIFE CYCLE ====================

    override fun onCleared() {
        super.onCleared()
        stopSensors()
        addLogMessage("ViewModel cleared")
    }

    // ==================== DATA CLASSES ====================

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
        val cpuUsage: Float = 0f
    )

    data class HomeUiState(
        val connectionStatus: ConnectionStatus = ConnectionStatus.DISCONNECTED,
        val serverIp: String = "",
        val serverPort: Int = 8080,
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
        val calibrationQuality: String = "UNKNOWN"
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
