// app/src/main/java/com/airmouse/data/datasource/local/entity/TrainingSampleEntity.kt
package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_samples")
data class TrainingSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "gesture_id")
    val gestureId: String = "",

    @ColumnInfo(name = "gyro_x")
    val gyroX: Float = 0f,

    @ColumnInfo(name = "gyro_y")
    val gyroY: Float = 0f,

    @ColumnInfo(name = "gyro_z")
    val gyroZ: Float = 0f,

    @ColumnInfo(name = "accel_x")
    val accelX: Float = 0f,

    @ColumnInfo(name = "accel_y")
    val accelY: Float = 0f,

    @ColumnInfo(name = "accel_z")
    val accelZ: Float = 0f,

    @ColumnInfo(name = "mag_x")
    val magX: Float = 0f,

    @ColumnInfo(name = "mag_y")
    val magY: Float = 0f,

    @ColumnInfo(name = "mag_z")
    val magZ: Float = 0f,

    @ColumnInfo(name = "label")
    val label: String = "",

    @ColumnInfo(name = "confidence")
    val confidence: Float = 0f,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_valid")
    val isValid: Boolean = true,

    @ColumnInfo(name = "session_id")
    val sessionId: String? = null
)
