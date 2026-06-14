package com.airmouse.presentation.ui.profiles

import android.os.Parcelable
import kotlinx.parcelize.Parcelize
import java.util.UUID

@Parcelize
data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val settings: ProfileSettings = ProfileSettings(),
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val tags: List<String> = emptyList(),
    val iconRes: Int? = null,
    val color: String = "#6366F1"
) : Parcelable {
    
    val formattedCreatedDate: String get() = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(createdAt))
    
    val formattedLastUsed: String get() = java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(lastUsedAt))
    
    fun incrementUsage(): UserProfile = copy(
        lastUsedAt = System.currentTimeMillis(),
        usageCount = usageCount + 1
    )
}

enum class ProfileSort(val displayName: String) {
    NAME("Name"),
    DATE_CREATED("Date Created"),
    LAST_USED("Last Used"),
    FAVORITE("Favorite"),
    USAGE_COUNT("Most Used")
}

enum class ViewMode {
    LIST, GRID, COMPACT
}