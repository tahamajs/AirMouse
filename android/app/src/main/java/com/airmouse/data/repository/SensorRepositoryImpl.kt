package com.airmouse.data.repository

import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorRate
import com.airmouse.domain.model.SensorStatus
import com.airmouse.domain.repository.ISensorRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ISensorRepository {

    private val _sensorData = MutableStateFlow(
        SensorData(
            gyroX = 0f, gyroY = 0f, gyroZ = 0f,
            accelX = 0f, accelY = 0f, accelZ = 0f,
            magX = 0f, magY = 0f, magZ = 0f,
            roll = 0f, pitch = 0f, yaw = 0f
        )
    )

    override fun getSensorData(): Flow<SensorData> = _sensorData

    override suspend fun setSamplingRate(rate: SensorRate) {
        // pass to service
    }

    override suspend fun calibrateGyro(): Boolean = true
    override suspend fun calibrateAccelerometer(): Boolean = true
    override suspend fun calibrateMagnetometer(): Boolean = true

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        return CalibrationStatus(
            gyroCalibrated = false,
            accelCalibrated = false,
            magCalibrated = false,
            allCalibrated = false,
            progress = 0,
            confidence = 0f
        )
    }

    override suspend fun startMonitoring() {
        // implementation
    }

    override suspend fun stopMonitoring() {
        // implementation
    }

    override fun getSensorStatus(): Flow<SensorStatus> {
        // return a dummy flow for now
        return MutableStateFlow(SensorStatus(false, false, 100, 25.0f))
    }

    override suspend fun getAvailableSensors(): List<SensorInfo> {
        return emptyList()
    }
}