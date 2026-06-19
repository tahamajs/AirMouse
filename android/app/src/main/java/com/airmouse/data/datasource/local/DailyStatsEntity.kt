// app/src/main/java/com/airmouse/data/datasource/local/DailyStatsEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "daily_stats")
data class DailyStatsEntity(
    @PrimaryKey
    val date: String, // yyyy-MM-dd format

    @ColumnInfo(name = "clicks")
    val clicks: Int = 0,

    @ColumnInfo(name = "double_clicks")
    val doubleClicks: Int = 0,

    @ColumnInfo(name = "right_clicks")
    val rightClicks: Int = 0,

    @ColumnInfo(name = "scrolls")
    val scrolls: Int = 0,

    @ColumnInfo(name = "movements")
    val movements: Int = 0,

    @ColumnInfo(name = "distance")
    val distance: Float = 0f,

    @ColumnInfo(name = "gestures")
    val gestures: Int = 0,

    @ColumnInfo(name = "last_updated")
    val lastUpdated: Long = System.currentTimeMillis()
) {
    fun hasActivity(): Boolean {
        return clicks > 0 || doubleClicks > 0 || rightClicks > 0 ||
                scrolls > 0 || movements > 0 || distance > 0 || gestures > 0
    }

    fun getTotalActions(): Int {
        return clicks + doubleClicks + rightClicks + scrolls + movements + gestures
    }
}