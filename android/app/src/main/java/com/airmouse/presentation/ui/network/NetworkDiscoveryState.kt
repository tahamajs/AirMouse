package com.airmouse.presentation.ui.network

import java.util.UUID

data class DiscoveredServer(
    val id: String = UUID.randomUUID().toString(),
    val ip: String,
    val port: Int,
    val name: String = "Air Mouse Server",
    val version: String = "3.0.0",
    val ping: Int = 0,
    val signalStrength: Int = 0,
    val isReachable: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis(),
    val isFavorite: Boolean = false,
    val notes: String = "",
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val protocol: Protocol = Protocol.WEBSOCKET
) {
    val formattedPing: String get() = if (ping < 0) "N/A" else "${ping}ms"
    val pingColor: Int get() = when {
        ping < 50 -> 0xFF4CAF50.toInt()
        ping < 100 -> 0xFFFFC107.toInt()
        ping >= 0 -> 0xFFF44336.toInt()
        else -> 0xFF9E9E9E.toInt()
    }
    val signalQuality: SignalQuality get() = when {
        ping < 50 -> SignalQuality.EXCELLENT
        ping < 100 -> SignalQuality.GOOD
        ping < 200 -> SignalQuality.FAIR
        ping >= 0 -> SignalQuality.POOR
        else -> SignalQuality.UNKNOWN
    }
}

enum class DeviceType(val displayName: String, val icon: String) {
    UNKNOWN("Unknown", "🖥️"),
    PC("PC", "💻"),
    LAPTOP("Laptop", "📱"),
    SERVER("Server", "🗄️"),
    RASPBERRY_PI("Raspberry Pi", "🍓"),
    MAC("Mac", "🍎")
}

enum class Protocol(val displayName: String) {
    WEBSOCKET("WebSocket"),
    TCP("TCP"),
    UDP("UDP"),
    AUTO("Auto Detect")
}

enum class SignalQuality(val displayName: String, val color: Long) {
    EXCELLENT("Excellent", 0xFF4CAF50),
    GOOD("Good", 0xFF8BC34A),
    FAIR("Fair", 0xFFFFC107),
    POOR("Poor", 0xFFF44336),
    UNKNOWN("Unknown", 0xFF9E9E9E)
}

enum class SortBy(val displayName: String) {
    IP("IP Address"),
    NAME("Name"),
    PING("Ping"),
    PORT("Port"),
    SIGNAL("Signal Strength"),
    LAST_SEEN("Last Seen")
}

enum class DiscoveryTab {
    DISCOVERED, SAVED, HISTORY
}

data class ConnectionHistory(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String,
    val ip: String,
    val port: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0,
    val success: Boolean = true,
    val errorMessage: String? = null
)

data class NetworkDiscoveryUiState(
    val isScanning: Boolean = false,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val savedServers: List<DiscoveredServer> = emptyList(),
    val connectionHistory: List<ConnectionHistory> = emptyList(),
    val status: String = "Tap Scan to discover Air Mouse servers",
    val scanProgress: Int = 0,
    val lastScanTime: Long? = null,
    val errorMessage: String? = null,
    val sortBy: SortBy = SortBy.IP,
    val filterText: String = "",
    val selectedServerId: String? = null,
    val isConnecting: Boolean = false,
    val connectionProgress: Int = 0,
    val activeTab: DiscoveryTab = DiscoveryTab.DISCOVERED,
    val showAdvancedOptions: Boolean = false,
    val customPort: String = "",
    val manualIp: String = "",
    val scanSubnet: String = "255.255.255.0",
    val scanTimeout: Int = 2000,
    val scanPorts: List<Int> = listOf(8080, 8081, 8082)
)