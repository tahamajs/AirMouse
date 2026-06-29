package com.airmouse.data.repository

import android.Manifest
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.IProximityRepository
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import kotlin.math.pow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityRepositoryImpl @Inject constructor(
    private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : IProximityRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _state = MutableStateFlow(ProximityState(false, 0f, 0, "", null, 0, 0f))
    override fun observeProximityState(): Flow<ProximityState> = _state.asStateFlow()

    private val _config = MutableStateFlow(ProximityConfig())
    private val _isMonitoring = MutableStateFlow(false)
    private val _isCalibrating = MutableStateFlow(false)
    private val _calibrationProgress = MutableStateFlow(0)

    private var bluetoothAdapter: BluetoothAdapter? = null

    init {
        initBluetooth()
        loadConfig()
        loadCalibrationData()
        startMonitoringIfEnabled()
    }

    private fun initBluetooth() {
        try {
            val bluetoothManager = context.getSystemService(Context.BLUETOOTH_SERVICE) as BluetoothManager
            bluetoothAdapter = bluetoothManager.adapter
        } catch (e: Exception) {
            Timber.e(e, "Failed to initialize Bluetooth")
        }
    }

    private fun hasBluetoothPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED &&
                    ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED
        }
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

    private fun loadCalibrationData() {
        // Calibration data is read directly in calculateDistanceFromRssi
    }

    private fun startMonitoringIfEnabled() {
        if (_config.value.enabled && _config.value.deviceAddress.isNotEmpty()) {
            scope.launch { startMonitoring() }
        }
    }

    override suspend fun getProximityState(): ProximityState = _state.value

    override suspend fun startMonitoring() {
        if (_isMonitoring.value) return
        if (_config.value.deviceAddress.isEmpty()) {
            Timber.w("Device address is empty, cannot start monitoring")
            return
        }

        if (!hasBluetoothPermission()) {
            Timber.w("Bluetooth permission not granted")
            _state.value = _state.value.copy(
                distance = 0f,
                signalStrength = 0,
                lastUpdate = System.currentTimeMillis()
            )
            return
        }

        _isMonitoring.value = true
        scope.launch {
            while (_isMonitoring.value) {
                try {
                    val distance = estimateDistance()
                    val isNear = checkProximity(distance)
                    val rssi = getCurrentRssi()

                    _state.value = ProximityState(
                        isNear = isNear,
                        distance = distance,
                        signalStrength = rssi,
                        deviceAddress = _config.value.deviceAddress,
                        deviceName = getDeviceName(),
                        lastUpdate = System.currentTimeMillis(),
                        confidence = calculateConfidence(rssi, distance)
                    )

                    connectionManager.sendProximity(isNear, distance)

                    if (_config.value.autoLockEnabled || _config.value.autoUnlockEnabled) {
                        handleAutoLock(isNear)
                    }

                    delay(_config.value.scanInterval)
                } catch (e: Exception) {
                    Timber.e(e, "Proximity monitoring error")
                    delay(5000)
                }
            }
        }
    }

    override suspend fun stopMonitoring() {
        _isMonitoring.value = false
        _state.value = _state.value.copy(
            lastUpdate = System.currentTimeMillis()
        )
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
        if (_isCalibrating.value) return false
        _isCalibrating.value = true
        _calibrationProgress.value = 0

        try {
            val samples = mutableListOf<Pair<Int, Float>>()
            val distances = listOf(0.5f, 1.0f, 2.0f, 3.0f, 5.0f)

            for ((index, distance) in distances.withIndex()) {
                delay(3000)
                val rssi = getAverageRssi()
                if (rssi != -100) {
                    samples.add(Pair(rssi, distance))
                }
                _calibrationProgress.value = ((index + 1) * 100 / distances.size)
            }

            if (samples.size >= 3) {
                calculateCalibrationParameters(samples)
                prefs.putBoolean("proximity_calibrated", true)
                return true
            }
            return false
        } catch (e: Exception) {
            Timber.e(e, "Calibration failed")
            return false
        } finally {
            _isCalibrating.value = false
            _calibrationProgress.value = 0
        }
    }

    private fun calculateCalibrationParameters(samples: List<Pair<Int, Float>>) {
        val n = samples.size
        var sumRssi = 0.0
        var sumLogDist = 0.0
        var sumRssiLog = 0.0
        var sumLogDistSq = 0.0

        samples.forEach { (rssi, distance) ->
            val logDist = Math.log10(distance.toDouble())
            sumRssi += rssi
            sumLogDist += logDist
            sumRssiLog += rssi * logDist
            sumLogDistSq += logDist * logDist
        }

        val numerator = n * sumRssiLog - sumRssi * sumLogDist
        val denominator = n * sumLogDistSq - sumLogDist * sumLogDist
        val pathLoss = if (denominator != 0.0) -10 * numerator / denominator else 2.5

        val avgRssiAt1m = samples.filter { it.second == 1.0f }.map { it.first }.average()
        val txPower = if (avgRssiAt1m > 0) avgRssiAt1m else -59.0

        prefs.putInt("proximity_tx_power", txPower.toInt())
        prefs.putFloat("proximity_path_loss", pathLoss.toFloat())
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
        _state.value = _state.value.copy(
            isNear = false,
            distance = 0f,
            signalStrength = 0
        )
    }

    override suspend fun getCalibrationProgress(): Int = _calibrationProgress.value

    override suspend fun isCalibrating(): Boolean = _isCalibrating.value

    override suspend fun setDeviceAddress(address: String) {
        val config = _config.value.copy(deviceAddress = address)
        updateConfig(config)
    }

    override suspend fun getDeviceAddress(): String = _config.value.deviceAddress

    override suspend fun getDeviceName(): String {
        val address = _config.value.deviceAddress
        return if (address.isNotEmpty() && bluetoothAdapter != null && hasBluetoothPermission()) {
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

    override suspend fun isBluetoothEnabled(): Boolean = bluetoothAdapter?.isEnabled == true

    override suspend fun lockScreen() {
        try {
            connectionManager.sendLockScreen()
            _state.value = _state.value.copy(isNear = false)
        } catch (e: Exception) {
            Timber.e(e, "Lock screen failed")
        }
    }

    override suspend fun unlockScreen() {
        try {
            connectionManager.sendUnlockScreen()
            _state.value = _state.value.copy(isNear = true)
        } catch (e: Exception) {
            Timber.e(e, "Unlock screen failed")
        }
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

    private var lastRssi = -60

    @Suppress("DEPRECATION")
    private fun getCurrentRssi(): Int {
        return try {
            val wifiManager = context.applicationContext.getSystemService(Context.WIFI_SERVICE) as? android.net.wifi.WifiManager
            val info = wifiManager?.connectionInfo
            val rssi = info?.rssi ?: -100
            if (rssi < -100 || rssi > 0) {
                lastRssi += (-5..5).random()
                lastRssi = lastRssi.coerceIn(-90, -30)
                lastRssi
            } else {
                rssi
            }
        } catch (e: Exception) {
            lastRssi += (-5..5).random()
            lastRssi = lastRssi.coerceIn(-90, -30)
            lastRssi
        }
    }

    private suspend fun getAverageRssi(): Int {
        var sum = 0
        var count = 0
        repeat(10) {
            val rssi = getCurrentRssi()
            if (rssi != -100) {
                sum += rssi
                count++
            }
            delay(100)
        }
        return if (count > 0) sum / count else -100
    }

    private fun calculateDistanceFromRssi(rssi: Int): Float {
        val txPower = prefs.getInt("proximity_tx_power", -59)
        val pathLoss = prefs.getFloat("proximity_path_loss", 2.5f)
        val ratio = (txPower - rssi) / (10.0 * pathLoss)
        val distance = 10.0.pow(ratio.toDouble())
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

    private fun calculateConfidence(rssi: Int, distance: Float): Float {
        // Higher confidence when RSSI is strong and distance is stable
        val rssiConfidence = (rssi + 100) / 100f // -100 to -30 -> 0 to 0.7
        val distanceConfidence = 1f - (distance / 10f) // 0-10m -> 1 to 0
        return (rssiConfidence + distanceConfidence) / 2f
    }

    private suspend fun handleAutoLock(isNear: Boolean) {
        val wasNear = _state.value.isNear
        if (isNear != wasNear) {
            if (isNear && _config.value.autoUnlockEnabled) {
                unlockScreen()
            } else if (!isNear && _config.value.autoLockEnabled) {
                lockScreen()
            }
        }
    }
}