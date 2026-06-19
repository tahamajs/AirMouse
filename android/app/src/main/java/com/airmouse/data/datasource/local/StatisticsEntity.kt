// app/src/main/java/com/airmouse/data/datasource/local/StatisticsEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "session_id")
    val sessionId: String,

    @ColumnInfo(name = "total_clicks")
    val totalClicks: Int = 0,

    @ColumnInfo(name = "total_double_clicks")
    val totalDoubleClicks: Int = 0,

    @ColumnInfo(name = "total_right_clicks")
    val totalRightClicks: Int = 0,

    @ColumnInfo(name = "total_scrolls")
    val totalScrolls: Int = 0,

    @ColumnInfo(name = "total_movements")
    val totalMovements: Int = 0,

    @ColumnInfo(name = "total_distance")
    val totalDistance: Float = 0f,

    @ColumnInfo(name = "average_speed")
    val averageSpeed: Float = 0f,

    @ColumnInfo(name = "max_speed")
    val maxSpeed: Float = 0f,

    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "end_time")
    val endTime: Long = 0,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true
)