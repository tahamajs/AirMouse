// app/src/main/java/com/airmouse/data/datasource/local/CalibrationDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.utils.PreferencesKeys
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ICalibrationDataSource {

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
            prefs.putFloat(PreferencesKeys.KEY_GYRO_BIAS_X, x)
            prefs.putFloat(PreferencesKeys.KEY_GYRO_BIAS_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_GYRO_BIAS_Z, z)
        }
    }

    override suspend fun getGyroBias(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat(PreferencesKeys.KEY_GYRO_BIAS_X, 0f),
                prefs.getFloat(PreferencesKeys.KEY_GYRO_BIAS_Y, 0f),
                prefs.getFloat(PreferencesKeys.KEY_GYRO_BIAS_Z, 0f)
            )
        }
    }

    override suspend fun saveGyroVariance(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.KEY_GYRO_VARIANCE_X, x)
            prefs.putFloat(PreferencesKeys.KEY_GYRO_VARIANCE_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_GYRO_VARIANCE_Z, z)
        }
    }

    override suspend fun getGyroVariance(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat(PreferencesKeys.KEY_GYRO_VARIANCE_X, 0f),
                prefs.getFloat(PreferencesKeys.KEY_GYRO_VARIANCE_Y, 0f),
                prefs.getFloat(PreferencesKeys.KEY_GYRO_VARIANCE_Z, 0f)
            )
        }
    }

    override suspend fun saveGyroSampleCount(count: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt(PreferencesKeys.KEY_GYRO_SAMPLES, count)
        }
    }

    override suspend fun getGyroSampleCount(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt(PreferencesKeys.KEY_GYRO_SAMPLES, 0)
        }
    }

    // ==========================================
    // Accelerometer
    // ==========================================

    override suspend fun saveAccelOffset(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.KEY_ACCEL_OFFSET_X, x)
            prefs.putFloat(PreferencesKeys.KEY_ACCEL_OFFSET_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_ACCEL_OFFSET_Z, z)
        }
    }

    override suspend fun getAccelOffset(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat(PreferencesKeys.KEY_ACCEL_OFFSET_X, 0f),
                prefs.getFloat(PreferencesKeys.KEY_ACCEL_OFFSET_Y, 0f),
                prefs.getFloat(PreferencesKeys.KEY_ACCEL_OFFSET_Z, 0f)
            )
        }
    }

    override suspend fun saveAccelScale(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.KEY_ACCEL_SCALE_X, x)
            prefs.putFloat(PreferencesKeys.KEY_ACCEL_SCALE_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_ACCEL_SCALE_Z, z)
        }
    }

    override suspend fun getAccelScale(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat(PreferencesKeys.KEY_ACCEL_SCALE_X, 1f),
                prefs.getFloat(PreferencesKeys.KEY_ACCEL_SCALE_Y, 1f),
                prefs.getFloat(PreferencesKeys.KEY_ACCEL_SCALE_Z, 1f)
            )
        }
    }

    override suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.getAccelPositionKey(position, "x"), values.first)
            prefs.putFloat(PreferencesKeys.getAccelPositionKey(position, "y"), values.second)
            prefs.putFloat(PreferencesKeys.getAccelPositionKey(position, "z"), values.third)
        }
    }

    override suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>? {
        return withContext(Dispatchers.IO) {
            val x = prefs.getFloat(PreferencesKeys.getAccelPositionKey(position, "x"), Float.NaN)
            val y = prefs.getFloat(PreferencesKeys.getAccelPositionKey(position, "y"), Float.NaN)
            val z = prefs.getFloat(PreferencesKeys.getAccelPositionKey(position, "z"), Float.NaN)
            if (x.isNaN() || y.isNaN() || z.isNaN()) null else Triple(x, y, z)
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
            prefs.putInt(PreferencesKeys.KEY_ACCEL_POSITIONS_COMPLETED, count)
        }
    }

    override suspend fun getAccelPositionsCompleted(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt(PreferencesKeys.KEY_ACCEL_POSITIONS_COMPLETED, 0)
        }
    }

    // ==========================================
    // Magnetometer
    // ==========================================

    override suspend fun saveMagOffset(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.KEY_MAG_OFFSET_X, x)
            prefs.putFloat(PreferencesKeys.KEY_MAG_OFFSET_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_MAG_OFFSET_Z, z)
        }
    }

    override suspend fun getMagOffset(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat(PreferencesKeys.KEY_MAG_OFFSET_X, 0f),
                prefs.getFloat(PreferencesKeys.KEY_MAG_OFFSET_Y, 0f),
                prefs.getFloat(PreferencesKeys.KEY_MAG_OFFSET_Z, 0f)
            )
        }
    }

    override suspend fun saveMagScale(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.KEY_MAG_SCALE_X, x)
            prefs.putFloat(PreferencesKeys.KEY_MAG_SCALE_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_MAG_SCALE_Z, z)
        }
    }

    override suspend fun getMagScale(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            Triple(
                prefs.getFloat(PreferencesKeys.KEY_MAG_SCALE_X, 1f),
                prefs.getFloat(PreferencesKeys.KEY_MAG_SCALE_Y, 1f),
                prefs.getFloat(PreferencesKeys.KEY_MAG_SCALE_Z, 1f)
            )
        }
    }

    override suspend fun saveMagSampleCount(count: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt(PreferencesKeys.KEY_MAG_SAMPLES, count)
        }
    }

    override suspend fun getMagSampleCount(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt(PreferencesKeys.KEY_MAG_SAMPLES, 0)
        }
    }

    // ==========================================
    // Calibration Status & Progress
    // ==========================================

    override suspend fun setCalibrationStatus(status: CalibrationStatus) {
        withContext(Dispatchers.IO) {
            prefs.putString(PreferencesKeys.KEY_CALIBRATION_STATUS, status.name)
        }
    }

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        return withContext(Dispatchers.IO) {
            val status = prefs.getString(PreferencesKeys.KEY_CALIBRATION_STATUS, "NOT_STARTED")
            try {
                CalibrationStatus.valueOf(status)
            } catch (e: Exception) {
                CalibrationStatus.NOT_STARTED
            }
        }
    }

    override suspend fun setCalibrationQuality(quality: CalibrationQuality) {
        withContext(Dispatchers.IO) {
            prefs.putString(PreferencesKeys.KEY_CALIBRATION_QUALITY, quality.name)
        }
    }

    override suspend fun getCalibrationQuality(): CalibrationQuality {
        return withContext(Dispatchers.IO) {
            val quality = prefs.getString(PreferencesKeys.KEY_CALIBRATION_QUALITY, "UNKNOWN")
            try {
                CalibrationQuality.valueOf(quality)
            } catch (e: Exception) {
                CalibrationQuality.UNKNOWN
            }
        }
    }

    override suspend fun setCalibrationProgress(progress: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt(PreferencesKeys.KEY_CALIBRATION_PROGRESS, progress.coerceIn(0, 100))
        }
    }

    override suspend fun getCalibrationProgress(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt(PreferencesKeys.KEY_CALIBRATION_PROGRESS, 0)
        }
    }

    override suspend fun setCurrentStep(step: Int) {
        withContext(Dispatchers.IO) {
            prefs.putInt(PreferencesKeys.KEY_CALIBRATION_CURRENT_STEP, step.coerceIn(0, 3))
        }
    }

    override suspend fun getCurrentStep(): Int {
        return withContext(Dispatchers.IO) {
            prefs.getInt(PreferencesKeys.KEY_CALIBRATION_CURRENT_STEP, 0)
        }
    }

    override suspend fun setCalibrationComplete(complete: Boolean) {
        withContext(Dispatchers.IO) {
            prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_COMPLETE, complete)
        }
    }

    override suspend fun isCalibrationComplete(): Boolean {
        return withContext(Dispatchers.IO) {
            prefs.getBoolean(PreferencesKeys.KEY_CALIBRATION_COMPLETE, false)
        }
    }

    override suspend fun setCalibrationTimestamp(timestamp: Long) {
        withContext(Dispatchers.IO) {
            prefs.putLong(PreferencesKeys.KEY_CALIBRATION_TIMESTAMP, timestamp)
        }
    }

    override suspend fun getCalibrationTimestamp(): Long {
        return withContext(Dispatchers.IO) {
            prefs.getLong(PreferencesKeys.KEY_CALIBRATION_TIMESTAMP, 0L)
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
            prefs.remove(PreferencesKeys.KEY_CALIBRATION_STATUS)
            prefs.remove(PreferencesKeys.KEY_CALIBRATION_QUALITY)
            prefs.remove(PreferencesKeys.KEY_CALIBRATION_PROGRESS)
            prefs.remove(PreferencesKeys.KEY_CALIBRATION_COMPLETE)
            prefs.remove(PreferencesKeys.KEY_CALIBRATION_TIMESTAMP)
            prefs.remove(PreferencesKeys.KEY_CALIBRATION_CURRENT_STEP)
            prefs.remove(PreferencesKeys.KEY_ACCEL_POSITIONS_COMPLETED)
            for (i in 0..5) {
                prefs.remove(PreferencesKeys.getAccelPositionKey(i, "x"))
                prefs.remove(PreferencesKeys.getAccelPositionKey(i, "y"))
                prefs.remove(PreferencesKeys.getAccelPositionKey(i, "z"))
            }
        }
    }

    override suspend fun resetGyro() {
        withContext(Dispatchers.IO) {
            prefs.remove(PreferencesKeys.KEY_GYRO_BIAS_X)
            prefs.remove(PreferencesKeys.KEY_GYRO_BIAS_Y)
            prefs.remove(PreferencesKeys.KEY_GYRO_BIAS_Z)
            prefs.remove(PreferencesKeys.KEY_GYRO_VARIANCE_X)
            prefs.remove(PreferencesKeys.KEY_GYRO_VARIANCE_Y)
            prefs.remove(PreferencesKeys.KEY_GYRO_VARIANCE_Z)
            prefs.remove(PreferencesKeys.KEY_GYRO_SAMPLES)
            prefs.remove(PreferencesKeys.KEY_GYRO_CALIBRATED)
        }
    }

    override suspend fun resetAccel() {
        withContext(Dispatchers.IO) {
            prefs.remove(PreferencesKeys.KEY_ACCEL_OFFSET_X)
            prefs.remove(PreferencesKeys.KEY_ACCEL_OFFSET_Y)
            prefs.remove(PreferencesKeys.KEY_ACCEL_OFFSET_Z)
            prefs.remove(PreferencesKeys.KEY_ACCEL_SCALE_X)
            prefs.remove(PreferencesKeys.KEY_ACCEL_SCALE_Y)
            prefs.remove(PreferencesKeys.KEY_ACCEL_SCALE_Z)
            prefs.remove(PreferencesKeys.KEY_ACCEL_CALIBRATED)
            prefs.remove(PreferencesKeys.KEY_ACCEL_POSITIONS_COMPLETED)
            for (i in 0..5) {
                prefs.remove(PreferencesKeys.getAccelPositionKey(i, "x"))
                prefs.remove(PreferencesKeys.getAccelPositionKey(i, "y"))
                prefs.remove(PreferencesKeys.getAccelPositionKey(i, "z"))
            }
        }
    }

    override suspend fun resetMag() {
        withContext(Dispatchers.IO) {
            prefs.remove(PreferencesKeys.KEY_MAG_OFFSET_X)
            prefs.remove(PreferencesKeys.KEY_MAG_OFFSET_Y)
            prefs.remove(PreferencesKeys.KEY_MAG_OFFSET_Z)
            prefs.remove(PreferencesKeys.KEY_MAG_SCALE_X)
            prefs.remove(PreferencesKeys.KEY_MAG_SCALE_Y)
            prefs.remove(PreferencesKeys.KEY_MAG_SCALE_Z)
            prefs.remove(PreferencesKeys.KEY_MAG_SAMPLES)
            prefs.remove(PreferencesKeys.KEY_MAG_CALIBRATED)
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
