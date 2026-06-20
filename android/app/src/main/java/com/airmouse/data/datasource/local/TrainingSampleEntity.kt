// app/src/main/java/com/airmouse/data/datasource/local/TrainingSampleEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_samples")
data class TrainingSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "gesture_id")
    val gestureId: String,

    @ColumnInfo(name = "gyro_x")
    val gyroX: Float,

    @ColumnInfo(name = "gyro_y")
    val gyroY: Float,

    @ColumnInfo(name = "gyro_z")
    val gyroZ: Float,

    @ColumnInfo(name = "accel_x")
    val accelX: Float,

    @ColumnInfo(name = "accel_y")
    val accelY: Float,

    @ColumnInfo(name = "accel_z")
    val accelZ: Float,

    @ColumnInfo(name = "mag_x")
    val magX: Float,

    @ColumnInfo(name = "mag_y")
    val magY: Float,

    @ColumnInfo(name = "mag_z")
    val magZ: Float,

    @ColumnInfo(name = "label")
    val label: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float,

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "is_valid")
    val isValid: Boolean = true,

    @ColumnInfo(name = "session_id")
    val sessionId: String? = null
)