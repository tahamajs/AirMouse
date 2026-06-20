package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "gesture_templates")
data class GestureTemplateEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "type")
    val type: String,

    @ColumnInfo(name = "action")
    val action: String,

    @ColumnInfo(name = "confidence")
    val confidence: Float = 0.8f,

    @ColumnInfo(name = "is_enabled")
    val isEnabled: Boolean = true,

    @ColumnInfo(name = "is_custom")
    val isCustom: Boolean = false,

    @ColumnInfo(name = "is_system")
    val isSystem: Boolean = false,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "detection_count")
    val detectionCount: Int = 0,

    @ColumnInfo(name = "confidence_threshold")
    val confidenceThreshold: Float = 0.7f,

    @ColumnInfo(name = "training_samples_count")
    val trainingSamplesCount: Int = 0,

    @ColumnInfo(name = "last_detected")
    val lastDetected: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "version")
    val version: Int = 1,

    @ColumnInfo(name = "updated_at")
    val updatedAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "metadata")
    val metadata: String? = null,

    @ColumnInfo(name = "description")
    val description: String? = null,

    @ColumnInfo(name = "icon_res")
    val iconRes: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis()
)