// app/src/main/java/com/airmouse/data/repository/CalibrationRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.*
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.abs

@Singleton
class CalibrationRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ICalibrationRepository {

    private val _calibrationComplete = MutableStateFlow(prefs.getBoolean("calibration_complete", false))

    // ... (save/get methods remain as you had them)

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        val gyroCal = prefs.getFloat("gyro_offset_x", 0f) != 0f ||
                prefs.getFloat("gyro_offset_y", 0f) != 0f ||
                prefs.getFloat("gyro_offset_z", 0f) != 0f

        val accelCal = prefs.getFloat("accel_offset_x", 0f) != 0f &&
                prefs.getFloat("accel_scale_x", 1f) != 1f

        val magCal = prefs.getFloat("mag_offset_x", 0f) != 0f &&
                prefs.getFloat("mag_scale_x", 1f) != 1f

        val isComplete = prefs.getBoolean("calibration_complete", false)
        val lastTime = prefs.getLong("calibration_complete_time", 0L)

        // Calculate quality score (0-100)
        var quality = 100f
        if (gyroCal && abs(prefs.getFloat("gyro_offset_x", 0f)) > 0.5f) quality -= 10
        if (accelCal && (abs(prefs.getFloat("accel_scale_x", 1f) - 1f) > 0.2f)) quality -= 10
        if (magCal && (abs(prefs.getFloat("mag_scale_x", 1f) - 1f) > 0.3f)) quality -= 10

        return CalibrationStatus(
            gyroCalibrated = gyroCal,
            accelCalibrated = accelCal,
            magCalibrated = magCal,
            allCalibrated = isComplete,
            progress = if (isComplete) 100 else 0,
            confidence = quality / 100f,
            lastCalibrationTime = lastTime
        )
    }

    override suspend fun importCalibrationData(data: String): Boolean {
        return try {
            val lines = data.lines()
            if (lines.firstOrNull() != "AIRMOUSE_CALIBRATION_DATA") return false

            lines.forEach { line ->
                val parts = line.split("=")
                if (parts.size == 2) {
                    val key = parts[0]
                    val value = parts[1]
                    when (key) {
                        "gyro_x" -> prefs.putFloat("gyro_offset_x", value.toFloat())
                        "gyro_y" -> prefs.putFloat("gyro_offset_y", value.toFloat())
                        "gyro_z" -> prefs.putFloat("gyro_offset_z", value.toFloat())
                        "accel_offset_x" -> prefs.putFloat("accel_offset_x", value.toFloat())
                        "accel_offset_y" -> prefs.putFloat("accel_offset_y", value.toFloat())
                        "accel_offset_z" -> prefs.putFloat("accel_offset_z", value.toFloat())
                        "accel_scale_x" -> prefs.putFloat("accel_scale_x", value.toFloat())
                        "accel_scale_y" -> prefs.putFloat("accel_scale_y", value.toFloat())
                        "accel_scale_z" -> prefs.putFloat("accel_scale_z", value.toFloat())
                        "mag_offset_x" -> prefs.putFloat("mag_offset_x", value.toFloat())
                        "mag_offset_y" -> prefs.putFloat("mag_offset_y", value.toFloat())
                        "mag_offset_z" -> prefs.putFloat("mag_offset_z", value.toFloat())
                        "mag_scale_x" -> prefs.putFloat("mag_scale_x", value.toFloat())
                        "mag_scale_y" -> prefs.putFloat("mag_scale_y", value.toFloat())
                        "mag_scale_z" -> prefs.putFloat("mag_scale_z", value.toFloat())
                        "calibration_complete" -> prefs.putBoolean("calibration_complete", value.toBoolean())
                    }
                }
            }
            true
        } catch (_: Exception) {
            // Using underscore for unused exception is cleaner in Kotlin
            false
        }
    }
}