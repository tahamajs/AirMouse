package com.airmouse.data.repository

import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.domain.model.OrientationData
import com.airmouse.domain.model.SensorCalibrationStatus
import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorInfo
import com.airmouse.domain.repository.ISensorRepository
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.sensors.MadgwickAHRS
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
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
    private val _orientation = MutableStateFlow(OrientationData(0f, 0f, 0f))
    private val _calibrationStatus = MutableStateFlow(SensorCalibrationStatus.NOT_CALIBRATED)
    private val _isActive = MutableStateFlow(false)
    private var listener: SensorEventListener? = null
    private var madgwick = MadgwickAHRS()

    override fun observeSensorData(): Flow<SensorData> = _sensorData.asStateFlow()
    override fun observeOrientation(): Flow<OrientationData> = _orientation.asStateFlow()
    override suspend fun getCurrentSensorData(): SensorData = _sensorData.value
    override suspend fun getCalibrationStatus(): SensorCalibrationStatus = _calibrationStatus.value
    override fun observeCalibrationStatus(): Flow<SensorCalibrationStatus> = _calibrationStatus.asStateFlow()

    override suspend fun startSensors() {
        if (_isActive.value) return
        val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        val l = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> _sensorData.value = _sensorData.value.copy(
                        gyroX = event.values[0], gyroY = event.values[1], gyroZ = event.values[2]
                    )
                    Sensor.TYPE_ACCELEROMETER -> {
                        _sensorData.value = _sensorData.value.copy(
                            accelX = event.values[0], accelY = event.values[1], accelZ = event.values[2]
                        )
                        _orientation.value = OrientationData(event.values[0], event.values[1], event.values[2])
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> _sensorData.value = _sensorData.value.copy(
                        magX = event.values[0], magY = event.values[1], magZ = event.values[2]
                    )
                }
            }
            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }
        listener = l
        gyro?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        accel?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        mag?.let { sensorManager.registerListener(l, it, SensorManager.SENSOR_DELAY_GAME) }
        _isActive.value = true
    }

    override suspend fun stopSensors() {
        listener?.let { sensorManager.unregisterListener(it) }
        listener = null
        _isActive.value = false
    }

    override suspend fun isSensorActive(): Boolean = _isActive.value

    override suspend fun getSensorInfo(): List<SensorInfo> =
        sensorManager.getSensorList(Sensor.TYPE_ALL).map { sensor ->
            SensorInfo(sensor.name, sensor.vendor, sensor.version, sensor.maximumRange, sensor.resolution, sensor.power, true)
        }

    override suspend fun calibrateSensors(): Boolean {
        val ok = calibrationHelper.calibrateGyroscope()
        if (ok) _calibrationStatus.value = SensorCalibrationStatus.CALIBRATED
        prefs.putBoolean("calibration_complete", ok)
        return ok
    }

    override suspend fun resetCalibration() {
        prefs.putBoolean("calibration_complete", false)
        _calibrationStatus.value = SensorCalibrationStatus.NOT_CALIBRATED
    }

    override suspend fun isCalibrated(): Boolean = prefs.isCalibrated()

    override suspend fun setPowerSaveMode(enabled: Boolean) {
        prefs.putBoolean("power_save_mode", enabled)
    }

    override suspend fun getRecommendedDelay(): Int {
        return if (prefs.getBoolean("power_save_mode", false)) SensorManager.SENSOR_DELAY_NORMAL else SensorManager.SENSOR_DELAY_GAME
    }
}
