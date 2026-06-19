// app/src/main/java/com/airmouse/data/datasource/local/TrainingSampleEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "training_samples")
data class TrainingSampleEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,

    @ColumnInfo(name = "gesture_name")
    val gestureName: String,

    @ColumnInfo(name = "sample_data")
    val sampleData: String, // JSON array of floats

    @ColumnInfo(name = "timestamp")
    val timestamp: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "confidence")
    val confidence: Float = 0f
)