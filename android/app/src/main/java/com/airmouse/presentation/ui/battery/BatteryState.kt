package com.airmouse.presentation.ui.battery

data class BatteryUiState(
    val level: Int = 0,
    val temperature: Float = 0f,
    val history: List<Float> = emptyList()
)