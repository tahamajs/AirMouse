package com.airmouse.presentation.ui.logs

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
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
    val listState = rememberLazyListState()
    val coroutineScope = rememberCoroutineScope()

    // Auto-scroll to bottom when new logs arrive
    LaunchedEffect(uiState.filteredLogs.size) {
        if (uiState.isAutoScroll && uiState.filteredLogs.isNotEmpty()) {
            coroutineScope.launch {
                listState.animateScrollToItem(0)
            }
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Server Logs") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.addTestLog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Test Log")
                    }
                    IconButton(onClick = { viewModel.shareLogs() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { viewModel.toggleAutoScroll() }) {
                        Icon(
                            imageVector = if (uiState.isAutoScroll) Icons.Filled.VerticalAlignBottom else Icons.Outlined.VerticalAlignBottom,
                            contentDescription = "Auto-scroll"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Search and Filter Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    // Search Input
                    OutlinedTextField(
                        value = uiState.filter,
                        onValueChange = viewModel::setFilter,
                        label = { Text("Filter logs...") },
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                        trailingIcon = {
                            if (uiState.filter.isNotEmpty()) {
                                IconButton(onClick = { viewModel.setFilter("") }) {
                                    Icon(Icons.Default.Close, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    // Action Buttons
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = viewModel::clearLogs,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.error
                            )
                        ) {
                            Icon(Icons.Default.Delete, contentDescription = "Clear")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Clear")
                        }

                        Button(
                            onClick = viewModel::exportLogs,
                            modifier = Modifier.weight(1f),
                            enabled = !uiState.isExporting
                        ) {
                            if (uiState.isExporting) {
                                CircularProgressIndicator(modifier = Modifier.size(16.dp))
                            } else {
                                Icon(Icons.Default.Save, contentDescription = "Export")
                            }
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(if (uiState.isExporting) "Exporting..." else "Export")
                        }

                        Button(
                            onClick = viewModel::copyLogsToClipboard,
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.ContentCopy, contentDescription = "Copy")
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Copy")
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Level Filters
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
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Log Stats
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "Total: ${uiState.filteredLogs.size} logs",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (uiState.filter.isNotEmpty()) {
                        Text(
                            text = "Filtered by: \"${uiState.filter}\"",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                    if (uiState.exportPath != null) {
                        Text(
                            text = "✓ Exported",
                            fontSize = 12.sp,
                            color = Color(0xFF4CAF50)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Logs List
            Card(
                modifier = Modifier.fillMaxSize(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            ) {
                if (uiState.filteredLogs.isEmpty()) {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.List,
                                contentDescription = "No logs",
                                modifier = Modifier.size(48.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "No logs to display",
                                style = MaterialTheme.typography.titleMedium,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            if (uiState.filter.isNotEmpty()) {
                                TextButton(onClick = { viewModel.setFilter("") }) {
                                    Text("Clear Filter")
                                }
                            } else {
                                TextButton(onClick = { viewModel.addTestLog() }) {
                                    Text("Add Test Log")
                                }
                            }
                        }
                    }
                } else {
                    LazyColumn(
                        state = listState,
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(8.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp)
                    ) {
                        items(uiState.filteredLogs) { log ->
                            LogEntryItem(log = log)
                        }
                    }
                }
            }

            // Error Message
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(16.dp),
                    action = {
                        TextButton(onClick = viewModel::clearErrorMessage) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.errorMessage!!)
                }
            }
        }
    }
}

@Composable
fun LogEntryItem(log: LogEntry) {
    val levelColor = Color(log.level.color)
    val dateFormat = remember { java.text.SimpleDateFormat("HH:mm:ss.SSS", java.util.Locale.getDefault()) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    // Level indicator
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(levelColor, shape = androidx.compose.foundation.shape.CircleShape)
                    )

                    // Level text
                    Text(
                        text = log.level.displayName,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        color = levelColor,
                        fontFamily = FontFamily.Monospace
                    )

                    // Tag
                    Text(
                        text = log.tag,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontFamily = FontFamily.Monospace
                    )
                }

                // Timestamp
                Text(
                    text = dateFormat.format(Date(log.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    fontFamily = FontFamily.Monospace
                )
            }

            Spacer(modifier = Modifier.height(4.dp))

            // Message
            Text(
                text = log.message,
                fontSize = 12.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurface
            )

            // Details (if any)
            if (log.details != null) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = log.details,
                    fontSize = 10.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}