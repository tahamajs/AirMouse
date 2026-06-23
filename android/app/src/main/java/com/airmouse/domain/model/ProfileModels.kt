
package com.airmouse.domain.model

import android.os.Parcelable
import kotlinx.parcelize.Parcelize

@Parcelize
data class UserProfile(
    val id: String = "",
    val name: String = "",
    val email: String = "",
    val avatarUri: String? = null,
    val settings: ProfileSettings = ProfileSettings(),
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false,
    val tags: List<String> = emptyList(),
    val iconRes: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val usageCount: Int = 0
) : Parcelable

@Parcelize
data class ProfileSettings(
    val sensitivity: Float = 1.0f,
    val clickThreshold: Float = 5.0f,
    val doubleClickInterval: Long = 400L,
    val scrollThreshold: Float = 8.0f,
    val rightClickTilt: Float = 45f,
    val hapticEnabled: Boolean = true,
    val theme: String = "dark",
    val aiSmoothing: Boolean = false,
    val predictiveMovement: Boolean = true,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val accelerationEnabled: Boolean = true,
    val smoothingEnabled: Boolean = true,
    val edgeGesturesEnabled: Boolean = false,
    val voiceCommandsEnabled: Boolean = false
) : Parcelable

enum class ProfileSort {
    NAME,
    LAST_USED,
    CREATED_AT,
    FAVORITE
}

enum class ViewMode {
    GRID,
    LIST
}