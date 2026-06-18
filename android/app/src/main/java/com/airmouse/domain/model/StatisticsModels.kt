package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

/**
 * Session statistics for the app.
 */
@Parcelize
data class SessionStatistics(
    val sessionId: String,
    val startTime: Long,
    val endTime: Long = 0,
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val gestures: List<String> = emptyList(),
    val totalDistance: Float = 0f,
    val averageSpeed: Float = 0f,
    val connectionAttempts: Int = 0,
    val connectionSuccesses: Int = 0,
    val batteryDrain: Int = 0,
    val cpuUsage: Float = 0f,
    val memoryUsage: Float = 0f
) : Parcelable

/**
 * Daily usage statistics.
 */
@Parcelize
data class DailyStatistics(
    val date: String, // YYYY-MM-DD
    val totalClicks: Int = 0,
    val totalGestures: Int = 0,
    val totalDistance: Float = 0f,
    val activeTime: Long = 0,
    val connectionCount: Int = 0
) : Parcelable