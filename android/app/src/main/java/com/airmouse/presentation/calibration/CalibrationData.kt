// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationExtensions.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.ui.graphics.Color
import com.airmouse.domain.model.CalibrationQuality
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// 1. CALIBRATION QUALITY EXTENSIONS
// ==========================================

/**
 * Convert CalibrationQuality to Color
 */
fun CalibrationQuality.toColor(): Color {
    return when (this) {
        CalibrationQuality.EXCELLENT -> Color(0xFF10B981)  // Green
        CalibrationQuality.GOOD -> Color(0xFF3B82F6)       // Blue
        CalibrationQuality.FAIR -> Color(0xFFF59E0B)       // Yellow/Orange
        CalibrationQuality.POOR -> Color(0xFFEF4444)       // Red
        CalibrationQuality.UNKNOWN -> Color(0xFF64748B)    // Gray
    }
}

/**
 * Convert CalibrationQuality to emoji
 */
fun CalibrationQuality.toEmoji(): String {
    return when (this) {
        CalibrationQuality.EXCELLENT -> "🌟"
        CalibrationQuality.GOOD -> "👍"
        CalibrationQuality.FAIR -> "⚠️"
        CalibrationQuality.POOR -> "❌"
        CalibrationQuality.UNKNOWN -> "❓"
    }
}

/**
 * Convert CalibrationQuality to description string
 */
fun CalibrationQuality.toDescription(): String {
    return when (this) {
        CalibrationQuality.EXCELLENT -> "Perfect calibration - optimal performance"
        CalibrationQuality.GOOD -> "Good calibration - accurate tracking"
        CalibrationQuality.FAIR -> "Fair calibration - may have slight drift"
        CalibrationQuality.POOR -> "Poor calibration - please recalibrate"
        CalibrationQuality.UNKNOWN -> "Unknown quality - calibration needed"
    }
}

/**
 * Convert CalibrationQuality to user-friendly display name
 */
fun CalibrationQuality.toDisplayName(): String {
    return when (this) {
        CalibrationQuality.EXCELLENT -> "Excellent"
        CalibrationQuality.GOOD -> "Good"
        CalibrationQuality.FAIR -> "Fair"
        CalibrationQuality.POOR -> "Poor"
        CalibrationQuality.UNKNOWN -> "Unknown"
    }
}

/**
 * Convert CalibrationQuality to numeric score (0-100)
 */
fun CalibrationQuality.toScore(): Int {
    return when (this) {
        CalibrationQuality.EXCELLENT -> 95
        CalibrationQuality.GOOD -> 80
        CalibrationQuality.FAIR -> 60
        CalibrationQuality.POOR -> 30
        CalibrationQuality.UNKNOWN -> 0
    }
}

/**
 * Check if calibration quality is acceptable
 */
fun CalibrationQuality.isAcceptable(): Boolean {
    return this == CalibrationQuality.EXCELLENT ||
            this == CalibrationQuality.GOOD ||
            this == CalibrationQuality.FAIR
}

/**
 * Get recommended action based on calibration quality
 */
fun CalibrationQuality.getRecommendation(): String {
    return when (this) {
        CalibrationQuality.EXCELLENT -> "Your device is perfectly calibrated. Enjoy optimal performance!"
        CalibrationQuality.GOOD -> "Your device is calibrated well. For best results, consider recalibrating in optimal conditions."
        CalibrationQuality.FAIR -> "Calibration is acceptable. For better accuracy, recalibrate in a stable environment."
        CalibrationQuality.POOR -> "Calibration quality is poor. Please recalibrate for accurate tracking."
        CalibrationQuality.UNKNOWN -> "Calibration status unknown. Please calibrate your device."
    }
}

/**
 * Check if calibration is usable
 */
fun CalibrationQuality.isUsable(): Boolean {
    return this == CalibrationQuality.EXCELLENT || this == CalibrationQuality.GOOD
}

// ==========================================
// 2. FLOAT EXTENSIONS
// ==========================================

/**
 * Format float as calibration value with 2 decimal places
 */
fun Float.formatCalibrationValue(): String {
    return String.format(Locale.US, "%.2f", this)
}

/**
 * Format float as calibration value with specified decimal places
 */
fun Float.formatCalibrationValue(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f", this)
}

/**
 * Format float as percentage
 */
fun Float.toPercentage(): String {
    return String.format(Locale.US, "%.1f%%", this * 100)
}

/**
 * Format float as percentage with decimals
 */
fun Float.toPercentage(decimals: Int): String {
    return String.format(Locale.US, "%.${decimals}f%%", this * 100)
}

/**
 * Format float as distance in meters
 */
fun Float.formatDistance(): String {
    return String.format(Locale.US, "%.2f m", this)
}

/**
 * Format float as distance in centimeters
 */
fun Float.formatDistanceCm(): String {
    return String.format(Locale.US, "%.1f cm", this * 100)
}

/**
 * Format float as speed in pixels/second
 */
fun Float.formatSpeed(): String {
    return String.format(Locale.US, "%.0f px/s", this)
}

/**
 * Format float as angle in degrees
 */
fun Float.formatAngle(): String {
    return String.format(Locale.US, "%.1f°", this)
}

/**
 * Format float as time in milliseconds
 */
fun Float.formatTimeMs(): String {
    return String.format(Locale.US, "%.0f ms", this)
}

/**
 * Format float as time in seconds
 */
fun Float.formatTimeSeconds(): String {
    return String.format(Locale.US, "%.2f s", this)
}

/**
 * Format float as frequency in Hertz
 */
fun Float.formatFrequency(): String {
    return String.format(Locale.US, "%.1f Hz", this)
}

/**
 * Clamp float between min and max
 */
fun Float.clamp(min: Float, max: Float): Float {
    return this.coerceIn(min, max)
}

/**
 * Check if float is within tolerance of target
 */
fun Float.isWithinTolerance(target: Float, tolerance: Float): Boolean {
    return kotlin.math.abs(this - target) <= tolerance
}

/**
 * Map float from one range to another
 */
fun Float.mapRange(fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float {
    return toLow + (this - fromLow) * (toHigh - toLow) / (fromHigh - fromLow)
}

/**
 * Check if float is approximately zero
 */
fun Float.isApproximatelyZero(tolerance: Float = 0.001f): Boolean {
    return kotlin.math.abs(this) < tolerance
}

/**
 * Get sign of float (-1, 0, 1)
 */
fun Float.sign(): Int {
    return when {
        this > 0 -> 1
        this < 0 -> -1
        else -> 0
    }
}

/**
 * Format float with unit suffix (K, M, B)
 */
fun Float.formatWithUnit(): String {
    return when {
        this >= 1_000_000_000 -> String.format(Locale.US, "%.1fB", this / 1_000_000_000)
        this >= 1_000_000 -> String.format(Locale.US, "%.1fM", this / 1_000_000)
        this >= 1_000 -> String.format(Locale.US, "%.1fK", this / 1_000)
        else -> String.format(Locale.US, "%.0f", this)
    }
}

/**
 * Convert float to radians
 */
fun Float.toRadians(): Float {
    return this * (Math.PI.toFloat() / 180f)
}

/**
 * Convert float to degrees
 */
fun Float.toDegrees(): Float {
    return this * (180f / Math.PI.toFloat())
}

// ==========================================
// 3. INT EXTENSIONS
// ==========================================

/**
 * Convert int to progress percentage (0-100)
 */
fun Int.toProgressPercentage(): Int {
    return this.coerceIn(0, 100)
}

/**
 * Convert int to progress percentage with cap
 */
fun Int.toProgressPercentage(max: Int): Int {
    return ((this.toFloat() / max) * 100).toInt().coerceIn(0, 100)
}

/**
 * Format int with ordinal suffix (1st, 2nd, 3rd, etc.)
 */
fun Int.toOrdinal(): String {
    return when {
        this % 100 in 11..13 -> "${this}th"
        this % 10 == 1 -> "${this}st"
        this % 10 == 2 -> "${this}nd"
        this % 10 == 3 -> "${this}rd"
        else -> "${this}th"
    }
}

/**
 * Convert int to duration string (mm:ss)
 */
fun Int.toDurationString(): String {
    val minutes = this / 60
    val seconds = this % 60
    return String.format(Locale.US, "%02d:%02d", minutes, seconds)
}

/**
 * Convert int to duration string with hours (HH:mm:ss)
 */
fun Int.toDurationStringLong(): String {
    val hours = this / 3600
    val minutes = (this % 3600) / 60
    val seconds = this % 60
    return String.format(Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)
}

/**
 * Convert int to file size string
 */
fun Int.toFileSize(): String {
    return when {
        this < 1024 -> "${this} B"
        this < 1024 * 1024 -> "${this / 1024} KB"
        else -> "${this / (1024 * 1024)} MB"
    }
}

/**
 * Check if int is even
 */
fun Int.isEven(): Boolean {
    return this % 2 == 0
}

/**
 * Check if int is odd
 */
fun Int.isOdd(): Boolean {
    return this % 2 != 0
}

/**
 * Get digit count of int
 */
fun Int.digitCount(): Int {
    return this.toString().length
}

// ==========================================
// 4. BOOLEAN EXTENSIONS
// ==========================================

/**
 * Convert boolean to status text
 */
fun Boolean.toStatusText(): String {
    return if (this) "Completed" else "Pending"
}

/**
 * Convert boolean to checkmark text
 */
fun Boolean.toCheckmark(): String {
    return if (this) "✓" else "✗"
}

/**
 * Convert boolean to status text with custom strings
 */
fun Boolean.toStatusText(positive: String, negative: String): String {
    return if (this) positive else negative
}

/**
 * Convert boolean to color (green if true, red if false)
 */
fun Boolean.toStatusColor(): Color {
    return if (this) Color(0xFF10B981) else Color(0xFFEF4444)
}

/**
 * Convert boolean to enabled/disabled text
 */
fun Boolean.toEnabledText(): String {
    return if (this) "Enabled" else "Disabled"
}

/**
 * Convert boolean to on/off text
 */
fun Boolean.toOnOffText(): String {
    return if (this) "ON" else "OFF"
}

/**
 * Convert boolean to yes/no text
 */
fun Boolean.toYesNoText(): String {
    return if (this) "Yes" else "No"
}

/**
 * Convert boolean to 1/0
 */
fun Boolean.toInt(): Int {
    return if (this) 1 else 0
}

// ==========================================
// 5. LONG EXTENSIONS
// ==========================================

/**
 * Format timestamp as date string
 */
fun Long.toDateString(): String {
    val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)
    return format.format(Date(this))
}

/**
 * Format timestamp as time string
 */
fun Long.toTimeString(): String {
    val format = SimpleDateFormat("HH:mm:ss", Locale.US)
    return format.format(Date(this))
}

/**
 * Format timestamp as short date
 */
fun Long.toShortDateString(): String {
    val format = SimpleDateFormat("MMM dd, yyyy", Locale.US)
    return format.format(Date(this))
}

/**
 * Format timestamp as date without time
 */
fun Long.toDateOnlyString(): String {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.US)
    return format.format(Date(this))
}

/**
 * Format duration in milliseconds to human-readable string
 */
fun Long.formatDuration(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60
    val remainingMinutes = minutes % 60
    val remainingSeconds = seconds % 60

    return when {
        hours > 0 -> String.format(Locale.US, "%dh %dm %ds", hours, remainingMinutes, remainingSeconds)
        minutes > 0 -> String.format(Locale.US, "%dm %ds", remainingMinutes, remainingSeconds)
        else -> String.format(Locale.US, "%ds", remainingSeconds)
    }
}

/**
 * Format duration in milliseconds to compact string
 */
fun Long.formatDurationCompact(): String {
    val seconds = this / 1000
    val minutes = seconds / 60
    val hours = minutes / 60

    return when {
        hours > 0 -> String.format(Locale.US, "%dh", hours)
        minutes > 0 -> String.format(Locale.US, "%dm", minutes)
        else -> String.format(Locale.US, "%ds", seconds)
    }
}

/**
 * Format duration with millisecond precision
 */
fun Long.formatDurationMs(): String {
    val ms = this % 1000
    val seconds = (this / 1000) % 60
    val minutes = (this / (1000 * 60)) % 60
    val hours = this / (1000 * 60 * 60)

    return when {
        hours > 0 -> String.format(Locale.US, "%dh %dm %ds", hours, minutes, seconds)
        minutes > 0 -> String.format(Locale.US, "%dm %ds", minutes, seconds)
        else -> String.format(Locale.US, "%d.%03ds", seconds, ms)
    }
}

/**
 * Check if timestamp is today
 */
fun Long.isToday(): Boolean {
    val today = Calendar.getInstance()
    val date = Calendar.getInstance().apply { time = Date(this@isToday) }
    return today.get(Calendar.YEAR) == date.get(Calendar.YEAR) &&
            today.get(Calendar.DAY_OF_YEAR) == date.get(Calendar.DAY_OF_YEAR)
}

/**
 * Check if timestamp is in the past
 */
fun Long.isPast(): Boolean {
    return this < System.currentTimeMillis()
}

/**
 * Check if timestamp is in the future
 */
fun Long.isFuture(): Boolean {
    return this > System.currentTimeMillis()
}

// ==========================================
// 6. STRING EXTENSIONS
// ==========================================

/**
 * Check if string is a valid calibration value (number)
 */
fun String.isValidCalibrationValue(): Boolean {
    return try {
        this.toFloat()
        true
    } catch (e: NumberFormatException) {
        false
    }
}

/**
 * Check if string is empty or blank
 */
fun String.isNullOrBlank(): Boolean {
    return this.isNullOrBlank()
}

/**
 * Truncate string to max length with ellipsis
 */
fun String.truncate(maxLength: Int): String {
    return if (this.length <= maxLength) {
        this
    } else {
        this.substring(0, maxLength - 3) + "..."
    }
}

/**
 * Capitalize first letter of string
 */
fun String.capitalizeFirst(): String {
    return if (this.isEmpty()) this else this[0].uppercase() + this.substring(1)
}

/**
 * Convert string to title case
 */
fun String.toTitleCase(): String {
    return this.split(" ").joinToString(" ") { it.capitalizeFirst() }
}

/**
 * Check if string contains only digits
 */
fun String.isNumeric(): Boolean {
    return this.all { it.isDigit() }
}

/**
 * Check if string contains only letters
 */
fun String.isAlpha(): Boolean {
    return this.all { it.isLetter() }
}

/**
 * Check if string is alphanumeric
 */
fun String.isAlphanumeric(): Boolean {
    return this.all { it.isLetterOrDigit() }
}

/**
 * Extract numbers from string
 */
fun String.extractNumbers(): String {
    return this.filter { it.isDigit() }
}

// ==========================================
// 7. COLLECTION EXTENSIONS
// ==========================================

/**
 * Calculate average of Float list
 */
fun List<Float>.averageOrZero(): Float {
    return if (this.isEmpty()) 0f else this.average().toFloat()
}

/**
 * Calculate variance of Float list
 */
fun List<Float>.variance(): Float {
    if (this.size < 2) return 0f
    val mean = this.average()
    return this.map { (it - mean) * (it - mean) }.average().toFloat()
}

/**
 * Calculate standard deviation of Float list
 */
fun List<Float>.standardDeviation(): Float {
    return kotlin.math.sqrt(this.variance().toDouble()).toFloat()
}

/**
 * Find min and max of Float list
 */
fun List<Float>.minMax(): Pair<Float, Float>? {
    if (this.isEmpty()) return null
    return Pair(this.minOrNull() ?: 0f, this.maxOrNull() ?: 0f)
}

/**
 * Get median of Float list
 */
fun List<Float>.median(): Float {
    if (this.isEmpty()) return 0f
    val sorted = this.sorted()
    return if (sorted.size % 2 == 0) {
        (sorted[sorted.size / 2 - 1] + sorted[sorted.size / 2]) / 2
    } else {
        sorted[sorted.size / 2]
    }
}

/**
 * Get range of Float list
 */
fun List<Float>.range(): Float {
    if (this.isEmpty()) return 0f
    return this.maxOrNull()!! - this.minOrNull()!!
}

/**
 * Check if list is stable (variance below threshold)
 */
fun List<Float>.isStable(threshold: Float = 0.05f): Boolean {
    return this.variance() < threshold
}

/**
 * Get moving average of Float list
 */
fun List<Float>.movingAverage(windowSize: Int): List<Float> {
    if (this.isEmpty() || windowSize <= 0) return emptyList()
    val result = mutableListOf<Float>()
    for (i in windowSize until this.size) {
        val window = this.subList(i - windowSize, i)
        result.add(window.averageOrZero())
    }
    return result
}

// ==========================================
// 8. CALIBRATION QUALITY HELPER FUNCTIONS
// ==========================================

/**
 * Get quality from variance value
 */
fun getCalibrationQualityFromVariance(variance: Float): CalibrationQuality {
    return when {
        variance < 0.01f -> CalibrationQuality.EXCELLENT
        variance < 0.05f -> CalibrationQuality.GOOD
        variance < 0.1f -> CalibrationQuality.FAIR
        variance < 0.5f -> CalibrationQuality.POOR
        else -> CalibrationQuality.UNKNOWN
    }
}

/**
 * Get quality from score (0-100)
 */
fun getCalibrationQualityFromScore(score: Int): CalibrationQuality {
    return when {
        score >= 90 -> CalibrationQuality.EXCELLENT
        score >= 70 -> CalibrationQuality.GOOD
        score >= 50 -> CalibrationQuality.FAIR
        score >= 30 -> CalibrationQuality.POOR
        else -> CalibrationQuality.UNKNOWN
    }
}

/**
 * Get quality from confidence value (0-1)
 */
fun getCalibrationQualityFromConfidence(confidence: Float): CalibrationQuality {
    return when {
        confidence >= 0.9f -> CalibrationQuality.EXCELLENT
        confidence >= 0.7f -> CalibrationQuality.GOOD
        confidence >= 0.5f -> CalibrationQuality.FAIR
        confidence >= 0.3f -> CalibrationQuality.POOR
        else -> CalibrationQuality.UNKNOWN
    }
}

/**
 * Check if calibration is good enough for use
 */
fun isCalibrationUsable(quality: CalibrationQuality): Boolean {
    return quality == CalibrationQuality.EXCELLENT ||
            quality == CalibrationQuality.GOOD
}

/**
 * Get recommended action based on calibration quality
 */
fun getCalibrationRecommendation(quality: CalibrationQuality): String {
    return when (quality) {
        CalibrationQuality.EXCELLENT -> "Your device is perfectly calibrated. Enjoy optimal performance!"
        CalibrationQuality.GOOD -> "Your device is calibrated well. For best results, consider recalibrating in optimal conditions."
        CalibrationQuality.FAIR -> "Calibration is acceptable. For better accuracy, recalibrate in a stable environment."
        CalibrationQuality.POOR -> "Calibration quality is poor. Please recalibrate for accurate tracking."
        CalibrationQuality.UNKNOWN -> "Calibration status unknown. Please calibrate your device."
    }
}

// ==========================================
// 9. SENSOR DATA HELPERS
// ==========================================

/**
 * Check if sensor data is stable (variance below threshold)
 */
fun isSensorDataStable(data: List<Float>, threshold: Float = 0.05f): Boolean {
    if (data.size < 10) return false
    return data.variance() < threshold
}

/**
 * Calculate confidence from sensor stability
 */
fun calculateConfidenceFromStability(variance: Float): Float {
    return when {
        variance < 0.01f -> 0.95f
        variance < 0.05f -> 0.8f
        variance < 0.1f -> 0.6f
        else -> 0.3f
    }
}

/**
 * Format sensor value with proper units
 */
fun formatSensorValue(value: Float, type: String): String {
    return when (type.lowercase()) {
        "gyro" -> String.format(Locale.US, "%.2f rad/s", value)
        "accel" -> String.format(Locale.US, "%.2f m/s²", value)
        "mag" -> String.format(Locale.US, "%.2f µT", value)
        "roll" -> String.format(Locale.US, "%.1f°", value)
        "pitch" -> String.format(Locale.US, "%.1f°", value)
        "yaw" -> String.format(Locale.US, "%.1f°", value)
        "rssi" -> String.format(Locale.US, "%d dBm", value.toInt())
        else -> String.format(Locale.US, "%.2f", value)
    }
}

/**
 * Get color for sensor value based on range
 */
fun getSensorValueColor(value: Float, min: Float, max: Float): Color {
    val normalized = ((value - min) / (max - min)).coerceIn(0f, 1f)
    return when {
        normalized < 0.3f -> Color(0xFF10B981)  // Green - good
        normalized < 0.6f -> Color(0xFFF59E0B)  // Yellow - warning
        normalized < 0.8f -> Color(0xFFF97316)  // Orange - caution
        else -> Color(0xFFEF4444)               // Red - bad
    }
}

/**
 * Check if sensor data is within normal range
 */
fun isSensorDataInRange(value: Float, min: Float, max: Float): Boolean {
    return value in min..max
}

/**
 * Calculate signal quality from RSSI
 */
fun calculateSignalQuality(rssi: Int): Float {
    return when {
        rssi > -50 -> 1.0f
        rssi > -60 -> 0.8f
        rssi > -70 -> 0.6f
        rssi > -80 -> 0.4f
        rssi > -90 -> 0.2f
        else -> 0.0f
    }
}

/**
 * Get signal quality description from RSSI
 */
fun getSignalQualityDescription(rssi: Int): String {
    return when {
        rssi > -50 -> "Excellent"
        rssi > -60 -> "Good"
        rssi > -70 -> "Fair"
        rssi > -80 -> "Poor"
        else -> "Very Poor"
    }
}

// ==========================================
// 10. GENERAL HELPER FUNCTIONS
// ==========================================

/**
 * Check if calibration is complete based on steps
 */
fun isCalibrationComplete(currentStep: Int, totalSteps: Int): Boolean {
    return currentStep >= totalSteps
}

/**
 * Get progress percentage from steps
 */
fun getStepProgress(currentStep: Int, totalSteps: Int): Int {
    return ((currentStep.toFloat() / totalSteps) * 100).toInt().coerceIn(0, 100)
}

/**
 * Format calibration status message
 */
fun formatCalibrationStatusMessage(step: Int, totalSteps: Int, isComplete: Boolean): String {
    return if (isComplete) {
        "Calibration Complete!"
    } else {
        "Step $step of $totalSteps"
    }
}