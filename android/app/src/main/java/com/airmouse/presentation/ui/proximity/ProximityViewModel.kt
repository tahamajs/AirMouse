package com.airmouse.presentation.ui.proximity

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
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
import kotlin.math.pow
import kotlin.math.sqrt

@HiltViewModel
class ProximityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProximityUiState())
    val uiState: StateFlow<ProximityUiState> = _uiState.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private var targetDevice: BluetoothDevice? = null
    private var rssiHistory = mutableListOf<Int>()
    private val historyLimit = 10
    private val smoothingFactor = 0.7f
    private var lastLockState = false
    private var calibrationSamples = mutableListOf<Pair<Int, Float>>()

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = intent.getParcelableExtra<BluetoothDevice>(BluetoothDevice.EXTRA_DEVICE)
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    device?.let {
                        if (it.address == _uiState.value.deviceMac) {
                            updateRssiReading(rssi)
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (isScanning && _uiState.value.isEnabled) {
                        startScanning()
                    }
                }
            }
        }
    }

    init {
        initializeBluetooth()
        loadSettings()
        startProximityMonitoring()
    }

    private fun initializeBluetooth() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun loadSettings() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isEnabled = prefs.getBoolean("proximity_enabled", false),
                    nearThreshold = prefs.getFloat("proximity_near_threshold", 1.5f),
                    farThreshold = prefs.getFloat("proximity_far_threshold", 3.0f),
                    deviceMac = prefs.getString("proximity_device_mac", ""),
                    lockActionEnabled = prefs.getBoolean("proximity_lock_action", true),
                    unlockActionEnabled = prefs.getBoolean("proximity_unlock_action", true),
                    lockScreenTimeout = prefs.getInt("proximity_lock_timeout", 0),
                    vibrationOnLock = prefs.getBoolean("proximity_vibration", true),
                    notificationOnLock = prefs.getBoolean("proximity_notification", true)
                )
            }
        }
    }

    private fun saveSettings() {
        prefs.putBoolean("proximity_enabled", _uiState.value.isEnabled)
        prefs.putFloat("proximity_near_threshold", _uiState.value.nearThreshold)
        prefs.putFloat("proximity_far_threshold", _uiState.value.farThreshold)
        prefs.putString("proximity_device_mac", _uiState.value.deviceMac)
        prefs.putBoolean("proximity_lock_action", _uiState.value.lockActionEnabled)
        prefs.putBoolean("proximity_unlock_action", _uiState.value.unlockActionEnabled)
        prefs.putInt("proximity_lock_timeout", _uiState.value.lockScreenTimeout)
        prefs.putBoolean("proximity_vibration", _uiState.value.vibrationOnLock)
        prefs.putBoolean("proximity_notification", _uiState.value.notificationOnLock)
    }

    private fun startProximityMonitoring() {
        viewModelScope.launch {
            while (true) {
                if (_uiState.value.isEnabled && _uiState.value.deviceMac.isNotEmpty()) {
                    updateDistance()
                    checkProximityState()
                }
                delay(1000)
            }
        }
    }

    private fun updateDistance() {
        val rssi = getSmoothedRssi()
        val distance = calculateDistanceFromRssi(rssi)

        _uiState.update {
            it.copy(
                currentDistance = distance,
                rssi = rssi,
                signalStrength = getSignalStrength(rssi)
            )
        }

        // Add to history
        addToHistory(distance)
    }

    private fun getSmoothedRssi(): Int {
        if (rssiHistory.isEmpty()) return -100

        // Exponential moving average
        var smoothed = rssiHistory.first().toFloat()
        for (i in 1 until rssiHistory.size) {
            smoothed = smoothingFactor * rssiHistory[i] + (1 - smoothingFactor) * smoothed
        }
        return smoothed.toInt()
    }

    private fun calculateDistanceFromRssi(rssi: Int): Float {
        // Improved RSSI to distance calculation using path-loss model
        // Reference RSSI at 1 meter (calibrated)
        val refRssi = -59 // Typical for Bluetooth at 1 meter
        val n = 2.0 // Path loss exponent (2 for free space, 2-4 for indoor)

        if (rssi == 0) return -1.0f

        val ratio = (refRssi - rssi) / (10 * n)
        val distance = 10.0.pow(ratio)

        return distance.toFloat().coerceIn(0.1f, 15.0f)
    }

    private fun getSignalStrength(rssi: Int): SignalStrength {
        return when {
            rssi > -60 -> SignalStrength.EXCELLENT
            rssi > -70 -> SignalStrength.GOOD
            rssi > -80 -> SignalStrength.FAIR
            rssi > -100 -> SignalStrength.POOR
            else -> SignalStrength.NONE
        }
    }

    private fun updateRssiReading(rssi: Int) {
        rssiHistory.add(rssi)
        while (rssiHistory.size > historyLimit) {
            rssiHistory.removeAt(0)
        }

        _uiState.update { it.copy(rssi = rssi) }
    }

    private fun checkProximityState() {
        val distance = _uiState.value.currentDistance ?: return
        val wasNear = lastLockState
        val isNear = distance < _uiState.value.nearThreshold

        if (wasNear != isNear) {
            lastLockState = isNear
            if (!isNear && _uiState.value.lockActionEnabled) {
                lockComputer()
            } else if (isNear && _uiState.value.unlockActionEnabled) {
                unlockComputer()
            }

            _uiState.update {
                it.copy(
                    isNear = isNear,
                    status = if (isNear) "Near - Device unlocked" else "Far - Device locked",
                    statusColor = if (isNear) 0xFF4CAF50 else 0xFFF44336
                )
            }

            if (_uiState.value.vibrationOnLock) {
                vibrate()
            }

            if (_uiState.value.notificationOnLock) {
                sendNotification(isNear)
            }
        }
    }

    private fun lockComputer() {
        // Send lock command to server
        sendCommandToServer("lock")
        addToHistory(_uiState.value.currentDistance ?: 0f)
    }

    private fun unlockComputer() {
        // Send unlock command to server
        sendCommandToServer("unlock")
    }

    private fun sendCommandToServer(command: String) {
        // Implementation to send command via WebSocket/TCP
        _uiState.update { it.copy(status = "Sending $command command...") }
    }

    private fun vibrate() {
        val vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as android.os.Vibrator
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            vibrator.vibrate(android.os.VibrationEffect.createOneShot(200, android.os.VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }

    private fun sendNotification(isNear: Boolean) {
        // Create and send notification
        val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
            val channel = android.app.NotificationChannel(
                "proximity_channel",
                "Proximity Lock",
                android.app.NotificationManager.IMPORTANCE_HIGH
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification = androidx.core.app.NotificationCompat.Builder(context, "proximity_channel")
            .setContentTitle("Proximity Lock")
            .setContentText(if (isNear) "Device unlocked" else "Device locked")
            .setSmallIcon(android.R.drawable.ic_menu_compass)
            .setAutoCancel(true)
            .build()

        notificationManager.notify(1001, notification)
    }

    private fun addToHistory(distance: Float) {
        val entry = ProximityHistoryEntry(
            timestamp = System.currentTimeMillis(),
            distance = distance,
            isNear = distance < _uiState.value.nearThreshold,
            rssi = _uiState.value.rssi
        )

        val newHistory = listOf(entry) + _uiState.value.history
        val trimmedHistory = if (newHistory.size > 50) newHistory.take(50) else newHistory

        _uiState.update { it.copy(history = trimmedHistory) }
    }

    private fun startScanning() {
        if (!_uiState.value.isEnabled || isScanning) return

        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }

        isScanning = true
        bluetoothAdapter?.startDiscovery()

        _uiState.update {
            it.copy(
                status = "Scanning for devices...",
                statusColor = 0xFFFF9800
            )
        }

        viewModelScope.launch {
            delay(12000) // Scan for 12 seconds
            if (isScanning) {
                stopScanning()
                if (_uiState.value.connectedDevice == null) {
                    _uiState.update {
                        it.copy(
                            status = "No device found. Make sure Bluetooth is enabled and device is discoverable.",
                            statusColor = 0xFFF44336
                        )
                    }
                }
            }
        }
    }

    private fun stopScanning() {
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        isScanning = false
    }

    fun toggleService(enabled: Boolean) {
        if (enabled && _uiState.value.deviceMac.isEmpty()) {
            startDeviceSelection()
        } else {
            _uiState.update {
                it.copy(
                    isEnabled = enabled,
                    isServiceRunning = enabled,
                    status = if (enabled) "Service running" else "Service stopped",
                    statusColor = if (enabled) 0xFF4CAF50 else 0xFF9E9E9E
                )
            }
            saveSettings()

            if (enabled) {
                startScanning()
            } else {
                stopScanning()
            }
        }
    }

    fun startDeviceSelection() {
        startScanning()
        _uiState.update { it.copy(status = "Select a device to pair...") }
    }

    fun selectDevice(device: BluetoothDevice) {
        stopScanning()
        _uiState.update {
            it.copy(
                deviceMac = device.address,
                connectedDevice = device.name ?: device.address,
                isEnabled = true,
                isServiceRunning = true,
                status = "Connected to ${device.name ?: device.address}",
                statusColor = 0xFF4CAF50
            )
        }
        saveSettings()
    }

    fun updateNearThreshold(value: Float) {
        _uiState.update { it.copy(nearThreshold = value) }
        saveSettings()
    }

    fun updateFarThreshold(value: Float) {
        _uiState.update { it.copy(farThreshold = value) }
        saveSettings()
    }

    fun calibrate() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCalibrating = true,
                    calibrationProgress = 0,
                    calibrationStatus = "Move 1 meter away from device..."
                )
            }

            calibrationSamples.clear()

            // Collect RSSI samples at different distances
            for (i in 1..5) {
                _uiState.update { it.copy(calibrationProgress = i * 20) }
                delay(2000)

                val avgRssi = rssiHistory.average().toInt()
                val distance = i.toFloat()
                calibrationSamples.add(Pair(avgRssi, distance))

                _uiState.update {
                    it.copy(calibrationStatus = "Sample $i/5 collected at ${distance}m (RSSI: $avgRssi)")
                }
            }

            // Calculate calibration parameters
            if (calibrationSamples.size >= 3) {
                calculateCalibrationParameters()
            }

            _uiState.update {
                it.copy(
                    isCalibrating = false,
                    calibrationProgress = 100,
                    calibrationStatus = "Calibration complete!",
                    currentDistance = calculateDistanceFromRssi(rssiHistory.lastOrNull() ?: -100)
                )
            }

            delay(2000)
            _uiState.update { it.copy(calibrationStatus = "") }
        }
    }

    private fun calculateCalibrationParameters() {
        // Linear regression to find optimal parameters
        val rssiValues = calibrationSamples.map { it.first.toDouble() }
        val distances = calibrationSamples.map { it.second.toDouble() }

        val n = distances.size
        val sumRssi = rssiValues.sum()
        val sumDist = distances.sum()
        val sumRssiDist = rssiValues.zip(distances).sumOf { it.first * it.second }
        val sumRssiSq = rssiValues.sumOf { it * it }

        val slope = (n * sumRssiDist - sumRssi * sumDist) / (n * sumRssiSq - sumRssi * sumRssi)
        val intercept = (sumDist - slope * sumRssi) / n

        // Save calibration parameters
        prefs.putFloat("proximity_calibration_slope", slope.toFloat())
        prefs.putFloat("proximity_calibration_intercept", intercept.toFloat())
    }

    fun toggleLockAction(enabled: Boolean) {
        _uiState.update { it.copy(lockActionEnabled = enabled) }
        saveSettings()
    }

    fun toggleUnlockAction(enabled: Boolean) {
        _uiState.update { it.copy(unlockActionEnabled = enabled) }
        saveSettings()
    }

    fun updateLockTimeout(timeout: Int) {
        _uiState.update { it.copy(lockScreenTimeout = timeout) }
        saveSettings()
    }

    fun toggleVibration(enabled: Boolean) {
        _uiState.update { it.copy(vibrationOnLock = enabled) }
        saveSettings()
    }

    fun toggleNotification(enabled: Boolean) {
        _uiState.update { it.copy(notificationOnLock = enabled) }
        saveSettings()
    }

    fun resetToDefaults() {
        _uiState.update {
            it.copy(
                nearThreshold = 1.5f,
                farThreshold = 3.0f,
                lockActionEnabled = true,
                unlockActionEnabled = true,
                lockScreenTimeout = 0,
                vibrationOnLock = true,
                notificationOnLock = true
            )
        }
        saveSettings()
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {
            // Receiver already unregistered
        }
    }
}