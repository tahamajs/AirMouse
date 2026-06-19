// app/src/main/java/com/airmouse/data/datasource/local/ICalibrationDataSource.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus

interface ICalibrationDataSource {

    // Save/Load calibration data
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun getCalibrationData(): CalibrationData

    // Gyroscope
    suspend fun saveGyroBias(x: Float, y: Float, z: Float)
    suspend fun getGyroBias(): Triple<Float, Float, Float>

    // Magnetometer
    suspend fun saveMagOffset(x: Float, y: Float, z: Float)
    suspend fun getMagOffset(): Triple<Float, Float, Float>
    suspend fun saveMagScale(x: Float, y: Float, z: Float)
    suspend fun getMagScale(): Triple<Float, Float, Float>

    // Accelerometer
    suspend fun saveAccelOffset(x: Float, y: Float, z: Float)
    suspend fun getAccelOffset(): Triple<Float, Float, Float>
    suspend fun saveAccelScale(x: Float, y: Float, z: Float)
    suspend fun getAccelScale(): Triple<Float, Float, Float>

    // Status
    suspend fun setCalibrationStatus(status: CalibrationStatus)
    suspend fun getCalibrationStatus(): CalibrationStatus

    suspend fun setCalibrationQuality(quality: CalibrationQuality)
    suspend fun getCalibrationQuality(): CalibrationQuality

    suspend fun setCalibrationProgress(progress: Int)
    suspend fun getCalibrationProgress(): Int

    suspend fun resetAll()
}