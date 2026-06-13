package com.airmouse.presentation.ui.sensor

data class SensorDataPoint(
    val timestamp: Long,
    val roll: Float,
    val pitch: Float,
    val yaw: Float
)

data class SensorHistory(
    val dataPoints: List<SensorDataPoint> = emptyList(),
    val maxHistorySize: Int = 100
)