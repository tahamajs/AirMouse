// app/src/main/java/com/airmouse/data/datasource/local/GestureStatsEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_stats")
data class GestureStatsEntity(
    @PrimaryKey
    val gestureName: String,

    @ColumnInfo(name = "detection_count")
    val detectionCount: Int = 0,

    @ColumnInfo(name = "confidence_percentage")
    val confidencePercentage: Float = 0f,

    @ColumnInfo(name = "last_detected")
    val lastDetected: Long = 0,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,

    @ColumnInfo(name = "category")
    val category: String = "general",

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "avg_execution_time_ms")
    val avgExecutionTimeMs: Float = 0f,

    @ColumnInfo(name = "total_confidence_sum")
    val totalConfidenceSum: Float = 0f
) {
    fun getAverageConfidence(): Float {
        return if (detectionCount > 0) totalConfidenceSum / detectionCount else 0f
    }
}