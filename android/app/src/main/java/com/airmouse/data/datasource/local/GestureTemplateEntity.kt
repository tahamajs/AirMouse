// app/src/main/java/com/airmouse/data/datasource/local/GestureTemplateEntity.kt
package com.airmouse.data.datasource.local

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airmouse.domain.model.GestureType

@Entity(tableName = "gesture_templates")
data class GestureTemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "data")
    val data: ByteArray? = null,

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "version")
    val version: Int = 1,

    @ColumnInfo(name = "usage_count")
    val usageCount: Int = 0,

    @ColumnInfo(name = "avg_score")
    val avgScore: Float = 0f,

    @ColumnInfo(name = "is_predefined")
    val isPredefined: Boolean = false
)