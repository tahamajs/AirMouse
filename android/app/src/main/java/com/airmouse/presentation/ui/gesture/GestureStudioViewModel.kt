package com.airmouse.presentation.ui.gesture

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.repository.IGestureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class GestureStudioViewModel @Inject constructor(
    private val gestureRepo: IGestureRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(GestureStudioUiState())
    val uiState: StateFlow<GestureStudioUiState> = _uiState.asStateFlow()

    fun updateGestureName(name: String) {
        _uiState.update { it.copy(gestureName = name) }
    }

    fun startRecording() {
        _uiState.update { it.copy(isRecording = true, status = "Recording... perform gesture") }
        // Simulate recording – in real app, start sensor collection
    }

    fun stopRecording() {
        _uiState.update { it.copy(isRecording = false, status = "Recording stopped, saved gesture") }
        // Save gesture
        loadSavedGestures()
    }

    fun exportDataset() {
        // Export CSV
    }

    private fun loadSavedGestures() {
        viewModelScope.launch {
            val gestures = gestureRepo.getAllCustomGestures()
            _uiState.update { it.copy(savedGestures = gestures) }
        }
    }
}