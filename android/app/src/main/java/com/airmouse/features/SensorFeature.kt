
package com.airmouse.features

import com.airmouse.domain.model.OrientationData
import com.airmouse.domain.model.SensorCalibrationStatus
import com.airmouse.domain.model.SensorData
import com.airmouse.domain.repository.ISensorRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class SensorFeature @Inject constructor(
    private val sensorRepository: ISensorRepository
) {

    data class SensorFeatureState(
        val sensorData: SensorData = SensorData(),
        val orientation: OrientationData = OrientationData(0f, 0f, 0f),
        val isActive: Boolean = false,
        val isCalibrated: Boolean = false,
        val calibrationStatus: SensorCalibrationStatus = SensorCalibrationStatus.NOT_CALIBRATED,
        val sampleRate: Int = 0,
        val isPowerSave: Boolean = false
    )

    private val _state = MutableStateFlow(SensorFeatureState())
    val state: StateFlow<SensorFeatureState> = _state.asStateFlow()

    init {
        startObservingSensorData()
        startObservingOrientation()
        startObservingCalibrationStatus()
    }

    private fun startObservingSensorData() {
        
    }

    private fun startObservingOrientation() {
        
    }

    private fun startObservingCalibrationStatus() {
        
    }

    suspend fun getCurrentSensorData(): SensorData {
        return sensorRepository.getCurrentSensorData()
    }

    fun observeSensorData(): Flow<SensorData> {
        return sensorRepository.observeSensorData()
    }

    fun observeOrientation(): Flow<OrientationData> {
        return sensorRepository.observeOrientation()
    }

    suspend fun getCalibrationStatus(): SensorCalibrationStatus {
        return sensorRepository.getCalibrationStatus()
    }

    fun observeCalibrationStatus(): Flow<SensorCalibrationStatus> {
        return sensorRepository.observeCalibrationStatus()
    }

    suspend fun startSensors() {
        sensorRepository.startSensors()
        _state.value = _state.value.copy(isActive = true)
    }

    suspend fun stopSensors() {
        sensorRepository.stopSensors()
        _state.value = _state.value.copy(isActive = false)
    }

    suspend fun isSensorActive(): Boolean {
        return sensorRepository.isSensorActive()
    }

    suspend fun getSensorInfo() {
        
    }

    suspend fun calibrateSensors(): Boolean {
        val result = sensorRepository.calibrateSensors()
        if (result) {
            _state.value = _state.value.copy(isCalibrated = true)
        }
        return result
    }

    suspend fun resetCalibration() {
        sensorRepository.resetCalibration()
        _state.value = _state.value.copy(isCalibrated = false)
    }

    suspend fun isCalibrated(): Boolean {
        return sensorRepository.isCalibrated()
    }

    suspend fun setPowerSaveMode(enabled: Boolean) {
        sensorRepository.setPowerSaveMode(enabled)
        _state.value = _state.value.copy(isPowerSave = enabled)
    }

    suspend fun getRecommendedDelay(): Int {
        return sensorRepository.getRecommendedDelay()
    }

    suspend fun updateState() {
        _state.value = _state.value.copy(
            sensorData = sensorRepository.getCurrentSensorData(),
            isCalibrated = sensorRepository.isCalibrated(),
            calibrationStatus = sensorRepository.getCalibrationStatus()
        )
    }

    fun getSensorFeatureState(): SensorFeatureState = _state.value
}
