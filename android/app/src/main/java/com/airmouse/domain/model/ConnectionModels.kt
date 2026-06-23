
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
    TIMEOUT
}

enum class ConnectionProtocol {
    TCP,
    WEBSOCKET,
    UDP
}

data class ConnectionConfig(
    val ip: String = "",
    val port: Int = 8080,
    val protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET,
    val useSSL: Boolean = false,
    val authToken: String? = null,
    val autoReconnect: Boolean = true,
    val timeoutMs: Long = 10000L
) {
    fun normalized(): ConnectionConfig {
        val effectivePort = when {
            protocol == ConnectionProtocol.WEBSOCKET && (port <= 0 || port == DEFAULT_TCP_PORT || port == DEFAULT_UDP_PORT) ->
                DEFAULT_WEBSOCKET_PORT
            protocol == ConnectionProtocol.TCP && (port <= 0 || port == DEFAULT_WEBSOCKET_PORT || port == DEFAULT_UDP_PORT) ->
                DEFAULT_TCP_PORT
            protocol == ConnectionProtocol.UDP && (port <= 0 || port == DEFAULT_TCP_PORT || port == DEFAULT_WEBSOCKET_PORT) ->
                DEFAULT_UDP_PORT
            port > 0 ->
                port
            else ->
                DEFAULT_TCP_PORT
        }
        return copy(port = effectivePort)
    }

    companion object {
        const val DEFAULT_TCP_PORT = 8080
        const val DEFAULT_WEBSOCKET_PORT = 8081
        const val DEFAULT_UDP_PORT = 8082
    }
}

@Parcelize
data class ConnectionQuality(
    val rssi: Int = 0,
    val ping: Int = 0,
    val jitter: Int = 0,
    val packetLoss: Float = 0f,
    val dataRate: Int = 0,
    val signalStrength: SignalStrength = SignalStrength.UNKNOWN
) : Parcelable {
    enum class SignalStrength {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        VERY_POOR,
        UNKNOWN
    }

    fun score(): Int = when (signalStrength) {
        SignalStrength.EXCELLENT -> 100
        SignalStrength.GOOD -> 75
        SignalStrength.FAIR -> 50
        SignalStrength.POOR -> 25
        SignalStrength.VERY_POOR -> 10
        SignalStrength.UNKNOWN -> 0
    }

    fun isHealthy(): Boolean = ping < 200 && packetLoss < 0.1f
}

data class TestResult(
    val success: Boolean,
    val message: String,
    val latency: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

data class DiscoveredServer(
    val ip: String,
    val port: Int,
    val name: String,
    val version: String = "3.0",
    val rssi: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)
