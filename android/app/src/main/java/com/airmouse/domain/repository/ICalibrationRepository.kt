// app/src/main/java/com/airmouse/domain/repository/ICalibrationRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.SensorCalibrationData
import kotlinx.coroutines.flow.Flow

interface ICalibrationRepository {
    // Calibration status
    suspend fun getCalibrationStatus(): CalibrationStatus
    fun observeCalibrationStatus(): Flow<CalibrationStatus>
    suspend fun getCalibrationProgress(): Int
    fun observeCalibrationProgress(): Flow<Int>
    // Gyroscope calibration
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean
    suspend fun getGyroBias(): GyroBias
    suspend fun saveGyroBias(bias: GyroBias)

    // Magnetometer calibration
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean
    suspend fun getMagOffset(): SensorCalibrationData
    suspend fun saveMagOffset(data: SensorCalibrationData)

    // Accelerometer calibration
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean
    suspend fun getAccelOffset(): SensorCalibrationData
    suspend fun saveAccelOffset(data: SensorCalibrationData)

    // Complete calibration
    suspend fun getCalibrationData(): CalibrationData
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun resetCalibration()

    // Quality
    suspend fun getCalibrationQuality(): CalibrationQuality
    fun observeCalibrationQuality(): Flow<CalibrationQuality>

    // Reset
    suspend fun resetAllCalibration()
}
