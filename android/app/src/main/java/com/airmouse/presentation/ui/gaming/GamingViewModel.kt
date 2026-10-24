package com.airmouse.presentation.ui.gaming

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

data class GameProfileItem(
    val id: String,
    val name: String,
    val genre: String,
    val description: String = ""
)

data class GamingUiState(
    val transmissionProtocol: String = "WebSocket",
    val pollingRate: String = "Standard (60 Hz)",
    val defaultGestures: String = "Enabled",
    val sensitivity: Float = 1.0f,
    val isGamingModeApplied: Boolean = false,
    val isConnected: Boolean = false,
    val gameProfiles: List<GameProfileItem> = listOf(
        GameProfileItem(
            id = "fps",
            name = "FPS Shooter",
            genre = "First-Person Shooter",
            description = "High polling rate (120 Hz) and ultra-low latency UDP stream for instant aim reaction."
        ),
        GameProfileItem(
            id = "moba",
            name = "MOBA",
            genre = "Multiplayer Online Battle Arena",
            description = "Precision cursor response with right-click optimization and rapid camera pan."
        )
    )
)

@HiltViewModel
class GamingViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(GamingUiState())
    val uiState: StateFlow<GamingUiState> = _uiState.asStateFlow()

    fun applyGamingPreset() {
        _uiState.update {
            it.copy(
                transmissionProtocol = "UDP",
                pollingRate = "Maximum (120 Hz)",
                defaultGestures = "Disabled",
                sensitivity = 1.5f,
                isGamingModeApplied = true
            )
        }
    }

    fun applyPreset() {
        applyGamingPreset()
    }

    fun resetPreset() {
        _uiState.update {
            GamingUiState()
        }
    }
}
