package com.airmouse.domain.repository

import com.airmouse.domain.model.*
import kotlinx.coroutines.flow.Flow

interface ISensorRepository {
    suspend fun startMonitoring()
    suspend fun stopMonitoring()
    fun getSensorData(): Flow<SensorData>
    fun getSensorStatus(): Flow<SensorStatus>
    suspend fun getAvailableSensors(): List<SensorInfo>
    suspend fun setSamplingRate(rate: SensorRate)
    suspend fun calibrateGyro(): Boolean
    suspend fun calibrateAccelerometer(): Boolean
    suspend fun calibrateMagnetometer(): Boolean
    suspend fun getCalibrationStatus(): CalibrationStatus
}
