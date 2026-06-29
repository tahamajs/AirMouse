
package com.airmouse.presentation.ui.network

import java.util.UUID

data class DiscoveredServer(
    val id: String = UUID.randomUUID().toString(),
    val ip: String,
    val port: Int,
    val name: String,
    val version: String = "3.0",
    val ping: Int = -1,
    val signalStrength: Int = 0,
    val isReachable: Boolean = false,
    val isFavorite: Boolean = false,
    val notes: String = "",
    val deviceType: DeviceType = DeviceType.UNKNOWN,
    val protocol: Protocol = Protocol.TCP,
    val lastSeen: Long = System.currentTimeMillis()
)

val DiscoveredServer.pingColor: Long
    get() = when {
        ping < 0 -> 0xFFF44336
        ping < 30 -> 0xFF4CAF50
        ping < 60 -> 0xFF8BC34A
        ping < 100 -> 0xFFFFC107
        ping < 200 -> 0xFFFF9800
        else -> 0xFFF44336
    }

data class ConnectionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String,
    val serverName: String,
    val ip: String,
    val port: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Disconnected"
)

data class NetworkDiscoveryUiState(
    val isScanning: Boolean = false,
    val scanProgress: Int = 0,
    val isConnecting: Boolean = false,
    val selectedServerId: String? = null,
    val connectionProgress: Int = 0,
    val discoveredServers: List<DiscoveredServer> = emptyList(),
    val savedServers: List<DiscoveredServer> = emptyList(),
    val connectionHistory: List<ConnectionHistoryItem> = emptyList(),
    val activeTab: DiscoveryTab = DiscoveryTab.DISCOVERED,
    val filterText: String = "",
    val sortBy: SortBy = SortBy.NAME,
    val errorMessage: String? = null,
    val status: String = "Ready",
    val lastScanTime: Long? = null,
    val manualIp: String = "",
    val customPort: Int = 8080
)

enum class DeviceType(val icon: String, val displayName: String) {
    DESKTOP("🖥️", "Desktop"),
    LAPTOP("💻", "Laptop"),
    SERVER("🗄️", "Server"),
    MAC("🍎", "Mac"),
    RASPBERRY_PI("🥧", "Raspberry Pi"),
    UNKNOWN("❓", "Unknown");

    companion object {
        fun fromString(value: String): DeviceType {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

enum class Protocol(val displayName: String) {
    TCP("TCP"),
    WEBSOCKET("WebSocket"),
    UDP("UDP");

    companion object {
        fun fromString(value: String): Protocol {
            return entries.find { it.name.equals(value, ignoreCase = true) } ?: TCP
        }
    }
}

enum class SignalQuality(val displayName: String) {
    EXCELLENT("Excellent"),
    GOOD("Good"),
    FAIR("Fair"),
    POOR("Poor"),
    VERY_POOR("Very Poor"),
    UNKNOWN("Unknown")
}
enum class SortBy(val displayName: String) {
    IP("IP Address"),
    NAME("Name"),
    PING("Ping"),
    PORT("Port"),
    SIGNAL("Signal Strength"),
    LAST_SEEN("Last Seen");

    companion object {
        fun fromString(value: String): SortBy {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: IP
        }
    }
}

enum class DiscoveryTab {
    DISCOVERED,
    SAVED,
    HISTORY
}
