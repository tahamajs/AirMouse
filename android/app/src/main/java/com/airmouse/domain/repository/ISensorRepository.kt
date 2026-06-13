// app/src/main/java/com/airmouse/domain/repository/ISensorRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorStatus
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

data class SensorData(
    val gyroscope: Triple<Float, Float, Float>,
    val accelerometer: Triple<Float, Float, Float>,
    val magnetometer: Triple<Float, Float, Float>,
    val orientation: Triple<Float, Float, Float>,
    val timestamp: Long
)

data class SensorStatus(
    val isActive: Boolean,
    val isCalibrated: Boolean,
    val batteryLevel: Int,
    val temperature: Float
)

data class SensorInfo(
    val name: String,
    val type: Int,
    val vendor: String,
    val isAvailable: Boolean
)

enum class SensorRate {
    FASTEST, GAME, UI, NORMAL
}// app/src/main/java/com/airmouse/domain/repository/ISensorRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.SensorData
import com.airmouse.domain.model.SensorStatus
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

data class SensorData(
    val gyroscope: Triple<Float, Float, Float>,
    val accelerometer: Triple<Float, Float, Float>,
    val magnetometer: Triple<Float, Float, Float>,
    val orientation: Triple<Float, Float, Float>,
    val timestamp: Long
)

data class SensorStatus(
    val isActive: Boolean,
    val isCalibrated: Boolean,
    val batteryLevel: Int,
    val temperature: Float
)

data class SensorInfo(
    val name: String,
    val type: Int,
    val vendor: String,
    val isAvailable: Boolean
)

enum class SensorRate {
    FASTEST, GAME, UI, NORMAL
}