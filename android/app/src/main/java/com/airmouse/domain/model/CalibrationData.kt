package com.airmouse.domain.model

import org.json.JSONObject

data class SensorCalibrationData(
    val offsetX: Float = 0f,
    val offsetY: Float = 0f,
    val offsetZ: Float = 0f,
    val scaleX: Float = 1f,
    val scaleY: Float = 1f,
    val scaleZ: Float = 1f
)

enum class CalibrationQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

data class CalibrationData(
    val gyroBias: SensorCalibrationData = SensorCalibrationData(),
    val accelOffset: SensorCalibrationData = SensorCalibrationData(),
    val magOffset: SensorCalibrationData = SensorCalibrationData(),
    val isCalibrated: Boolean = false,
    val quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("gyroBiasX", gyroBias.offsetX)
        put("gyroBiasY", gyroBias.offsetY)
        put("gyroBiasZ", gyroBias.offsetZ)
        put("accelOffsetX", accelOffset.offsetX)
        put("accelOffsetY", accelOffset.offsetY)
        put("accelOffsetZ", accelOffset.offsetZ)
        put("magOffsetX", magOffset.offsetX)
        put("magOffsetY", magOffset.offsetY)
        put("magOffsetZ", magOffset.offsetZ)
        put("isCalibrated", isCalibrated)
        put("quality", quality.name)
        put("timestamp", timestamp)
    }

    companion object {
        fun fromJson(json: JSONObject): CalibrationData? {
            return try {
                CalibrationData(
                    gyroBias = SensorCalibrationData(
                        offsetX = json.optDouble("gyroBiasX", 0.0).toFloat(),
                        offsetY = json.optDouble("gyroBiasY", 0.0).toFloat(),
                        offsetZ = json.optDouble("gyroBiasZ", 0.0).toFloat()
                    ),
                    accelOffset = SensorCalibrationData(
                        offsetX = json.optDouble("accelOffsetX", 0.0).toFloat(),
                        offsetY = json.optDouble("accelOffsetY", 0.0).toFloat(),
                        offsetZ = json.optDouble("accelOffsetZ", 0.0).toFloat()
                    ),
                    magOffset = SensorCalibrationData(
                        offsetX = json.optDouble("magOffsetX", 0.0).toFloat(),
                        offsetY = json.optDouble("magOffsetY", 0.0).toFloat(),
                        offsetZ = json.optDouble("magOffsetZ", 0.0).toFloat()
                    ),
                    isCalibrated = json.optBoolean("isCalibrated", false),
                    quality = try {
                        CalibrationQuality.valueOf(json.optString("quality", "UNKNOWN"))
                    } catch (_: Exception) {
                        CalibrationQuality.UNKNOWN
                    },
                    timestamp = json.optLong("timestamp", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}