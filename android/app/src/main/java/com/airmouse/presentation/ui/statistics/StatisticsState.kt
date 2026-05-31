package com.airmouse.presentation.ui.statistics

data class StatisticsUiState(
    val clicks: Int = 0,
    val doubleClicks: Int = 0,
    val rightClicks: Int = 0,
    val scrolls: Int = 0,
    val sessionTime: Long = 0
)