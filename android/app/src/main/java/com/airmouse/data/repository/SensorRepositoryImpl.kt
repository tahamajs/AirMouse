// app/src/main/java/com/airmouse/data/repository/SensorRepositoryImpl.kt
package com.airmouse.data.repository

import android.hardware.Sensor
import android.hardware.SensorManager
import com.airmouse.domain.model.*
import com.airmouse.domain.repository.ISensorRepository
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MadgwickAHRS
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val sensorManager: SensorManager,
    private val prefs: PreferencesManager,
    private val calibrationHelper: CalibrationHelper
) : ISensorRepository {

    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _sensorData = MutableStateFlow(SensorData())
    override fun observeSensorData(): Flow<SensorData> = _sensorData.asStateFlow()

    private val _orientation = MutableStateFlow(OrientationData(0f, 0f, 0f))
    override fun observeOrientation(): Flow<OrientationData> = _orientation.asStateFlow()

    private val _calibrationStatus = MutableStateFlow(SensorCalibrationStatus.NOT_CALIBRATED)
    override fun observeCalibrationStatus(): Flow<SensorCalibrationStatus> = _calibrationStatus.asStateFlow()

    private val _isActive = MutableStateFlow(false)

    private var madgwick = MadgwickAHRS()
    private var sensorListener: SensorEventListener? = null

    init {
        loadCalibrationStatus()
    }

    private fun loadCalibrationStatus() {
        _calibrationStatus.value = if (prefs.isCalibrated()) {
            SensorCalibrationStatus.CALIBRATED
        } else {
            SensorCalibrationStatus.NOT_CALIBRATED
        }
    }

    override suspend fun getCurrentSensorData(): SensorData = _sensorData.value

    override suspend fun getCalibrationStatus(): SensorCalibrationStatus = _calibrationStatus.value

    override suspend fun startSensors() {
        if (_isActive.value) return

        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        val calibrated = calibrationHelper.correctGyro(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateGyro(calibrated.first, calibrated.second, calibrated.third, 0.01f)
                        updateSensorData(gyroX = event.values[0], gyroY = event.values[1], gyroZ = event.values[2])
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val calibrated = calibrationHelper.correctAccelerometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateAccel(calibrated.first, calibrated.second, calibrated.third, 0.01f)
                        updateSensorData(
                            accelX = event.values[0],
                            accelY = event.values[1],
                            accelZ = event.values[2]
                        )
                        updateOrientation()
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val calibrated = calibrationHelper.correctMagnetometer(
                            event.values[0], event.values[1], event.values[2]
                        )
                        madgwick.updateMag(calibrated.first, calibrated.second, calibrated.third, 0.01f)
                        updateSensorData(
                            magX = event.values[0],
                            magY = event.values[1],
                            magZ = event.values[2]
                        )
                    }
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorListener = listener
        gyroscope?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        accelerometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }
        magnetometer?.let { sensorManager.registerListener(listener, it, SensorManager.SENSOR_DELAY_GAME) }

        _isActive.value = true
    }

    override suspend fun stopSensors() {
        if (!_isActive.value) return

        sensorListener?.let {
            sensorManager.unregisterListener(it)
            sensorListener = null
        }

        _isActive.value = false
    }

    override suspend fun isSensorActive(): Boolean = _isActive.value

    override suspend fun getSensorInfo(): List<SensorInfo> {
        val sensors = sensorManager.getSensorList(Sensor.TYPE_ALL)
        return sensors.map { sensor ->
            SensorInfo(
                name = sensor.name,
                vendor = sensor.vendor,
                version = sensor.version,
                maxRange = sensor.maximumRange,
                resolution = sensor.resolution,
                power = sensor.power,
                isAvailable = true
            )
        }
    }

    override suspend fun calibrateSensors(): Boolean {
        // Perform full calibration
        val success = calibrationHelper.calibrateGyroscope { }
        if (success) {
            calibrationHelper.calibrateMagnetometer { }
            calibrationHelper.calibrateAccelerometer { }
            _calibrationStatus.value = SensorCalibrationStatus.CALIBRATED
            prefs.putBoolean("calibration_complete", true)
        }
        return success
    }

    override suspend fun resetCalibration() {
        calibrationHelper.reset()
        _calibrationStatus.value = SensorCalibrationStatus.NOT_CALIBRATED
        prefs.putBoolean("calibration_complete", false)
    }

    override suspend fun isCalibrated(): Boolean = prefs.isCalibrated()

    override suspend fun setPowerSaveMode(enabled: Boolean) {
        prefs.putBoolean("power_save_mode", enabled)
        // Adjust sensor delay based on power mode
        val delay = if (enabled) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }
        // Re-register sensors with new delay
        if (_isActive.value) {
            stopSensors()
            startSensors()
        }
    }

    override suspend fun getRecommendedDelay(): Int {
        return if (prefs.getBoolean("power_save_mode", false)) {
            SensorManager.SENSOR_DELAY_NORMAL
        } else {
            SensorManager.SENSOR_DELAY_GAME
        }
    }

    private fun updateSensorData(
        gyroX: Float = _sensorData.value.gyroX,
        gyroY: Float = _sensorData.value.gyroY,
        gyroZ: Float = _sensorData.value.gyroZ,
        accelX: Float = _sensorData.value.accelX,
        accelY: Float = _sensorData.value.accelY,
        accelZ: Float = _sensorData.value.accelZ,
        magX: Float = _sensorData.value.magX,
        magY: Float = _sensorData.value.magY,
        magZ: Float = _sensorData.value.magZ
    ) {
        val current = _sensorData.value
        _sensorData.value = current.copy(
            gyroX = gyroX,
            gyroY = gyroY,
            gyroZ = gyroZ,
            accelX = accelX,
            accelY = accelY,
            accelZ = accelZ,
            magX = magX,
            magY = magY,
            magZ = magZ,
            timestamp = System.currentTimeMillis()
        )
    }

    private fun updateOrientation() {
        val roll = madgwick.getRollDegrees()
        val pitch = madgwick.getPitchDegrees()
        val yaw = madgwick.getYawDegrees()

        _orientation.value = OrientationData(
            roll = roll,
            pitch = pitch,
            yaw = yaw,
            rollDeg = roll,
            pitchDeg = pitch,
            yawDeg = yaw
        )

        val current = _sensorData.value
        _sensorData.value = current.copy(
            roll = roll,
            pitch = pitch,
            yaw = yaw
        )
    }
}