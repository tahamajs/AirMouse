// app/src/main/java/com/airmouse/data/datasource/local/GestureStatsEntity.kt
package com.airmouse.data.datasource.local

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
    val detectionRate: Float = 0f
)