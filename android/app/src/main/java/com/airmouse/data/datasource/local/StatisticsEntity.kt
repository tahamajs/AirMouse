// app/src/main/java/com/airmouse/data/datasource/local/StatisticsEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "statistics")
data class StatisticsEntity(
    @PrimaryKey
    val id: String = "default",

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