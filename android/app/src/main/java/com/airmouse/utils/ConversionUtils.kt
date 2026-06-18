package com.airmouse.utils

import kotlin.math.*

/**
 * Comprehensive unit conversion utilities for Air Mouse Pro.
 * Provides conversions for UI, time, data, networking, sensors, and more.
 */
object ConversionUtils {

    // ==================== UI / Display Conversions ====================

    /**
     * Convert pixels to density-independent pixels (dp).
     * @param px Value in pixels
     * @param density Screen density (from Resources.displayMetrics.density)
     * @return Value in dp
     */
    fun pxToDp(px: Int, density: Float): Int = (px / density).toInt()

    /**
     * Convert density-independent pixels (dp) to pixels.
     * @param dp Value in dp
     * @param density Screen density (from Resources.displayMetrics.density)
     * @return Value in pixels
     */
    fun dpToPx(dp: Int, density: Float): Int = (dp * density).toInt()

    /**
     * Convert scaled pixels (sp) to pixels.
     * @param sp Value in sp
     * @param scaledDensity Screen scaled density (from Resources.displayMetrics.scaledDensity)
     * @return Value in pixels
     */
    fun spToPx(sp: Int, scaledDensity: Float): Int = (sp * scaledDensity).toInt()

    /**
     * Convert pixels to scaled pixels (sp).
     * @param px Value in pixels
     * @param scaledDensity Screen scaled density (from Resources.displayMetrics.scaledDensity)
     * @return Value in sp
     */
    fun pxToSp(px: Int, scaledDensity: Float): Int = (px / scaledDensity).toInt()

    // ==================== Time Conversions ====================

    /**
     * Convert milliseconds to seconds.
     * @param ms Milliseconds
     * @return Seconds as float
     */
    fun msToSeconds(ms: Long): Float = ms / 1000f

    /**
     * Convert seconds to milliseconds.
     * @param seconds Seconds
     * @return Milliseconds
     */
    fun secondsToMs(seconds: Float): Long = (seconds * 1000).toLong()

    /**
     * Convert milliseconds to minutes.
     * @param ms Milliseconds
     * @return Minutes as float
     */
    fun msToMinutes(ms: Long): Float = ms / 60000f

    /**
     * Convert minutes to milliseconds.
     * @param minutes Minutes
     * @return Milliseconds
     */
    fun minutesToMs(minutes: Float): Long = (minutes * 60000).toLong()

    /**
     * Format milliseconds into HH:MM:SS or MM:SS format.
     * @param ms Milliseconds
     * @param includeHours Whether to include hours in output
     * @return Formatted time string
     */
    fun formatTime(ms: Long, includeHours: Boolean = true): String {
        val totalSeconds = ms / 1000
        val hours = totalSeconds / 3600
        val minutes = (totalSeconds % 3600) / 60
        val seconds = totalSeconds % 60

        return if (includeHours && hours > 0) {
            String.format("%02d:%02d:%02d", hours, minutes, seconds)
        } else {
            String.format("%02d:%02d", minutes, seconds)
        }
    }

    /**
     * Format milliseconds into a human-readable duration (e.g., "2h 30m" or "45s").
     * @param ms Milliseconds
     * @return Human-readable duration string
     */
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

    /**
     * Convert bytes to megabytes.
     * @param bytes Bytes
     * @return Megabytes
     */
    fun bytesToMB(bytes: Long): Float = bytes / (1024f * 1024f)

    /**
     * Convert bytes to kilobytes.
     * @param bytes Bytes
     * @return Kilobytes
     */
    fun bytesToKB(bytes: Long): Float = bytes / 1024f

    /**
     * Convert bytes to gigabytes.
     * @param bytes Bytes
     * @return Gigabytes
     */
    fun bytesToGB(bytes: Long): Float = bytes / (1024f * 1024f * 1024f)

    /**
     * Convert megabytes to bytes.
     * @param mb Megabytes
     * @return Bytes
     */
    fun mbToBytes(mb: Float): Long = (mb * 1024f * 1024f).toLong()

    /**
     * Convert kilobytes to bytes.
     * @param kb Kilobytes
     * @return Bytes
     */
    fun kbToBytes(kb: Float): Long = (kb * 1024f).toLong()

    /**
     * Format bytes to a human-readable string.
     * @param bytes Bytes
     * @return Human-readable size (e.g., "1.5 MB")
     */
    fun formatBytes(bytes: Long): String {
        return when {
            bytes < 1024 -> "$bytes B"
            bytes < 1024 * 1024 -> String.format("%.1f KB", bytesToKB(bytes))
            bytes < 1024 * 1024 * 1024 -> String.format("%.1f MB", bytesToMB(bytes))
            else -> String.format("%.1f GB", bytesToGB(bytes))
        }
    }

    // ==================== Data Rate / Network Conversions ====================

    /**
     * Convert bits per second to megabits per second.
     * @param bps Bits per second
     * @return Megabits per second
     */
    fun bpsToMbps(bps: Long): Float = bps / 1_000_000f

    /**
     * Convert bits per second to kilobits per second.
     * @param bps Bits per second
     * @return Kilobits per second
     */
    fun bpsToKbps(bps: Long): Float = bps / 1000f

    /**
     * Convert megabits per second to bits per second.
     * @param mbps Megabits per second
     * @return Bits per second
     */
    fun mbpsToBps(mbps: Float): Long = (mbps * 1_000_000).toLong()

    /**
     * Convert bytes per second to megabits per second.
     * @param bps Bytes per second
     * @return Megabits per second
     */
    fun bpsMbpsFromBytes(bytesPerSecond: Long): Float = (bytesPerSecond * 8) / 1_000_000f

    /**
     * Format network speed to a human-readable string.
     * @param bps Bits per second
     * @return Human-readable speed (e.g., "45 Mbps")
     */
    fun formatSpeed(bps: Long): String {
        return when {
            bps < 1000 -> "$bps bps"
            bps < 1_000_000 -> String.format("%.1f Kbps", bpsToKbps(bps))
            else -> String.format("%.1f Mbps", bpsToMbps(bps))
        }
    }

    // ==================== Angle Conversions ====================

    /**
     * Convert radians to degrees.
     * @param rad Radians
     * @return Degrees
     */
    fun radToDeg(rad: Double): Double = rad * 180.0 / Math.PI

    /**
     * Convert degrees to radians.
     * @param deg Degrees
     * @return Radians
     */
    fun degToRad(deg: Double): Double = deg * Math.PI / 180.0

    /**
     * Convert radians to degrees (Float version).
     * @param rad Radians
     * @return Degrees
     */
    fun radToDeg(rad: Float): Float = rad * 180f / Math.PI.toFloat()

    /**
     * Convert degrees to radians (Float version).
     * @param deg Degrees
     * @return Radians
     */
    fun degToRad(deg: Float): Float = deg * Math.PI.toFloat() / 180f

    /**
     * Normalize an angle to the range [-180, 180] degrees.
     * @param angle Angle in degrees
     * @return Normalized angle
     */
    fun normalizeDeg(angle: Double): Double {
        var a = angle % 360
        if (a > 180) a -= 360
        if (a < -180) a += 360
        return a
    }

    /**
     * Normalize an angle to the range [-π, π] radians.
     * @param angle Angle in radians
     * @return Normalized angle
     */
    fun normalizeRad(angle: Double): Double {
        var a = angle % (2 * Math.PI)
        if (a > Math.PI) a -= 2 * Math.PI
        if (a < -Math.PI) a += 2 * Math.PI
        return a
    }

    // ==================== Sensor Data Conversions ====================

    /**
     * Convert acceleration from m/s² to g-force.
     * @param ms2 Acceleration in m/s²
     * @return Acceleration in g (9.81 m/s²)
     */
    fun mps2ToG(ms2: Float): Float = ms2 / 9.81f

    /**
     * Convert g-force to m/s².
     * @param g Acceleration in g
     * @return Acceleration in m/s²
     */
    fun gToMps2(g: Float): Float = g * 9.81f

    /**
     * Convert angular velocity from rad/s to degrees/s.
     * @param rads Angular velocity in rad/s
     * @return Angular velocity in degrees/s
     */
    fun radsToDegs(rads: Float): Float = rads * 180f / Math.PI.toFloat()

    /**
     * Convert angular velocity from degrees/s to rad/s.
     * @param degs Angular velocity in degrees/s
     * @return Angular velocity in rad/s
     */
    fun degsToRads(degs: Float): Float = degs * Math.PI.toFloat() / 180f

    /**
     * Calculate the magnitude of a 3D vector.
     * @param x X component
     * @param y Y component
     * @param z Z component
     * @return Vector magnitude
     */
    fun vectorMagnitude(x: Float, y: Float, z: Float): Float = sqrt(x * x + y * y + z * z)

    /**
     * Calculate the magnitude of a 2D vector.
     * @param x X component
     * @param y Y component
     * @return Vector magnitude
     */
    fun vectorMagnitude(x: Float, y: Float): Float = sqrt(x * x + y * y)

    // ==================== Coordinate Conversions ====================

    /**
     * Convert Android sensor rotation vector to Euler angles.
     * @param rotationVector Rotation vector [x, y, z, w]
     * @return Triple of (roll, pitch, yaw) in radians
     */
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

    /**
     * Convert Euler angles to a rotation matrix.
     * @param roll Roll angle in radians
     * @param pitch Pitch angle in radians
     * @param yaw Yaw angle in radians
     * @return 3x3 rotation matrix as a 9-element array
     */
    fun eulerToRotationMatrix(roll: Double, pitch: Double, yaw: Double): FloatArray {
        val cr = cos(roll).toFloat()
        val sr = sin(roll).toFloat()
        val cp = cos(pitch).toFloat()
        val sp = sin(pitch).toFloat()
        val cy = cos(yaw).toFloat()
        val sy = sin(yaw).toFloat()

        return floatArrayOf(
            cy * cp, sy * cp, -sp,
            cy * sp * sr - sy * cr, sy * sp * sr + cy * cr, cp * sr,
            cy * sp * cr + sy * sr, sy * sp * cr - cy * sr, cp * cr
        )
    }

    // ==================== Temperature Conversions ====================

    /**
     * Convert Celsius to Fahrenheit.
     * @param celsius Temperature in Celsius
     * @return Temperature in Fahrenheit
     */
    fun celsiusToFahrenheit(celsius: Float): Float = (celsius * 9f / 5f) + 32f

    /**
     * Convert Fahrenheit to Celsius.
     * @param fahrenheit Temperature in Fahrenheit
     * @return Temperature in Celsius
     */
    fun fahrenheitToCelsius(fahrenheit: Float): Float = (fahrenheit - 32f) * 5f / 9f

    // ==================== Pressure Conversions ====================

    /**
     * Convert hPa to mmHg.
     * @param hpa Pressure in hPa
     * @return Pressure in mmHg
     */
    fun hpaToMmhg(hpa: Float): Float = hpa * 0.75006156f

    /**
     * Convert mmHg to hPa.
     * @param mmhg Pressure in mmHg
     * @return Pressure in hPa
     */
    fun mmhgToHpa(mmhg: Float): Float = mmhg / 0.75006156f

    // ==================== Speed Conversions ====================

    /**
     * Convert meters per second to kilometers per hour.
     * @param mps Speed in m/s
     * @return Speed in km/h
     */
    fun mpsToKmh(mps: Float): Float = mps * 3.6f

    /**
     * Convert kilometers per hour to meters per second.
     * @param kmh Speed in km/h
     * @return Speed in m/s
     */
    fun kmhToMps(kmh: Float): Float = kmh / 3.6f

    // ==================== Distance Conversions ====================

    /**
     * Convert meters to feet.
     * @param meters Distance in meters
     * @return Distance in feet
     */
    fun metersToFeet(meters: Float): Float = meters * 3.28084f

    /**
     * Convert feet to meters.
     * @param feet Distance in feet
     * @return Distance in meters
     */
    fun feetToMeters(feet: Float): Float = feet / 3.28084f

    /**
     * Convert meters to miles.
     * @param meters Distance in meters
     * @return Distance in miles
     */
    fun metersToMiles(meters: Float): Float = meters / 1609.34f

    // ==================== Memory / Unit Helpers ====================

    /** Earth's gravity in m/s² */
    const val GRAVITY = 9.80665f

    /** Pi constant (Float) */
    const val PI = Math.PI.toFloat()

    /** Speed of light in m/s */
    const val SPEED_OF_LIGHT = 299792458f

    /** Standard atmosphere in hPa */
    const val STANDARD_ATMOSPHERE = 1013.25f

    // ==================== Android / Sensor Helpers ====================

    /**
     * Convert sensor timestamp (nanoseconds) to milliseconds.
     * @param nanoseconds Sensor timestamp in nanoseconds
     * @return Timestamp in milliseconds
     */
    fun sensorTimestampToMs(nanoseconds: Long): Long = nanoseconds / 1_000_000

    /**
     * Convert sensor timestamp (nanoseconds) to seconds.
     * @param nanoseconds Sensor timestamp in nanoseconds
     * @return Timestamp in seconds
     */
    fun sensorTimestampToSeconds(nanoseconds: Long): Double = nanoseconds / 1_000_000_000.0

    /**
     * Check if a value is within a range with tolerance.
     * @param value Value to check
     * @param target Target value
     * @param tolerance Allowed tolerance
     * @return True if within range
     */
    fun approxEquals(value: Float, target: Float, tolerance: Float = 0.001f): Boolean =
        abs(value - target) <= tolerance

    /**
     * Linearly interpolate between two values.
     * @param a Start value
     * @param b End value
     * @param t Interpolation factor (0-1)
     * @return Interpolated value
     */
    fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /**
     * Linearly interpolate between two angles (shortest path).
     * @param a Start angle in radians
     * @param b End angle in radians
     * @param t Interpolation factor (0-1)
     * @return Interpolated angle in radians
     */
    fun lerpAngle(a: Float, b: Float, t: Float): Float {
        var diff = b - a
        while (diff > Math.PI.toFloat()) diff -= 2 * Math.PI.toFloat()
        while (diff < -Math.PI.toFloat()) diff += 2 * Math.PI.toFloat()
        return a + diff * t
    }

    /**
     * Clamp a value between a minimum and maximum.
     * @param value Value to clamp
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Clamped value
     */
    fun clamp(value: Float, min: Float, max: Float): Float = max(min, min(value, max))

    /**
     * Clamp a value between a minimum and maximum (Int version).
     * @param value Value to clamp
     * @param min Minimum allowed value
     * @param max Maximum allowed value
     * @return Clamped value
     */
    fun clamp(value: Int, min: Int, max: Int): Int = max(min, min(value, max))

    /**
     * Map a value from one range to another.
     * @param value Value to map
     * @param fromMin Source range minimum
     * @param fromMax Source range maximum
     * @param toMin Target range minimum
     * @param toMax Target range maximum
     * @return Mapped value
     */
    fun map(value: Float, fromMin: Float, fromMax: Float, toMin: Float, toMax: Float): Float {
        val normalized = (value - fromMin) / (fromMax - fromMin)
        return toMin + normalized * (toMax - toMin)
    }

    /**
     * Convert a value to a percentage.
     * @param value Current value
     * @param min Minimum value
     * @param max Maximum value
     * @return Percentage (0-100)
     */
    fun toPercentage(value: Float, min: Float, max: Float): Float {
        return clamp(((value - min) / (max - min)) * 100f, 0f, 100f)
    }

    /**
     * Round a float to a specific number of decimal places.
     * @param value Value to round
     * @param places Number of decimal places
     * @return Rounded value
     */
    fun roundTo(value: Float, places: Int): Float {
        val factor = 10.0f.pow(places)
        return (value * factor).roundToInt() / factor
    }

    /**
     * Format a float with a specific number of decimal places.
     * @param value Value to format
     * @param places Number of decimal places
     * @return Formatted string
     */
    fun formatDecimal(value: Float, places: Int): String {
        return String.format("%.${places}f", value)
    }
}