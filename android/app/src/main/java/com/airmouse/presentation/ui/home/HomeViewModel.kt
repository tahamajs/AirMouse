package com.airmouse.presentation.ui.home

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.network.ConnectionManager
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
import kotlin.math.*

@HiltViewModel
class HomeViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(HomeUiState())
    val uiState: StateFlow<HomeUiState> = _uiState.asStateFlow()

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var rotationVector: Sensor? = null

    // Sensor state
    private var isSensorsActive = false
    private var gyroOffsetX = 0f
    private var gyroOffsetY = 0f
    private var gyroOffsetZ = 0f

    // Orientation
    private var yaw = 0f
    private var pitch = 0f
    private var roll = 0f
    private var gravity = FloatArray(3)
    private var magnetic = FloatArray(3)
    private var rotationMatrix = FloatArray(9)
    private var orientation = FloatArray(3)

    // Gesture detection state
    private var lastClickTime = 0L
    private var lastScrollTime = 0L
    private var scrollCooldown = 300L

    // Log buffer
    private val maxLogs = 50

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (!_uiState.value.isActive || !_uiState.value.isCalibrated) return

            when (event.sensor.type) {
                Sensor.TYPE_GYROSCOPE -> processGyroscope(event.values)
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
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        rotationVector = sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)

        loadSettingsAndCalibration()
        observeConnection()
        startBatteryMonitoring()
        loadGestureStats()
    }

    private fun loadSettingsAndCalibration() {
        _uiState.update {
            it.copy(
                serverIp = prefs.getString("last_ip", ""),
                serverPort = prefs.getInt("last_port", 8080),
                controlMode = prefs.getString("control_mode", "motion"),
                isCalibrated = prefs.getBoolean("calibration_complete", false)
            )
        }
        // Load gyro offsets
        gyroOffsetX = prefs.getFloat("gyro_offset_x", 0f)
        gyroOffsetY = prefs.getFloat("gyro_offset_y", 0f)
        gyroOffsetZ = prefs.getFloat("gyro_offset_z", 0f)

        // Update calibration progress
        val gyroOk = gyroOffsetX != 0f || gyroOffsetY != 0f || gyroOffsetZ != 0f
        val accelOk = prefs.getFloat("accel_offset_x", 0f) != 0f
        val magOk = prefs.getFloat("mag_offset_x", 0f) != 0f
        val calibratedCount = listOf(gyroOk, accelOk, magOk).count { it }
        _uiState.update {
            it.copy(
                sensorsCalibrated = calibratedCount,
                calibrationProgress = if (it.totalSensors > 0) calibratedCount * 100 / it.totalSensors else 0
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
                }
                _uiState.update { it.copy(connectionStatus = mapped, isConnecting = status == ConnectionManager.ConnectionStatus.CONNECTING) }
                if (status == ConnectionManager.ConnectionStatus.CONNECTED) {
                    addLogMessage("Connected to ${connectionManager.currentIp.value}")
                    updateConnectionQuality()
                    startSensors()
                } else if (status == ConnectionManager.ConnectionStatus.DISCONNECTED) {
                    addLogMessage("Disconnected")
                    stopSensors()
                    _uiState.update { it.copy(isActive = false) }
                }
            }
        }
    }

    private fun updateConnectionQuality() {
        viewModelScope.launch {
            // Simulate ping (you could use real ping via ConnectionManager)
            val quality = ConnectionQuality.GOOD
            _uiState.update { it.copy(connectionQuality = quality) }
        }
    }

    private fun processGyroscope(values: FloatArray) {
        val sensitivity = prefs.getSensitivity()
        val dx = (values[2] - gyroOffsetZ) * sensitivity
        val dy = (values[0] - gyroOffsetX) * sensitivity

        if (abs(dx) > 0.5f || abs(dy) > 0.5f) {
            connectionManager.sendMove(dx.toInt(), dy.toInt())
        }

        // Simple click detection based on gyro magnitude
        val magnitude = sqrt((values[0]-gyroOffsetX).pow(2) + (values[1]-gyroOffsetY).pow(2) + (values[2]-gyroOffsetZ).pow(2))
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

    private fun detectGesture(magnitude: Float) {
        val now = System.currentTimeMillis()
        if (now - lastScrollTime < scrollCooldown) return

        when {
            magnitude > 8f -> {
                val timeSinceLastClick = now - lastClickTime
                if (timeSinceLastClick < 400) {
                    connectionManager.sendDoubleClick()
                    updateGestureStats("double_click")
                    addLogMessage("Double click")
                    _uiState.update { it.copy(lastGesture = "Double Click") }
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
            magnitude > 5f -> {
                val scrollDelta = if (magnitude > 6f) 3 else 1
                connectionManager.sendScroll(scrollDelta)
                updateGestureStats("scroll")
                addLogMessage("Scroll")
                _uiState.update { it.copy(lastGesture = "Scroll") }
                lastScrollTime = now
            }
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

    fun startSensors() {
        if (isSensorsActive) return
        isSensorsActive = true

        val sensorDelay = SensorManager.SENSOR_DELAY_GAME
        gyroscope?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }
        accelerometer?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }
        rotationVector?.let { sensorManager.registerListener(sensorListener, it, sensorDelay) }

        addLogMessage("Sensors activated")
    }

    fun stopSensors() {
        if (!isSensorsActive) return
        isSensorsActive = false
        sensorManager.unregisterListener(sensorListener)
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
        _uiState.update { it.copy(isActive = true) }
        startSensors()
        addLogMessage("Air Mouse activated")
    }

    fun stopAirMouse() {
        _uiState.update { it.copy(isActive = false) }
        stopSensors()
        addLogMessage("Air Mouse deactivated")
    }

    fun updateIp(ip: String) {
        _uiState.update { it.copy(serverIp = ip) }
    }

    fun updatePort(port: Int) {
        _uiState.update { it.copy(serverPort = port) }
    }

    fun connect() {
        val ip = _uiState.value.serverIp
        if (ip.isBlank()) {
            addLogMessage("Please enter server IP")
            return
        }
        _uiState.update { it.copy(isConnecting = true) }
        connectionManager.connect(ip, _uiState.value.serverPort, ConnectionManager.ConnectionProtocol.TCP)
        prefs.putString("last_ip", ip)
        prefs.putInt("last_port", _uiState.value.serverPort)
    }

    fun disconnect() {
        connectionManager.disconnect()
        _uiState.update { it.copy(isActive = false) }
        stopSensors()
    }

    fun setControlMode(mode: String) {
        prefs.putString("control_mode", mode)
        _uiState.update { it.copy(controlMode = mode) }
        addLogMessage("Control mode changed to $mode")
        if (mode == "touchpad") {
            // Optionally open touchpad screen? We'll let the user navigate.
        }
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
        val timestamp = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
            .format(java.util.Date())
        val newLogs = (listOf("[$timestamp] $message") + _uiState.value.logMessages).take(maxLogs)
        _uiState.update { it.copy(logMessages = newLogs) }
    }

    fun clearLogs() {
        _uiState.update { it.copy(logMessages = emptyList()) }
    }

    private fun startBatteryMonitoring() {
        viewModelScope.launch {
            while (true) {
                val batteryIntent = context.registerReceiver(null,
                    android.content.IntentFilter(android.content.Intent.ACTION_BATTERY_CHANGED))
                val level = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryIntent?.getIntExtra(android.os.BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryLevel = if (level >= 0 && scale > 0) (level * 100 / scale) else 100
                _uiState.update { it.copy(batteryLevel = batteryLevel) }
                delay(60000)
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        stopSensors()
    }
}


init {
    if (prefs.getBoolean("auto_connect", true)) {
        viewModelScope.launch {
            connectionManager.connect()
        }
    }
}