// app/src/main/java/com/airmouse/presentation/ui/network/NetworkModels.kt
package com.airmouse.presentation.ui.network

import java.util.UUID

// ==================== CORE DATA CLASSES ====================

/**
 * Represents a discovered Air Mouse server on the network
 */
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
        ping < 200 -> 0xFFFF9800.toInt()
        else -> 0xFFF44336.toInt()
    }

    val signalQuality: SignalQuality get() = when {
        ping < 50 -> SignalQuality.EXCELLENT
        ping < 100 -> SignalQuality.GOOD
        ping < 200 -> SignalQuality.FAIR
        else -> SignalQuality.POOR
    }
}

/**
 * Connection history record
 */
data class ConnectionHistoryItem(
    val id: String = UUID.randomUUID().toString(),
    val serverId: String,
    val serverName: String,
    val ip: String,
    val port: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val status: String = "Disconnected"
)

/**
 * UI State for Network Discovery screen
 */
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

// ==================== ENUMS ====================

/**
 * Device types for server identification
 */
enum class DeviceType(val displayName: String, val icon: String) {
    UNKNOWN("Unknown", "🖥️"),
    PC("PC", "💻"),
    LAPTOP("Laptop", "📱"),
    SERVER("Server", "🗄️"),
    RASPBERRY_PI("Raspberry Pi", "🍓"),
    MAC("Mac", "🍎");

    companion object {
        fun fromString(value: String): DeviceType {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: UNKNOWN
        }
    }
}

/**
 * Communication protocols
 */
enum class Protocol(val displayName: String) {
    WEBSOCKET("WebSocket"),
    TCP("TCP"),
    UDP("UDP"),
    AUTO("Auto Detect");

    companion object {
        fun fromString(value: String): Protocol {
            return entries.find { it.displayName.equals(value, ignoreCase = true) } ?: WEBSOCKET
        }
    }
}

/**
 * Signal quality levels
 */
enum class SignalQuality(val displayName: String, val color: Long) {
    EXCELLENT("Excellent", 0xFF4CAF50),
    GOOD("Good", 0xFF8BC34A),
    FAIR("Fair", 0xFFFFC107),
    POOR("Poor", 0xFFF44336),
    UNKNOWN("Unknown", 0xFF9E9E9E);

    fun getColor(): Int = color.toInt()
}

/**
 * Sort options for server list
 */
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

/**
 * Discovery tabs
 */
enum class DiscoveryTab {
    DISCOVERED,
    SAVED,
    HISTORY
}