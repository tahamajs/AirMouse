package com.airmouse.presentation.ui.logs

data class ServerLogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val filter: String = "",
    val level: String = "All",
    val isAutoScroll: Boolean = true,
    val isExporting: Boolean = false,
    val exportPath: String? = null,
    val errorMessage: String? = null
)

data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null
)

enum class LogLevel(val displayName: String, val color: Long) {
    DEBUG("Debug", 0xFF9E9E9E),
    INFO("Info", 0xFF2196F3),
    WARN("Warn", 0xFFFF9800),
    ERROR("Error", 0xFFF44336),
    VERBOSE("Verbose", 0xFF8BC34A)
}