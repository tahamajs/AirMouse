// app/src/main/java/com/airmouse/domain/model/ConnectionModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Connection status enum
 */
enum class ConnectionStatus {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    RECONNECTING,
    ERROR,
    TIMEOUT
}

/**
 * Connection protocol enum
 */
enum class ConnectionProtocol {
    TCP,
    WEBSOCKET,
    UDP
}

/**
 * Connection configuration
 */
data class ConnectionConfig(
    val ip: String = "",
    val port: Int = 8080,
    val protocol: ConnectionProtocol = ConnectionProtocol.WEBSOCKET,
    val useSSL: Boolean = false,
    val authToken: String? = null,
    val autoReconnect: Boolean = true,
    val timeoutMs: Long = 10000L
)

/**
 * Connection quality metrics
 */
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

/**
 * Connection test result
 */
data class TestResult(
    val success: Boolean,
    val message: String,
    val latency: Long = 0,
    val timestamp: Long = System.currentTimeMillis()
)

/**
 * Server discovery result
 */
data class DiscoveredServer(
    val ip: String,
    val port: Int,
    val name: String,
    val version: String = "3.0",
    val rssi: Int = 0,
    val lastSeen: Long = System.currentTimeMillis()
)