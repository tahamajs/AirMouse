package com.airmouse.presentation.ui.logs

import androidx.compose.ui.graphics.Color
import java.text.SimpleDateFormat
import java.util.*

data class ServerLogsUiState(
    val logs: List<LogEntry> = emptyList(),
    val filteredLogs: List<LogEntry> = emptyList(),
    val filter: String = "",
    val level: String = "All",
    val isAutoScroll: Boolean = true,
    val isExporting: Boolean = false,
    val isSharing: Boolean = false,
    val exportPath: String? = null,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val selectedLogIds: Set<String> = emptySet(),
    val isSelectionMode: Boolean = false,
    val searchResultsCount: Int = 0,
    val currentPage: Int = 0,
    val pageSize: Int = 50,
    val totalPages: Int = 0,
    val sortOrder: SortOrder = SortOrder.NEWEST_FIRST,
    val isRefreshing: Boolean = false,
    val autoRefreshEnabled: Boolean = true,
    val autoRefreshInterval: Int = 5, // seconds
    val logRetentionDays: Int = 7,
    val showTimestamps: Boolean = true,
    val showTags: Boolean = true,
    val fontSize: Float = 12f,
    val compactView: Boolean = false,
    val wordWrap: Boolean = true
)

enum class SortOrder(val displayName: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    LEVEL("By Level"),
    TAG("By Tag")
}

data class LogEntry(
    val id: String,
    val timestamp: Long,
    val level: LogLevel,
    val tag: String,
    val message: String,
    val details: String? = null,
    val stackTrace: String? = null,
    val isSelected: Boolean = false
) {
    val formattedTime: String
        get() = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    
    val formattedDate: String
        get() = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date(timestamp))
    
    val levelColor: Color
        get() = when (level) {
            LogLevel.DEBUG -> Color(0xFF9E9E9E)
            LogLevel.INFO -> Color(0xFF2196F3)
            LogLevel.WARN -> Color(0xFFFF9800)
            LogLevel.ERROR -> Color(0xFFF44336)
            LogLevel.VERBOSE -> Color(0xFF8BC34A)
            LogLevel.FATAL -> Color(0xFF9C27B0)
        }
    
    val levelIcon: String
        get() = when (level) {
            LogLevel.DEBUG -> "🔍"
            LogLevel.INFO -> "ℹ️"
            LogLevel.WARN -> "⚠️"
            LogLevel.ERROR -> "❌"
            LogLevel.VERBOSE -> "📝"
            LogLevel.FATAL -> "💀"
        }
}

enum class LogLevel(val displayName: String, val color: Long) {
    DEBUG("Debug", 0xFF9E9E9E),
    INFO("Info", 0xFF2196F3),
    WARN("Warn", 0xFFFF9800),
    ERROR("Error", 0xFFF44336),
    VERBOSE("Verbose", 0xFF8BC34A),
    FATAL("Fatal", 0xFF9C27B0)
}

data class LogStatistics(
    val totalCount: Int = 0,
    val debugCount: Int = 0,
    val infoCount: Int = 0,
    val warnCount: Int = 0,
    val errorCount: Int = 0,
    val verboseCount: Int = 0,
    val fatalCount: Int = 0,
    val uniqueTags: Int = 0,
    val oldestTimestamp: Long = 0,
    val newestTimestamp: Long = 0,
    val averageLogsPerHour: Float = 0f
) {
    val formattedOldest: String
        get() = if (oldestTimestamp > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(oldestTimestamp)) else "N/A"
    
    val formattedNewest: String
        get() = if (newestTimestamp > 0) SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(newestTimestamp)) else "N/A"
}