package com.airmouse.domain.model

data class ConnectionQuality(
    val ping: Int = 0,
    val rssi: Int = 0,
    val jitter: Int = 0,
    val packetLoss: Float = 0f,
    val signalStrength: SignalStrength = SignalStrength.UNKNOWN
) {
    enum class SignalStrength {
        EXCELLENT,
        GOOD,
        FAIR,
        POOR,
        VERY_POOR,
        UNKNOWN
    }

    fun level(): Int = when (signalStrength) {
        SignalStrength.EXCELLENT -> 100
        SignalStrength.GOOD -> 75
        SignalStrength.FAIR -> 50
        SignalStrength.POOR -> 25
        SignalStrength.VERY_POOR -> 10
        SignalStrength.UNKNOWN -> 0
    }

    fun description(): String = when (signalStrength) {
        SignalStrength.EXCELLENT -> "Excellent Connection"
        SignalStrength.GOOD -> "Good Connection"
        SignalStrength.FAIR -> "Fair Connection"
        SignalStrength.POOR -> "Poor Connection"
        SignalStrength.VERY_POOR -> "Very Poor Connection"
        SignalStrength.UNKNOWN -> "Unknown"
    }

    fun isHealthy(): Boolean {
        return signalStrength == SignalStrength.EXCELLENT || signalStrength == SignalStrength.GOOD
    }

    companion object {
        val UNKNOWN = ConnectionQuality(signalStrength = SignalStrength.UNKNOWN)
        val EXCELLENT = ConnectionQuality(signalStrength = SignalStrength.EXCELLENT)
        val GOOD = ConnectionQuality(signalStrength = SignalStrength.GOOD)
        val FAIR = ConnectionQuality(signalStrength = SignalStrength.FAIR)
        val POOR = ConnectionQuality(signalStrength = SignalStrength.POOR)
        val VERY_POOR = ConnectionQuality(signalStrength = SignalStrength.VERY_POOR)
    }
}
