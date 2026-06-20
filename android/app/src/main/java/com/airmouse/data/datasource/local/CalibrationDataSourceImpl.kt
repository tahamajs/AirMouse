// app/src/main/java/com/airmouse/data/datasource/local/CalibrationDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ICalibrationDataSource {

    // ==========================================
    // Complete Calibration Data
    // ==========================================

    override suspend fun saveCalibrationData(data: CalibrationData) {
        withContext(Dispatchers.IO) {
            saveGyroBias(
                data.gyroBias.offsetX,
                data.gyroBias.offsetY,
                data.gyroBias.offsetZ
            )
            saveAccelOffset(
                data.accelOffset.offsetX,
                data.accelOffset.offsetY,
                data.accelOffset.offsetZ
            )
            saveAccelScale(
                data.accelOffset.scaleX,
                data.accelOffset.scaleY,
                data.accelOffset.scaleZ
            )
            saveMagOffset(
                data.magOffset.offsetX,
                data.magOffset.offsetY,
                data.magOffset.offsetZ
            )
            saveMagScale(
                data.magOffset.scaleX,
                data.magOffset.scaleY,
                data.magOffset.scaleZ
            )
            setCalibrationStatus(
                if (data.isCalibrated) CalibrationStatus.COMPLETED else CalibrationStatus.NOT_STARTED
            )
            setCalibrationQuality(data.quality)
            setCalibrationTimestamp(data.timestamp)
        }
    }

    override suspend fun getCalibrationData(): CalibrationData {
        return withContext(Dispatchers.IO) {
            val (gx, gy, gz) = getGyroBias()
            val (aox, aoy, aoz) = getAccelOffset()
            val (asx, asy, asz) = getAccelScale()
            val (mox, moy, moz) = getMagOffset()
            val (msx, msy, msz) = getMagScale()

            CalibrationData(
                gyroBias = SensorCalibrationData(
                    offsetX = gx,
                    offsetY = gy,
                    offsetZ = gz
                ),
                accelOffset = SensorCalibrationData(
                    offsetX = aox,
                    offsetY = aoy,
                    offsetZ = aoz,
                    scaleX = asx,
                    scaleY = asy,
                    scaleZ = asz
                ),
                magOffset = SensorCalibrationData(
                    offsetX = mox,
                    offsetY = moy,
                    offsetZ = moz,
                    scaleX = msx,
                    scaleY = msy,
                    scaleZ = msz
                ),
                isCalibrated = isCalibrationComplete(),
                quality = getCalibrationQuality(),
                timestamp = getCalibrationTimestamp()
            )
        }
    }

    // ==========================================
    // Gyroscope
    // ==========================================

    override suspend fun saveGyroBias(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("gyro_bias_x", x)
            prefs.putFloat("gyro_bias_y", y)
            prefs.putFloat("gyro_bias_z", z)
        }
    }

    override suspend fun getGyroBias(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat("gyro_bias_x", 0f),
                prefs.getFloat("gyro_bias_y", 0f),
                prefs.getFloat("gyro_bias_z", 0f)
            )
        }
    }

    override suspend fun saveGyroVariance(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("gyro_variance_x", x)
            prefs.putFloat("gyro_variance_y", y)
            prefs.putFloat("gyro_variance_z", z)
        }
    }

    override suspend fun getGyroVariance(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat("gyro_variance_x", 0f),
                prefs.getFloat("gyro_variance_y", 0f),
                prefs.getFloat("gyro_variance_z", 0f)
            )
        }
    }

    override suspend fun saveGyroSampleCount(count: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt("gyro_samples", count)
        }
    }

    override suspend fun getGyroSampleCount(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt("gyro_samples", 0)
        }
    }

    // ==========================================
    // Accelerometer
    // ==========================================

    override suspend fun saveAccelOffset(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("accel_offset_x", x)
            prefs.putFloat("accel_offset_y", y)
            prefs.putFloat("accel_offset_z", z)
        }
    }

    override suspend fun getAccelOffset(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat("accel_offset_x", 0f),
                prefs.getFloat("accel_offset_y", 0f),
                prefs.getFloat("accel_offset_z", 0f)
            )
        }
    }

    override suspend fun saveAccelScale(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("accel_scale_x", x)
            prefs.putFloat("accel_scale_y", y)
            prefs.putFloat("accel_scale_z", z)
        }
    }

    override suspend fun getAccelScale(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat("accel_scale_x", 1f),
                prefs.getFloat("accel_scale_y", 1f),
                prefs.getFloat("accel_scale_z", 1f)
            )
        }
    }

    override suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("accel_pos_${position}_x", values.first)
            prefs.putFloat("accel_pos_${position}_y", values.second)
            prefs.putFloat("accel_pos_${position}_z", values.third)
        }
    }

    override suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>? {
        return withContext(Dispatchers.IO) {
            val x = prefs.getFloat("accel_pos_${position}_x", Float.NaN)
            val y = prefs.getFloat("accel_pos_${position}_y", Float.NaN)
            val z = prefs.getFloat("accel_pos_${position}_z", Float.NaN)

            if (x.isNaN() || y.isNaN() || z.isNaN()) {
                null
            } else {
                Triple(x, y, z)
            }
        }
    }

    override suspend fun getAllAccelPositions(): Map<Int, Triple<Float, Float, Float>> {
        return withContext(Dispatchers.IO) {
            val result = mutableMapOf<Int, Triple<Float, Float, Float>>()
            for (i in 0..5) {
                getAccelPosition(i)?.let { result[i] = it }
            }
            result
        }
    }

    override suspend fun saveAccelPositionsCompleted(count: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt("accel_positions_completed", count)
        }
    }

    override suspend fun getAccelPositionsCompleted(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt("accel_positions_completed", 0)
        }
    }

    // ==========================================
    // Magnetometer
    // ==========================================

    override suspend fun saveMagOffset(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("mag_offset_x", x)
            prefs.putFloat("mag_offset_y", y)
            prefs.putFloat("mag_offset_z", z)
        }
    }

    override suspend fun getMagOffset(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat("mag_offset_x", 0f),
                prefs.getFloat("mag_offset_y", 0f),
                prefs.getFloat("mag_offset_z", 0f)
            )
        }
    }

    override suspend fun saveMagScale(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat("mag_scale_x", x)
            prefs.putFloat("mag_scale_y", y)
            prefs.putFloat("mag_scale_z", z)
        }
    }

    override suspend fun getMagScale(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat("mag_scale_x", 1f),
                prefs.getFloat("mag_scale_y", 1f),
                prefs.getFloat("mag_scale_z", 1f)
            )
        }
    }

    override suspend fun saveMagSampleCount(count: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt("mag_samples", count)
        }
    }

    override suspend fun getMagSampleCount(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt("mag_samples", 0)
        }
    }

    // ==========================================
    // Calibration Status & Progress
    // ==========================================

    override suspend fun setCalibrationStatus(status: CalibrationStatus) {
        withContext(Dispatchers.IO) {
            prefs.putString("calibration_status", status.name)
        }
    }

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        return withContext(Dispatchers.IO) {
            val status = prefs.getString("calibration_status", "NOT_STARTED")
            try {
                CalibrationStatus.valueOf(status)
            } catch (e: Exception) {
                CalibrationStatus.NOT_STARTED
            }
        }
    }

    override suspend fun setCalibrationQuality(quality: CalibrationQuality) {
        withContext(Dispatchers.IO) {
            prefs.putString("calibration_quality", quality.name)
        }
    }

    override suspend fun getCalibrationQuality(): CalibrationQuality {
        return withContext(Dispatchers.IO) {
            val quality = prefs.getString("calibration_quality", "UNKNOWN")
            try {
                CalibrationQuality.valueOf(quality)
            } catch (e: Exception) {
                CalibrationQuality.UNKNOWN
            }
        }
    }

    override suspend fun setCalibrationProgress(progress: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt("calibration_progress", progress.coerceIn(0, 100))
        }
    }

    override suspend fun getCalibrationProgress(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt("calibration_progress", 0)
        }
    }

    override suspend fun setCurrentStep(step: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt("calibration_current_step", step.coerceIn(0, 3))
        }
    }

    override suspend fun getCurrentStep(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt("calibration_current_step", 0)
        }
    }

    override suspend fun setCalibrationComplete(complete: Boolean) {
        withContext(Dispatchers.IO) {
            prefs.putBoolean("calibration_complete", complete)
        }
    }

    override suspend fun isCalibrationComplete(): Boolean {
        return withContext(Dispatchers.IO) {
            prefs.getBoolean("calibration_complete", false)
        }
    }

    override suspend fun setCalibrationTimestamp(timestamp: Long) {
        withContext(Dispatchers.IO) {
            prefs.putLong("calibration_timestamp", timestamp)
        }
    }

    override suspend fun getCalibrationTimestamp(): Long {
        return withContext(Dispatchers.IO) {
            prefs.getLong("calibration_timestamp", 0L)
        }
    }

    // ==========================================
    // Helper Methods
    // ==========================================

    override suspend fun hasCalibrationData(): Boolean {
        return withContext(Dispatchers.IO) {
            val (gx, gy, gz) = getGyroBias()
            val (aox, aoy, aoz) = getAccelOffset()
            val (mox, moy, moz) = getMagOffset()

            gx != 0f || gy != 0f || gz != 0f ||
                    aox != 0f || aoy != 0f || aoz != 0f ||
                    mox != 0f || moy != 0f || moz != 0f
        }
    }

    override suspend fun resetAll() {
        withContext(Dispatchers.IO) {
            resetGyro()
            resetAccel()
            resetMag()

            prefs.remove("calibration_status")
            prefs.remove("calibration_quality")
            prefs.remove("calibration_progress")
            prefs.remove("calibration_complete")
            prefs.remove("calibration_timestamp")
            prefs.remove("calibration_current_step")
            prefs.remove("accel_positions_completed")

            // Remove all accel positions
            for (i in 0..5) {
                prefs.remove("accel_pos_${i}_x")
                prefs.remove("accel_pos_${i}_y")
                prefs.remove("accel_pos_${i}_z")
            }
        }
    }

    override suspend fun resetGyro() {
        withContext(Dispatchers.IO) {
            prefs.remove("gyro_bias_x")
            prefs.remove("gyro_bias_y")
            prefs.remove("gyro_bias_z")
            prefs.remove("gyro_variance_x")
            prefs.remove("gyro_variance_y")
            prefs.remove("gyro_variance_z")
            prefs.remove("gyro_samples")
        }
    }

    override suspend fun resetAccel() {
        withContext(Dispatchers.IO) {
            prefs.remove("accel_offset_x")
            prefs.remove("accel_offset_y")
            prefs.remove("accel_offset_z")
            prefs.remove("accel_scale_x")
            prefs.remove("accel_scale_y")
            prefs.remove("accel_scale_z")

            for (i in 0..5) {
                prefs.remove("accel_pos_${i}_x")
                prefs.remove("accel_pos_${i}_y")
                prefs.remove("accel_pos_${i}_z")
            }
            prefs.remove("accel_positions_completed")
        }
    }

    override suspend fun resetMag() {
        withContext(Dispatchers.IO) {
            prefs.remove("mag_offset_x")
            prefs.remove("mag_offset_y")
            prefs.remove("mag_offset_z")
            prefs.remove("mag_scale_x")
            prefs.remove("mag_scale_y")
            prefs.remove("mag_scale_z")
            prefs.remove("mag_samples")
        }
    }

    override suspend fun getCalibrationSummary(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val (gx, gy, gz) = getGyroBias()
            val (aox, aoy, aoz) = getAccelOffset()
            val (mox, moy, moz) = getMagOffset()

            mapOf(
                "gyro_bias" to mapOf("x" to gx, "y" to gy, "z" to gz),
                "accel_offset" to mapOf("x" to aox, "y" to aoy, "z" to aoz),
                "mag_offset" to mapOf("x" to mox, "y" to moy, "z" to moz),
                "status" to getCalibrationStatus().name,
                "quality" to getCalibrationQuality().name,
                "progress" to getCalibrationProgress(),
                "is_complete" to isCalibrationComplete(),
                "timestamp" to getCalibrationTimestamp()
            )
        }
    }
}