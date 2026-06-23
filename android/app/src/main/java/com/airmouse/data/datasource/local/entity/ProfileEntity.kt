package com.airmouse.data.datasource.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

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
)