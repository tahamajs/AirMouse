// app/src/main/java/com/airmouse/domain/model/StatisticsModels.kt
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Statistics summary
 */
@Parcelize
data class StatisticsSummary(
    val totalClicks: Int = 0,
    val totalDoubleClicks: Int = 0,
    val totalRightClicks: Int = 0,
    val totalScrolls: Int = 0,
    val totalMovements: Int = 0,
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val maxSpeed: Float = 0f,
    val sessionDuration: Long = 0
) : Parcelable

/**
 * Daily statistics
 */
@Parcelize
data class DailyStats(
    val date: String = "",
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val movements: Int = 0,
    val distance: Float = 0f
) : Parcelable

/**
 * Gesture statistics
 */
@Parcelize
data class GestureStatistics(
    val gestureName: String = "",
    val detectionCount: Int = 0,
    val confidencePercentage: Float = 0f,
    val lastDetected: Long = 0
) : Parcelable

/**
 * Historical statistics
 */
data class HistoricalStatistics(
    val totalGestures: Int = 0,
    val gesturesByType: Map<String, Int> = emptyMap(),
    val mostUsedGesture: String = "",
    val lastGestureTime: Long = 0,
    val customGestureUsage: Map<String, Int> = emptyMap()
)