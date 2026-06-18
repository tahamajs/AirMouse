package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Protocol used for communication with the server.
 */
enum class ConnectionProtocol(val displayName: String) {
    TCP("TCP"),
    WEBSOCKET("WebSocket"),
    UDP("UDP"),
    BLUETOOTH("Bluetooth"),
    USB("USB")
}

/**
 * Configuration for connecting to the desktop server.
 */
@Parcelize
data class ConnectionConfig(
    val serverIp: String,
    val serverPort: Int,
    val protocol: ConnectionProtocol = ConnectionProtocol.TCP,
    val useEncryption: Boolean = false,
    val authenticationToken: String? = null,
    val timeoutMs: Int = 5000,
    val autoReconnect: Boolean = true,
    val keepAlive: Boolean = true,
    val lastConnected: Long = System.currentTimeMillis()
) : Parcelable

/**
 * Status of the connection.
 */
enum class ConnectionStatus(val displayName: String) {
    DISCONNECTED("Disconnected"),
    CONNECTING("Connecting..."),
    CONNECTED("Connected"),
    RECONNECTING("Reconnecting..."),
    ERROR("Error")
}

/**
 * Connection statistics.
 */
@Parcelize
data class ConnectionStatistics(
    val totalConnections: Int = 0,
    val successfulConnections: Int = 0,
    val failedConnections: Int = 0,
    val averageLatency: Int = 0,
    val lastConnectionTime: Long = 0,
    val totalDataSent: Long = 0,
    val totalDataReceived: Long = 0,
    val connectionUptime: Long = 0
) : Parcelable

/**
 * Server discovery result.
 */
@Parcelize
data class DiscoveredServer(
    val ip: String,
    val port: Int,
    val name: String = "Air Mouse Server",
    val version: String = "3.0.0",
    val ping: Int = 0,
    val isReachable: Boolean = true,
    val lastSeen: Long = System.currentTimeMillis()
) : Parcelable