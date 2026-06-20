package com.airmouse.presentation.ui.logs

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.network.ConnectionManager
import com.airmouse.utils.LogManager
import com.airmouse.utils.PreferencesManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.flow.update
import androidx.lifecycle.Observer
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import java.io.FileWriter
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject

@HiltViewModel
class ServerLogsViewModel @Inject constructor(
    @ApplicationContext private val context: Context,
    private val prefs: PreferencesManager,
    private val connectionManager: ConnectionManager
) : ViewModel() {

    private val _uiState = MutableStateFlow(ServerLogsUiState())
    val uiState: StateFlow<ServerLogsUiState> = _uiState.asStateFlow()
    
    private val _statistics = MutableStateFlow(LogStatistics())
    val statistics: StateFlow<LogStatistics> = _statistics.asStateFlow()
    
    private val _isConnected = MutableStateFlow(false)
    val isConnected: StateFlow<Boolean> = _isConnected.asStateFlow()
    
    private val logEntries = mutableListOf<LogEntry>()
    private var nextId = 0
    private var autoRefreshJob: Job? = null
    private var sharedLogObserver: Observer<List<LogManager.LogEntry>>? = null
    private var lastSharedLogCount = 0
    private val dateFormat = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault())
    
    companion object {
        private const val MAX_LOGS = 5000
        private const val SAVED_LOGS_KEY = "saved_logs_v2"
        private const val AUTO_REFRESH_PREF = "logs_auto_refresh"
        private const val RETENTION_DAYS_PREF = "logs_retention_days"
    }

    init {
        loadSavedLogs()
        setupLogCapture()
        loadSettings()
        startAutoRefresh()
        deleteOldLogs()
        attachSharedLogStream()
    }

    private fun loadSettings() {
        _uiState.update {
            it.copy(
                autoRefreshEnabled = prefs.getBoolean(AUTO_REFRESH_PREF, true),
                logRetentionDays = prefs.getInt(RETENTION_DAYS_PREF, 7),
                compactView = prefs.getBoolean("logs_compact_view", false),
                fontSize = prefs.getFloat("logs_font_size", 12f)
            )
        }
    }

    private fun saveSettings() {
        prefs.putBoolean(AUTO_REFRESH_PREF, _uiState.value.autoRefreshEnabled)
        prefs.putInt(RETENTION_DAYS_PREF, _uiState.value.logRetentionDays)
        prefs.putBoolean("logs_compact_view", _uiState.value.compactView)
        prefs.putFloat("logs_font_size", _uiState.value.fontSize)
    }

    private fun setupLogCapture() {
        // Capture connection manager logs
        viewModelScope.launch {
            connectionManager.connectionStatus.collect { status ->
                addLogEntry(
                    level = when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> LogLevel.INFO
                        ConnectionManager.ConnectionStatus.CONNECTING -> LogLevel.DEBUG
                        ConnectionManager.ConnectionStatus.DISCONNECTED -> LogLevel.WARN
                        ConnectionManager.ConnectionStatus.ERROR -> LogLevel.ERROR
                        else -> LogLevel.INFO
                    },
                    tag = "Connection",
                    message = "Status changed to: $status"
                )
                _isConnected.value = status == ConnectionManager.ConnectionStatus.CONNECTED
            }
        }
        
        viewModelScope.launch {
            connectionManager.connectionQuality.collect { quality ->
                addLogEntry(
                    level = LogLevel.DEBUG,
                    tag = "Quality",
                    message = "Ping: ${quality.ping}ms, Signal: ${quality.level()}%",
                    details = "Jitter: ${quality.jitter}ms, Packet Loss: ${quality.packetLoss * 100}%"
                )
            }
        }
        
        // Capture custom logs from other components
        setupCustomLogCapture()
    }

    private fun setupCustomLogCapture() {
        // This would capture logs from various components
        addLogEntry(LogLevel.INFO, "System", "Log viewer initialized", "Version 3.0.0")
        addLogEntry(LogLevel.DEBUG, "Storage", "Log retention: ${_uiState.value.logRetentionDays} days")
    }

    private fun attachSharedLogStream() {
        val observer = Observer<List<LogManager.LogEntry>> { entries ->
            if (entries.isNullOrEmpty()) return@Observer

            val newEntries = if (entries.size > lastSharedLogCount) {
                entries.take(entries.size - lastSharedLogCount)
            } else {
                entries
            }

            lastSharedLogCount = entries.size
            addSharedLogEntries(newEntries)
        }

        sharedLogObserver = observer
        LogManager.logEntries.observeForever(observer)
    }

    private fun addSharedLogEntries(entries: List<LogManager.LogEntry>) {
        if (entries.isEmpty()) return
        val newEntries = entries.reversed().map { shared ->
            LogEntry(
                id = (nextId++).toString(),
                timestamp = shared.timestampMs,
                level = shared.level.toLogLevel(),
                tag = shared.tag,
                message = shared.message
            )
        }

        if (newEntries.isNotEmpty()) {
            logEntries.addAll(0, newEntries)
            while (logEntries.size > MAX_LOGS) {
                logEntries.removeAt(logEntries.lastIndex)
            }
            updateFilteredLogs()
            updateStatistics()
            saveLogs()
        }
    }

    private fun loadSavedLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val savedLogsJson = prefs.getString(SAVED_LOGS_KEY, "")
                if (savedLogsJson.isNotEmpty()) {
                    val jsonArray = JSONArray(savedLogsJson)
                    val loadedLogs = mutableListOf<LogEntry>()
                    for (i in 0 until jsonArray.length()) {
                        val json = jsonArray.getJSONObject(i)
                        val entry = LogEntry(
                            id = json.getString("id"),
                            timestamp = json.getLong("timestamp"),
                            level = LogLevel.valueOf(json.getString("level")),
                            tag = json.getString("tag"),
                            message = json.getString("message"),
                            details = json.optString("details").takeIf { it.isNotEmpty() },
                            stackTrace = json.optString("stackTrace").takeIf { it.isNotEmpty() }
                        )
                        loadedLogs.add(entry)
                        nextId = maxOf(nextId, entry.id.toIntOrNull() ?: 0) + 1
                    }
                    logEntries.addAll(loadedLogs)
                    updateFilteredLogs()
                    updateStatistics()
                } else {
                    addSampleLogs()
                }
            } catch (e: Exception) {
                addLogEntry(LogLevel.ERROR, "System", "Failed to load saved logs", e.message)
            }
        }
    }

    private fun saveLogs() {
        viewModelScope.launch(Dispatchers.IO) {
            try {
                val jsonArray = JSONArray()
                logEntries.take(MAX_LOGS).forEach { entry ->
                    val json = JSONObject().apply {
                        put("id", entry.id)
                        put("timestamp", entry.timestamp)
                        put("level", entry.level.name)
                        put("tag", entry.tag)
                        put("message", entry.message)
                        entry.details?.let { put("details", it) }
                        entry.stackTrace?.let { put("stackTrace", it) }
                    }
                    jsonArray.put(json)
                }
                prefs.putString(SAVED_LOGS_KEY, jsonArray.toString())
            } catch (e: Exception) {
                // Log error but don't crash
            }
        }
    }

    private fun addSampleLogs() {
        addLogEntry(LogLevel.INFO, "System", "Welcome to Air Mouse Pro Log Viewer")
        addLogEntry(LogLevel.INFO, "System", "Logs from server and app will appear here")
        addLogEntry(LogLevel.DEBUG, "Network", "Starting discovery on port 8082")
        addLogEntry(LogLevel.INFO, "Network", "Server found at 192.168.1.100:8080")
        addLogEntry(LogLevel.INFO, "Connection", "WebSocket connected successfully")
        addLogEntry(LogLevel.DEBUG, "Sensor", "Gyroscope initialized: 100Hz")
        addLogEntry(LogLevel.DEBUG, "Sensor", "Accelerometer initialized: 100Hz")
        addLogEntry(LogLevel.INFO, "Calibration", "Gyroscope calibration completed")
        addLogEntry(LogLevel.WARN, "Gesture", "Low confidence detection: 0.45")
        addLogEntry(LogLevel.ERROR, "Connection", "WebSocket connection timeout, retrying...")
        addLogEntry(LogLevel.INFO, "Connection", "Reconnection successful")
    }

    fun addLogEntry(
        level: LogLevel,
        tag: String,
        message: String,
        details: String? = null,
        stackTrace: String? = null
    ) {
        val entry = LogEntry(
            id = (nextId++).toString(),
            timestamp = System.currentTimeMillis(),
            level = level,
            tag = tag,
            message = message,
            details = details,
            stackTrace = stackTrace
        )
        
        logEntries.add(0, entry)
        
        while (logEntries.size > MAX_LOGS) {
            logEntries.removeAt(logEntries.lastIndex)
        }
        
        updateFilteredLogs()
        updateStatistics()
        saveLogs()
    }

    fun setFilter(filter: String) {
        _uiState.update { it.copy(filter = filter, currentPage = 0) }
        updateFilteredLogs()
    }

    fun setLevel(level: String) {
        _uiState.update { it.copy(level = level, currentPage = 0) }
        updateFilteredLogs()
    }

    fun setSortOrder(order: SortOrder) {
        _uiState.update { it.copy(sortOrder = order) }
        updateFilteredLogs()
    }

    fun toggleAutoScroll() {
        _uiState.update { it.copy(isAutoScroll = !it.isAutoScroll) }
    }

    fun toggleAutoRefresh() {
        val newValue = !_uiState.value.autoRefreshEnabled
        _uiState.update { it.copy(autoRefreshEnabled = newValue) }
        saveSettings()
        if (newValue) {
            startAutoRefresh()
        } else {
            autoRefreshJob?.cancel()
        }
    }

    fun setAutoRefreshInterval(seconds: Int) {
        _uiState.update { it.copy(autoRefreshInterval = seconds) }
        if (_uiState.value.autoRefreshEnabled) {
            startAutoRefresh()
        }
    }

    private fun startAutoRefresh() {
        autoRefreshJob?.cancel()
        if (_uiState.value.autoRefreshEnabled) {
            autoRefreshJob = viewModelScope.launch {
                while (isActive) {
                    delay(_uiState.value.autoRefreshInterval * 1000L)
                    refreshLogs()
                }
            }
        }
    }

    fun refreshLogs() {
        _uiState.update { it.copy(isRefreshing = true) }
        // Simulate refresh - in production, fetch from server
        addLogEntry(LogLevel.DEBUG, "System", "Manual log refresh")
        viewModelScope.launch {
            delay(500)
            _uiState.update { it.copy(isRefreshing = false) }
        }
    }

    fun toggleCompactView() {
        val newValue = !_uiState.value.compactView
        _uiState.update { it.copy(compactView = newValue) }
        saveSettings()
    }

    fun setFontSize(size: Float) {
        _uiState.update { it.copy(fontSize = size.coerceIn(8f, 20f)) }
        saveSettings()
    }

    fun toggleWordWrap() {
        _uiState.update { it.copy(wordWrap = !it.wordWrap) }
    }

    fun toggleShowTimestamps() {
        _uiState.update { it.copy(showTimestamps = !it.showTimestamps) }
    }

    fun toggleShowTags() {
        _uiState.update { it.copy(showTags = !it.showTags) }
    }

    fun toggleSelectionMode() {
        val newMode = !_uiState.value.isSelectionMode
        _uiState.update { 
            it.copy(
                isSelectionMode = newMode,
                selectedLogIds = if (newMode) it.selectedLogIds else emptySet()
            )
        }
    }

    fun toggleLogSelection(logId: String) {
        val currentIds = _uiState.value.selectedLogIds.toMutableSet()
        if (currentIds.contains(logId)) {
            currentIds.remove(logId)
        } else {
            currentIds.add(logId)
        }
        _uiState.update { it.copy(selectedLogIds = currentIds) }
    }

    fun selectAllLogs() {
        val allIds = _uiState.value.filteredLogs.map { it.id }.toSet()
        _uiState.update { it.copy(selectedLogIds = allIds) }
    }

    fun clearSelection() {
        _uiState.update { it.copy(selectedLogIds = emptySet(), isSelectionMode = false) }
    }

    fun deleteSelectedLogs() {
        val idsToDelete = _uiState.value.selectedLogIds
        logEntries.removeAll { it.id in idsToDelete }
        updateFilteredLogs()
        updateStatistics()
        saveLogs()
        clearSelection()
        addLogEntry(LogLevel.INFO, "System", "Deleted ${idsToDelete.size} log entries")
    }

    fun clearLogs() {
        logEntries.clear()
        updateFilteredLogs()
        updateStatistics()
        saveLogs()
        addLogEntry(LogLevel.INFO, "System", "All logs cleared by user")
        _uiState.update { it.copy(successMessage = "All logs cleared") }
        clearMessagesAfterDelay()
    }

    fun deleteOldLogs() {
        val cutoffTime = System.currentTimeMillis() - (_uiState.value.logRetentionDays * 24L * 60L * 60L * 1000L)
        val toRemove = logEntries.filter { it.timestamp < cutoffTime }
        val removedCount = toRemove.size
        logEntries.removeAll(toRemove)
        updateFilteredLogs()
        updateStatistics()
        saveLogs()
        if (removedCount > 0) {
            addLogEntry(LogLevel.INFO, "System", "Deleted $removedCount old logs (older than ${_uiState.value.logRetentionDays} days)")
        }
    }

    fun setLogRetentionDays(days: Int) {
        _uiState.update { it.copy(logRetentionDays = days.coerceIn(1, 30)) }
        saveSettings()
        deleteOldLogs()
    }

    fun nextPage() {
        val totalPages = (_uiState.value.filteredLogs.size + _uiState.value.pageSize - 1) / _uiState.value.pageSize
        if (_uiState.value.currentPage < totalPages - 1) {
            _uiState.update { it.copy(currentPage = it.currentPage + 1) }
        }
    }

    fun previousPage() {
        if (_uiState.value.currentPage > 0) {
            _uiState.update { it.copy(currentPage = it.currentPage - 1) }
        }
    }

    fun goToPage(page: Int) {
        val totalPages = (_uiState.value.filteredLogs.size + _uiState.value.pageSize - 1) / _uiState.value.pageSize
        if (page in 0 until totalPages) {
            _uiState.update { it.copy(currentPage = page) }
        }
    }

    private fun updateFilteredLogs() {
        val filter = _uiState.value.filter.lowercase()
        val level = _uiState.value.level
        val sortOrder = _uiState.value.sortOrder
        
        var filtered = logEntries.filter { entry ->
            val levelMatch = when (level) {
                "All" -> true
                "Debug" -> entry.level == LogLevel.DEBUG
                "Info" -> entry.level == LogLevel.INFO
                "Warn" -> entry.level == LogLevel.WARN
                "Error" -> entry.level == LogLevel.ERROR
                "Verbose" -> entry.level == LogLevel.VERBOSE
                "Fatal" -> entry.level == LogLevel.FATAL
                else -> true
            }
            
            val textMatch = filter.isEmpty() ||
                    entry.tag.lowercase().contains(filter) ||
                    entry.message.lowercase().contains(filter) ||
                    (entry.details?.lowercase()?.contains(filter) ?: false) ||
                    (entry.stackTrace?.lowercase()?.contains(filter) ?: false)
            
            levelMatch && textMatch
        }
        
        filtered = when (sortOrder) {
            SortOrder.NEWEST_FIRST -> filtered.sortedByDescending { it.timestamp }
            SortOrder.OLDEST_FIRST -> filtered.sortedBy { it.timestamp }
            SortOrder.LEVEL -> filtered.sortedBy { it.level.ordinal }
            SortOrder.TAG -> filtered.sortedBy { it.tag }
        }
        
        val startIndex = _uiState.value.currentPage * _uiState.value.pageSize
        val endIndex = minOf(startIndex + _uiState.value.pageSize, filtered.size)
        val paginated = filtered.subList(startIndex, endIndex)
        
        _uiState.update { 
            it.copy(
                filteredLogs = paginated,
                searchResultsCount = filtered.size,
                totalPages = (filtered.size + it.pageSize - 1) / it.pageSize
            )
        }
    }

    private fun updateStatistics() {
        var debug = 0
        var info = 0
        var warn = 0
        var error = 0
        var verbose = 0
        var fatal = 0
        val uniqueTags = mutableSetOf<String>()
        var oldestTimestamp = Long.MAX_VALUE
        var newestTimestamp = 0L
        
        logEntries.forEach { entry ->
            when (entry.level) {
                LogLevel.DEBUG -> debug++
                LogLevel.INFO -> info++
                LogLevel.WARN -> warn++
                LogLevel.ERROR -> error++
                LogLevel.VERBOSE -> verbose++
                LogLevel.FATAL -> fatal++
            }
            uniqueTags.add(entry.tag)
            if (entry.timestamp < oldestTimestamp) oldestTimestamp = entry.timestamp
            if (entry.timestamp > newestTimestamp) newestTimestamp = entry.timestamp
        }
        
        val timeRangeHours = if (oldestTimestamp < newestTimestamp) {
            (newestTimestamp - oldestTimestamp) / (1000f * 3600f)
        } else 1f
        
        _statistics.value = LogStatistics(
            totalCount = logEntries.size,
            debugCount = debug,
            infoCount = info,
            warnCount = warn,
            errorCount = error,
            verboseCount = verbose,
            fatalCount = fatal,
            uniqueTags = uniqueTags.size,
            oldestTimestamp = if (oldestTimestamp != Long.MAX_VALUE) oldestTimestamp else 0,
            newestTimestamp = newestTimestamp,
            averageLogsPerHour = if (timeRangeHours > 0) logEntries.size / timeRangeHours else 0f
        )
    }

    fun exportLogs(format: String = "txt") {
        viewModelScope.launch {
            _uiState.update { it.copy(isExporting = true, errorMessage = null) }
            
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "airmouse_logs_$timestamp.$format"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                withContext(Dispatchers.IO) {
                    FileWriter(file).use { writer ->
                        when (format) {
                            "txt" -> exportAsText(writer)
                            "csv" -> exportAsCsv(writer)
                            "json" -> exportAsJson(writer)
                            "html" -> exportAsHtml(writer)
                        }
                    }
                }
                
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        exportPath = file.absolutePath,
                        successMessage = "Exported to ${file.name}"
                    )
                }
                
                addLogEntry(LogLevel.INFO, "Export", "Logs exported to ${file.name} (${format.uppercase()})")
                clearMessagesAfterDelay()
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isExporting = false,
                        errorMessage = "Export failed: ${e.message}"
                    )
                }
                addLogEntry(LogLevel.ERROR, "Export", "Failed to export logs", e.message)
            }
        }
    }

    private fun exportAsText(writer: FileWriter) {
        writer.write("Air Mouse Pro - Server Logs Export\n")
        writer.write("=".repeat(60) + "\n")
        writer.write("Exported: ${dateFormat.format(Date())}\n")
        writer.write("Total logs: ${logEntries.size}\n")
        writer.write("Filtered logs: ${_uiState.value.searchResultsCount}\n")
        writer.write("=".repeat(60) + "\n\n")
        
        _uiState.value.filteredLogs.forEach { entry ->
            writer.write("[${entry.formattedDate}] ")
            writer.write("[${entry.level.displayName}] ")
            writer.write("[${entry.tag}] ")
            writer.write(entry.message)
            if (entry.details != null) {
                writer.write("\n  Details: ${entry.details}")
            }
            if (entry.stackTrace != null) {
                writer.write("\n  Stack Trace:\n${entry.stackTrace}")
            }
            writer.write("\n")
        }
    }

    private fun exportAsCsv(writer: FileWriter) {
        writer.write("Timestamp,Level,Tag,Message,Details\n")
        _uiState.value.filteredLogs.forEach { entry ->
            writer.write("${entry.formattedDate},${entry.level.displayName},${entry.tag},\"${entry.message.replace("\"", "\"\"")}\",\"${entry.details?.replace("\"", "\"\"") ?: ""}\"\n")
        }
    }

    private fun exportAsJson(writer: FileWriter) {
        val jsonArray = JSONArray()
        _uiState.value.filteredLogs.forEach { entry ->
            val json = JSONObject().apply {
                put("timestamp", entry.timestamp)
                put("formattedTime", entry.formattedDate)
                put("level", entry.level.name)
                put("tag", entry.tag)
                put("message", entry.message)
                entry.details?.let { put("details", it) }
                entry.stackTrace?.let { put("stackTrace", it) }
            }
            jsonArray.put(json)
        }
        writer.write(jsonArray.toString(2))
    }

    private fun exportAsHtml(writer: FileWriter) {
        writer.write("""
            <!DOCTYPE html>
            <html>
            <head>
                <meta charset="UTF-8">
                <title>Air Mouse Pro Logs</title>
                <style>
                    body { font-family: monospace; background: #1e1e1e; color: #d4d4d4; padding: 20px; }
                    .log-entry { margin-bottom: 10px; border-bottom: 1px solid #333; padding-bottom: 5px; }
                    .timestamp { color: #6a9955; }
                    .level-INFO { color: #4ec9b0; }
                    .level-WARN { color: #dcdcaa; }
                    .level-ERROR { color: #f48771; }
                    .level-DEBUG { color: #9cdcfe; }
                    .tag { color: #ce9178; }
                    .message { color: #d4d4d4; }
                </style>
            </head>
            <body>
                <h1>Air Mouse Pro Logs</h1>
                <p>Exported: ${dateFormat.format(Date())}</p>
                <hr>
        """.trimIndent())
        
        _uiState.value.filteredLogs.forEach { entry ->
            writer.write("""
                <div class="log-entry">
                    <span class="timestamp">[${entry.formattedDate}]</span>
                    <span class="level-${entry.level.name}">[${entry.level.displayName}]</span>
                    <span class="tag">[${entry.tag}]</span>
                    <span class="message">${entry.message.replace("<", "&lt;").replace(">", "&gt;")}</span>
                    ${if (entry.details != null) "<br><span class=\"details\">  Details: ${entry.details.replace("<", "&lt;").replace(">", "&gt;")}</span>" else ""}
                </div>
            """.trimIndent())
        }
        
        writer.write("</body></html>")
    }

    fun shareLogs() {
        viewModelScope.launch {
            _uiState.update { it.copy(isSharing = true, errorMessage = null) }
            
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "airmouse_logs_$timestamp.txt"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                withContext(Dispatchers.IO) {
                    FileWriter(file).use { writer ->
                        exportAsText(writer)
                    }
                }
                
                val uri = FileProvider.getUriForFile(
                    context,
                    "${context.packageName}.fileprovider",
                    file
                )
                
                val shareIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_STREAM, uri)
                    addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                }
                
                context.startActivity(Intent.createChooser(shareIntent, "Share Logs"))
                addLogEntry(LogLevel.INFO, "Share", "Logs shared")
                
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(errorMessage = "Share failed: ${e.message}")
                }
                addLogEntry(LogLevel.ERROR, "Share", "Failed to share logs", e.message)
            } finally {
                _uiState.update { it.copy(isSharing = false) }
            }
        }
    }

    fun copyLogsToClipboard() {
        val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val logsText = _uiState.value.filteredLogs.joinToString("\n") { entry ->
            "[${entry.formattedDate}] [${entry.level.displayName}] [${entry.tag}] ${entry.message}"
        }
        
        val clip = ClipData.newPlainText("Air Mouse Logs", logsText)
        clipboard.setPrimaryClip(clip)
        
        addLogEntry(LogLevel.INFO, "System", "${_uiState.value.searchResultsCount} logs copied to clipboard")
        _uiState.update { it.copy(successMessage = "${_uiState.value.searchResultsCount} logs copied") }
        clearMessagesAfterDelay()
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null, exportPath = null) }
    }

    private fun clearMessagesAfterDelay() {
        viewModelScope.launch {
            delay(3000)
            clearMessages()
        }
    }

    fun clearErrorMessage() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    fun addTestLog() {
        val levels = listOf(LogLevel.DEBUG, LogLevel.INFO, LogLevel.WARN, LogLevel.ERROR, LogLevel.VERBOSE, LogLevel.FATAL)
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
            "WiFi signal strength: -45dBm",
            "WebSocket connection established",
            "TCP packet received: size=128 bytes",
            "UDP discovery broadcast sent",
            "Proximity update: near=false, distance=4.2m",
            "Screen locked due to proximity timeout"
        )
        val randomMessage = messages.random()
        val tags = listOf("Network", "Gesture", "Sensor", "Calibration", "Connection", "System")
        val randomTag = tags.random()
        
        addLogEntry(
            level = randomLevel,
            tag = randomTag,
            message = randomMessage,
            details = if (randomLevel == LogLevel.ERROR) "Error code: ${(1000..5000).random()}" else null
        )
        
        _uiState.update { it.copy(successMessage = "Test log added") }
        clearMessagesAfterDelay()
    }

    fun filterByTag(tag: String) {
        _uiState.update { it.copy(filter = tag, currentPage = 0) }
        updateFilteredLogs()
    }

    fun getUniqueTags(): List<String> {
        return logEntries.map { it.tag }.distinct().sorted()
    }

    override fun onCleared() {
        super.onCleared()
        sharedLogObserver?.let { LogManager.logEntries.removeObserver(it) }
        autoRefreshJob?.cancel()
        saveLogs()
    }
}

private fun String.toLogLevel(): LogLevel = when (uppercase(Locale.getDefault())) {
    "DEBUG" -> LogLevel.DEBUG
    "INFO" -> LogLevel.INFO
    "WARN" -> LogLevel.WARN
    "ERROR" -> LogLevel.ERROR
    "VERBOSE" -> LogLevel.VERBOSE
    "FATAL" -> LogLevel.FATAL
    else -> LogLevel.INFO
}
