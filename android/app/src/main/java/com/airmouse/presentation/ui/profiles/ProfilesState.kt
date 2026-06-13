package com.airmouse.presentation.ui.profiles

import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.model.UserProfile

data class ProfilesUiState(
    val profiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val newProfileName: String = "",
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val exportPath: String? = null,
    val searchQuery: String = "",
    val sortBy: ProfileSort = ProfileSort.NAME,
    val viewMode: ViewMode = ViewMode.GRID
)

enum class ProfileSort(val displayName: String) {
    NAME("Name"),
    DATE("Date Created"),
    LAST_USED("Last Used"),
    FAVORITE("Favorite")
}

enum class ViewMode {
    LIST, GRID
}