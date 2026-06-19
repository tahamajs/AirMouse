// app/src/main/java/com/airmouse/data/datasource/local/CalibrationDataSourceImpl.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.utils.PreferencesManager
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ICalibrationDataSource {

    override suspend fun saveCalibrationData(data: CalibrationData) {
        saveGyroBias(data.gyroBias.offsetX, data.gyroBias.offsetY, data.gyroBias.offsetZ)
        saveAccelOffset(data.accelOffset.offsetX, data.accelOffset.offsetY, data.accelOffset.offsetZ)
        saveAccelScale(data.accelOffset.scaleX, data.accelOffset.scaleY, data.accelOffset.scaleZ)
        saveMagOffset(data.magOffset.offsetX, data.magOffset.offsetY, data.magOffset.offsetZ)
        saveMagScale(data.magOffset.scaleX, data.magOffset.scaleY, data.magOffset.scaleZ)
        setCalibrationStatus(if (data.isCalibrated) CalibrationStatus.COMPLETED else CalibrationStatus.NOT_STARTED)
        setCalibrationQuality(data.quality)
    }

    override suspend fun getCalibrationData(): CalibrationData {
        val (gx, gy, gz) = getGyroBias()
        val (aox, aoy, aoz) = getAccelOffset()
        val (asx, asy, asz) = getAccelScale()
        val (mox, moy, moz) = getMagOffset()
        val (msx, msy, msz) = getMagScale()

        return CalibrationData(
            gyroBias = SensorCalibrationData(offsetX = gx, offsetY = gy, offsetZ = gz),
            accelOffset = SensorCalibrationData(
                offsetX = aox, offsetY = aoy, offsetZ = aoz,
                scaleX = asx, scaleY = asy, scaleZ = asz
            ),
            magOffset = SensorCalibrationData(
                offsetX = mox, offsetY = moy, offsetZ = moz,
                scaleX = msx, scaleY = msy, scaleZ = msz
            ),
            isCalibrated = getCalibrationStatus() == CalibrationStatus.COMPLETED,
            quality = getCalibrationQuality()
        )
    }

    override suspend fun saveGyroBias(x: Float, y: Float, z: Float) {
        prefs.putFloat("gyro_bias_x", x)
        prefs.putFloat("gyro_bias_y", y)
        prefs.putFloat("gyro_bias_z", z)
    }

    override suspend fun getGyroBias(): Triple<Float, Float, Float> {
        return Triple(
            prefs.getFloat("gyro_bias_x", 0f),
            prefs.getFloat("gyro_bias_y", 0f),
            prefs.getFloat("gyro_bias_z", 0f)
        )
    }

    override suspend fun saveMagOffset(x: Float, y: Float, z: Float) {
        prefs.putFloat("mag_offset_x", x)
        prefs.putFloat("mag_offset_y", y)
        prefs.putFloat("mag_offset_z", z)
    }

    override suspend fun getMagOffset(): Triple<Float, Float, Float> {
        return Triple(
            prefs.getFloat("mag_offset_x", 0f),
            prefs.getFloat("mag_offset_y", 0f),
            prefs.getFloat("mag_offset_z", 0f)
        )
    }

    override suspend fun saveMagScale(x: Float, y: Float, z: Float) {
        prefs.putFloat("mag_scale_x", x)
        prefs.putFloat("mag_scale_y", y)
        prefs.putFloat("mag_scale_z", z)
    }

    override suspend fun getMagScale(): Triple<Float, Float, Float> {
        return Triple(
            prefs.getFloat("mag_scale_x", 1f),
            prefs.getFloat("mag_scale_y", 1f),
            prefs.getFloat("mag_scale_z", 1f)
        )
    }

    override suspend fun saveAccelOffset(x: Float, y: Float, z: Float) {
        prefs.putFloat("accel_offset_x", x)
        prefs.putFloat("accel_offset_y", y)
        prefs.putFloat("accel_offset_z", z)
    }

    override suspend fun getAccelOffset(): Triple<Float, Float, Float> {
        return Triple(
            prefs.getFloat("accel_offset_x", 0f),
            prefs.getFloat("accel_offset_y", 0f),
            prefs.getFloat("accel_offset_z", 0f)
        )
    }

    override suspend fun saveAccelScale(x: Float, y: Float, z: Float) {
        prefs.putFloat("accel_scale_x", x)
        prefs.putFloat("accel_scale_y", y)
        prefs.putFloat("accel_scale_z", z)
    }

    override suspend fun getAccelScale(): Triple<Float, Float, Float> {
        return Triple(
            prefs.getFloat("accel_scale_x", 1f),
            prefs.getFloat("accel_scale_y", 1f),
            prefs.getFloat("accel_scale_z", 1f)
        )
    }

    override suspend fun setCalibrationStatus(status: CalibrationStatus) {
        prefs.putString("calibration_status", status.name)
    }

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        val status = prefs.getString("calibration_status", "NOT_STARTED")
        return try {
            CalibrationStatus.valueOf(status)
        } catch (e: Exception) {
            CalibrationStatus.NOT_STARTED
        }
    }

    override suspend fun setCalibrationQuality(quality: CalibrationQuality) {
        prefs.putString("calibration_quality", quality.name)
    }

    override suspend fun getCalibrationQuality(): CalibrationQuality {
        val quality = prefs.getString("calibration_quality", "UNKNOWN")
        return try {
            CalibrationQuality.valueOf(quality)
        } catch (e: Exception) {
            CalibrationQuality.UNKNOWN
        }
    }

    override suspend fun setCalibrationProgress(progress: Int) {
        prefs.putInt("calibration_progress", progress)
    }

    override suspend fun getCalibrationProgress(): Int {
        return prefs.getInt("calibration_progress", 0)
    }

    override suspend fun resetAll() {
        prefs.remove("gyro_bias_x")
        prefs.remove("gyro_bias_y")
        prefs.remove("gyro_bias_z")
        prefs.remove("mag_offset_x")
        prefs.remove("mag_offset_y")
        prefs.remove("mag_offset_z")
        prefs.remove("mag_scale_x")
        prefs.remove("mag_scale_y")
        prefs.remove("mag_scale_z")
        prefs.remove("accel_offset_x")
        prefs.remove("accel_offset_y")
        prefs.remove("accel_offset_z")
        prefs.remove("accel_scale_x")
        prefs.remove("accel_scale_y")
        prefs.remove("accel_scale_z")
        prefs.remove("calibration_status")
        prefs.remove("calibration_quality")
        prefs.remove("calibration_progress")
        prefs.remove("calibration_complete")
    }
}
