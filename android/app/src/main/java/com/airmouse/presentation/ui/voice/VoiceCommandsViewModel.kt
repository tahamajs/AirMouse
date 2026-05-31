package com.airmouse.presentation.ui.voice

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class VoiceCommandsViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(VoiceCommandsUiState())
    val uiState: StateFlow<VoiceCommandsUiState> = _uiState.asStateFlow()

    fun startListening() {
        _uiState.update { it.copy(isListening = true, status = "Listening...") }
        // In real app, start PocketSphinx recognition service
    }

    fun stopListening() {
        _uiState.update { it.copy(isListening = false, status = "Stopped") }
    }

    fun onCommandRecognized(command: String) {
        _uiState.update {
            it.copy(lastCommand = command, status = "Recognised: $command")
        }
        // Send command to server via use case
    }
}