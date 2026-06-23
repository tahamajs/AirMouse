
package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String, 

    @ColumnInfo(name = "clicks")
    val clicks: Int = 0,

    @ColumnInfo(name = "double_clicks")
    val doubleClicks: Int = 0,

    @ColumnInfo(name = "right_clicks")
    val rightClicks: Int = 0,

    @ColumnInfo(name = "scrolls")
    val scrolls: Int = 0,

    @ColumnInfo(name = "gestures")
    val gestures: Int = 0,

    @ColumnInfo(name = "movements")
    val movements: Int = 0,

    @ColumnInfo(name = "session_time")
    val sessionTime: Long = 0,

    @ColumnInfo(name = "active_time")
    val activeTime: Long = 0,

    @ColumnInfo(name = "distance")
    val distance: Float = 0f,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
)
