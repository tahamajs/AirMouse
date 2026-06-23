
package com.airmouse.domain.repository

import com.airmouse.domain.model.ProximityCalibrationStatus
import com.airmouse.domain.model.ProximityConfig
import com.airmouse.domain.model.ProximityState
import kotlinx.coroutines.flow.Flow

interface IProximityRepository {

    

    fun observeProximityState(): Flow<ProximityState>
    suspend fun getProximityState(): ProximityState

    

    suspend fun startMonitoring()
    suspend fun stopMonitoring()
    suspend fun isMonitoring(): Boolean

    

    suspend fun getConfig(): ProximityConfig
    suspend fun updateConfig(config: ProximityConfig)

    

    suspend fun calibrate(): Boolean
    suspend fun getCalibrationStatus(): ProximityCalibrationStatus
    suspend fun resetCalibration()
    suspend fun getCalibrationProgress(): Int
    suspend fun isCalibrating(): Boolean

    

    suspend fun setDeviceAddress(address: String)
    suspend fun getDeviceAddress(): String
    suspend fun getDeviceName(): String
    suspend fun isBluetoothEnabled(): Boolean

    

    suspend fun lockScreen()
    suspend fun unlockScreen()
    suspend fun disconnect()
}