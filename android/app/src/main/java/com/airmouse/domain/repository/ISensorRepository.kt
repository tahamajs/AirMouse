
package com.airmouse.domain.repository

import com.airmouse.domain.model.OrientationData
import com.airmouse.domain.model.SensorCalibrationStatus
import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorInfo
import kotlinx.coroutines.flow.Flow

interface ISensorRepository {
    
    fun observeSensorData(): Flow<SensorData>
    fun observeOrientation(): Flow<OrientationData>
    suspend fun getCurrentSensorData(): SensorData

    
    suspend fun getCalibrationStatus(): SensorCalibrationStatus
    fun observeCalibrationStatus(): Flow<SensorCalibrationStatus>

    
    suspend fun startSensors()
    suspend fun stopSensors()
    suspend fun isSensorActive(): Boolean

    
    suspend fun getSensorInfo(): List<SensorInfo>

    
    suspend fun calibrateSensors(): Boolean
    suspend fun resetCalibration()
    suspend fun isCalibrated(): Boolean

    
    suspend fun setPowerSaveMode(enabled: Boolean)
    suspend fun getRecommendedDelay(): Int
}
