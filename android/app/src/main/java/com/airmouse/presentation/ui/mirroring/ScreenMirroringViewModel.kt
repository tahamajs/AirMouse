package com.airmouse.presentation.ui.mirroring

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.network.ConnectionQuality
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class ScreenMirroringViewModel @Inject constructor(
    private val connectionManager: ConnectionManager
) : ViewModel() {
    val connectionState: StateFlow<ConnectionManager.ConnectionStatus> =
        connectionManager.connectionStatus.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ConnectionManager.ConnectionStatus.DISCONNECTED
        )

    val quality: StateFlow<ConnectionQuality> =
        connectionManager.connectionQuality.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            ConnectionQuality()
        )

    fun buildServerUrl(): String {
        val ip = connectionManager.currentIp.value
        val port = connectionManager.currentPort.value
        return if (ip.isBlank()) "" else "ws://$ip:$port/ws"
    }
}

