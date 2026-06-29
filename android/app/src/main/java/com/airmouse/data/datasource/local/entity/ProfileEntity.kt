package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey
import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile

/**
 * Room entity for user profiles.
 * Stores all profile settings and metadata.
 */
@Entity(tableName = "profiles")
data class ProfileEntity(
    @PrimaryKey
    val id: String,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "email")
    val email: String = "",

    @ColumnInfo(name = "avatar_uri")
    val avatarUri: String? = null,

    @ColumnInfo(name = "sensitivity")
    val sensitivity: Float = 1.0f,

    @ColumnInfo(name = "click_threshold")
    val clickThreshold: Float = 5.0f,

    @ColumnInfo(name = "double_click_interval")
    val doubleClickInterval: Long = 400L,

    @ColumnInfo(name = "scroll_threshold")
    val scrollThreshold: Float = 8.0f,

    @ColumnInfo(name = "right_click_tilt")
    val rightClickTilt: Float = 45f,

    @ColumnInfo(name = "haptic_enabled")
    val hapticEnabled: Boolean = true,

    @ColumnInfo(name = "theme")
    val theme: String = "dark",

    @ColumnInfo(name = "ai_smoothing")
    val aiSmoothing: Boolean = false,

    @ColumnInfo(name = "predictive_movement")
    val predictiveMovement: Boolean = true,

    @ColumnInfo(name = "invert_x")
    val invertX: Boolean = false,

    @ColumnInfo(name = "invert_y")
    val invertY: Boolean = false,

    @ColumnInfo(name = "acceleration_enabled")
    val accelerationEnabled: Boolean = true,

    @ColumnInfo(name = "smoothing_enabled")
    val smoothingEnabled: Boolean = true,

    @ColumnInfo(name = "edge_gestures_enabled")
    val edgeGesturesEnabled: Boolean = false,

    @ColumnInfo(name = "voice_commands_enabled")
    val voiceCommandsEnabled: Boolean = false,

    @ColumnInfo(name = "is_default")
    val isDefault: Boolean = false,

    @ColumnInfo(name = "is_favorite")
    val isFavorite: Boolean = false,

    @ColumnInfo(name = "is_active")
    val isActive: Boolean = true,

    @ColumnInfo(name = "tags")
    val tags: String? = null,

    @ColumnInfo(name = "icon_res")
    val iconRes: Int = 0,

    @ColumnInfo(name = "created_at")
    val createdAt: Long = System.currentTimeMillis(),

    @ColumnInfo(name = "last_used")
    val lastUsed: Long = System.currentTimeMillis()
) {

    /**
     * Convert this Room entity to a domain UserProfile.
     */
    fun toDomainModel(): UserProfile {
        return UserProfile(
            id = id,
            name = name,
            email = email,
            avatarUri = avatarUri,
            settings = ProfileSettings(
                sensitivity = sensitivity,
                clickThreshold = clickThreshold,
                doubleClickInterval = doubleClickInterval,
                scrollThreshold = scrollThreshold,
                rightClickTilt = rightClickTilt,
                hapticEnabled = hapticEnabled,
                theme = theme,
                aiSmoothing = aiSmoothing,
                predictiveMovement = predictiveMovement,
                invertX = invertX,
                invertY = invertY,
                accelerationEnabled = accelerationEnabled,
                smoothingEnabled = smoothingEnabled,
                edgeGesturesEnabled = edgeGesturesEnabled,
                voiceCommandsEnabled = voiceCommandsEnabled
            ),
            isDefault = isDefault,
            isFavorite = isFavorite,
            tags = tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            iconRes = iconRes,
            createdAt = createdAt,
            updatedAt = lastUsed,
            usageCount = 0
        )
    }

    companion object {
        /**
         * Convert a domain UserProfile to a Room ProfileEntity.
         */
        fun fromDomainModel(profile: UserProfile): ProfileEntity {
            return ProfileEntity(
                id = profile.id,
                name = profile.name,
                email = profile.email,
                avatarUri = profile.avatarUri,
                sensitivity = profile.settings.sensitivity,
                clickThreshold = profile.settings.clickThreshold,
                doubleClickInterval = profile.settings.doubleClickInterval,
                scrollThreshold = profile.settings.scrollThreshold,
                rightClickTilt = profile.settings.rightClickTilt,
                hapticEnabled = profile.settings.hapticEnabled,
                theme = profile.settings.theme,
                aiSmoothing = profile.settings.aiSmoothing,
                predictiveMovement = profile.settings.predictiveMovement,
                invertX = profile.settings.invertX,
                invertY = profile.settings.invertY,
                accelerationEnabled = profile.settings.accelerationEnabled,
                smoothingEnabled = profile.settings.smoothingEnabled,
                edgeGesturesEnabled = profile.settings.edgeGesturesEnabled,
                voiceCommandsEnabled = profile.settings.voiceCommandsEnabled,
                isDefault = profile.isDefault,
                isFavorite = profile.isFavorite,
                isActive = true,
                tags = profile.tags.joinToString(","),
                iconRes = profile.iconRes,
                createdAt = profile.createdAt,
                lastUsed = profile.updatedAt
            )
        }

        /**
         * Create a default profile entity.
         */
        fun default(name: String = "Default User"): ProfileEntity {
            return ProfileEntity(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                isDefault = true,
                createdAt = System.currentTimeMillis(),
                lastUsed = System.currentTimeMillis()
            )
        }
    }
}