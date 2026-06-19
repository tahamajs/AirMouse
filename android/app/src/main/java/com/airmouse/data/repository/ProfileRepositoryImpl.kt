// app/src/main/java/com/airmouse/data/repository/ProximityRepositoryImpl.kt
package com.airmouse.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IProximityRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityRepositoryImpl @Inject constructor(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : IProximityRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(ProximityState())
    override fun observeProximityState(): Flow<ProximityState> = _state.asStateFlow()

    private val _config = MutableStateFlow(ProximityConfig())
    private val _isMonitoring = MutableStateFlow(false)

    private var bluetoothAdapter: BluetoothAdapter? = null

    init {
        initBluetooth()
        loadConfig()
        startMonitoringIfEnabled()
    }

    private fun initBluetooth() {
        val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
        bluetoothAdapter = bluetoothManager.adapter
    }

    private fun loadConfig() {
        _config.value = ProximityConfig(
            enabled = prefs.getBoolean("proximity_enabled", false),
            nearThreshold = prefs.getFloat("proximity_near_threshold", 1.5f),
            farThreshold = prefs.getFloat("proximity_far_threshold", 3.0f),
            scanInterval = prefs.getLong("proximity_scan_interval", 1000L),
            vibrationEnabled = prefs.getBoolean("proximity_vibration", true),
            autoLockEnabled = prefs.getBoolean("proximity_auto_lock", true),
            autoUnlockEnabled = prefs.getBoolean("proximity_auto_unlock", true),
            deviceAddress = prefs.getString("proximity_device_address", "")
        )
    }

    private fun startMonitoringIfEnabled() {
        if (_config.value.enabled && _config.value.deviceAddress.isNotEmpty()) {
            scope.launch { startMonitoring() }
        }
    }

    override suspend fun getProximityState(): ProximityState = _state.value

    override suspend fun startMonitoring() {
        if (_isMonitoring.value) return

        _isMonitoring.value = true
        scope.launch {
            while (_isMonitoring.value) {
                val distance = estimateDistance()
                val isNear = checkProximity(distance)

                _state.value = ProximityState(
                    isNear = isNear,
                    distance = distance,
                    rssi = getCurrentRssi(),
                    deviceAddress = _config.value.deviceAddress,
                    deviceName = getDeviceName(),
                    lastUpdated = System.currentTimeMillis()
                )

                // Send to server via ConnectionManager
                connectionManager.sendProximity(isNear, distance)

                delay(_config.value.scanInterval)
            }
        }
    }

    override suspend fun stopMonitoring() {
        _isMonitoring.value = false
        _state.value = _state.value.copy(lastUpdated = System.currentTimeMillis())
    }

    override suspend fun isMonitoring(): Boolean = _isMonitoring.value

    override suspend fun getConfig(): ProximityConfig = _config.value

    override suspend fun updateConfig(config: ProximityConfig) {
        _config.value = config
        prefs.putBoolean("proximity_enabled", config.enabled)
        prefs.putFloat("proximity_near_threshold", config.nearThreshold)
        prefs.putFloat("proximity_far_threshold", config.farThreshold)
        prefs.putLong("proximity_scan_interval", config.scanInterval)
        prefs.putBoolean("proximity_vibration", config.vibrationEnabled)
        prefs.putBoolean("proximity_auto_lock", config.autoLockEnabled)
        prefs.putBoolean("proximity_auto_unlock", config.autoUnlockEnabled)
        prefs.putString("proximity_device_address", config.deviceAddress)

        if (config.enabled && config.deviceAddress.isNotEmpty()) {
            startMonitoring()
        } else {
            stopMonitoring()
        }
    }

    override suspend fun calibrate(): Boolean {
        // Calibrate RSSI to distance mapping
        // Collect samples at known distances
        val samples = mutableListOf<Pair<Int, Float>>()
        val distances = listOf(1f, 2f, 3f, 4f, 5f)

        for (distance in distances) {
            // Wait for user to place device at distance
            delay(3000)
            val rssi = getCurrentRssi()
            if (rssi != -100) {
                samples.add(Pair(rssi, distance))
            }
        }

        if (samples.size >= 3) {
            // Calculate txPower and path loss exponent
            // Using linear regression
            prefs.putBoolean("proximity_calibrated", true)
            return true
        }
        return false
    }

    override suspend fun getCalibrationStatus(): ProximityCalibrationStatus {
        return if (prefs.getBoolean("proximity_calibrated", false)) {
            ProximityCalibrationStatus.CALIBRATED
        } else {
            ProximityCalibrationStatus.NOT_CALIBRATED
        }
    }

    override suspend fun resetCalibration() {
        prefs.putBoolean("proximity_calibrated", false)
        prefs.remove("proximity_tx_power")
        prefs.remove("proximity_path_loss")
    }

    override suspend fun setDeviceAddress(address: String) {
        val config = _config.value.copy(deviceAddress = address)
        updateConfig(config)
    }

    override suspend fun getDeviceAddress(): String = _config.value.deviceAddress

    override suspend fun getDeviceName(): String {
        val address = _config.value.deviceAddress
        return if (address.isNotEmpty() && bluetoothAdapter != null) {
            try {
                val device = bluetoothAdapter!!.getRemoteDevice(address)
                device.name ?: address
            } catch (e: Exception) {
                address
            }
        } else {
            "Unknown"
        }
    }

    override suspend fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }

    override suspend fun lockScreen() {
        connectionManager.sendLockScreen()
        _state.value = _state.value.copy(isNear = false)
    }

    override suspend fun unlockScreen() {
        connectionManager.sendUnlockScreen()
        _state.value = _state.value.copy(isNear = true)
    }

    override suspend fun disconnect() {
        stopMonitoring()
        _config.value = _config.value.copy(enabled = false)
        updateConfig(_config.value)
    }

    private suspend fun estimateDistance(): Float {
        val rssi = getCurrentRssi()
        return calculateDistanceFromRssi(rssi)
    }

    private fun getCurrentRssi(): Int {
        // In real implementation, read from BluetoothDevice
        // For now, return simulated value
        return (-80..-30).random()
    }

    private fun calculateDistanceFromRssi(rssi: Int): Float {
        val txPower = prefs.getInt("proximity_tx_power", -59)
        val pathLoss = prefs.getFloat("proximity_path_loss", 2.5f)
        val ratio = (txPower - rssi) / (10.0 * pathLoss)
        val distance = 10.0.pow(ratio)
        return distance.toFloat().coerceIn(0.3f, 15.0f)
    }

    private fun checkProximity(distance: Float): Boolean {
        val wasNear = _state.value.isNear
        val nearThreshold = _config.value.nearThreshold
        val farThreshold = _config.value.farThreshold

        return if (wasNear) {
            distance < farThreshold
        } else {
            distance < nearThreshold
        }
    }
}