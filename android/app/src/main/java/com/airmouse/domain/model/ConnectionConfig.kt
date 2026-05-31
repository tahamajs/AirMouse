// app/src/main/java/com/airmouse/domain/model/ConnectionConfig.kt
package com.airmouse.domain.model

/**
 * Protocol used for communication with the server.
 */
enum class ConnectionProtocol {
    TCP,
    WEBSOCKET,
    UDP,
    BLUETOOTH,
    USB
}

/**
 * Configuration for connecting to the desktop server.
 */
data class ConnectionConfig(
    val serverIp: String,
    val serverPort: Int,
    val protocol: ConnectionProtocol = ConnectionProtocol.TCP,
    val useEncryption: Boolean = false,
    val authenticationToken: String? = null
)

/**
 * Status of the connection.
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR
}