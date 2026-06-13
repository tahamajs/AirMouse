// app/src/main/java/com/airmouse/presentation/ui/main/MainViewModel.kt
package com.airmouse.presentation.ui.main

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class MainViewModel @Inject constructor(
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()

    init {
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                _isConnected.value = status == ConnectionManager.ConnectionStatus.CONNECTED
            }
        }

        _uiState.update {
            it.copy(
                controlMode = prefs.getString("control_mode", "motion")
            )
        }
    }

    fun updateControlMode(mode: String) {
        prefs.putString("control_mode", mode)
        _uiState.update { it.copy(controlMode = mode) }
    }

    data class MainUiState(
        val controlMode: String = "motion"
    )
}