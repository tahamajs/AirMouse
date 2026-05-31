package com.airmouse.presentation.ui.profiles

data class ProfilesUiState(
    val profiles: List<String> = listOf("Default"),
    val selectedProfile: String = "Default",
    val newProfileName: String = "",
    val isLoading: Boolean = false
)