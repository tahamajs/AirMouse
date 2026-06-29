
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

    @ColumnInfo(name = "is_custom")  // ✅ ADD THIS
    val isCustom: Boolean = false
)