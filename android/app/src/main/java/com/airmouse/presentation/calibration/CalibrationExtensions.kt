// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationExtensions.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.ui.graphics.Color
import com.airmouse.domain.model.CalibrationQuality

fun CalibrationQuality.toColor(): Color {
    return when (this) {
        CalibrationQuality.EXCELLENT -> Color(0xFF10B981)
        CalibrationQuality.GOOD -> Color(0xFF3B82F6)
        CalibrationQuality.FAIR -> Color(0xFFF59E0B)
        CalibrationQuality.POOR -> Color(0xFFEF4444)
        CalibrationQuality.UNKNOWN -> Color(0xFF64748B)
    }
}

fun CalibrationQuality.toEmoji(): String {
    return when (this) {
        CalibrationQuality.EXCELLENT -> "🌟"
        CalibrationQuality.GOOD -> "👍"
        CalibrationQuality.FAIR -> "⚠️"
        CalibrationQuality.POOR -> "❌"
        CalibrationQuality.UNKNOWN -> "❓"
    }
}

fun CalibrationQuality.toDescription(): String {
    return when (this) {
        CalibrationQuality.EXCELLENT -> "Perfect calibration - optimal performance"
        CalibrationQuality.GOOD -> "Good calibration - accurate tracking"
        CalibrationQuality.FAIR -> "Fair calibration - may have slight drift"
        CalibrationQuality.POOR -> "Poor calibration - please recalibrate"
        CalibrationQuality.UNKNOWN -> "Unknown quality - calibration needed"
    }
}

fun Float.formatCalibrationValue(): String {
    return String.format("%.2f", this)
}

fun Int.toProgressPercentage(): Int {
    return this.coerceIn(0, 100)
}

fun Boolean.toStatusText(): String {
    return if (this) "Completed" else "Pending"
}