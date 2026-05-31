package com.airmouse.presentation.ui.network

data class NetworkDiscoveryUiState(
    val isScanning: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val status: String = "No scan yet"
)

data class DiscoveredServer(val ip: String, val port: Int)