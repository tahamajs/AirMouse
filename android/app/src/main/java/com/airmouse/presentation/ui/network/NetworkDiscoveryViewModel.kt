package com.airmouse.presentation.ui.network

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class NetworkDiscoveryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(NetworkDiscoveryUiState())
    val uiState: StateFlow<NetworkDiscoveryUiState> = _uiState.asStateFlow()

    fun scanNetwork() {
        _uiState.update { it.copy(isScanning = true, status = "Scanning...") }
        viewModelScope.launch {
            delay(2000) // Simulate scanning
            _uiState.update {
                it.copy(
                    isScanning = false,
                    discoveredServers = listOf(DiscoveredServer("192.168.1.100", 8080)),
                    status = "Found 1 server(s)"
                )
            }
        }
    }

    fun selectServer(server: DiscoveredServer) {
        // Send to home screen via callback or saved config
    }
}