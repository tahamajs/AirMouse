// app/src/main/java/com/airmouse/features/ProximityFeature.kt
package com.airmouse.features

import com.airmouse.domain.model.ProximityConfig
import com.airmouse.domain.model.ProximityState
import com.airmouse.domain.usecase.GetProximityStateUseCase
import com.airmouse.domain.usecase.UpdateProximityConfigUseCase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class ProximityFeature @Inject constructor(
    private val getProximityStateUseCase: GetProximityStateUseCase,
    private val updateProximityConfigUseCase: UpdateProximityConfigUseCase
) {

    data class ProximityFeatureState(
        val isNear: Boolean = false,
        val distance: Float = 0f,
        val rssi: Int = 0,
        val deviceName: String = "",
        val deviceAddress: String = "",
        val isEnabled: Boolean = false,
        val isMonitoring: Boolean = false,
        val nearThreshold: Float = 1.5f,
        val farThreshold: Float = 3.0f,
        val isLocked: Boolean = false,
        val lastLockTime: Long = 0,
        val lastUnlockTime: Long = 0
    )

    private val _state = MutableStateFlow(ProximityFeatureState())
    val state: StateFlow<ProximityFeatureState> = _state.asStateFlow()

    private val _lockHistory = MutableStateFlow<List<Pair<Boolean, Long>>>(emptyList())
    val lockHistory: StateFlow<List<Pair<Boolean, Long>>> = _lockHistory.asStateFlow()

    init {
        startObservingProximityState()
    }

    private fun startObservingProximityState() {
        // In production, observe proximity state
    }

    suspend fun getProximityState(): ProximityState {
        return getProximityStateUseCase()
    }

    fun observeProximityState(): Flow<ProximityState> {
        return getProximityStateUseCase.observeProximityState()
    }

    suspend fun isDeviceNear(): Boolean {
        return getProximityStateUseCase.isDeviceNear()
    }

    suspend fun getCurrentDistance(): Float {
        return getProximityStateUseCase.getCurrentDistance()
    }

    suspend fun isProximityEnabled(): Boolean {
        return getProximityStateUseCase.isProximityEnabled()
    }

    suspend fun startMonitoring(): Result<Unit> {
        val result = getProximityStateUseCase.startMonitoring()
        if (result.isSuccess) {
            _state.value = _state.value.copy(isMonitoring = true)
        }
        return result
    }

    suspend fun stopMonitoring(): Result<Unit> {
        val result = getProximityStateUseCase.stopMonitoring()
        if (result.isSuccess) {
            _state.value = _state.value.copy(isMonitoring = false)
        }
        return result
    }

    suspend fun updateConfig(config: ProximityConfig): Result<Unit> {
        val result = updateProximityConfigUseCase(config)
        if (result.isSuccess) {
            _state.value = _state.value.copy(
                isEnabled = config.enabled,
                nearThreshold = config.nearThreshold,
                farThreshold = config.farThreshold
            )
        }
        return result
    }

    suspend fun setDeviceAddress(address: String): Result<Unit> {
        return updateProximityConfigUseCase.setDeviceAddress(address)
    }

    suspend fun updateThresholds(near: Float, far: Float): Result<Unit> {
        val result = updateProximityConfigUseCase.updateThresholds(near, far)
        if (result.isSuccess) {
            _state.value = _state.value.copy(
                nearThreshold = near,
                farThreshold = far
            )
        }
        return result
    }

    suspend fun toggleProximity(enabled: Boolean): Result<Unit> {
        val result = updateProximityConfigUseCase.toggleProximity(enabled)
        if (result.isSuccess) {
            _state.value = _state.value.copy(isEnabled = enabled)
        }
        return result
    }

    suspend fun calibrate(): Result<Boolean> {
        return updateProximityConfigUseCase.calibrate()
    }

    suspend fun lockScreen() {
        _state.value = _state.value.copy(isLocked = true, lastLockTime = System.currentTimeMillis())
        addToLockHistory(true)
    }

    suspend fun unlockScreen() {
        _state.value = _state.value.copy(isLocked = false, lastUnlockTime = System.currentTimeMillis())
        addToLockHistory(false)
    }

    suspend fun getDeviceName(): String {
        return _state.value.deviceName
    }

    suspend fun getDeviceAddress(): String {
        return _state.value.deviceAddress
    }

    private fun addToLockHistory(isLock: Boolean) {
        val history = _lockHistory.value.toMutableList()
        history.add(Pair(isLock, System.currentTimeMillis()))
        if (history.size > 50) {
            history.removeAt(0)
        }
        _lockHistory.value = history
    }

    suspend fun clearLockHistory() {
        _lockHistory.value = emptyList()
    }

    fun getProximityFeatureState(): ProximityFeatureState = _state.value

    suspend fun isMonitoring(): Boolean = _state.value.isMonitoring
}
