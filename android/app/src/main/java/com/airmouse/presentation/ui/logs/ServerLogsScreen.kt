package com.airmouse.presentation.ui.logs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.outlined.CheckBox
import androidx.compose.material.icons.outlined.VerticalAlignBottom
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLogsScreen(
    navigationActions: NavigationActions,
    viewModel: ServerLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val statistics by viewModel.statistics.collectAsStateWithLifecycle()
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()
    var showStatsDialog by remember { mutableStateOf(false) }
    var showSettingsDialog by remember { mutableStateOf(false) }
    var showFilterDialog by remember { mutableStateOf(false) }

    
    LaunchedEffect(uiState.filteredLogs.size) {
        if (uiState.isAutoScroll && uiState.filteredLogs.isNotEmpty() && uiState.currentPage == 0) {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text("Server Logs", fontWeight = FontWeight.Bold)
                        if (uiState.isRefreshing) {
                            Spacer(modifier = Modifier.width(8.dp))
                            CircularProgressIndicator(modifier = Modifier.size(16.dp), strokeWidth = 2.dp)
                        }
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Statistics")
                    }
                    
                    IconButton(onClick = { showSettingsDialog = true }) {
                        Icon(Icons.Default.Settings, contentDescription = "Settings")
                    }
                    
                    IconButton(onClick = { showFilterDialog = true }) {
                        Badge(
                            containerColor = if (uiState.filter.isNotEmpty() || uiState.level != "All") 
                                MaterialTheme.colorScheme.primary 
                            else Color.Transparent
                        ) {
                            Icon(Icons.Default.FilterList, contentDescription = "Filter")
                        }
                    }
                    
                    IconButton(onClick = { viewModel.refreshLogs() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.isSelectionMode && uiState.selectedLogIds.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.deleteSelectedLogs() },
                    icon = { Icon(Icons.Default.Delete, contentDescription = "Delete") },
                    text = { Text("Delete (${uiState.selectedLogIds.size})") },
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.padding(16.dp)
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
            
            OutlinedTextField(
                value = uiState.filter,
                onValueChange = viewModel::setFilter,
                label = { Text("Search logs...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.filter.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setFilter("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp)
            )

            Spacer(modifier = Modifier.height(8.dp))

            
            LogOverviewCard(
                statistics = statistics,
                uiState = uiState,
                onToggleSelectionMode = { viewModel.toggleSelectionMode() },
                onToggleAutoScroll = { viewModel.toggleAutoScroll() },
                onClearFilter = {
                    viewModel.setFilter("")
                    viewModel.setLevel("All")
                }
            )

            Spacer(modifier = Modifier.height(8.dp))

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                FilterChip(
                    selected = uiState.level == "All",
                    onClick = { viewModel.setLevel("All") },
                    label = { Text("All") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.level == "Info",
                    onClick = { viewModel.setLevel("Info") },
                    label = { Text("Info") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFF2196F3).copy(alpha = 0.2f)
                    )
                )
                FilterChip(
                    selected = uiState.level == "Warn",
                    onClick = { viewModel.setLevel("Warn") },
                    label = { Text("Warn") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFFF9800).copy(alpha = 0.2f)
                    )
                )
                FilterChip(
                    selected = uiState.level == "Error",
                    onClick = { viewModel.setLevel("Error") },
                    label = { Text("Error") },
                    modifier = Modifier.weight(1f),
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color(0xFFF44336).copy(alpha = 0.2f)
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.filteredLogs.isEmpty()) {
                    EmptyLogsState(
                        hasFilter = uiState.filter.isNotEmpty() || uiState.level != "All",
                        onClearFilter = { viewModel.setFilter(""); viewModel.setLevel("All") },
                        onAddTestLog = { viewModel.addTestLog() }
                    )
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.filteredLogs, key = { it.id }) { log ->
                            LogEntryItem(
                                log = log,
                                isSelected = uiState.selectedLogIds.contains(log.id),
                                isSelectionMode = uiState.isSelectionMode,
                                compactView = uiState.compactView,
                                fontSize = uiState.fontSize,
                                showTimestamps = uiState.showTimestamps,
                                showTags = uiState.showTags,
                                wordWrap = uiState.wordWrap,
                                onSelect = { viewModel.toggleLogSelection(log.id) },
                                onTagClick = { viewModel.filterByTag(it) }
                            )
                        }
                        
                        
                        if (uiState.totalPages > 1) {
                            item {
                                PaginationControls(
                                    currentPage = uiState.currentPage,
                                    totalPages = uiState.totalPages,
                                    onPageChange = { viewModel.goToPage(it) },
                                    onNext = { viewModel.nextPage() },
                                    onPrevious = { viewModel.previousPage() }
                                )
                            }
                        }
                    }
                }
            }

            
            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    shape = RoundedCornerShape(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.successMessage!!)
                }
            }
            
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    containerColor = MaterialTheme.colorScheme.error,
                    shape = RoundedCornerShape(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("Dismiss", color = Color.White)
                        }
                    }
                ) {
                    Text(uiState.errorMessage!!, color = Color.White)
                }
            }
        }
    }

    
    if (showStatsDialog) {
        StatisticsDialog(
            statistics = statistics,
            onDismiss = { showStatsDialog = false }
        )
    }

    
    if (showSettingsDialog) {
        SettingsDialog(
            uiState = uiState,
            onAutoRefreshChange = { viewModel.toggleAutoRefresh() },
            onAutoRefreshIntervalChange = { viewModel.setAutoRefreshInterval(it) },
            onRetentionDaysChange = { viewModel.setLogRetentionDays(it) },
            onCompactViewChange = { viewModel.toggleCompactView() },
            onFontSizeChange = { viewModel.setFontSize(it) },
            onWordWrapChange = { viewModel.toggleWordWrap() },
            onShowTimestampsChange = { viewModel.toggleShowTimestamps() },
            onShowTagsChange = { viewModel.toggleShowTags() },
            onClearLogs = { viewModel.clearLogs() },
            onExportLogs = { format -> viewModel.exportLogs(format) },
            onDismiss = { showSettingsDialog = false }
        )
    }

    
    if (showFilterDialog) {
        FilterDialog(
            currentFilter = uiState.filter,
            currentLevel = uiState.level,
            sortOrder = uiState.sortOrder,
            availableTags = viewModel.getUniqueTags(),
            onFilterChange = { viewModel.setFilter(it) },
            onLevelChange = { viewModel.setLevel(it) },
            onSortChange = { viewModel.setSortOrder(it) },
            onTagSelect = { viewModel.filterByTag(it) },
            onClearFilters = { viewModel.setFilter(""); viewModel.setLevel("All") },
            onDismiss = { showFilterDialog = false }
        )
    }
}

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
            .animateContentSize(),
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
                .clickable { if (isSelectionMode) onSelect() }
                .padding(if (compactView) 8.dp else 12.dp),
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            verticalAlignment = Alignment.Top
        ) {
            if (isSelectionMode) {
                Checkbox(
                    checked = isSelected,
                    onCheckedChange = { onSelect() },
                    modifier = Modifier.size(20.dp)
                )
            }

            Box(
                modifier = Modifier
                    .width(4.dp)
                    .height(if (compactView) 48.dp else 72.dp)
                    .clip(RoundedCornerShape(2.dp))
                    .background(log.levelColor)
            )

            Column(modifier = Modifier.weight(1f)) {
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

                Text(
                    text = log.message,
                    fontSize = fontSize.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurface,
                    softWrap = wordWrap,
                    overflow = TextOverflow.Ellipsis,
                    maxLines = if (compactView && !wordWrap) 2 else 4
                )

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

@Composable
fun LogOverviewCard(
    statistics: LogStatistics,
    uiState: ServerLogsUiState,
    onToggleSelectionMode: () -> Unit,
    onToggleAutoScroll: () -> Unit,
    onClearFilter: () -> Unit
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
                    Text(
                        text = "Log stream",
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                }
                if (uiState.filter.isNotEmpty() || uiState.level != "All") {
                    TextButton(onClick = onClearFilter) {
                        Text("Clear filters")
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                LogMetricChip(label = "Total", value = statistics.totalCount.toString(), modifier = Modifier.weight(1f))
                LogMetricChip(label = "Visible", value = activeLogs.toString(), modifier = Modifier.weight(1f))
                LogMetricChip(label = "Critical", value = criticalLogs.toString(), modifier = Modifier.weight(1f))
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                FilterChip(
                    selected = uiState.isSelectionMode,
                    onClick = onToggleSelectionMode,
                    label = { Text("Selection") },
                    modifier = Modifier.weight(1f)
                )
                FilterChip(
                    selected = uiState.isAutoScroll,
                    onClick = onToggleAutoScroll,
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

@Composable
fun LogMetricChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier
) {
    Surface(
        modifier = modifier,
        color = MaterialTheme.colorScheme.surface,
        shape = RoundedCornerShape(10.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
fun EmptyLogsState(hasFilter: Boolean, onClearFilter: () -> Unit, onAddTestLog: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            Icons.Default.List,
            contentDescription = "No logs",
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "No logs to display",
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = if (hasFilter) "Try clearing your filters to widen the feed." else "Logs from the server will appear here.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        
        if (hasFilter) {
            Button(onClick = onClearFilter) {
                Text("Clear Filters")
            }
        } else {
            OutlinedButton(onClick = onAddTestLog) {
                Text("Add Test Log")
            }
        }
    }
}

@Composable
fun PaginationControls(
    currentPage: Int,
    totalPages: Int,
    onPageChange: (Int) -> Unit,
    onNext: () -> Unit,
    onPrevious: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        IconButton(
            onClick = onPrevious,
            enabled = currentPage > 0
        ) {
            Icon(Icons.Default.ChevronLeft, contentDescription = "Previous")
        }
        
        Text(
            text = "Page ${currentPage + 1} of $totalPages",
            modifier = Modifier.padding(horizontal = 16.dp),
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        IconButton(
            onClick = onNext,
            enabled = currentPage < totalPages - 1
        ) {
            Icon(Icons.Default.ChevronRight, contentDescription = "Next")
        }
    }
}

@Composable
fun StatisticsDialog(statistics: LogStatistics, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log statistics") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp)
            ) {
                StatisticRow("Total logs", statistics.totalCount.toString())
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                StatisticRow("Debug", statistics.debugCount.toString(), Color(0xFF9E9E9E))
                StatisticRow("Info", statistics.infoCount.toString(), Color(0xFF2196F3))
                StatisticRow("Warning", statistics.warnCount.toString(), Color(0xFFFF9800))
                StatisticRow("Error", statistics.errorCount.toString(), Color(0xFFF44336))
                StatisticRow("Verbose", statistics.verboseCount.toString(), Color(0xFF8BC34A))
                StatisticRow("Fatal", statistics.fatalCount.toString(), Color(0xFF9C27B0))
                Divider(modifier = Modifier.padding(vertical = 4.dp))
                StatisticRow("Unique tags", statistics.uniqueTags.toString())
                StatisticRow("Oldest log", statistics.formattedOldest)
                StatisticRow("Newest log", statistics.formattedNewest)
                StatisticRow(
                    "Average logs/hour",
                    String.format(java.util.Locale.US, "%.1f", statistics.averageLogsPerHour)
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun StatisticRow(label: String, value: String, color: Color? = null) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Bold,
            color = color ?: MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
fun SectionHeader(
    title: String,
    color: Color = MaterialTheme.colorScheme.onSurface
) {
    Text(
        text = title,
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = color,
        modifier = Modifier.padding(bottom = 6.dp)
    )
}

@Composable
fun SettingsDialog(
    uiState: ServerLogsUiState,
    onAutoRefreshChange: () -> Unit,
    onAutoRefreshIntervalChange: (Int) -> Unit,
    onRetentionDaysChange: (Int) -> Unit,
    onCompactViewChange: () -> Unit,
    onFontSizeChange: (Float) -> Unit,
    onWordWrapChange: () -> Unit,
    onShowTimestampsChange: () -> Unit,
    onShowTagsChange: () -> Unit,
    onClearLogs: () -> Unit,
    onExportLogs: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Log settings") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                
                SectionHeader("Live updates")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Auto refresh")
                    Switch(
                        checked = uiState.autoRefreshEnabled,
                        onCheckedChange = { onAutoRefreshChange() }
                    )
                }
                
                if (uiState.autoRefreshEnabled) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Refresh interval (seconds)", fontSize = 12.sp)
                    Slider(
                        value = uiState.autoRefreshInterval.toFloat(),
                        onValueChange = { onAutoRefreshIntervalChange(it.toInt()) },
                        valueRange = 1f..30f,
                        steps = 29
                    )
                    Text("${uiState.autoRefreshInterval} seconds", fontSize = 11.sp, color = Color.Gray)
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                
                SectionHeader("Display")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Compact view")
                    Switch(
                        checked = uiState.compactView,
                        onCheckedChange = { onCompactViewChange() }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Word wrap")
                    Switch(
                        checked = uiState.wordWrap,
                        onCheckedChange = { onWordWrapChange() }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show timestamps")
                    Switch(
                        checked = uiState.showTimestamps,
                        onCheckedChange = { onShowTimestampsChange() }
                    )
                }
                
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text("Show tags")
                    Switch(
                        checked = uiState.showTags,
                        onCheckedChange = { onShowTagsChange() }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                Text("Font size: ${uiState.fontSize.toInt()}sp", fontSize = 12.sp)
                Slider(
                    value = uiState.fontSize,
                    onValueChange = onFontSizeChange,
                    valueRange = 8f..20f,
                    steps = 12
                )
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                
                SectionHeader("Retention")
                Text("Log retention (days)", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Slider(
                    value = uiState.logRetentionDays.toFloat(),
                    onValueChange = { onRetentionDaysChange(it.toInt()) },
                    valueRange = 1f..30f,
                    steps = 29
                )
                Text("${uiState.logRetentionDays} days", fontSize = 12.sp, color = Color.Gray)
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                
                SectionHeader("Export")
                Text("Export format", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(onClick = { onExportLogs("txt") }, modifier = Modifier.weight(1f)) {
                        Text("TXT")
                    }
                    OutlinedButton(onClick = { onExportLogs("csv") }, modifier = Modifier.weight(1f)) {
                        Text("CSV")
                    }
                    OutlinedButton(onClick = { onExportLogs("json") }, modifier = Modifier.weight(1f)) {
                        Text("JSON")
                    }
                    OutlinedButton(onClick = { onExportLogs("html") }, modifier = Modifier.weight(1f)) {
                        Text("HTML")
                    }
                }
                
                Divider(modifier = Modifier.padding(vertical = 12.dp))
                
                
                SectionHeader("Danger zone", color = MaterialTheme.colorScheme.error)
                Spacer(modifier = Modifier.height(8.dp))
                Button(
                    onClick = onClearLogs,
                    colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Clear All Logs")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun FilterDialog(
    currentFilter: String,
    currentLevel: String,
    sortOrder: SortOrder,
    availableTags: List<String>,
    onFilterChange: (String) -> Unit,
    onLevelChange: (String) -> Unit,
    onSortChange: (SortOrder) -> Unit,
    onTagSelect: (String) -> Unit,
    onClearFilters: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Filter logs") },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .heightIn(max = 400.dp)
                    .verticalScroll(rememberScrollState())
                    .padding(vertical = 8.dp)
            ) {
                
                OutlinedTextField(
                    value = currentFilter,
                    onValueChange = onFilterChange,
                    label = { Text("Search") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true
                )
                
                Spacer(modifier = Modifier.height(12.dp))
                
                
                SectionHeader("Log level")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("All", "Info", "Warn", "Error").forEach { level ->
                        FilterChip(
                            selected = currentLevel == level,
                            onClick = { onLevelChange(level) },
                            label = { Text(level) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.height(12.dp))
                
                
                SectionHeader("Sort by")
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    SortOrder.values().forEach { order ->
                        FilterChip(
                            selected = sortOrder == order,
                            onClick = { onSortChange(order) },
                            label = { Text(order.displayName) },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
                
                if (availableTags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(12.dp))
                    SectionHeader("Filter by tag")
                    LazyColumn(
                        modifier = Modifier.heightIn(max = 150.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(availableTags) { tag ->
                            TextButton(
                                onClick = { onTagSelect(tag) },
                                modifier = Modifier.fillMaxWidth()
                            ) {
                                Text(tag, modifier = Modifier.padding(vertical = 4.dp))
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Row {
                TextButton(onClick = onClearFilters) {
                    Text("Clear All")
                }
                TextButton(onClick = onDismiss) {
                    Text("Apply")
                }
            }
        }
    )
}
