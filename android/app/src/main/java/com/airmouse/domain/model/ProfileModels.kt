package com.airmouse.domain.model

import android.os.Parcelable
import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Entity(tableName = "profiles")
@Parcelize
data class Profile(
    @PrimaryKey val id: String = UUID.randomUUID().toString(),
    val name: String,
    val sensitivity: Float = 0.5f,
    val clickThreshold: Float = 8f,
    val doubleClickInterval: Long = 400L,
    val scrollThreshold: Float = 6f,
    val rightClickTilt: Float = 45f,
    val hapticEnabled: Boolean = true,
    val theme: String = "system",
    val aiSmoothing: Boolean = false,
    val predictiveMovement: Boolean = true,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val accelerationEnabled: Boolean = true,
    val smoothingEnabled: Boolean = true,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsed: Long = System.currentTimeMillis(),
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false
) : Parcelable

/**
 * Profile statistics for comparison.
 */
@Parcelize
data class ProfileStats(
    val profileName: String,
    val usageCount: Int,
    val lastUsed: Long,
    val isDefault: Boolean
) : Parcelable
