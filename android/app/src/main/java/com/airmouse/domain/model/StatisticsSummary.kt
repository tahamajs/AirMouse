package com.airmouse.domain.model

import org.json.JSONObject

data class StatisticsSummary(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovements: Int = 0,
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val sessionDuration: Long = 0L,
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("totalClicks", totalClicks)
        put("totalDoubleClicks", totalDoubleClicks)
        put("totalRightClicks", totalRightClicks)
        put("totalScrolls", totalScrolls)
        put("totalMovements", totalMovements)
        put("totalDistance", totalDistance)
        put("averageSpeed", averageSpeed)
        put("maxSpeed", maxSpeed)
        put("sessionDuration", sessionDuration)
        put("lastUpdated", lastUpdated)
    }

    companion object {
        fun fromJson(json: JSONObject): StatisticsSummary? {
            return try {
                StatisticsSummary(
                    totalClicks = json.optInt("totalClicks", 0),
                    totalDoubleClicks = json.optInt("totalDoubleClicks", 0),
                    totalRightClicks = json.optInt("totalRightClicks", 0),
                    totalScrolls = json.optInt("totalScrolls", 0),
                    totalMovements = json.optInt("totalMovements", 0),
                    totalDistance = json.optDouble("totalDistance", 0.0).toFloat(),
                    averageSpeed = json.optDouble("averageSpeed", 0.0).toFloat(),
                    maxSpeed = json.optDouble("maxSpeed", 0.0).toFloat(),
                    sessionDuration = json.optLong("sessionDuration", 0L),
                    lastUpdated = json.optLong("lastUpdated", System.currentTimeMillis())
                )
            } catch (_: Exception) {
                null
            }
        }
    }
}