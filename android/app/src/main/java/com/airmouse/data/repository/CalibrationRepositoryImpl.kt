// app/src/main/java/com/airmouse/data/repository/CalibrationRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepositoryImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ICalibrationRepository {

    private val _calibrationComplete = MutableStateFlow(prefs.getBoolean("calibration_complete", false))

    override suspend fun saveGyroBias(bias: GyroBias) {
        prefs.putFloat("gyro_offset_x", bias.offsetX)
        prefs.putFloat("gyro_offset_y", bias.offsetY)
        prefs.putFloat("gyro_offset_z", bias.offsetZ)
        prefs.putLong("gyro_calibration_time", bias.timestamp)
    }

    override suspend fun getGyroBias(): GyroBias {
        return GyroBias(
            offsetX = prefs.getFloat("gyro_offset_x", 0f),
            offsetY = prefs.getFloat("gyro_offset_y", 0f),
            offsetZ = prefs.getFloat("gyro_offset_z", 0f),
            timestamp = prefs.getLong("gyro_calibration_time", 0)
        )
    }

    override suspend fun saveAccelCalibration(calibration: AccelCalibration) {
        prefs.putFloat("accel_offset_x", calibration.offsetX)
        prefs.putFloat("accel_offset_y", calibration.offsetY)
        prefs.putFloat("accel_offset_z", calibration.offsetZ)
        prefs.putFloat("accel_scale_x", calibration.scaleX)
        prefs.putFloat("accel_scale_y", calibration.scaleY)
        prefs.putFloat("accel_scale_z", calibration.scaleZ)
        prefs.putLong("accel_calibration_time", calibration.timestamp)
    }

    override suspend fun getAccelCalibration(): AccelCalibration {
        return AccelCalibration(
            offsetX = prefs.getFloat("accel_offset_x", 0f),
            offsetY = prefs.getFloat("accel_offset_y", 0f),
            offsetZ = prefs.getFloat("accel_offset_z", 0f),
            scaleX = prefs.getFloat("accel_scale_x", 1f),
            scaleY = prefs.getFloat("accel_scale_y", 1f),
            scaleZ = prefs.getFloat("accel_scale_z", 1f),
            timestamp = prefs.getLong("accel_calibration_time", 0)
        )
    }

    override suspend fun saveMagCalibration(calibration: MagCalibration) {
        prefs.putFloat("mag_offset_x", calibration.offsetX)
        prefs.putFloat("mag_offset_y", calibration.offsetY)
        prefs.putFloat("mag_offset_z", calibration.offsetZ)
        prefs.putFloat("mag_scale_x", calibration.scaleX)
        prefs.putFloat("mag_scale_y", calibration.scaleY)
        prefs.putFloat("mag_scale_z", calibration.scaleZ)
        prefs.putLong("mag_calibration_time", calibration.timestamp)
    }

    override suspend fun getMagCalibration(): MagCalibration {
        return MagCalibration(
            offsetX = prefs.getFloat("mag_offset_x", 0f),
            offsetY = prefs.getFloat("mag_offset_y", 0f),
            offsetZ = prefs.getFloat("mag_offset_z", 0f),
            scaleX = prefs.getFloat("mag_scale_x", 1f),
            scaleY = prefs.getFloat("mag_scale_y", 1f),
            scaleZ = prefs.getFloat("mag_scale_z", 1f),
            timestamp = prefs.getLong("mag_calibration_time", 0)
        )
    }

    override suspend fun markCalibrationComplete() {
        prefs.putBoolean("calibration_complete", true)
        prefs.putLong("calibration_complete_time", System.currentTimeMillis())
        _calibrationComplete.value = true
    }

    override fun isCalibrationComplete(): Flow<Boolean> {
        return _calibrationComplete.asStateFlow()
    }

    override suspend fun resetCalibration() {
        prefs.putBoolean("calibration_complete", false)
        prefs.putFloat("gyro_offset_x", 0f)
        prefs.putFloat("gyro_offset_y", 0f)
        prefs.putFloat("gyro_offset_z", 0f)
        prefs.putFloat("accel_offset_x", 0f)
        prefs.putFloat("accel_offset_y", 0f)
        prefs.putFloat("accel_offset_z", 0f)
        prefs.putFloat("accel_scale_x", 1f)
        prefs.putFloat("accel_scale_y", 1f)
        prefs.putFloat("accel_scale_z", 1f)
        prefs.putFloat("mag_offset_x", 0f)
        prefs.putFloat("mag_offset_y", 0f)
        prefs.putFloat("mag_offset_z", 0f)
        prefs.putFloat("mag_scale_x", 1f)
        prefs.putFloat("mag_scale_y", 1f)
        prefs.putFloat("mag_scale_z", 1f)
        _calibrationComplete.value = false
    }

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        val gyroCal = prefs.getFloat("gyro_offset_x", 0f) != 0f ||
                prefs.getFloat("gyro_offset_y", 0f) != 0f ||
                prefs.getFloat("gyro_offset_z", 0f) != 0f

        val accelCal = prefs.getFloat("accel_offset_x", 0f) != 0f &&
                prefs.getFloat("accel_scale_x", 1f) != 1f

        val magCal = prefs.getFloat("mag_offset_x", 0f) != 0f &&
                prefs.getFloat("mag_scale_x", 1f) != 1f

        val isComplete = prefs.getBoolean("calibration_complete", false)
        val lastTime = prefs.getLong("calibration_complete_time", 0)

        // Calculate quality score (0-100)
        var quality = 100f
        if (gyroCal && abs(prefs.getFloat("gyro_offset_x", 0f)) > 0.5f) quality -= 10
        if (accelCal && (abs(prefs.getFloat("accel_scale_x", 1f) - 1f) > 0.2f)) quality -= 10
        if (magCal && (abs(prefs.getFloat("mag_scale_x", 1f) - 1f) > 0.3f)) quality -= 10

        return CalibrationStatus(
            isGyroCalibrated = gyroCal,
            isAccelCalibrated = accelCal,
            isMagCalibrated = magCal,
            isComplete = isComplete,
            lastCalibrationTime = lastTime,
            quality = quality.coerceIn(0f, 100f)
        )
    }

    override suspend fun exportCalibrationData(): String {
        val gyro = getGyroBias()
        val accel = getAccelCalibration()
        val mag = getMagCalibration()

        return buildString {
            appendLine("AIRMOUSE_CALIBRATION_DATA")
            appendLine("version=1")
            appendLine("timestamp=${System.currentTimeMillis()}")
            appendLine("gyro_x=${gyro.offsetX}")
            appendLine("gyro_y=${gyro.offsetY}")
            appendLine("gyro_z=${gyro.offsetZ}")
            appendLine("accel_offset_x=${accel.offsetX}")
            appendLine("accel_offset_y=${accel.offsetY}")
            appendLine("accel_offset_z=${accel.offsetZ}")
            appendLine("accel_scale_x=${accel.scaleX}")
            appendLine("accel_scale_y=${accel.scaleY}")
            appendLine("accel_scale_z=${accel.scaleZ}")
            appendLine("mag_offset_x=${mag.offsetX}")
            appendLine("mag_offset_y=${mag.offsetY}")
            appendLine("mag_offset_z=${mag.offsetZ}")
            appendLine("mag_scale_x=${mag.scaleX}")
            appendLine("mag_scale_y=${mag.scaleY}")
            appendLine("mag_scale_z=${mag.scaleZ}")
            appendLine("calibration_complete=${prefs.getBoolean("calibration_complete", false)}")
        }
    }

    override suspend fun importCalibrationData(data: String): Boolean {
        return try {
            val lines = data.lines()
            if (lines.firstOrNull() != "AIRMOUSE_CALIBRATION_DATA") return false

            for (line in lines) {
                when {
                    line.startsWith("gyro_x=") -> prefs.putFloat("gyro_offset_x", line.substringAfter("=").toFloat())
                    line.startsWith("gyro_y=") -> prefs.putFloat("gyro_offset_y", line.substringAfter("=").toFloat())
                    line.startsWith("gyro_z=") -> prefs.putFloat("gyro_offset_z", line.substringAfter("=").toFloat())
                    line.startsWith("accel_offset_x=") -> prefs.putFloat("accel_offset_x", line.substringAfter("=").toFloat())
                    line.startsWith("accel_offset_y=") -> prefs.putFloat("accel_offset_y", line.substringAfter("=").toFloat())
                    line.startsWith("accel_offset_z=") -> prefs.putFloat("accel_offset_z", line.substringAfter("=").toFloat())
                    line.startsWith("accel_scale_x=") -> prefs.putFloat("accel_scale_x", line.substringAfter("=").toFloat())
                    line.startsWith("accel_scale_y=") -> prefs.putFloat("accel_scale_y", line.substringAfter("=").toFloat())
                    line.startsWith("accel_scale_z=") -> prefs.putFloat("accel_scale_z", line.substringAfter("=").toFloat())
                    line.startsWith("mag_offset_x=") -> prefs.putFloat("mag_offset_x", line.substringAfter("=").toFloat())
                    line.startsWith("mag_offset_y=") -> prefs.putFloat("mag_offset_y", line.substringAfter("=").toFloat())
                    line.startsWith("mag_offset_z=") -> prefs.putFloat("mag_offset_z", line.substringAfter("=").toFloat())
                    line.startsWith("mag_scale_x=") -> prefs.putFloat("mag_scale_x", line.substringAfter("=").toFloat())
                    line.startsWith("mag_scale_y=") -> prefs.putFloat("mag_scale_y", line.substringAfter("=").toFloat())
                    line.startsWith("mag_scale_z=") -> prefs.putFloat("mag_scale_z", line.substringAfter("=").toFloat())
                    line.startsWith("calibration_complete=") -> prefs.putBoolean("calibration_complete", line.substringAfter("=").toBoolean())
                }
            }
            true
        } catch (e: Exception) {
            false
        }
    }
}