// app/src/main/java/com/airmouse/domain/repository/ISensorRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.OrientationData
import com.airmouse.domain.model.SensorCalibrationStatus
import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorInfo
import kotlinx.coroutines.flow.Flow

interface ISensorRepository {
    // Sensor data
    fun observeSensorData(): Flow<SensorData>
    fun observeOrientation(): Flow<OrientationData>
    suspend fun getCurrentSensorData(): SensorData

    // Calibration status
    suspend fun getCalibrationStatus(): SensorCalibrationStatus
    fun observeCalibrationStatus(): Flow<SensorCalibrationStatus>

    // Sensor management
    suspend fun startSensors()
    suspend fun stopSensors()
    suspend fun isSensorActive(): Boolean

    // Sensor info
    suspend fun getSensorInfo(): List<SensorInfo>

    // Calibration
    suspend fun calibrateSensors(): Boolean
    suspend fun resetCalibration()
    suspend fun isCalibrated(): Boolean

    // Battery optimization
    suspend fun setPowerSaveMode(enabled: Boolean)
    suspend fun getRecommendedDelay(): Int
}
