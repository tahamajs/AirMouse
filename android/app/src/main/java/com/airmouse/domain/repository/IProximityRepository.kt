// app/src/main/java/com/airmouse/domain/repository/IProximityRepository.kt
package com.airmouse.domain.repository

import com.airmouse.domain.model.ProximityCalibrationStatus
import com.airmouse.domain.model.ProximityConfig
import com.airmouse.domain.model.ProximityState
import kotlinx.coroutines.flow.Flow

interface IProximityRepository {
    // State
    fun observeProximityState(): Flow<ProximityState>
    suspend fun getProximityState(): ProximityState

    // Monitoring
    suspend fun startMonitoring()
    suspend fun stopMonitoring()
    suspend fun isMonitoring(): Boolean

    // Configuration
    suspend fun getConfig(): ProximityConfig
    suspend fun updateConfig(config: ProximityConfig)

    // Calibration
    suspend fun calibrate(): Boolean
    suspend fun getCalibrationStatus(): ProximityCalibrationStatus
    suspend fun resetCalibration()

    // Device management
    suspend fun setDeviceAddress(address: String)
    suspend fun getDeviceAddress(): String
    suspend fun getDeviceName(): String
    suspend fun isBluetoothEnabled(): Boolean

    // Actions
    suspend fun lockScreen()
    suspend fun unlockScreen()
    suspend fun disconnect()
}