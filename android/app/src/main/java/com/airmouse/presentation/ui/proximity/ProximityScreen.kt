package com.airmouse.presentation.ui.proximity

import android.Manifest
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.airmouse.presentation.navigation.NavigationActions
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
import java.util.*
import javax.inject.Inject
import kotlin.math.pow
import kotlin.math.log10

// ============================================================
// Data Models
// ============================================================

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
    val bondableDevices: List<BluetoothDevice> = emptyList()
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
) : androidx.lifecycle.ViewModel() {

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
                            // Discover other devices – store for UI to pick
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
                        startScanning() // restart scan to keep finding devices
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
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            try {
                if (ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) != PackageManager.PERMISSION_GRANTED) {
                    _uiState.update { it.copy(status = "Bluetooth permission not granted", statusColor = 0xFFF44336) }
                    return
                }
            } catch (_: SecurityException) {
                _uiState.update { it.copy(status = "Bluetooth permission error", statusColor = 0xFFF44336) }
                return
            }
        }

        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
        val isEnabled = bluetoothAdapter?.isEnabled == true
        _uiState.update { it.copy(isBluetoothEnabled = isEnabled) }

        val filter = IntentFilter().apply {
            addAction(BluetoothDevice.ACTION_FOUND)
            addAction(BluetoothAdapter.ACTION_DISCOVERY_FINISHED)
            addAction(BluetoothAdapter.ACTION_STATE_CHANGED)
        }
        context.registerReceiver(bluetoothReceiver, filter)
    }

    private fun loadSettings() {
        viewModelScope.launch {
            val savedMac = prefs.getString("proximity_device_mac", "")
            val trustedMac = if (savedMac.isNotBlank()) {
                val bonded = bluetoothAdapter?.bondedDevices?.map { it.address }?.toSet().orEmpty()
                if (bonded.contains(savedMac)) savedMac else ""
            } else ""

            // Load calibrated parameters
            calibratedTxPower = prefs.getInt("proximity_tx_power", -59)
            calibratedPathLoss = prefs.getFloat("proximity_path_loss", 2.0f)

            _uiState.update {
                it.copy(
                    isEnabled = prefs.getBoolean("proximity_enabled", false),
                    nearThreshold = prefs.getFloat("proximity_near_threshold", 1.5f),
                    farThreshold = prefs.getFloat("proximity_far_threshold", 3.0f),
                    deviceMac = trustedMac,
                    lockActionEnabled = prefs.getBoolean("proximity_lock_action", true),
                    unlockActionEnabled = prefs.getBoolean("proximity_unlock_action", true),
                    vibrationOnLock = prefs.getBoolean("proximity_vibration", true),
                    notificationOnLock = prefs.getBoolean("proximity_notification", true),
                    bondableDevices = bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
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

    // Use calibrated parameters if available, otherwise fallback to defaults
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
        // Also update connected device name if available
        val device = bluetoothAdapter?.bondedDevices?.find { it.address == _uiState.value.deviceMac }
        _uiState.update { state ->
            state.copy(
                rssi = rssi,
                connectedDevice = device?.name ?: state.connectedDevice
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
            // Perform lock/unlock actions
            if (isNear && _uiState.value.unlockActionEnabled) {
                // Trigger unlock
                performUnlock()
            } else if (!isNear && _uiState.value.lockActionEnabled) {
                // Trigger lock
                performLock()
            }
        }
    }

    private fun performLock() {
        // In a real app, send a command to the PC via network
        Log.d("Proximity", "Locking PC")
        if (_uiState.value.vibrationOnLock) vibrate(200)
        if (_uiState.value.notificationOnLock) showNotification("PC Locked")
    }

    private fun performUnlock() {
        Log.d("Proximity", "Unlocking PC")
        if (_uiState.value.vibrationOnLock) vibrate(100)
        if (_uiState.value.notificationOnLock) showNotification("PC Unlocked")
    }

    private fun vibrate(duration: Long) {
        // Use Vibrator service if permission granted
    }

    private fun showNotification(message: String) {
        // Use NotificationManager
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

    fun selectDevice(device: BluetoothDevice) {
        val mac = device.address
        _uiState.update {
            it.copy(
                deviceMac = mac,
                connectedDevice = device.name,
                status = "Device selected: ${device.name}",
                statusColor = 0xFF4CAF50
            )
        }
        prefs.putString("proximity_device_mac", mac)
        saveSettings()
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

                // Collect RSSI for 3 seconds at this distance
                val samples = mutableListOf<Int>()
                var attempts = 0
                while (attempts < 15) { // ~3 seconds at 200ms interval
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
        // Fit a line: rssi = txPower - 10 * n * log10(distance)
        // We solve for txPower and n using least squares.
        // For simplicity, we estimate txPower as the average RSSI at 1 meter.
        val oneMeterSample = samples.find { it.second == 1.0f }
        val txPower = if (oneMeterSample != null) {
            oneMeterSample.first
        } else {
            // Approximate: use average of all samples
            samples.map { it.first }.average().toInt()
        }

        // Estimate path loss n from the slope of log(distance) vs RSSI
        val logDistances = samples.map { log10(it.second.toDouble()) }
        val rssis = samples.map { it.first.toDouble() }
        // Simple linear regression: slope = covariance / variance
        val n = if (logDistances.isNotEmpty() && logDistances.size > 1) {
            val meanLog = logDistances.average()
            val meanRssi = rssis.average()
            val cov = logDistances.zip(rssis).map { (l, r) -> (l - meanLog) * (r - meanRssi) }.sum()
            val varLog = logDistances.map { (it - meanLog) * (it - meanLog) }.sum()
            if (varLog != 0.0) {
                -(cov / varLog) / 10.0 // because rssi = txPower - 10*n*log10(d)
            } else {
                2.0
            }
        } else {
            2.0
        }

        calibratedTxPower = txPower
        calibratedPathLoss = n.toFloat()

        // Save to prefs
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

// ============================================================
// Compose UI Screen
// ============================================================

@Composable
fun ProximityScreen(
    navigationActions: NavigationActions,
    viewModel: ProximityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Proximity Lock") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Main status card
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = "Proximity Service",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Text(
                                    text = "Auto‑lock PC when you walk away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = viewModel::toggleService,
                                enabled = uiState.deviceMac.isNotBlank()
                            )
                        }

                        if (uiState.isEnabled) {
                            Spacer(modifier = Modifier.height(12.dp))
                            HorizontalDivider()
                            Spacer(modifier = Modifier.height(12.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Device:", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    uiState.connectedDevice ?: "None",
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Signal:", style = MaterialTheme.typography.bodyMedium)
                                Row(verticalAlignment = Alignment.CenterVertically) {
                                    Box(
                                        modifier = Modifier
                                            .size(10.dp)
                                            .clip(CircleShape)
                                            .background(Color(uiState.signalStrength.color))
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(uiState.signalStrength.displayName)
                                }
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("RSSI:", style = MaterialTheme.typography.bodyMedium)
                                Text("${uiState.rssi} dBm", fontFamily = FontFamily.Monospace)
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            Text("Distance", style = MaterialTheme.typography.titleMedium)
                            Text(
                                text = if (uiState.currentDistance != null)
                                    String.format(Locale.US, "%.2f m", uiState.currentDistance)
                                else
                                    "Calculating...",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
                            )

                            Spacer(modifier = Modifier.height(8.dp))
                            Surface(
                                shape = RoundedCornerShape(8.dp),
                                color = Color(uiState.statusColor).copy(alpha = 0.2f)
                            ) {
                                Text(
                                    text = uiState.status,
                                    modifier = Modifier.padding(8.dp),
                                    fontSize = 12.sp,
                                    color = Color(uiState.statusColor)
                                )
                            }
                        } else {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "Select a trusted Bluetooth device below to enable.",
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }

            if (uiState.isEnabled) {
                // Device selection
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Trusted Device",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.bondableDevices.isEmpty()) {
                                Text("No bonded devices found. Pair a device via Bluetooth settings.")
                            } else {
                                uiState.bondableDevices.forEach { device ->
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .clickable { viewModel.selectDevice(device) }
                                            .padding(8.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.SpaceBetween
                                    ) {
                                        Text(device.name ?: device.address)
                                        if (device.address == uiState.deviceMac) {
                                            Icon(Icons.Default.Check, contentDescription = "Selected")
                                        }
                                    }
                                    HorizontalDivider()
                                }
                            }
                        }
                    }
                }

                // Threshold settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Thresholds",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Near (Unlock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.nearThreshold,
                                onValueChange = viewModel::updateNearThreshold,
                                valueRange = 0.5f..5.0f,
                                steps = 45
                            )
                            Text("${String.format(Locale.US, "%.1f", uiState.nearThreshold)} m", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Far (Lock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.farThreshold,
                                onValueChange = viewModel::updateFarThreshold,
                                valueRange = 1.0f..10.0f,
                                steps = 90
                            )
                            Text("${String.format(Locale.US, "%.1f", uiState.farThreshold)} m", fontSize = 12.sp)
                        }
                    }
                }

                // Calibration
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Calibration",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            if (uiState.isCalibrating) {
                                LinearProgressIndicator(
                                    progress = { uiState.calibrationProgress / 100f },
                                    modifier = Modifier.fillMaxWidth()
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    uiState.calibrationStatus,
                                    fontSize = 12.sp,
                                    color = MaterialTheme.colorScheme.primary
                                )
                            }

                            Button(
                                onClick = { viewModel.calibrate() },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isCalibrating
                            ) {
                                Icon(Icons.Default.Tune, contentDescription = "Calibrate")
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(if (uiState.isCalibrating) "Calibrating..." else "Calibrate Distance")
                            }
                            Text(
                                text = "Place device at 0.5m, 1m, 2m, 3m, and 5m distances when prompted.",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }

                // Advanced settings
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Actions & Feedback",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lock when far")
                                Switch(
                                    checked = uiState.lockActionEnabled,
                                    onCheckedChange = viewModel::toggleLockAction
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Unlock when near")
                                Switch(
                                    checked = uiState.unlockActionEnabled,
                                    onCheckedChange = viewModel::toggleUnlockAction
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Vibrate on lock/unlock")
                                Switch(
                                    checked = uiState.vibrationOnLock,
                                    onCheckedChange = viewModel::toggleVibration
                                )
                            }

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Show notifications")
                                Switch(
                                    checked = uiState.notificationOnLock,
                                    onCheckedChange = viewModel::toggleNotification
                                )
                            }

                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedButton(
                                onClick = { viewModel.resetToDefaults() },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text("Reset to Defaults")
                            }
                        }
                    }
                }

                // History
                if (uiState.history.isNotEmpty()) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Recent History",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold
                                )
                                Spacer(modifier = Modifier.height(8.dp))

                                uiState.history.take(5).forEach { entry ->
                                    HistoryEntryItem(entry)
                                    Spacer(modifier = Modifier.height(8.dp))
                                }
                            }
                        }
                    }
                }
            }

            // Information card (always visible)
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.secondaryContainer
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "How It Works",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "• Uses Bluetooth RSSI to estimate distance from your PC\n" +
                                    "• Walks away beyond FAR threshold → PC locks automatically\n" +
                                    "• Returns within NEAR threshold → PC unlocks\n" +
                                    "• Calibrate for best accuracy in your environment\n" +
                                    "• Make sure Bluetooth is enabled on both devices",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSecondaryContainer
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun HistoryEntryItem(entry: ProximityHistoryEntry) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column {
            Text(
                text = java.text.SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                    .format(Date(entry.timestamp)),
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = "RSSI: ${entry.rssi} dBm",
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = String.format(Locale.US, "%.2f m", entry.distance),
            fontWeight = FontWeight.Bold,
            color = if (entry.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
        )
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = if (entry.isNear) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f)
        ) {
            Text(
                text = if (entry.isNear) "Near" else "Far",
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                color = if (entry.isNear) Color(0xFF4CAF50) else Color(0xFFF44336)
            )
        }
    }
}
