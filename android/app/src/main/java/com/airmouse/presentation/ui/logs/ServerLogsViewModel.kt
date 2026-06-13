package com.airmouse.presentation.ui.logs

import android.content.Context
import android.os.Environment
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.WebSocketManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ServerLogsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerLogsUiState())
    val uiState: StateFlow<ServerLogsUiState> = _uiState.asStateFlow()

    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    private val logEntries = mutableListOf<LogEntry>()
    private var nextId = 0

    init {
        setupLogCapture()
        loadSavedLogs()
    }

    private fun setupLogCapture() {
        // Capture WebSocket logs
        WebSocketManager.onMessage = { message ->
            addLogEntry(
                level = LogLevel.INFO,
                tag = "WebSocket",
                message = "Received: $message"
            )
        }

        WebSocketManager.onConnected = {
            addLogEntry(LogLevel.INFO, "Connection", "WebSocket connected")
        }

        WebSocketManager.onDisconnected = {
            addLogEntry(LogLevel.WARN, "Connection", "WebSocket disconnected")
        }

        WebSocketManager.onError = { error ->
            addLogEntry(LogLevel.ERROR, "Connection", "WebSocket error: $error")
        }
    }

    private fun loadSavedLogs() {
        viewModelScope.launch {
            val savedLogs = prefs.getString("saved_logs", "")
            if (savedLogs.isNotEmpty()) {
                // Parse and load saved logs
                // For now, just add a welcome message
                addLogEntry(LogLevel.INFO, "System", "Log viewer started", "Application initialized")
            }
        }
    }

    fun addLogEntry(
        level: LogLevel,
        tag: String,
        message: String,
        details: String? = null
    ) {
        val entry = LogEntry(
            id = (nextId++).toString(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            details = details
        )

        logEntries.add(0, entry) // Add to beginning for newest first

        // Limit logs to 1000 entries
        while (logEntries.size > 1000) {
            logEntries.removeAt(logEntries.lastIndex)
        }

        updateFilteredLogs()
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter) }
        updateFilteredLogs()
    }

    fun setLevel(level: String) {
        _uiState.update { it.copy(level = level) }
        updateFilteredLogs()
    }

    fun toggleAutoScroll() {
        _uiState.update { it.copy(isAutoScroll = !it.isAutoScroll) }
    }

    private fun updateFilteredLogs() {
        val filter = _uiState.value.filter.lowercase()
        val level = _uiState.value.level

        val filtered = logEntries.filter { entry ->
            // Filter by level
            val levelMatch = when (level) {
                "All" -> true
                "Debug" -> entry.level == LogLevel.DEBUG
                "Info" -> entry.level == LogLevel.INFO
                "Warn" -> entry.level == LogLevel.WARN
                "Error" -> entry.level == LogLevel.ERROR
                else -> true
            }

            // Filter by text
            val textMatch = filter.isEmpty() ||
                    entry.tag.lowercase().contains(filter) ||
                    entry.message.lowercase().contains(filter) ||
                    (entry.details?.lowercase()?.contains(filter) ?: false)

            levelMatch && textMatch
        }

        _uiState.update { it.copy(filteredLogs = filtered) }
    }

    fun clearLogs() {
        logEntries.clear()
        updateFilteredLogs()
        addLogEntry(LogLevel.INFO, "System", "Logs cleared by user")
    }

    fun exportLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }

            try {
                val fileName = "airmouse_logs_${System.currentTimeMillis()}.txt"
                val file = withContext(Dispatchers.IO) {
                    val directory = context.getExternalFilesDir(null) ?: context.filesDir
                    File(directory, fileName)
                }

                withContext(Dispatchers.IO) {
                    FileWriter(file).use { writer ->
                        writer.write("Air Mouse Pro - Server Logs\n")
                        writer.write("=" .repeat(50) + "\n")
                        writer.write("Exported: ${dateFormat.format(Date())}\n")
                        writer.write("Total logs: ${logEntries.size}\n")
                        writer.write("=" .repeat(50) + "\n\n")

                        logEntries.forEach { entry ->
                            writer.write("[${dateFormat.format(Date(entry.timestamp))}] ")
                            writer.write("[${entry.level.displayName}] ")
                            writer.write("[${entry.tag}] ")
                            writer.write(entry.message)
                            if (entry.details != null) {
                                writer.write("\n  Details: ${entry.details}")
                            }
                            writer.write("\n")
                        }
                    }
                }

                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportPath = file.absolutePath
                    )
                }

                addLogEntry(LogLevel.INFO, "Export", "Logs exported to ${file.name}")

                // Clear export path after 3 seconds
                kotlinx.coroutines.delay(3000)
                _uiState.update { it.copy(exportPath = null) }

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    )
                }
                addLogEntry(LogLevel.ERROR, "Export", "Failed to export logs: ${e.message}")
            }
        }
    }

    fun shareLogs() {
        viewModelScope.launch {
            try {
                val fileName = "airmouse_logs_${System.currentTimeMillis()}.txt"
                val file = withContext(Dispatchers.IO) {
                    val directory = context.getExternalFilesDir(null) ?: context.filesDir
                    File(directory, fileName)
                }

                withContext(Dispatchers.IO) {
                    FileWriter(file).use { writer ->
                        writer.write("Air Mouse Pro - Server Logs\n")
                        writer.write("=" .repeat(50) + "\n")
                        writer.write("Exported: ${dateFormat.format(Date())}\n")
                        writer.write("Total logs: ${logEntries.size}\n")
                        writer.write("=" .repeat(50) + "\n\n")

                        logEntries.forEach { entry ->
                            writer.write("[${dateFormat.format(Date(entry.timestamp))}] ")
                            writer.write("[${entry.level.displayName}] ")
                            writer.write("[${entry.tag}] ")
                            writer.write(entry.message)
                            writer.write("\n")
                        }
                    }
                }

                // Create share intent
                val uri = androidx.core.content.FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )

                val shareIntent = android.content.Intent(android.content.Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(android.content.Intent.EXTRA_STREAM, uri)
                    addFlags(android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }

                context.startActivity(android.content.Intent.createChooser(shareIntent, "Share Logs"))

                addLogEntry(LogLevel.INFO, "Share", "Logs shared")

            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Share failed: ${e.message}")
                }
                addLogEntry(LogLevel.ERROR, "Share", "Failed to share logs: ${e.message}")
            }
        }
    }

    fun copyLogsToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
        val logsText = _uiState.value.filteredLogs.joinToString("\n") { entry ->
            "[${dateFormat.format(Date(entry.timestamp))}] [${entry.level.displayName}] [${entry.tag}] ${entry.message}"
        }

        val clip = android.content.ClipData.newPlainText("Air Mouse Logs", logsText)
        clipboard.setPrimaryClip(clip)

        addLogEntry(LogLevel.INFO, "System", "Logs copied to clipboard")
        _uiState.update { it.copy(errorMessage = "Logs copied to clipboard") }

        viewModelScope.launch {
            kotlinx.coroutines.delay(3000)
            _uiState.update { it.copy(errorMessage = null) }
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun addTestLog() {
        val levels = listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR)
        val randomLevel = levels.random()
        val messages = listOf(
            "User connected from 192.168.1.100",
            "Gesture detected: ThumbsUp (confidence: 0.95)",
            "Movement delta: dx=12.5, dy=-3.2",
            "Calibration completed successfully",
            "Connection timeout, retrying...",
            "Sensor data received: gyroX=0.12, gyroY=0.05, gyroZ=0.08",
            "Server response: ACK received",
            "Battery level: 85%",
            "WiFi signal strength: -45dBm"
        )
        val randomMessage = messages.random()

        addLogEntry(
            level = randomLevel,
            tag = when (randomLevel) {
                LogLevel.DEBUG -> "Debug"
                LogLevel.INFO -> "Info"
                LogLevel.WARN -> "Warning"
                LogLevel.ERROR -> "Error"
                else -> "System"
            },
            message = randomMessage
        )
    }

    fun deleteOldLogs(daysOld: Int) {
        val cutoffTime = System.currentTimeMillis() - (daysOld * 24L * 60L * 60L * 1000L)
        val toRemove = logEntries.filter { it.timestamp < cutoffTime }
        logEntries.removeAll(toRemove)
        updateFilteredLogs()
        addLogEntry(LogLevel.INFO, "System", "Deleted ${toRemove.size} old logs (older than $daysOld days)")
    }
}