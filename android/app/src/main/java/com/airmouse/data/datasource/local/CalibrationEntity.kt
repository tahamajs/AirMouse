// app/src/main/java/com/airmouse/data/datasource/local/CalibrationEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality

@Entity(tableName = "calibration")
data class CalibrationEntity(
    @PrimaryKey
    val id: String = "default",

    // Gyroscope Calibration
    @ColumnInfo(name = "gyro_bias_x")
    val gyroBiasX: Float = 0f,

    @ColumnInfo(name = "gyro_bias_y")
    val gyroBiasY: Float = 0f,

    @ColumnInfo(name = "gyro_bias_z")
    val gyroBiasZ: Float = 0f,

    @ColumnInfo(name = "gyro_variance_x")
    val gyroVarianceX: Float = 0f,

    @ColumnInfo(name = "gyro_variance_y")
    val gyroVarianceY: Float = 0f,

    @ColumnInfo(name = "gyro_variance_z")
    val gyroVarianceZ: Float = 0f,

    // Accelerometer Calibration
    @ColumnInfo(name = "accel_offset_x")
    val accelOffsetX: Float = 0f,

    @ColumnInfo(name = "accel_offset_y")
    val accelOffsetY: Float = 0f,

    @ColumnInfo(name = "accel_offset_z")
    val accelOffsetZ: Float = 0f,

    @ColumnInfo(name = "accel_scale_x")
    val accelScaleX: Float = 1f,

    @ColumnInfo(name = "accel_scale_y")
    val accelScaleY: Float = 1f,

    @ColumnInfo(name = "accel_scale_z")
    val accelScaleZ: Float = 1f,

    // Magnetometer Calibration
    @ColumnInfo(name = "mag_offset_x")
    val magOffsetX: Float = 0f,

    @ColumnInfo(name = "mag_offset_y")
    val magOffsetY: Float = 0f,

    @ColumnInfo(name = "mag_offset_z")
    val magOffsetZ: Float = 0f,

    @ColumnInfo(name = "mag_scale_x")
    val magScaleX: Float = 1f,

    @ColumnInfo(name = "mag_scale_y")
    val magScaleY: Float = 1f,

    @ColumnInfo(name = "mag_scale_z")
    val magScaleZ: Float = 1f,

    // Status
    @ColumnInfo(name = "is_calibrated")
    val isCalibrated: Boolean = false,

    @ColumnInfo(name = "calibration_quality")
    val calibrationQuality: String = "UNKNOWN",

    @ColumnInfo(name = "calibration_timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "calibration_version")
    val version: Int = 2,

    @ColumnInfo(name = "device_model")
    val deviceModel: String = "",

    @ColumnInfo(name = "android_version")
    val androidVersion: String = ""
) {
    fun toDomainModel(): CalibrationData {
        return CalibrationData(
            gyroBias = com.airmouse.domain.model.SensorCalibrationData(
                offsetX = gyroBiasX,
                offsetY = gyroBiasY,
                offsetZ = gyroBiasZ
            ),
            accelOffset = com.airmouse.domain.model.SensorCalibrationData(
                offsetX = accelOffsetX,
                offsetY = accelOffsetY,
                offsetZ = accelOffsetZ,
                scaleX = accelScaleX,
                scaleY = accelScaleY,
                scaleZ = accelScaleZ
            ),
            magOffset = com.airmouse.domain.model.SensorCalibrationData(
                offsetX = magOffsetX,
                offsetY = magOffsetY,
                offsetZ = magOffsetZ,
                scaleX = magScaleX,
                scaleY = magScaleY,
                scaleZ = magScaleZ
            ),
            isCalibrated = isCalibrated,
            quality = try {
                CalibrationQuality.valueOf(calibrationQuality)
            } catch (e: Exception) {
                CalibrationQuality.UNKNOWN
            },
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomainModel(data: CalibrationData): CalibrationEntity {
            return CalibrationEntity(
                gyroBiasX = data.gyroBias.offsetX,
                gyroBiasY = data.gyroBias.offsetY,
                gyroBiasZ = data.gyroBias.offsetZ,
                accelOffsetX = data.accelOffset.offsetX,
                accelOffsetY = data.accelOffset.offsetY,
                accelOffsetZ = data.accelOffset.offsetZ,
                accelScaleX = data.accelOffset.scaleX,
                accelScaleY = data.accelOffset.scaleY,
                accelScaleZ = data.accelOffset.scaleZ,
                magOffsetX = data.magOffset.offsetX,
                magOffsetY = data.magOffset.offsetY,
                magOffsetZ = data.magOffset.offsetZ,
                magScaleX = data.magOffset.scaleX,
                magScaleY = data.magOffset.scaleY,
                magScaleZ = data.magOffset.scaleZ,
                isCalibrated = data.isCalibrated,
                calibrationQuality = data.quality.name,
                timestamp = data.timestamp,
                version = data.version,
                deviceModel = data.deviceModel,
                androidVersion = data.androidVersion
            )
        }
    }
}