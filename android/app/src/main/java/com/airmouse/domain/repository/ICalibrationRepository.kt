
package com.airmouse.domain.repository

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.SensorCalibrationData
import kotlinx.coroutines.flow.Flow

interface ICalibrationRepository {
    
    suspend fun getCalibrationStatus(): CalibrationStatus
    fun observeCalibrationStatus(): Flow<CalibrationStatus>
    suspend fun getCalibrationProgress(): Int
    fun observeCalibrationProgress(): Flow<Int>
    fun observeCalibrationQuality(): Flow<CalibrationQuality>

    
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean
    suspend fun getGyroBias(): GyroBias
    suspend fun saveGyroBias(bias: GyroBias)

    
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean
    suspend fun getMagOffset(): SensorCalibrationData
    suspend fun saveMagOffset(data: SensorCalibrationData)

    
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean
    suspend fun getAccelOffset(): SensorCalibrationData
    suspend fun saveAccelOffset(data: SensorCalibrationData)

    
    suspend fun getCalibrationData(): CalibrationData
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun resetCalibration()
    suspend fun getCalibrationQuality(): CalibrationQuality
    suspend fun resetAllCalibration()
    suspend fun updateCalibrationStatus(status: CalibrationStatus)
    suspend fun updateCalibrationQuality(quality: CalibrationQuality)
    suspend fun updateCalibrationProgress(progress: Int)
}
