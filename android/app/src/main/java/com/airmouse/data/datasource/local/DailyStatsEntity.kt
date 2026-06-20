// app/src/main/java/com/airmouse/data/datasource/local/DailyStatsEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String, // Format: yyyy-MM-dd

    @ColumnInfo(name = "clicks")
    val clicks: Int = 0,

    @ColumnInfo(name = "scrolls")
    val scrolls: Int = 0,

    @ColumnInfo(name = "gestures")
    val gestures: Int = 0,

    @ColumnInfo(name = "movements")
    val movements: Int = 0,

    @ColumnInfo(name = "session_time")
    val sessionTime: Long = 0,

    @ColumnInfo(name = "active_time")
    val activeTime: Long = 0
)