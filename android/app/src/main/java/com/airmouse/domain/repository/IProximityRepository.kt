// app/src/main/java/com/airmouse/domain/repository/IProximityRepository.kt
package com.airmouse.domain.repository

import kotlinx.coroutines.flow.Flow

interface IProximityRepository {
    suspend fun startMonitoring(deviceAddress: String)
    suspend fun stopMonitoring()
    fun getDistance(): Flow<Float>
    fun getProximityState(): Flow<ProximityState>
    suspend fun setThresholds(near: Float, far: Float)
    suspend fun calibrate()
    suspend fun getCalibrationStatus(): ProximityCalibrationStatus
}

data class ProximityState(
    val isNear: Boolean,
    val distance: Float,
    val signalStrength: Int,
    val lastUpdate: Long
)

data class ProximityCalibrationStatus(
    val isCalibrated: Boolean,
    val referenceRssi: Int,
    val pathLossExponent: Float,
    val accuracy: Float
)// app/src/main/java/com/airmouse/domain/repository/IProximityRepository.kt
package com.airmouse.domain.repository

import kotlinx.coroutines.flow.Flow

interface IProximityRepository {
    suspend fun startMonitoring(deviceAddress: String)
    suspend fun stopMonitoring()
    fun getDistance(): Flow<Float>
    fun getProximityState(): Flow<ProximityState>
    suspend fun setThresholds(near: Float, far: Float)
    suspend fun calibrate()
    suspend fun getCalibrationStatus(): ProximityCalibrationStatus
}

data class ProximityState(
    val isNear: Boolean,
    val distance: Float,
    val signalStrength: Int,
    val lastUpdate: Long
)

data class ProximityCalibrationStatus(
    val isCalibrated: Boolean,
    val referenceRssi: Int,
    val pathLossExponent: Float,
    val accuracy: Float
)