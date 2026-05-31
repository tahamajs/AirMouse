package com.airmouse.presentation.ui.logs

data class ServerLogsUiState(
    val logs: List<String> = emptyList(),
    val filter: String = "",
    val level: String = "All"
)