// app/src/main/java/com/airmouse/data/datasource/local/CalibrationEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "calibration")
data class CalibrationEntity(
    @PrimaryKey
    val id: String = "default",

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

    @ColumnInfo(name = "is_calibrated")
    val isCalibrated: Boolean = false,

    @ColumnInfo(name = "calibration_quality")
    val calibrationQuality: String = "UNKNOWN",

    @ColumnInfo(name = "calibration_timestamp")
    val timestamp: Long = System.currentTimeMillis()
)