package com.airmouse.presentation.ui.profiles

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.repository.ISettingsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val settingsRepo: ISettingsRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfilesUiState())
    val uiState: StateFlow<ProfilesUiState> = _uiState.asStateFlow()

    init {
        loadProfiles()
    }

    private fun loadProfiles() {
        viewModelScope.launch {
            // Simulate – replace with actual profile repository
            _uiState.update { it.copy(profiles = listOf("Default", "Gaming", "Presentation")) }
        }
    }

    fun updateNewProfileName(name: String) {
        _uiState.update { it.copy(newProfileName = name) }
    }

    fun selectProfile(profile: String) {
        _uiState.update { it.copy(selectedProfile = profile) }
        // Load profile settings
    }

    fun saveProfile() {
        val name = _uiState.value.newProfileName
        if (name.isNotBlank()) {
            _uiState.update { state ->
                state.copy(
                    profiles = state.profiles + name,
                    newProfileName = "",
                    selectedProfile = name
                )
            }
        }
    }

    fun deleteProfile() {
        val profile = _uiState.value.selectedProfile
        if (profile != "Default") {
            _uiState.update { state ->
                state.copy(
                    profiles = state.profiles.filter { it != profile },
                    selectedProfile = "Default"
                )
            }
        }
    }
}