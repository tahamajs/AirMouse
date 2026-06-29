package com.airmouse.presentation.ui.gaming

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.gaming.GameProfilesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

data class GameProfilesUiState(
    val profileCount: Int = 0,
    val currentGame: String = "None",
    val summary: String = ""
)

@HiltViewModel
class GameProfilesViewModel @Inject constructor(
    private val gameProfilesManager: GameProfilesManager
) : ViewModel() {
    val uiState: StateFlow<GameProfilesUiState> =
        gameProfilesManager.profiles.stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            emptyList()
        ).let { profiles ->
            gameProfilesManager.currentGame.stateIn(
                viewModelScope,
                SharingStarted.WhileSubscribed(5_000),
                null
            ).let { currentGame ->
                kotlinx.coroutines.flow.combine(profiles, currentGame) { profileList, current ->
                    GameProfilesUiState(
                        profileCount = profileList.size,
                        currentGame = current?.gameName ?: "None",
                        summary = profileList.take(3).joinToString { it.gameName }
                    )
                }.stateIn(
                    viewModelScope,
                    SharingStarted.WhileSubscribed(5_000),
                    GameProfilesUiState()
                )
            }
        }

    fun refreshProfiles() {
        gameProfilesManager.refreshProfiles()
    }
}
