# 📘 Air Mouse Server Logs Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.logs` package contains the **Server Logs screen** for the Air Mouse application. This screen provides a comprehensive log viewer with real-time streaming, filtering, search, export capabilities, and detailed statistics.

```
com.airmouse.presentation.ui.logs/
├── ServerLogsScreen.kt          # Main logs UI
├── ServerLogsViewModel.kt       # Logs ViewModel
├── ServerLogsUiState.kt         # Logs state models
├── LogEntry.kt                  # Log entry data class
├── LogLevel.kt                  # Log level enum
├── LogFilter.kt                 # Filter options
├── LogStatistics.kt             # Statistics data class
└── LogExport.kt                 # Export functionality
```

---

## 📋 1. ServerLogsScreen

### Purpose
Provides a **comprehensive log viewer** with real-time streaming, filtering, search, export, and statistical analysis of server and application logs.

### Key Features

| Feature | Description |
|---------|-------------|
| **Real-time Logging** | Live log streaming from server |
| **Search & Filter** | Filter by text, log level, tags |
| **Pagination** | Page through large log files |
| **Export** | Export logs as TXT, CSV, JSON, HTML |
| **Statistics** | Log statistics and analytics |
| **Selection Mode** | Select multiple logs for batch operations |
| **Auto-Refresh** | Configurable auto-refresh interval |
| **Customizable Display** | Compact view, font size, word wrap |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLogsScreen(
    navigationActions: NavigationActions,
    viewModel: ServerLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()

    Scaffold(
        topBar = { /* TopAppBar with actions */ },
        floatingActionButton = {
            if (uiState.isSelectionMode && uiState.selectedLogIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.deleteSelectedLogs() },
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                    text = { Text("Delete (${uiState.selectedLogIds.size})") },
                    containerColor = MaterialTheme.colorScheme.error
                )
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search bar
            SearchBar(uiState, viewModel)

            // Overview card
            LogOverviewCard(statistics, uiState, viewModel)

            // Level filter chips
            LevelFilterChips(uiState, viewModel)

            // Log entries list
            LogEntryList(uiState, listState, viewModel)

            // Messages (success/error)
            if (uiState.successMessage != null) {
                SuccessMessage(uiState.successMessage, viewModel)
            }
            if (uiState.errorMessage != null) {
                ErrorMessage(uiState.errorMessage, viewModel)
            }
        }
    }

    // Dialogs
    if (uiState.showStatsDialog) {
        StatisticsDialog(statistics, viewModel)
    }
    if (uiState.showSettingsDialog) {
        SettingsDialog(uiState, viewModel)
    }
    if (uiState.showFilterDialog) {
        FilterDialog(uiState, viewModel)
    }
}
```

---

## 🎯 2. ServerLogsUiState

### Purpose
Defines the **complete state model** for the logs screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `logs` | `List<LogEntry>` | All log entries |
| `filteredLogs` | `List<LogEntry>` | Filtered log entries |
| `filter` | `String` | Search/filter text |
| `level` | `String` | Filter by log level ("All", "Info", "Warn", etc.) |
| `isAutoScroll` | `Boolean` | Auto-scroll to newest logs |
| `isExporting` | `Boolean` | Whether export is in progress |
| `isSharing` | `Boolean` | Whether sharing is in progress |
| `exportPath` | `String?` | Export file path |
| `errorMessage` | `String?` | Error message |
| `successMessage` | `String?` | Success message |
| `selectedLogIds` | `Set<String>` | Selected log IDs |
| `isSelectionMode` | `Boolean` | Whether in selection mode |
| `searchResultsCount` | `Int` | Number of search results |
| `currentPage` | `Int` | Current page number |
| `pageSize` | `Int` | Items per page |
| `totalPages` | `Int` | Total pages |
| `sortOrder` | `SortOrder` | Current sort order |
| `isRefreshing` | `Boolean` | Whether refreshing |
| `autoRefreshEnabled` | `Boolean` | Auto-refresh enabled |
| `autoRefreshInterval` | `Int` | Auto-refresh interval in seconds |
| `logRetentionDays` | `Int` | Log retention in days |
| `showTimestamps` | `Boolean` | Show timestamps |
| `showTags` | `Boolean` | Show tags |
| `fontSize` | `Float` | Font size for logs |
| `compactView` | `Boolean` | Compact view mode |
| `wordWrap` | `Boolean` | Word wrap enabled |

### Enums

```kotlin
enum class SortOrder(val displayName: String) {
    NEWEST_FIRST("Newest First"),
    OLDEST_FIRST("Oldest First"),
    LEVEL("By Level"),
    TAG("By Tag")
}

enum class LogLevel(val displayName: String, val color: Long) {
    DEBUG("Debug", 0xFF9E9E9E),
    INFO("Info", 0xFF2196F3),
    WARN("Warn", 0xFFFF9800),
    ERROR("Error", 0xFFF44336),
    VERBOSE("Verbose", 0xFF8BC34A),
    FATAL("Fatal", 0xFF9C27B0)
}
```

### LogEntry

```kotlin
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
```

### LogStatistics

```kotlin
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
        get() = if (oldestTimestamp > 0) 
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(oldestTimestamp)) 
        else "N/A"
    
    val formattedNewest: String
        get() = if (newestTimestamp > 0) 
            SimpleDateFormat("MMM dd, HH:mm", Locale.getDefault()).format(Date(newestTimestamp)) 
        else "N/A"
}
```

---

## 🎯 3. ServerLogsViewModel

### Purpose
Manages **log capture, filtering, export, and persistence**.

### Key State Properties

| Property | Type | Description |
|----------|------|-------------|
| `uiState` | `StateFlow<ServerLogsUiState>` | Complete UI state |
| `statistics` | `StateFlow<LogStatistics>` | Log statistics |
| `isConnected` | `StateFlow<Boolean>` | Connection status |
| `logEntries` | `MutableList<LogEntry>` | All log entries |

### Key Methods

| Method | Purpose |
|--------|---------|
| `addLogEntry(level, tag, message, details, stackTrace)` | Add a new log entry |
| `setFilter(filter)` | Set search/filter text |
| `setLevel(level)` | Filter by log level |
| `setSortOrder(order)` | Set sort order |
| `toggleAutoScroll()` | Toggle auto-scroll |
| `toggleAutoRefresh()` | Toggle auto-refresh |
| `setAutoRefreshInterval(seconds)` | Set auto-refresh interval |
| `refreshLogs()` | Manually refresh logs |
| `toggleCompactView()` | Toggle compact view |
| `setFontSize(size)` | Set font size |
| `toggleWordWrap()` | Toggle word wrap |
| `toggleShowTimestamps()` | Toggle timestamps |
| `toggleShowTags()` | Toggle tags |
| `toggleSelectionMode()` | Toggle selection mode |
| `toggleLogSelection(logId)` | Select/deselect a log |
| `selectAllLogs()` | Select all visible logs |
| `clearSelection()` | Clear selection |
| `deleteSelectedLogs()` | Delete selected logs |
| `clearLogs()` | Clear all logs |
| `deleteOldLogs()` | Delete logs older than retention days |
| `setLogRetentionDays(days)` | Set log retention |
| `exportLogs(format)` | Export logs in specified format |
| `shareLogs()` | Share logs via system share |
| `copyLogsToClipboard()` | Copy logs to clipboard |
| `addTestLog()` | Add a test log entry |
| `filterByTag(tag)` | Filter by tag |
| `getUniqueTags()` | Get all unique tags |

### Log Capture

```kotlin
private fun setupLogCapture() {
    // Observe connection status
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
    
    // Observe connection quality
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
}
```

### Log Filtering

```kotlin
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
    
    // Paginate
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
```

### Export

```kotlin
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
        } catch (e: Exception) {
            _uiState.update {
                it.copy(isExporting = false, errorMessage = "Export failed: ${e.message}")
            }
        }
    }
}
```

---

## 🧩 4. UI Components

### LogEntryItem

```kotlin
@Composable
fun LogEntryItem(
    log: LogEntry,
    isSelected: Boolean,
    isSelectionMode: Boolean,
    compactView: Boolean,
    fontSize: Float,
    showTimestamps: Boolean,
    showTags: Boolean,
    wordWrap: Boolean,
    onSelect: () -> Unit,
    onTagClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { if (isSelectionMode) onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(10.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(if (compactView) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            // Selection checkbox
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.size(20.dp)
                )
            }

            // Level indicator bar
            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (compactView) 48.dp else 72.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(log.levelColor)
            )

            // Content
            Column(modifier = Modifier.weight(1f)) {
                // Header (level, tag, timestamp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Surface(
                        color = log.levelColor.copy(alpha = 0.14f),
                        contentColor = log.levelColor,
                        shape = RoundedCornerShape(999.dp)
                    ) {
                        Text(
                            text = log.level.displayName,
                            fontSize = if (compactView) 9.sp else 10.sp,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                            fontFamily = FontFamily.Monospace
                        )
                    }

                    if (showTags) {
                        Surface(
                            modifier = Modifier.clickable { onTagClick(log.tag) },
                            shape = RoundedCornerShape(999.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer
                        ) {
                            Text(
                                text = log.tag,
                                fontSize = if (compactView) 9.sp else 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
                                fontFamily = FontFamily.Monospace
                            )
                        }
                    }

                    if (showTimestamps) {
                        Text(
                            text = log.formattedTime,
                            fontSize = if (compactView) 9.sp else 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontFamily = FontFamily.Monospace
                        )
                    }
                }

                Spacer(modifier = Modifier.height(4.dp))

                // Message
                Text(
                    text = log.message,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = wordWrap,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (compactView && !wordWrap) 2 else 4
                )

                // Details
                if (log.details != null && !compactView) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Surface(
                        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                        shape = RoundedCornerShape(8.dp)
                    ) {
                        Text(
                            text = log.details,
                            fontSize = (fontSize - 1).sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                        )
                    }
                }

                // Stack trace (expandable)
                if (log.stackTrace != null && !compactView) {
                    var expanded by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { expanded = !expanded }
                            .padding(top = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            if (expanded) Icons.Default.KeyboardArrowDown else Icons.Default.KeyboardArrowRight,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            "Stack trace",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (expanded) {
                        Surface(
                            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.55f),
                            shape = RoundedCornerShape(8.dp),
                            modifier = Modifier.padding(top = 4.dp)
                        ) {
                            Text(
                                text = log.stackTrace,
                                fontSize = (fontSize - 1).sp,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.padding(10.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
```

### LogOverviewCard

```kotlin
@Composable
fun LogOverviewCard(
    statistics: LogStatistics,
    uiState: ServerLogsUiState,
    viewModel: ServerLogsViewModel
) {
    val activeLogs = uiState.searchResultsCount
    val criticalLogs = statistics.warnCount + statistics.errorCount + statistics.fatalCount

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Log stream", fontWeight = FontWeight.SemiBold)
                }
                if (uiState.filter.isNotEmpty() || uiState.level != "All") {
                    TextButton(onClick = { 
                        viewModel.setFilter("")
                        viewModel.setLevel("All")
                    }) {
                        Text("Clear filters")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogMetricChip("Total", statistics.totalCount.toString(), modifier = Modifier.weight(1f))
                LogMetricChip("Visible", activeLogs.toString(), modifier = Modifier.weight(1f))
                LogMetricChip("Critical", criticalLogs.toString(), modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = uiState.isSelectionMode,
                    onClick = { viewModel.toggleSelectionMode() },
                    label = { Text("Selection") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.isAutoScroll,
                    onClick = { viewModel.toggleAutoScroll() },
                    label = { Text("Auto-scroll") },
                    modifier = Modifier.weight(1f)
                )
                Surface(
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                    shape = RoundedCornerShape(999.dp)
                ) {
                    Text(
                        text = if (uiState.sortOrder == SortOrder.NEWEST_FIRST) "Newest first" else "Oldest first",
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
    }
}
```

---

## 📊 Logs Screen Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        LOGS SCREEN FLOW                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. LOG CAPTURE                                                        │
│     ├── Connection status changes                                     │
│     ├── Connection quality updates                                    │
│     ├── Custom log entries                                            │
│     └── Shared log stream from LogManager                            │
│                                                                         │
│  2. LOG STORAGE                                                        │
│     ├── Store in memory (MAX_LOGS = 5000)                            │
│     ├── Save to preferences (SAVED_LOGS_KEY)                         │
│     └── Load on startup                                              │
│                                                                         │
│  3. LOG FILTERING                                                      │
│     ├── Text search                                                   │
│     ├── Log level filter                                              │
│     ├── Sort order                                                    │
│     └── Pagination                                                    │
│                                                                         │
│  4. LOG DISPLAY                                                        │
│     ├── Render filtered logs                                         │
│     ├── Expandable details/stack traces                              │
│     └── Auto-scroll to newest                                        │
│                                                                         │
│  5. LOG MANAGEMENT                                                     │
│     ├── Select/Deselect                                               │
│     ├── Delete selected                                               │
│     ├── Clear all                                                     │
│     └── Delete old (retention)                                       │
│                                                                         │
│  6. LOG EXPORT                                                         │
│     ├── TXT format                                                    │
│     ├── CSV format                                                    │
│     ├── JSON format                                                   │
│     ├── HTML format                                                   │
│     └── Share via system share                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📊 Statistics Calculation

```kotlin
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
```

---

## 📋 Export Formats

### TXT Format
```
Air Mouse Pro - Server Logs Export
============================================================
Exported: 2025-01-15 14:30:00
Total logs: 150
Filtered logs: 45
============================================================

[2025-01-15 14:28:15.123] [Info] [Connection] Connected to server
[2025-01-15 14:28:20.456] [Debug] [Sensor] Gyroscope initialized: 100Hz
```

### CSV Format
```
Timestamp,Level,Tag,Message,Details
2025-01-15 14:28:15.123,Info,Connection,Connected to server,
2025-01-15 14:28:20.456,Debug,Sensor,Gyroscope initialized: 100Hz,
```

### JSON Format
```json
[
  {
    "timestamp": 1700000000123,
    "formattedTime": "2025-01-15 14:28:15.123",
    "level": "INFO",
    "tag": "Connection",
    "message": "Connected to server"
  }
]
```

### HTML Format
```html
<!DOCTYPE html>
<html>
<head>
    <title>Air Mouse Pro Logs</title>
    <style>
        body { font-family: monospace; background: #1e1e1e; color: #d4d4d4; }
        .log-entry { margin-bottom: 10px; border-bottom: 1px solid #333; }
        .level-INFO { color: #4ec9b0; }
    </style>
</head>
<body>
    <div class="log-entry">
        <span class="timestamp">[2025-01-15 14:28:15.123]</span>
        <span class="level-INFO">[Info]</span>
        <span class="tag">[Connection]</span>
        <span class="message">Connected to server</span>
    </div>
</body>
</html>
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Real-time** | Live log streaming from server |
| **Searchable** | Full-text search with filters |
| **Exportable** | Multiple export formats |
| **Manageable** | Selection, deletion, retention |
| **Customizable** | Compact view, font size, word wrap |
| **Performant** | Pagination, lazy loading |
| **Persistent** | Logs saved to preferences |
| **Informative** | Detailed statistics |

---

**The Server Logs Screen provides a comprehensive, real-time log viewer with powerful filtering, export, and management capabilities for debugging and monitoring the Air Mouse application.**