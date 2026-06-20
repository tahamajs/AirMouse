package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey
    val id: String = "default",

    @ColumnInfo(name = "session_id")
    val sessionId: String = "default",

    @ColumnInfo(name = "total_clicks")
    val totalClicks: Int = 0,

    @ColumnInfo(name = "total_double_clicks")
    val totalDoubleClicks: Int = 0,

    @ColumnInfo(name = "total_right_clicks")
    val totalRightClicks: Int = 0,

    @ColumnInfo(name = "total_scrolls")
    val totalScrolls: Int = 0,

    @ColumnInfo(name = "total_movements_legacy")
    val totalMovements: Int = 0,

    @ColumnInfo(name = "total_distance_legacy")
    val totalDistance: Float = 0f,

    @ColumnInfo(name = "average_speed_legacy")
    val averageSpeed: Float = 0f,

    @ColumnInfo(name = "max_speed_legacy")
    val maxSpeed: Float = 0f,

    @ColumnInfo(name = "start_time")
    val startTime: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "end_time")
    val endTime: Long = 0L,

    @ColumnInfo(name = "is_active_legacy")
    val isActive: Boolean = true,

    @ColumnInfo(name = "total_movement")
    val totalMovement: Float = 0f,

    @ColumnInfo(name = "movement_count")
    val movementCount: Long = 0,

    @ColumnInfo(name = "click_count")
    val clickCount: Long = 0,

    @ColumnInfo(name = "double_click_count")
    val doubleClickCount: Long = 0,

    @ColumnInfo(name = "right_click_count")
    val rightClickCount: Long = 0,

    @ColumnInfo(name = "middle_click_count")
    val middleClickCount: Long = 0,

    @ColumnInfo(name = "scroll_count")
    val scrollCount: Long = 0,

    @ColumnInfo(name = "total_scroll_delta")
    val totalScrollDelta: Long = 0,

    @ColumnInfo(name = "gesture_count")
    val gestureCount: Long = 0,

    @ColumnInfo(name = "session_count")
    val sessionCount: Long = 0,

    @ColumnInfo(name = "total_session_time")
    val totalSessionTime: Long = 0,

    @ColumnInfo(name = "last_reset")
    val lastReset: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
