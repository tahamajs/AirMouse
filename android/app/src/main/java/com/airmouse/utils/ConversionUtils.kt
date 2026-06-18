// app/src/main/java/com/airmouse/utils/ConversionUtils.kt
package com.airmouse.utils

import java.util.Locale
import kotlin.math.*

/**
 * Comprehensive unit conversion utilities for Air Mouse Pro.
 * Provides conversions for UI, time, data, networking, sensors, and more.
 */
object ConversionUtils {

    // ==================== UI / Display Conversions ====================

    fun pxToDp(px: Int, density: Float): Int = (px / density).toInt()
    fun dpToPx(dp: Int, density: Float): Int = (dp * density).toInt()
    fun spToPx(sp: Int, scaledDensity: Float): Int = (sp * scaledDensity).toInt()
    fun pxToSp(px: Int, scaledDensity: Float): Int = (px / scaledDensity).toInt()

    // ==================== Time Conversions ====================

    fun msToSeconds(ms: Long): Float = ms / 1000f
    fun secondsToMs(seconds: Float): Long = (seconds * 1000).toLong()
    fun msToMinutes(ms: Long): Float = ms / 60000f
    fun minutesToMs(minutes: Float): Long = (minutes * 60000).toLong()

    fun formatTime(ms: Long, includeHours: Boolean = true): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (includeHours && hours > 0) {
            String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format(Locale.US, "%02d:%02d", minutes, seconds)
        }
    }

    fun formatDuration(ms: Long): String {
        val seconds = ms / 1000
        val minutes = seconds / 60
        val hours = minutes / 60
        val days = hours / 24

        return when {
            days > 0 -> "${days}d ${hours % 24}h"
            hours > 0 -> "${hours}h ${minutes % 60}m"
            minutes > 0 -> "${minutes}m ${seconds % 60}s"
            else -> "${seconds}s"
        }
    }

    // ==================== Data Size Conversions ====================

    fun bytesToMB(bytes: Long): Float = bytes / (1024f * 1024f)
    fun bytesToKB(bytes: Long): Float = bytes / 1024f
    fun bytesToGB(bytes: Long): Float = bytes / (1024f * 1024f * 1024f)
    fun mbToBytes(mb: Float): Long = (mb * 1024f * 1024f).toLong()
    fun kbToBytes(kb: Float): Long = (kb * 1024f).toLong()

    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format(Locale.US, "%.1f KB", bytesToKB(bytes))
            bytes < 1024 * 1024 * 1024 -> String.format(Locale.US, "%.1f MB", bytesToMB(bytes))
            else -> String.format(Locale.US, "%.1f GB", bytesToGB(bytes))
        }
    }

    // ==================== Data Rate / Network Conversions ====================

    fun bpsToMbps(bps: Long): Float = bps / 1_000_000f
    fun bpsToKbps(bps: Long): Float = bps / 1000f
    fun mbpsToBps(mbps: Float): Long = (mbps * 1_000_000).toLong()

    fun bpsMbpsFromBytes(bytesPerSecond: Long): Float {
        // Fixed: Mapped computation target to match input argument parameter naming structure
        return (bytesPerSecond * 8) / 1_000_000f
    }

    fun formatSpeed(bps: Long): String {
        return when {
            bps < 1000 -> "$bps bps"
            bps < 1_000_000 -> String.format(Locale.US, "%.1f Kbps", bpsToKbps(bps))
            else -> String.format(Locale.US, "%.1f Mbps", bpsToMbps(bps))
        }
    }

    // ==================== Angle Conversions ====================

    fun radToDeg(rad: Double): Double = rad * 180.0 / Math.PI
    fun degToRad(deg: Double): Double = deg * Math.PI / 180.0
    fun radToDeg(rad: Float): Float = rad * 180f / Math.PI.toFloat()
    fun degToRad(deg: Float): Float = deg * Math.PI.toFloat() / 180f

    fun normalizeDeg(angle: Double): Double {
        var a = angle % 360
        if (a > 180) a -= 360
        if (a < -180) a += 360
        return a
    }

    fun normalizeRad(angle: Double): Double {
        var a = angle % (2 * Math.PI)
        if (a > Math.PI) a -= 2 * Math.PI
        if (a < -Math.PI) a += 2 * Math.PI
        return a
    }

    // ==================== Sensor Data Conversions ====================

    fun mps2ToG(ms2: Float): Float = ms2 / 9.81f
    fun gToMps2(g: Float): Float = g * 9.81f
    fun radsToDegs(rads: Float): Float = rads * 180f / Math.PI.toFloat()
    fun degsToRads(degs: Float): Float = degs * Math.PI.toFloat() / 180f
    fun vectorMagnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)
    fun vectorMagnitude(x: Float, y: Float): Float = sqrt(x * x + y * y)

    // ==================== Coordinate Conversions ====================

    fun rotationVectorToEuler(rotationVector: FloatArray): Triple<Double, Double, Double> {
        val q0 = rotationVector[3]
        val q1 = rotationVector[0]
        val q2 = rotationVector[1]
        val q3 = rotationVector[2]

        val roll = atan2(2.0 * (q0 * q1 + q2 * q3), 1.0 - 2.0 * (q1 * q1 + q2 * q2))
        val pitch = asin(2.0 * (q0 * q2 - q3 * q1).coerceIn(-1.0, 1.0))
        val yaw = atan2(2.0 * (q0 * q3 + q1 * q2), 1.0 - 2.0 * (q2 * q2 + q3 * q3))

        return Triple(roll, pitch, yaw)
    }

    fun eulerToRotationMatrix(roll: Float, pitch: Float, yaw: Float): FloatArray {
        // Fixed: Adjusted calculations to handle Float expressions directly to match mathematical models
        val cr = cos(roll)
        val sr = sin(roll)
        val cp = cos(pitch)
        val sp = sin(pitch)
        val cy = cos(yaw)
        val sy = sin(yaw)

        return floatArrayOf(
            cy * cp, sy * cp, -sp,
            cy * sp * sr - sy * cr, sy * sp * sr + cy * cr, cp * sr,
            cy * sp * cr + sy * sr, sy * sp * cr - cy * sr, cp * cr
        )
    }

    // ==================== Temperature Conversions ====================

    fun celsiusToFahrenheit(celsius: Float): Float = (celsius * 9f / 5f) + 32f
    fun fahrenheitToCelsius(fahrenheit: Float): Float = (fahrenheit - 32f) * 5f / 9f

    // ==================== Pressure Conversions ====================

    fun hpaToMmhg(hpa: Float): Float = hpa * 0.7500616f
    fun mmhgToHpa(mmhg: Float): Float = mmhg / 0.7500616f

    // ==================== Speed Conversions ====================

    fun mpsToKmh(mps: Float): Float = mps * 3.6f
    fun kmhToMps(kmh: Float): Float = kmh / 3.6f

    // ==================== Distance Conversions ====================

    fun metersToFeet(meters: Float): Float = meters * 3.28084f
    fun feetToMeters(feet: Float): Float = feet / 3.28084f
    fun metersToMiles(meters: Float): Float = meters / 1609.34f

    // ==================== Constants ====================

    const val GRAVITY = 9.80665f
    const val PI_FLOAT = 3.1415927f
    const val SPEED_OF_LIGHT = 299792458f
    const val STANDARD_ATMOSPHERE = 1013.25f

    // ==================== Android / Sensor Helpers ====================

    fun sensorTimestampToMs(nanoseconds: Long): Long = nanoseconds / 1_000_000
    fun sensorTimestampToSeconds(nanoseconds: Long): Double = nanoseconds / 1_000_000_000.0
    fun approxEquals(value: Float, target: Float, tolerance: Float = 0.001f): Boolean = abs(value - target) <= tolerance
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = b - a
        while (diff > PI_FLOAT) diff -= 2 * PI_FLOAT
        while (diff < -PI_FLOAT) diff += 2 * PI_FLOAT
        return a + diff * t
    }

    fun clamp(value: Float, min: Float, max: Float): Float = max(min, min(value, max))
    fun clamp(value: Int, min: Int, max: Int): Int = max(min, min(value, max))
    fun map(value: Float, fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
        val normalized = (value - fromMin) / (fromMax - fromMin)
        return toMin + normalized * (toMax - toMin)
    }

    fun toPercentage(value: Float, min: Float, max: Float): Float {
        return clamp(((value - min) / (max - min)) * 100f, 0f, 100f)
    }

    fun roundTo(value: Float, places: Int): Float {
        val factor = 10.0f.pow(places)
        return (value * factor).roundToInt() / factor
    }

    fun formatDecimal(value: Float, places: Int): String {
        return String.format(Locale.US, "%.${places}f", value)
    }
}