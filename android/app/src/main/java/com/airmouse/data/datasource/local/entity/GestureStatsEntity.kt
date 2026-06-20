// app/src/main/java/com/airmouse/data/datasource/local/entity/GestureStatsEntity.kt
package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_stats")
data class GestureStatsEntity(
    @PrimaryKey
    val gesture_name: String,

    @ColumnInfo(name = "count")
    val count: Int = 0,

    @ColumnInfo(name = "avg_confidence")
    val avgConfidence: Float = 0f,

    @ColumnInfo(name = "last_detected")
    val lastDetected: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "detection_rate")
    val detectionRate: Float = 0f,

    @androidx.room.Ignore
    val gestureName: String = gesture_name,

    @androidx.room.Ignore
    val detectionCount: Int = count,

    @androidx.room.Ignore
    val confidencePercentage: Float = avgConfidence,

    @androidx.room.Ignore
    val isCustom: Boolean = false
)
