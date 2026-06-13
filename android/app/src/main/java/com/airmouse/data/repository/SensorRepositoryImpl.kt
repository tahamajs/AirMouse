// app/src/main/java/com/airmouse/data/repository/SensorRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorStatus
import com.airmouse.domain.model.SensorInfo
import com.airmouse.domain.model.SensorRate
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.repository.ISensorRepository
import com.airmouse.sensors.SensorService
import com.airmouse.sensors.CalibrationHelper
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val sensorService: SensorService,
    private val calibrationHelper: CalibrationHelper,
    private val prefs: PreferencesManager
) : ISensorRepository {

    private val _sensorData = MutableStateFlow<SensorData?>(null)
    private val _sensorStatus = MutableStateFlow(SensorStatus(false, false, 100, 25f))

    override suspend fun startMonitoring() {
        sensorService.start()
        _sensorStatus.value = _sensorStatus.value.copy(isActive = true)
    }

    override suspend fun stopMonitoring() {
        sensorService.stop()
        _sensorStatus.value = _sensorStatus.value.copy(isActive = false)
    }

    override fun getSensorData(): Flow<SensorData?> = _sensorData.asStateFlow()

    override fun getSensorStatus(): Flow<SensorStatus> = _sensorStatus.asStateFlow()

    override suspend fun getAvailableSensors(): List<SensorInfo> = emptyList()

    override suspend fun setSamplingRate(rate: SensorRate) {
        val delay = when (rate) {
            SensorRate.FASTEST -> android.hardware.SensorManager.SENSOR_DELAY_FASTEST
            SensorRate.GAME -> android.hardware.SensorManager.SENSOR_DELAY_GAME
            SensorRate.UI -> android.hardware.SensorManager.SENSOR_DELAY_UI
            SensorRate.NORMAL -> android.hardware.SensorManager.SENSOR_DELAY_NORMAL
        }
        sensorService.setSamplingRate(delay)
    }

    override suspend fun calibrateGyro(): Boolean = true
    override suspend fun calibrateAccelerometer(): Boolean = true
    override suspend fun calibrateMagnetometer(): Boolean = true

    override suspend fun getCalibrationStatus(): CalibrationStatus = calibrationHelper.getCalibrationStatus()
}