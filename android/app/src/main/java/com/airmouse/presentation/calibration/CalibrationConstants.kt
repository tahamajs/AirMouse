// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationConstants.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.ui.graphics.Color

object CalibrationConstants {
    // Calibration steps
    const val STEP_GYROSCOPE = 1
    const val STEP_MAGNETOMETER = 2
    const val STEP_ACCELEROMETER = 3
    const val STEP_COMPLETE = 4

    // Sample counts
    const val GYRO_SAMPLES_NEEDED = 500
    const val MAG_SAMPLES_NEEDED = 300
    const val ACCEL_SAMPLES_PER_POSE = 50

    // Default thresholds
    const val DEFAULT_NEAR_THRESHOLD = 1.5f
    const val DEFAULT_FAR_THRESHOLD = 3.0f
    const val DEFAULT_SENSITIVITY = 1.0f

    // Quality thresholds
    const val QUALITY_EXCELLENT_THRESHOLD = 0.95f
    const val QUALITY_GOOD_THRESHOLD = 0.8f
    const val QUALITY_FAIR_THRESHOLD = 0.6f

    // Animation durations
    const val ANIMATION_DURATION_MS = 500L
    const val PROGRESS_UPDATE_INTERVAL_MS = 100L

    // Colors
    val COLOR_PRIMARY = Color(0xFF6366F1)
    val COLOR_SUCCESS = Color(0xFF10B981)
    val COLOR_WARNING = Color(0xFFF59E0B)
    val COLOR_ERROR = Color(0xFFEF4444)
    val COLOR_BACKGROUND = Color(0xFF0F172A)
    val COLOR_SURFACE = Color(0xFF1E293B)

    // Text
    val STEP_TITLES = listOf(
        "Gyroscope Calibration",
        "Magnetometer Calibration",
        "Accelerometer Calibration",
        "Calibration Complete"
    )

    val STEP_INSTRUCTIONS = listOf(
        "Place device on a flat, stationary surface",
        "Move device in a figure-8 pattern",
        "Follow the position guide",
        "All sensors calibrated successfully"
    )

    val STEP_DESCRIPTIONS = listOf(
        "Collecting gyroscope bias data",
        "Collecting magnetic field data",
        "Collecting gravity alignment data",
        "Your device is now calibrated"
    )
}
