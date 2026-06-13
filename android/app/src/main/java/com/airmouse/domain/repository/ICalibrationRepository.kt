// app/src/main/java/com/airmouse/domain/repository/ICalibrationRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration
import kotlinx.coroutines.flow.Flow

interface ICalibrationRepository {
    suspend fun saveGyroBias(bias: GyroBias)
    suspend fun getGyroBias(): GyroBias
    suspend fun saveAccelCalibration(calibration: AccelCalibration)
    suspend fun getAccelCalibration(): AccelCalibration
    suspend fun saveMagCalibration(calibration: MagCalibration)
    suspend fun getMagCalibration(): MagCalibration
    suspend fun markCalibrationComplete()
    fun isCalibrationComplete(): Flow<Boolean>
    suspend fun resetCalibration()
    suspend fun getCalibrationStatus(): CalibrationStatus
    suspend fun exportCalibrationData(): String
    suspend fun importCalibrationData(data: String): Boolean
}

data class CalibrationStatus(
    val isGyroCalibrated: Boolean,
    val isAccelCalibrated: Boolean,
    val isMagCalibrated: Boolean,
    val isComplete: Boolean,
    val lastCalibrationTime: Long,
    val quality: Float
)