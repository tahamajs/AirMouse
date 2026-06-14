package com.airmouse.presentation.ui.profiles

data class ProfilesUiState(
    val profiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val newProfileName: String = "",
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDetailsDialog: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val exportPath: String? = null,
    val searchQuery: String = "",
    val sortBy: ProfileSort = ProfileSort.NAME,
    val viewMode: ViewMode = ViewMode.GRID,
    val selectedTags: List<String> = emptyList(),
    val availableTags: List<String> = listOf("Gaming", "Work", "Presentation", "Design", "Casual", "Precision")
)