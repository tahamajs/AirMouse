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
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
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

// ==================== DATA CLASSES ====================

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
    val lockScreenTimeout: Int = 0,
    val vibrationOnLock: Boolean = true,
    val notificationOnLock: Boolean = true
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

// ==================== VIEW MODEL ====================

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

    @Suppress("DEPRECATION")
    private fun initializeBluetooth() {
        // Check Bluetooth permission
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

    private fun calculateDistanceFromRssi(rssi: Int): Float {
        val refRssi = -59
        val n = 2.0
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
            _uiState.update {
                it.copy(
                    isNear = isNear,
                    status = if (isNear) "Near - Device unlocked" else "Far - Device locked"
                )
            }
        }
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

    // ==================== PUBLIC METHODS ====================

    fun toggleService(enabled: Boolean) {
        viewModelScope.launch {
            _uiState.update { it.copy(isEnabled = enabled) }
            if (enabled) {
                startScanning()
                _uiState.update { it.copy(status = "Service starting...") }
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
                delay(3000)

                val avgRssi = getAverageRssi()
                if (avgRssi != -100) {
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

    private fun getAverageRssi(): Int {
        var samples = 0
        var sum = 0
        repeat(10) {
            if (_uiState.value.rssi != -100) {
                sum += _uiState.value.rssi
                samples++
            }
        }
        return if (samples > 0) sum / samples else -100
    }

    private fun calculateOptimalParameters(samples: List<Pair<Int, Float>>) {
        val avgTxPower = samples.map { it.first }.average().toInt()
        val avgPathLoss = samples.map { (rssi, distance) ->
            if (distance > 0) (avgTxPower - rssi) / (10 * log10(distance.toDouble())) else 2.5
        }.average().toFloat()

        prefs.putInt("proximity_tx_power", avgTxPower)
        prefs.putFloat("proximity_path_loss", avgPathLoss)
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
                lockScreenTimeout = 0,
                vibrationOnLock = true,
                notificationOnLock = true
            )
        }
        saveSettings()
        _uiState.update { it.copy(status = "Reset to defaults") }
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (_: Exception) {}
    }
}

// ==================== SCREEN ====================

@Composable
fun ProximityScreen(
    navigationActions: NavigationActions,
    viewModel: ProximityViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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
            // Main Control Card
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
                                    text = "Auto-lock PC when you walk away",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.isEnabled,
                                onCheckedChange = viewModel::toggleService
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
                                Text("Connected Device:", style = MaterialTheme.typography.bodyMedium)
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
                                Text("Signal Strength:", style = MaterialTheme.typography.bodyMedium)
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
                            Text(
                                text = "Current Distance",
                                style = MaterialTheme.typography.titleMedium
                            )
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
                        }
                    }
                }
            }

            if (uiState.isEnabled) {
                // Threshold Settings Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Threshold Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(12.dp))

                            Text("Near Threshold (Unlock)", style = MaterialTheme.typography.titleMedium)
                            Slider(
                                value = uiState.nearThreshold,
                                onValueChange = viewModel::updateNearThreshold,
                                valueRange = 0.5f..5.0f,
                                steps = 45
                            )
                            Text("${String.format(Locale.US, "%.1f", uiState.nearThreshold)} m", fontSize = 12.sp)

                            Spacer(modifier = Modifier.height(16.dp))

                            Text("Far Threshold (Lock)", style = MaterialTheme.typography.titleMedium)
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

                // Calibration Card
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
                        }
                    }
                }

                // Advanced Settings Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Advanced Settings",
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold
                            )

                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text("Lock PC when far")
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
                                Text("Unlock PC when near")
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
                                Text("Vibration on lock/unlock")
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

                // History Card
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

                                // Fixed: items with explicit receiver
                                androidx.compose.foundation.lazy.items(
                                    items = uiState.history.take(5),
                                    key = { it.timestamp }
                                ) { entry ->
                                    HistoryEntryItem(entry)
                                }
                            }
                        }
                    }
                }
            }

            // Info Card
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
                            text = "• The app uses Bluetooth RSSI to estimate distance from your PC\n" +
                                    "• When you walk away beyond the FAR threshold, your PC locks automatically\n" +
                                    "• When you return within the NEAR threshold, your PC unlocks\n" +
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