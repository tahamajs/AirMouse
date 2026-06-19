// app/src/main/java/com/airmouse/presentation/ui/proximity/ProximityViewModel.kt
package com.airmouse.presentation.ui.proximity

import com.airmouse.domain.model.ProximityConfig
import com.airmouse.features.ProximityFeature
import com.airmouse.presentation.base.BaseViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProximityViewModel @Inject constructor(
    private val proximityFeature: ProximityFeature
) : BaseViewModel<ProximityUiState, ProximityEvent>(ProximityUiState()) {

    init {
        observeProximityState()
        loadConfig()
    }

    private fun observeProximityState() {
        viewModelScope.launch {
            proximityFeature.observeProximityState().collect { state ->
                setState {
                    copy(
                        isNear = state.isNear,
                        distance = state.distance,
                        rssi = state.rssi,
                        deviceName = state.deviceName,
                        deviceAddress = state.deviceAddress,
                        lastUpdated = state.lastUpdated
                    )
                }
            }
        }

        viewModelScope.launch {
            proximityFeature.state.collect { featureState ->
                setState {
                    copy(
                        isEnabled = featureState.isEnabled,
                        isMonitoring = featureState.isMonitoring,
                        isLocked = featureState.isLocked,
                        nearThreshold = featureState.nearThreshold,
                        farThreshold = featureState.farThreshold
                    )
                }
            }
        }
    }

    private fun loadConfig() {
        viewModelScope.launch {
            val config = proximityFeature.getConfig()
            setState {
                copy(
                    isEnabled = config.enabled,
                    nearThreshold = config.nearThreshold,
                    farThreshold = config.farThreshold,
                    deviceAddress = config.deviceAddress,
                    vibrationEnabled = config.vibrationEnabled,
                    autoLockEnabled = config.autoLockEnabled,
                    autoUnlockEnabled = config.autoUnlockEnabled
                )
            }
        }
    }

    override fun onEvent(event: ProximityEvent) {
        when (event) {
            is ProximityEvent.ToggleProximity -> toggleProximity(event.enabled)
            is ProximityEvent.UpdateThresholds -> updateThresholds(event.near, event.far)
            is ProximityEvent.SetDeviceAddress -> setDeviceAddress(event.address)
            is ProximityEvent.StartMonitoring -> startMonitoring()
            is ProximityEvent.StopMonitoring -> stopMonitoring()
            is ProximityEvent.Calibrate -> calibrate()
            is ProximityEvent.LockNow -> lockNow()
            is ProximityEvent.UnlockNow -> unlockNow()
            is ProximityEvent.ToggleVibration -> toggleVibration()
            is ProximityEvent.ToggleAutoLock -> toggleAutoLock()
            is ProximityEvent.ToggleAutoUnlock -> toggleAutoUnlock()
        }
    }

    private fun toggleProximity(enabled: Boolean) {
        setState { copy(isEnabled = enabled) }
        viewModelScope.launch {
            val result = proximityFeature.toggleProximity(enabled)
            if (result.isSuccess) {
                sendEvent(ProximityEvent.ShowToast(if (enabled) "Proximity enabled" else "Proximity disabled"))
            } else {
                sendEvent(ProximityEvent.ShowError("Failed to toggle proximity"))
            }
        }
    }

    private fun updateThresholds(near: Float, far: Float) {
        setState { copy(nearThreshold = near, farThreshold = far) }
        viewModelScope.launch {
            val result = proximityFeature.updateThresholds(near, far)
            if (result.isSuccess) {
                sendEvent(ProximityEvent.ShowToast("Thresholds updated"))
            } else {
                sendEvent(ProximityEvent.ShowError("Failed to update thresholds"))
            }
        }
    }

    private fun setDeviceAddress(address: String) {
        setState { copy(deviceAddress = address) }
        viewModelScope.launch {
            val result = proximityFeature.setDeviceAddress(address)
            if (result.isSuccess) {
                sendEvent(ProximityEvent.ShowToast("Device set"))
            }
        }
    }

    private fun startMonitoring() {
        viewModelScope.launch {
            val result = proximityFeature.startMonitoring()
            if (result.isSuccess) {
                setState { copy(isMonitoring = true) }
                sendEvent(ProximityEvent.ShowToast("Monitoring started"))
            }
        }
    }

    private fun stopMonitoring() {
        viewModelScope.launch {
            val result = proximityFeature.stopMonitoring()
            if (result.isSuccess) {
                setState { copy(isMonitoring = false) }
                sendEvent(ProximityEvent.ShowToast("Monitoring stopped"))
            }
        }
    }

    private fun calibrate() {
        viewModelScope.launch {
            sendEvent(ProximityEvent.ShowToast("Calibrating..."))
            val result = proximityFeature.calibrate()
            if (result.isSuccess) {
                sendEvent(ProximityEvent.ShowToast("Calibration complete"))
            } else {
                sendEvent(ProximityEvent.ShowError("Calibration failed"))
            }
        }
    }

    private fun lockNow() {
        viewModelScope.launch {
            proximityFeature.lockScreen()
            sendEvent(ProximityEvent.ShowToast("Screen locked"))
        }
    }

    private fun unlockNow() {
        viewModelScope.launch {
            proximityFeature.unlockScreen()
            sendEvent(ProximityEvent.ShowToast("Screen unlocked"))
        }
    }

    private fun toggleVibration() {
        val enabled = !state.value.vibrationEnabled
        setState { copy(vibrationEnabled = enabled) }
        viewModelScope.launch {
            // Save vibration preference
        }
    }

    private fun toggleAutoLock() {
        val enabled = !state.value.autoLockEnabled
        setState { copy(autoLockEnabled = enabled) }
        // Save auto lock preference
    }

    private fun toggleAutoUnlock() {
        val enabled = !state.value.autoUnlockEnabled
        setState { copy(autoUnlockEnabled = enabled) }
        // Save auto unlock preference
    }
}

data class ProximityUiState(
    val isNear: Boolean = false,
    val distance: Float = 0f,
    val rssi: Int = 0,
    val deviceName: String = "",
    val deviceAddress: String = "",
    val isEnabled: Boolean = false,
    val isMonitoring: Boolean = false,
    val isLocked: Boolean = false,
    val nearThreshold: Float = 1.5f,
    val farThreshold: Float = 3.0f,
    val vibrationEnabled: Boolean = true,
    val autoLockEnabled: Boolean = true,
    val autoUnlockEnabled: Boolean = true,
    val lastUpdated: Long = 0
)

sealed class ProximityEvent {
    data class ToggleProximity(val enabled: Boolean) : ProximityEvent()
    data class UpdateThresholds(val near: Float, val far: Float) : ProximityEvent()
    data class SetDeviceAddress(val address: String) : ProximityEvent()
    object StartMonitoring : ProximityEvent()
    object StopMonitoring : ProximityEvent()
    object Calibrate : ProximityEvent()
    object LockNow : ProximityEvent()
    object UnlockNow : ProximityEvent()
    object ToggleVibration : ProximityEvent()
    object ToggleAutoLock : ProximityEvent()
    object ToggleAutoUnlock : ProximityEvent()
    data class ShowToast(val message: String) : ProximityEvent()
    data class ShowError(val message: String) : ProximityEvent()
}