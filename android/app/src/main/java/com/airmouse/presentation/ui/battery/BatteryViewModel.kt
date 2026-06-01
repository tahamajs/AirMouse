package com.airmouse.presentation.ui.battery

import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import javax.inject.Inject

@HiltViewModel
class BatteryViewModel @Inject constructor() : ViewModel() {

    private val _uiState = MutableStateFlow(BatteryUiState())
    val uiState: StateFlow<BatteryUiState> = _uiState.asStateFlow()

    init {
        // Simulate periodic updates (in real app, use BatteryManager)
        kotlinx.coroutines.GlobalScope.launch {
            while (true) {
                _uiState.update { it.copy(level = (75..100).random(), temperature = 25f + (0..5).random()) }
                kotlinx.coroutines.delay(5000)
            }
        }
    }
}

data class BatteryUiState(
    val level: Int = 0,
    val temperature: Float = 0f,
    val history: List<Float> = emptyList()
)