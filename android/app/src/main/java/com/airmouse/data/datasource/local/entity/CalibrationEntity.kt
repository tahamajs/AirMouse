package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airmouse.data.mapper.EntityToDomainMapper
import com.airmouse.data.mapper.DomainToEntityMapper
import com.airmouse.domain.model.CalibrationData

/**
 * Room entity for storing sensor calibration parameters.
 * All fields are primitive types, making it safe for Room persistence.
 */
@Entity(tableName = "calibration")
data class CalibrationEntity(
    @PrimaryKey
    val id: String = "default",

    // Gyroscope bias (rad/s)
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

    // Accelerometer offset (m/s²) and scale factors
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

    // Magnetometer offset (µT) and scale factors
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

    // Calibration status
    @ColumnInfo(name = "is_calibrated")
    val isCalibrated: Boolean = false,

    @ColumnInfo(name = "calibration_quality")
    val calibrationQuality: String = "UNKNOWN",  // Use String to avoid enum conversion issues

    @ColumnInfo(name = "calibration_timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "calibration_version")
    val version: Int = 2,

    @ColumnInfo(name = "device_model")
    val deviceModel: String = "",

    @ColumnInfo(name = "android_version")
    val androidVersion: String = ""
) {
    fun toDomainModel(): CalibrationData = EntityToDomainMapper.mapToDomain(this)

    companion object {
        fun fromDomainModel(data: CalibrationData): CalibrationEntity = DomainToEntityMapper.mapToEntity(data)
    }
}
