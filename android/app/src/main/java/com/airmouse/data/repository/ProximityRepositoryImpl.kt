package com.airmouse.data.repository

import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothManager
import android.content.Context
import com.airmouse.domain.model.ProximityCalibration
import com.airmouse.domain.model.ProximityCalibrationStatus
import com.airmouse.domain.model.ProximityState
import com.airmouse.domain.repository.IProximityRepository
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val bluetoothAdapter: BluetoothAdapter
) : IProximityRepository {

    private val _proximityState = MutableStateFlow(ProximityState.UNKNOWN)
    override fun getProximityState(): StateFlow<ProximityState> = _proximityState.asStateFlow()

    private val _distance = MutableStateFlow(5.0f)
    override fun getDistance(): StateFlow<Float> = _distance.asStateFlow()

    private val _isMonitoring = MutableStateFlow(false)
    override fun isMonitoring(): StateFlow<Boolean> = _isMonitoring.asStateFlow()

    private val _calibration = MutableStateFlow(ProximityCalibration.DEFAULT)
    override fun getCalibration(): StateFlow<ProximityCalibration> = _calibration.asStateFlow()

    private var scanJob = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private var targetDevice: BluetoothDevice? = null
    private var currentRssi = -100

    init {
        loadCalibration()
    }

    private fun loadCalibration() {
        _calibration.value = ProximityCalibration(
            isCalibrated = prefs.getBoolean("proximity_calibrated", false),
            referenceRssi = prefs.getInt("proximity_reference_rssi", -59),
            pathLossExponent = prefs.getFloat("proximity_path_loss", 2.5f),
            nearThreshold = prefs.getFloat("proximity_near_threshold", 1.5f),
            farThreshold = prefs.getFloat("proximity_far_threshold", 3.0f),
            accuracy = prefs.getFloat("proximity_accuracy", 0.7f),
            calibrationTime = prefs.getLong("proximity_calibration_time", 0)
        )
    }

    private fun saveCalibration() {
        val cal = _calibration.value
        prefs.putBoolean("proximity_calibrated", cal.isCalibrated)
        prefs.putInt("proximity_reference_rssi", cal.referenceRssi)
        prefs.putFloat("proximity_path_loss", cal.pathLossExponent)
        prefs.putFloat("proximity_near_threshold", cal.nearThreshold)
        prefs.putFloat("proximity_far_threshold", cal.farThreshold)
        prefs.putFloat("proximity_accuracy", cal.accuracy)
        prefs.putLong("proximity_calibration_time", cal.calibrationTime)
    }

    override suspend fun calibrate(): Unit = calibrate(
        referenceRssi = _calibration.value.referenceRssi,
        pathLossExponent = _calibration.value.pathLossExponent,
        nearThreshold = _calibration.value.nearThreshold,
        farThreshold = _calibration.value.farThreshold
    )

    override suspend fun calibrate(
        referenceRssi: Int,
        pathLossExponent: Float,
        nearThreshold: Float,
        farThreshold: Float
    ) {
        _calibration.value = ProximityCalibration(
            isCalibrated = true,
            referenceRssi = referenceRssi,
            pathLossExponent = pathLossExponent,
            nearThreshold = nearThreshold,
            farThreshold = farThreshold,
            accuracy = 0.8f,
            calibrationTime = System.currentTimeMillis()
        )
        saveCalibration()
    }

    override suspend fun startMonitoring(deviceAddress: String) {
        if (_isMonitoring.value) return

        try {
            targetDevice = bluetoothAdapter?.getRemoteDevice(deviceAddress)
            if (targetDevice == null) {
                _isMonitoring.value = false
                return
            }

            _isMonitoring.value = true
            startRssiScanning()
        } catch (e: Exception) {
            _isMonitoring.value = false
        }
    }

    private fun startRssiScanning() {
        scanJob.cancel()
        scanJob = CoroutineScope(Dispatchers.IO + SupervisorJob())

        scanJob.launch {
            while (_isMonitoring.value) {
                try {
                    // In production, use BluetoothGatt to read RSSI
                    // For now, simulate RSSI changes
                    val rssi = simulateRssi()
                    currentRssi = rssi
                    updateProximity(rssi, targetDevice?.address ?: "", targetDevice?.name)
                    delay(500)
                } catch (e: Exception) {
                    delay(1000)
                }
            }
        }
    }

    private fun simulateRssi(): Int {
        // Simulate realistic RSSI fluctuation
        val baseRssi = -65
        val variation = (Math.random() * 20 - 10).toInt()
        return baseRssi + variation
    }

    private suspend fun updateProximity(rssi: Int, deviceAddress: String, deviceName: String?) {
        val calibration = _calibration.value
        val distance = calibration.calculateDistance(rssi)

        val isNear = if (_proximityState.value.isNear) {
            distance < calibration.farThreshold
        } else {
            distance < calibration.nearThreshold
        }

        val confidence = calculateConfidence(rssi, distance)

        _distance.value = distance
        _proximityState.value = ProximityState(
            isNear = isNear,
            distance = distance,
            signalStrength = rssi,
            deviceAddress = deviceAddress,
            deviceName = deviceName,
            confidence = confidence
        )
    }

    private fun calculateConfidence(rssi: Int, distance: Float): Float {
        val signalConfidence = when {
            rssi > -50 -> 0.95f
            rssi > -60 -> 0.85f
            rssi > -70 -> 0.7f
            rssi > -80 -> 0.5f
            else -> 0.3f
        }

        val distanceConfidence = when {
            distance < 1.0f -> 0.95f
            distance < 2.0f -> 0.85f
            distance < 3.0f -> 0.7f
            else -> 0.5f
        }

        return (signalConfidence * 0.6f + distanceConfidence * 0.4f)
    }

    override suspend fun stopMonitoring() {
        _isMonitoring.value = false
        scanJob.cancel()
        scanJob = CoroutineScope(Dispatchers.IO + SupervisorJob())
        _proximityState.value = ProximityState.UNKNOWN
        _distance.value = 5.0f
    }

    override suspend fun setThresholds(near: Float, far: Float) {
        val cal = _calibration.value
        _calibration.value = cal.copy(
            nearThreshold = near.coerceIn(0.3f, 5.0f),
            farThreshold = far.coerceIn(1.0f, 10.0f)
        )
        saveCalibration()
    }

    override suspend fun getCalibrationStatus(): ProximityCalibrationStatus {
        val cal = _calibration.value
        return ProximityCalibrationStatus(
            isCalibrated = cal.isCalibrated,
            accuracy = cal.accuracy,
            nearThreshold = cal.nearThreshold,
            farThreshold = cal.farThreshold,
            referenceRssi = cal.referenceRssi,
            pathLossExponent = cal.pathLossExponent,
            qualityScore = cal.qualityScore,
            qualityColor = cal.qualityColor,
            confidenceDescription = cal.confidenceDescription
        )
    }

    override suspend fun resetCalibration() {
        _calibration.value = ProximityCalibration.DEFAULT
        prefs.putBoolean("proximity_calibrated", false)
    }

    override suspend fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled ?: false
    }

    override suspend fun getDeviceAddress(): String? {
        return bluetoothAdapter?.address
    }

    override suspend fun disconnect() {
        stopMonitoring()
    }

    fun destroy() {
        scanJob.cancel()
    }
}