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
import kotlin.math.pow

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
            if (!isNear && _uiState.value.lockActionEnabled) {
                // lock
            } else if (isNear && _uiState.value.unlockActionEnabled) {
                // unlock
            }

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
        _uiState.update { it.copy(history = trimmedHistory(newHistory)) }
    }
    
    private fun trimmedHistory(history: List<ProximityHistoryEntry>) = history.take(50)

    private fun startScanning() {
        if (!_uiState.value.isEnabled || isScanning) return
        isScanning = true
        bluetoothAdapter?.startDiscovery()
    }

    private fun stopScanning() {
        bluetoothAdapter?.cancelDiscovery()
        isScanning = false
    }

    override fun onCleared() {
        super.onCleared()
        stopScanning()
        try {
            context.unregisterReceiver(bluetoothReceiver)
        } catch (e: Exception) {}
    }
}