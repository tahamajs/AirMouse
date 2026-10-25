package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureType

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
) {
    fun toDomainModel(): CustomGestureTemplate {
        return CustomGestureTemplate(
            id = id,
            name = name,
            type = try { GestureType.valueOf(type) } catch (_: Exception) { GestureType.CUSTOM },
            action = action,
            confidence = confidenceThreshold,
            isEnabled = isEnabled,
            createdAt = createdAt,
            updatedAt = updatedAt,
            usageCount = detectionCount,
            description = description ?: ""
        )
    }

    companion object {
        fun fromDomainModel(template: CustomGestureTemplate): GestureTemplateEntity {
            return GestureTemplateEntity(
                id = template.id,
                name = template.name,
                type = template.type.name,
                action = template.action,
                confidence = template.confidence,
                isEnabled = template.isEnabled,
                isCustom = template.type == GestureType.CUSTOM,
                isSystem = template.type != GestureType.CUSTOM,
                isFavorite = template.isFavorite,
                detectionCount = template.usageCount,
                confidenceThreshold = template.confidence,
                createdAt = template.createdAt,
                updatedAt = template.updatedAt,
                description = template.description
            )
        }
    }
}
