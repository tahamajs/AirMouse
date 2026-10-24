package com.airmouse.presentation.ui.proximity

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.log10
import kotlin.math.pow

// ============================================================
// Data Models
// ============================================================

data class PairedDeviceInfo(
    val name: String,
    val address: String
)

data class ProximityUiState(
    val isEnabled: Boolean = false,
    val isServiceRunning: Boolean = false,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val currentDistance: Float? = null,
    val isNear: Boolean = false,
    val status: String = "Service stopped",
    val statusColor: Long = 0xFF9E9E9E,
    val isCalibrating: Boolean = false,
    val calibrationProgress: Int = 0,
    val calibrationStatus: String = "",
    val connectedDevice: String? = null,
    val deviceMac: String = "",
    val rssi: Int = -100,
    val signalStrength: SignalStrength = SignalStrength.NONE,
    val history: List<ProximityHistoryEntry> = emptyList(),
    val errorMessage: String? = null,
    val lockActionEnabled: Boolean = true,
    val unlockActionEnabled: Boolean = true,
    val vibrationOnLock: Boolean = true,
    val notificationOnLock: Boolean = true,
    val isBluetoothEnabled: Boolean = false,
    val bondableDevices: List<BluetoothDevice> = emptyList(),
    val pairedDevices: List<PairedDeviceInfo> = emptyList()
)

enum class SignalStrength(val displayName: String, val color: Long) {
    EXCELLENT("Excellent", 0xFF4CAF50),
    GOOD("Good", 0xFF8BC34A),
    FAIR("Fair", 0xFFFFC107),
    POOR("Poor", 0xFFFF5722),
    NONE("None", 0xFF9E9E9E)
}

data class ProximityHistoryEntry(
    val timestamp: Long,
    val distance: Float,
    val isNear: Boolean,
    val rssi: Int
)

// ============================================================
// ViewModel
// ============================================================

@HiltViewModel
class ProximityViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProximityUiState())
    val uiState: StateFlow<ProximityUiState> = _uiState.asStateFlow()

    private var bluetoothAdapter: BluetoothAdapter? = null
    private var isScanning = false
    private var rssiHistory = mutableListOf<Int>()
    private val historyLimit = 10
    private val smoothingFactor = 0.7f
    private var lastLockState = false
    private var calibrationSamples = mutableListOf<Pair<Int, Float>>()
    private var monitoringJob: Job? = null

    // Calibrated parameters (loaded from prefs)
    private var calibratedTxPower = prefs.getInt("proximity_tx_power", -59)
    private var calibratedPathLoss = prefs.getFloat("proximity_path_loss", 2.0f)

    private val bluetoothReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val action = intent.action
            when (action) {
                BluetoothDevice.ACTION_FOUND -> {
                    val device = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE, BluetoothDevice::class.java)
                    } else {
                        @Suppress("DEPRECATION")
                        intent.getParcelableExtra(BluetoothDevice.EXTRA_DEVICE)
                    }
                    val rssi = intent.getShortExtra(BluetoothDevice.EXTRA_RSSI, Short.MIN_VALUE).toInt()

                    device?.let {
                        if (it.address == _uiState.value.deviceMac) {
                            updateRssiReading(rssi)
                        } else {
                            val currentList = _uiState.value.bondableDevices.toMutableList()
                            if (!currentList.any { d -> d.address == it.address }) {
                                currentList.add(it)
                                _uiState.update { state -> state.copy(bondableDevices = currentList) }
                            }
                        }
                    }
                }
                BluetoothAdapter.ACTION_DISCOVERY_FINISHED -> {
                    if (isScanning && _uiState.value.isEnabled) {
                        startScanning()
                    }
                }
                BluetoothAdapter.ACTION_STATE_CHANGED -> {
                    val state = intent.getIntExtra(BluetoothAdapter.EXTRA_STATE, BluetoothAdapter.ERROR)
                    val enabled = state == BluetoothAdapter.STATE_ON
                    _uiState.update { it.copy(isBluetoothEnabled = enabled) }
                }
            }
        }
    }

    init {
        initializeBluetooth()
        loadSettings()
        startProximityMonitoring()
    }

    @Suppress("DEPRECATION")
    private fun initializeBluetooth() {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as? BluetoothManager
            bluetoothAdapter = bluetoothManager?.adapter ?: BluetoothAdapter.getDefaultAdapter()
            val isEnabled = bluetoothAdapter?.isEnabled == true
            _uiState.update { it.copy(isBluetoothEnabled = isEnabled) }

            val filter = IntentFilter().apply {
                addAction(BluetoothDevice.ACTION_FOUND)
                addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
                addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
            }
            context.registerReceiver(bluetoothReceiver, filter)
        } catch (e: Exception) {
            Log.w("ProximityViewModel", "Error initializing Bluetooth", e)
        }
    }

    fun setBluetoothAdapter(adapter: BluetoothAdapter?) {
        this.bluetoothAdapter = adapter
        refreshPairedDevices()
    }

    fun setPairedDevices(devices: List<PairedDeviceInfo>) {
        _uiState.update { it.copy(pairedDevices = devices) }
    }

    fun refreshPairedDevices() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    return
                }
            }

            val bonded = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
            val pairedList = bonded.map { device ->
                val devName = try {
                    device.name ?: device.address
                } catch (_: SecurityException) {
                    device.address
                }
                PairedDeviceInfo(name = devName, address = device.address)
            }

            _uiState.update {
                it.copy(
                    bondableDevices = bonded,
                    pairedDevices = pairedList
                )
            }
        } catch (e: Exception) {
            Log.w("ProximityViewModel", "Error fetching paired devices", e)
        }
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val savedMac = prefs.getString("proximity_device_mac", "")
            refreshPairedDevices()
            val paired = _uiState.value.pairedDevices
            val trustedMac = if (savedMac.isNotBlank()) savedMac else ""
            val trustedName = paired.find { it.address == trustedMac }?.name

            calibratedTxPower = prefs.getInt("proximity_tx_power", -59)
            calibratedPathLoss = prefs.getFloat("proximity_path_loss", 2.0f)

            _uiState.update {
                it.copy(
                    isEnabled = prefs.getBoolean("proximity_enabled", false) && trustedMac.isNotBlank(),
                    nearThreshold = prefs.getFloat("proximity_near_threshold", 1.5f),
                    farThreshold = prefs.getFloat("proximity_far_threshold", 3.0f),
                    deviceMac = trustedMac,
                    connectedDevice = trustedName,
                    lockActionEnabled = prefs.getBoolean("proximity_lock_action", true),
                    unlockActionEnabled = prefs.getBoolean("proximity_unlock_action", true),
                    vibrationOnLock = prefs.getBoolean("proximity_vibration", true),
                    notificationOnLock = prefs.getBoolean("proximity_notification", true)
                )
            }

            if (trustedMac.isBlank()) {
                _uiState.update {
                    it.copy(
                        isEnabled = false,
                        status = "No trusted Bluetooth device selected",
                        statusColor = 0xFFF59E0B
                    )
                }
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
        prefs.putBoolean("proximity_vibration", _uiState.value.vibrationOnLock)
        prefs.putBoolean("proximity_notification", _uiState.value.notificationOnLock)
    }

    private fun startProximityMonitoring() {
        monitoringJob?.cancel()
        monitoringJob = viewModelScope.launch {
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
        val distance = calculateDistanceFromRssi(rssi, calibratedTxPower, calibratedPathLoss)

        _uiState.update {
            it.copy(
                currentDistance = distance,
                rssi = rssi,
                signalStrength = getSignalStrength(rssi)
            )
        }

        addToHistory(distance)
    }

    private fun getSmoothedRssi(): Int {
        if (rssiHistory.isEmpty()) return -100
        var smoothed = rssiHistory.first().toFloat()
        for (i in 1 until rssiHistory.size) {
            smoothed = smoothingFactor * rssiHistory[i] + (1 - smoothingFactor) * smoothed
        }
        return smoothed.toInt()
    }

    private fun calculateDistanceFromRssi(rssi: Int, txPower: Int, pathLoss: Float): Float {
        if (rssi == 0) return -1.0f
        val ratio = (txPower - rssi) / (10 * pathLoss)
        val distance = 10.0.pow(ratio.toDouble())
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
        val device = bluetoothAdapter?.bondedDevices?.find { it.address == _uiState.value.deviceMac }
        _uiState.update { state ->
            state.copy(
                rssi = rssi,
                connectedDevice = try { device?.name } catch (_: SecurityException) { null } ?: state.connectedDevice
            )
        }
    }

    private fun checkProximityState() {
        val distance = _uiState.value.currentDistance ?: return
        val wasNear = lastLockState
        val isNear = distance < _uiState.value.nearThreshold

        if (wasNear != isNear) {
            lastLockState = isNear
            _uiState.update {
                it.copy(
                    isNear = isNear,
                    status = if (isNear) "Near – Device unlocked" else "Far – Device locked",
                    statusColor = if (isNear) 0xFF4CAF50 else 0xFFF44336
                )
            }
            if (isNear && _uiState.value.unlockActionEnabled) {
                performUnlock()
            } else if (!isNear && _uiState.value.lockActionEnabled) {
                performLock()
            }
        }
    }

    private fun performLock() {
        Log.d("Proximity", "Locking PC")
    }

    private fun performUnlock() {
        Log.d("Proximity", "Unlocking PC")
    }

    private fun addToHistory(distance: Float) {
        val entry = ProximityHistoryEntry(
            timestamp = System.currentTimeMillis(),
            distance = distance,
            isNear = distance < _uiState.value.nearThreshold,
            rssi = _uiState.value.rssi
        )
        val newHistory = (listOf(entry) + _uiState.value.history).take(50)
        _uiState.update { it.copy(history = newHistory) }
    }

    @Suppress("DEPRECATION")
    private fun startScanning() {
        if (!_uiState.value.isEnabled || isScanning) return

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) != PackageManager.PERMISSION_GRANTED) {
                    _uiState.update { it.copy(status = "Bluetooth scan permission not granted", statusColor = 0xFFF44336) }
                    return
                }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(status = "Bluetooth scan permission error", statusColor = 0xFFF44336) }
                return
            }
        }

        isScanning = true
        bluetoothAdapter?.startDiscovery()
    }

    private fun stopScanning() {
        bluetoothAdapter?.cancelDiscovery()
        isScanning = false
    }

    // ============================================================
    // Public API
    // ============================================================

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            if (enabled && _uiState.value.deviceMac.isBlank()) {
                _uiState.update {
                    it.copy(
                        isEnabled = false,
                        status = "Select a trusted Bluetooth device first",
                        statusColor = 0xFFF59E0B
                    )
                }
                saveSettings()
                return@launch
            }
            _uiState.update { it.copy(isEnabled = enabled) }
            if (enabled) {
                startScanning()
                _uiState.update { it.copy(status = "Service waiting for trusted device signal...") }
            } else {
                stopScanning()
                _uiState.update {
                    it.copy(
                        status = "Service stopped",
                        statusColor = 0xFF9E9E9E,
                        currentDistance = null,
                        isNear = false
                    )
                }
            }
            saveSettings()
        }
    }

    fun updateNearThreshold(value: Float) {
        _uiState.update { it.copy(nearThreshold = value) }
        saveSettings()
    }

    fun updateFarThreshold(value: Float) {
        _uiState.update { it.copy(farThreshold = value) }
        saveSettings()
    }

    fun selectDevice(address: String, name: String? = null) {
        val devName = name ?: _uiState.value.pairedDevices.find { it.address == address }?.name ?: address
        _uiState.update {
            it.copy(
                deviceMac = address,
                connectedDevice = devName,
                status = "Device selected: $devName",
                statusColor = 0xFF4CAF50
            )
        }
        prefs.putString("proximity_device_mac", address)
        saveSettings()
    }

    fun selectDevice(device: BluetoothDevice) {
        val devName = try { device.name ?: device.address } catch (_: SecurityException) { device.address }
        selectDevice(device.address, devName)
    }

    fun calibrate() {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isCalibrating = true,
                    calibrationProgress = 0,
                    calibrationStatus = "Starting calibration..."
                )
            }

            calibrationSamples.clear()
            val steps = listOf(
                0.5f to "Place device 0.5m away",
                1.0f to "Place device 1.0m away",
                2.0f to "Place device 2.0m away",
                3.0f to "Place device 3.0m away",
                5.0f to "Place device 5.0m away"
            )

            for ((stepIndex, step) in steps.withIndex()) {
                val targetDistance = step.first
                val instruction = step.second
                _uiState.update {
                    it.copy(
                        calibrationStatus = instruction,
                        calibrationProgress = (stepIndex * 100 / steps.size)
                    )
                }

                val samples = mutableListOf<Int>()
                var attempts = 0
                while (attempts < 15) {
                    delay(200)
                    val currentRssi = _uiState.value.rssi
                    if (currentRssi != -100) {
                        samples.add(currentRssi)
                    }
                    attempts++
                }

                if (samples.isNotEmpty()) {
                    val avgRssi = samples.average().toInt()
                    calibrationSamples.add(Pair(avgRssi, targetDistance))
                }
            }

            if (calibrationSamples.size >= 3) {
                calculateOptimalParameters(calibrationSamples)
                _uiState.update {
                    it.copy(
                        isCalibrating = false,
                        calibrationProgress = 100,
                        calibrationStatus = "Calibration complete!",
                        status = "Calibrated successfully"
                    )
                }
            } else {
                _uiState.update {
                    it.copy(
                        isCalibrating = false,
                        calibrationStatus = "Calibration failed. Please try again.",
                        status = "Calibration failed"
                    )
                }
            }
        }
    }

    private fun calculateOptimalParameters(samples: List<Pair<Int, Float>>) {
        val oneMeterSample = samples.find { it.second == 1.0f }
        val txPower = oneMeterSample?.first ?: samples.map { it.first }.average().toInt()

        val logDistances = samples.map { log10(it.second.toDouble()) }
        val rssis = samples.map { it.first.toDouble() }
        val n = if (logDistances.size > 1) {
            val meanLog = logDistances.average()
            val meanRssi = rssis.average()
            val cov = logDistances.zip(rssis).sumOf { (l, r) -> (l - meanLog) * (r - meanRssi) }
            val varLog = logDistances.sumOf { (it - meanLog) * (it - meanLog) }
            if (varLog != 0.0) {
                -(cov / varLog) / 10.0
            } else {
                2.0
            }
        } else {
            2.0
        }

        calibratedTxPower = txPower
        calibratedPathLoss = n.toFloat()

        prefs.putInt("proximity_tx_power", calibratedTxPower)
        prefs.putFloat("proximity_path_loss", calibratedPathLoss)
    }

    fun toggleLockAction(enabled: Boolean) {
        _uiState.update { it.copy(lockActionEnabled = enabled) }
        saveSettings()
    }

    fun toggleUnlockAction(enabled: Boolean) {
        _uiState.update { it.copy(unlockActionEnabled = enabled) }
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
                vibrationOnLock = true,
                notificationOnLock = true
            )
        }
        saveSettings()
        _uiState.update { it.copy(status = "Reset to defaults") }
    }

    override fun onCleared() {
        super.onCleared()
        monitoringJob?.cancel()
        stopScanning()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
    }
}
