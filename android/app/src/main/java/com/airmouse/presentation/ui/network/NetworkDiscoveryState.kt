// app/src/main/java/com/airmouse/presentation/ui/network/DiscoveredServer.kt
package com.airmouse.presentation.ui.network

import java.util.UUID

data class DiscoveredServer(
    val id: String = UUID.randomUUID().toString(),
    val ip: String,
    val port: Int,
    val name: String = "Air Mouse Server",
    val version: String = "3.0.0",
    val ping: Int = 0,
    val isReachable: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val notes: String = ""
)

data class NetworkDiscoveryUiState(
    val isScanning: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val savedServers: List<DiscoveredServer> = emptyList(),
    val status: String = "Tap Scan to discover Air Mouse servers",
    val scanProgress: Int = 0,
    val lastScanTime: Long? = null,
    val errorMessage: String? = null,
    val sortBy: SortBy = SortBy.IP,
    val filterText: String = "",
    val selectedServerId: String? = null,
    val isConnecting: Boolean = false,
    val activeTab: DiscoveryTab = DiscoveryTab.DISCOVERED
)

enum class SortBy(val displayName: String) {
    IP("IP Address"),
    NAME("Name"),
    PING("Ping"),
    PORT("Port")
}

enum class DiscoveryTab {
    DISCOVERED, SAVED
}