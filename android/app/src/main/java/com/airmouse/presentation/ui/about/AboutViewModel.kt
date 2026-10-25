// app/src/main/java/com/airmouse/presentation/ui/about/AboutViewModel.kt
package com.airmouse.presentation.ui.about

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.repository.IUpdateRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class AboutViewModel @Inject constructor(
    private val updateRepository: IUpdateRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(AboutUiState())
    val uiState: StateFlow<AboutUiState> = _uiState.asStateFlow()

    private val _effect = MutableStateFlow<AboutEffect?>(null)
    val effect: StateFlow<AboutEffect?> = _effect.asStateFlow()

    fun onEvent(event: AboutEvent) {
        when (event) {
            is AboutEvent.ShareApp -> shareApp()
            is AboutEvent.RateApp -> rateApp()
            is AboutEvent.CheckForUpdates -> checkForUpdates()
            is AboutEvent.OpenUrl -> openUrl(event.url)
            is AboutEvent.NavigateBack -> navigateBack()
        }
    }

    private fun shareApp() {
        _effect.value = AboutEffect.ShowToast("Sharing initialized")
    }

    private fun rateApp() {
        _effect.value = AboutEffect.OpenUrl("https://github.com/tahamajs/AirMouse")
    }

    private fun checkForUpdates() {
        viewModelScope.launch {
            _uiState.update { it.copy(isLoading = true, errorMessage = null) }
            try {
                val result = updateRepository.checkForUpdates()
                if (result.isAvailable) {
                    _uiState.update {
                        it.copy(
                            isUpdateAvailable = true,
                            isLoading = false,
                            updateVersion = result.version
                        )
                    }
                    _effect.value = AboutEffect.ShowToast("Update available: v${result.version}")
                } else {
                    _uiState.update { it.copy(isUpdateAvailable = false, isLoading = false) }
                    _effect.value = AboutEffect.ShowToast("AirMouse Pro is up to date (v1.0.0)")
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        errorMessage = e.message ?: "Failed to check for updates"
                    )
                }
                _effect.value = AboutEffect.ShowToast("AirMouse Pro is up to date (v1.0.0)")
            }
        }
    }

    private fun openUrl(url: String) {
        _effect.value = AboutEffect.OpenUrl(url)
    }

    private fun navigateBack() {
        _effect.value = AboutEffect.NavigateBack
    }

    fun clearEffect() {
        _effect.value = null
    }
}