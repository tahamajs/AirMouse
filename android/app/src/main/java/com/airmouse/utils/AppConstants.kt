// app/src/main/java/com/airmouse/utils/AppConstants.kt
package com.airmouse.utils

import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object AppConstants {
    // Server
    const val DEFAULT_SERVER_PORT = 8080
    const val DEFAULT_WEBSOCKET_PORT = 8081
    const val DEFAULT_UDP_PORT = 8082

    // Connection
    const val HEARTBEAT_INTERVAL_MS = 30000L
    const val RECONNECT_DELAY_MS = 3000L
    const val MAX_RECONNECT_ATTEMPTS = 10

    // Gesture
    const val DEFAULT_GESTURE_CONFIDENCE = 0.7f
    const val DEFAULT_GESTURE_COOLDOWN_MS = 500L

    // Sensor
    const val GYRO_SAMPLES_NEEDED = 500
    const val MAG_SAMPLES_NEEDED = 300
    const val ACCEL_SAMPLES_PER_POSE = 50

    // Proximity
    const val DEFAULT_NEAR_THRESHOLD = 1.5f
    const val DEFAULT_FAR_THRESHOLD = 3.0f

    // Statistics
    const val MAX_HISTORY_SIZE = 1000

    // Logging
    const val MAX_LOG_ENTRIES = 500

    // Database
    const val DATABASE_NAME = "airmouse_database"
    const val DATABASE_VERSION = 3

    // Shared Preferences
    const val PREF_NAME = "airmouse_prefs"

    // Intent Actions
    const val ACTION_START_SERVICE = "START_SERVICE"
    const val ACTION_STOP_SERVICE = "STOP_SERVICE"
    const val ACTION_UPDATE = "UPDATE_ACTION"

    // Broadcast Actions
    const val BROADCAST_CONNECTION_CHANGED = "com.airmouse.CONNECTION_CHANGED"
    const val BROADCAST_GESTURE_DETECTED = "com.airmouse.GESTURE_DETECTED"
    const val BROADCAST_PROXIMITY_CHANGED = "com.airmouse.PROXIMITY_CHANGED"

    // Notification IDs
    const val NOTIFICATION_CONNECTION = 1001
    const val NOTIFICATION_GESTURE = 1002
    const val NOTIFICATION_PROXIMITY = 1003
    const val NOTIFICATION_FOREGROUND = 1004
    const val NOTIFICATION_UPDATE = 1005
}

// 2.2 String Extensions
fun String.isValidIpAddress(): Boolean {
    val pattern = "^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$"
    return Regex(pattern).matches(this)
}

fun String.toValidFileName(): String {
    return this.replace(Regex("[^a-zA-Z0-9.-]"), "_")
}

// 2.3 Number Extensions
fun Float.format(digits: Int = 2): String {
    return String.format("%.${digits}f", this)
}

// 2.4 Date Extensions
fun Long.toDateString(): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toDateShort(): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.toTimeString(): String {
    val format = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
    return format.format(Date(this))
}

fun Long.formatDuration(): String {
    val seconds = this / 1000
    val hours = seconds / 3600
    val minutes = (seconds % 3600) / 60
    val secs = seconds % 60
    return when {
        hours > 0 -> String.format("%02d:%02d:%02d", hours, minutes, secs)
        minutes > 0 -> String.format("%02d:%02d", minutes, secs)
        else -> String.format("%02ds", secs)
    }
}

// 2.5 Collection Extensions
fun <T> List<T>.safeGet(index: Int): T? {
    return if (index in indices) this[index] else null
}

fun <T> List<T>.chunkedBySize(size: Int): List<List<T>> {
    return this.chunked(size)
}

fun <T> MutableList<T>.addUnique(element: T): Boolean {
    return if (!contains(element)) {
        add(element)
        true
    } else false
}
