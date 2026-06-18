package com.airmouse.presentation.ui.network

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.automirrored.filled.Comment
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==================== MAIN SCREEN ====================

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NetworkDiscoveryScreen(
    navigationActions: NavigationActions,
    viewModel: NetworkDiscoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredServers by viewModel.filteredServers.collectAsStateWithLifecycle()
    val filteredSavedServers by viewModel.filteredSavedServers.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }
    var showManualConnectDialog by remember { mutableStateOf(false) }
    fun clearConnectionHistory() {
        viewModelScope.launch {
            // Step A: Perform the heavy lifting (Data Layer)
            // If you are using a database (e.g., Room), do the delete operation here.
            // repository.clearConnectionHistory()

            // Step B: Update the state so the UI reacts
            _uiState.update { currentState ->
                currentState.copy(
                    connectionHistory = emptyList(), // Clear the list
                    status = "History cleared"         // Optional: Update status message
                )
            }
        }
    }
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Network Discovery",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.Sort,
                            contentDescription = "Sort"
                        )
                    }
                    IconButton(onClick = { viewModel.refreshScan() }) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (uiState.isScanning) "Stop Scan" else "Refresh"
                        )
                    }
                    IconButton(onClick = { showManualConnectDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "Manual Connect"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.isConnecting) {
                FloatingActionButton(
                    onClick = { viewModel.disconnect() },
                    containerColor = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Connection"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Sort Dropdown
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortBy.entries.forEach { sortBy ->
                    DropdownMenuItem(
                        text = { Text(sortBy.displayName) },
                        onClick = {
                            viewModel.setSortBy(sortBy)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (uiState.sortBy == sortBy) {
                                Icon(
                                    imageVector = Icons.Default.Check,
                                    contentDescription = "Selected"
                                )
                            }
                        }
                    )
                }
            }

            // Manual Connect Dialog
            if (showManualConnectDialog) {
                ManualConnectDialog(
                    onDismiss = { showManualConnectDialog = false },
                    onConnect = { ip: String, port: Int ->
                        scope.launch {
                            viewModel.connectManual(ip, port)
                        }
                        showManualConnectDialog = false
                    },
                    initialIp = uiState.manualIp,
                    initialPort = uiState.customPort
                )
            }

            // Search Bar
            OutlinedTextField(
                value = uiState.filterText,
                onValueChange = viewModel::setFilterText,
                label = { Text("Filter servers...") },
                leadingIcon = {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Search"
                    )
                },
                trailingIcon = {
                    if (uiState.filterText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setFilterText("") }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Clear"
                            )
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = MaterialTheme.colorScheme.primary,
                    unfocusedBorderColor = MaterialTheme.colorScheme.outline
                )
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Network Info Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Local IP Address", style = MaterialTheme.typography.labelSmall)
                        Text(
                            viewModel.getLocalIpAddress() ?: "Not connected",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace
                        )
                        if (uiState.lastScanTime != null) {
                            Text(
                                "Last scan: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(uiState.lastScanTime)}",
                                fontSize = 10.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        if (viewModel.isWifiConnected()) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "WiFi Connected",
                                tint = Color(0xFF4CAF50)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Connected", fontSize = 12.sp, color = Color(0xFF4CAF50))
                        } else {
                            Icon(
                                imageVector = Icons.Default.WifiOff,
                                contentDescription = "No WiFi",
                                tint = Color(0xFFF44336)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("No WiFi", fontSize = 12.sp, color = Color(0xFFF44336))
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scan Button
            Button(
                onClick = { viewModel.scanNetwork() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning,
                shape = RoundedCornerShape(12.dp)
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(
                        progress = { uiState.scanProgress / 100f },
                        modifier = Modifier.size(20.dp),
                        strokeWidth = 2.dp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning... ${uiState.scanProgress}%")
                } else {
                    Icon(
                        imageVector = Icons.Default.Search,
                        contentDescription = "Scan Network"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Network")
                }
            }

            if (uiState.isScanning) {
                Spacer(modifier = Modifier.height(8.dp))
                LinearProgressIndicator(
                    progress = { uiState.scanProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary,
                    trackColor = MaterialTheme.colorScheme.surfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Status and Error - FIXED: Local variable for smart cast
            val errorMessage = uiState.errorMessage
            if (errorMessage != null) {
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(8.dp),
                    color = MaterialTheme.colorScheme.errorContainer
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = errorMessage,
                            color = MaterialTheme.colorScheme.onErrorContainer,
                            fontSize = 12.sp,
                            modifier = Modifier.weight(1f)
                        )
                        IconButton(onClick = { viewModel.clearErrorMessage() }) {
                            Icon(
                                imageVector = Icons.Default.Close,
                                contentDescription = "Dismiss Error",
                                tint = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
            } else {
                Text(
                    text = uiState.status,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            ScrollableTabRow(
                selectedTabIndex = when (uiState.activeTab) {
                    DiscoveryTab.DISCOVERED -> 0
                    DiscoveryTab.SAVED -> 1
                    DiscoveryTab.HISTORY -> 2
                },
                containerColor = MaterialTheme.colorScheme.surface,
                edgePadding = 0.dp,
                divider = {}
            ) {
                Tab(
                    selected = uiState.activeTab == DiscoveryTab.DISCOVERED,
                    onClick = { viewModel.setActiveTab(DiscoveryTab.DISCOVERED) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Wifi,
                                contentDescription = "Discovered",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Discovered")
                            if (uiState.discoveredServers.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier.padding(start = 4.dp),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text("${uiState.discoveredServers.size}", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = uiState.activeTab == DiscoveryTab.SAVED,
                    onClick = { viewModel.setActiveTab(DiscoveryTab.SAVED) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Bookmark,
                                contentDescription = "Saved",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Saved")
                            if (uiState.savedServers.isNotEmpty()) {
                                Badge(
                                    modifier = Modifier.padding(start = 4.dp),
                                    containerColor = MaterialTheme.colorScheme.primary
                                ) {
                                    Text("${uiState.savedServers.size}", fontSize = 10.sp)
                                }
                            }
                        }
                    }
                )
                Tab(
                    selected = uiState.activeTab == DiscoveryTab.HISTORY,
                    onClick = { viewModel.setActiveTab(DiscoveryTab.HISTORY) },
                    text = {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = "History",
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("History")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content
            when (uiState.activeTab) {
                DiscoveryTab.DISCOVERED -> {
                    if (filteredServers.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredServers, key = { it.id }) { server ->
                                DiscoveredServerItem(
                                    server = server,
                                    isConnecting = uiState.isConnecting && uiState.selectedServerId == server.id,
                                    connectionProgress = uiState.connectionProgress,
                                    onConnect = {
                                        scope.launch {
                                            viewModel.connectToServer(server)
                                        }
                                    },
                                    onSave = { viewModel.addServerToSaved(server) }
                                )
                            }
                        }
                    } else if (uiState.isScanning) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                                CircularProgressIndicator()
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("Searching for servers...", color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    } else {
                        EmptyStateCard(
                            title = "No Servers Found",
                            message = "Make sure the Air Mouse server is running and both devices are on the same network",
                            icon = Icons.Default.WifiOff,
                            onRetry = { viewModel.scanNetwork() }
                        )
                    }
                }

                DiscoveryTab.SAVED -> {
                    if (filteredSavedServers.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(filteredSavedServers, key = { it.id }) { server ->
                                SavedServerItem(
                                    server = server,
                                    isConnecting = uiState.isConnecting && uiState.selectedServerId == server.id,
                                    connectionProgress = uiState.connectionProgress,
                                    onConnect = {
                                        scope.launch {
                                            viewModel.connectToServer(server)
                                        }
                                    },
                                    onDelete = { viewModel.removeSavedServer(server.id) },
                                    onFavorite = { viewModel.toggleFavorite(server.id) },
                                    onEditNotes = { notes: String -> viewModel.updateServerNotes(server.id, notes) },
                                    onEditDeviceType = { deviceType: DeviceType -> viewModel.updateDeviceType(server.id, deviceType) }
                                )
                            }
                        }
                    } else {
                        EmptyStateCard(
                            title = "No Saved Servers",
                            message = "Discover servers and tap the save icon to add them here",
                            icon = Icons.Default.BookmarkAdd,
                            onRetry = { viewModel.setActiveTab(DiscoveryTab.DISCOVERED) }
                        )
                    }
                }

                DiscoveryTab.HISTORY -> {
                    if (uiState.connectionHistory.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            item {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        "Last 50 connections",
                                        style = MaterialTheme.typography.labelMedium,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                    TextButton(onClick = { viewModel.clearConnectionHistory() }) {
                                        Text("Clear History")
                                    }
                                }
                            }
                            items(uiState.connectionHistory, key = { it.id }) { history ->
                                HistoryItem(history = history)
                            }
                        }
                    } else {
                        EmptyStateCard(
                            title = "No Connection History",
                            message = "Your connection history will appear here",
                            icon = Icons.Default.History,
                            onRetry = {}
                        )
                    }
                }
            }
        }
    }
}

// ==================== COMPONENTS ====================

@Composable
fun DiscoveredServerItem(
    server: DiscoveredServer,
    isConnecting: Boolean,
    connectionProgress: Int,
    onConnect: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnecting)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(server.deviceType.icon, fontSize = 20.sp)
                    Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    StatusChip(isReachable = server.isReachable, ping = server.ping)
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "${server.ip}:${server.port}",
                    fontSize = 12.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                    if (server.ping > 0) {
                        Text(
                            "Ping: ${server.ping}ms",
                            fontSize = 11.sp,
                            color = Color(server.pingColor)
                        )
                    }
                    Text(
                        "v${server.version}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (server.signalStrength > 0) {
                        SignalStrengthBar(strength = server.signalStrength)
                    }
                }
            }
            Row {
                IconButton(onClick = onSave) {
                    Icon(
                        imageVector = Icons.Default.BookmarkAdd,
                        contentDescription = "Save Server",
                        modifier = Modifier.size(22.dp)
                    )
                }
                if (isConnecting) {
                    CircularProgressIndicator(
                        progress = { connectionProgress / 100f },
                        modifier = Modifier.size(32.dp),
                        strokeWidth = 2.dp
                    )
                } else {
                    IconButton(onClick = onConnect) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                            contentDescription = "Connect to Server",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }
        }
        if (isConnecting) {
            LinearProgressIndicator(
                progress = { connectionProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun SavedServerItem(
    server: DiscoveredServer,
    isConnecting: Boolean,
    connectionProgress: Int,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onEditNotes: (String) -> Unit,
    onEditDeviceType: (DeviceType) -> Unit
) {
    var showNotesDialog by remember { mutableStateOf(false) }
    var showDeviceTypeDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isConnecting)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(server.deviceType.icon, fontSize = 20.sp)
                        Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                        if (server.isFavorite) {
                            Icon(
                                imageVector = Icons.Filled.Star,
                                contentDescription = "Favorite",
                                tint = Color(0xFFFFC107),
                                modifier = Modifier.size(16.dp)
                            )
                        }
                        StatusChip(isReachable = server.isReachable, ping = server.ping)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${server.ip}:${server.port}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (server.notes.isNotEmpty()) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.Comment,
                                contentDescription = "Notes",
                                modifier = Modifier.size(12.dp),
                                tint = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("📝 ${server.notes}", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                Row {
                    IconButton(onClick = { showDeviceTypeDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.Devices,
                            contentDescription = "Device Type",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = { showNotesDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.EditNote,
                            contentDescription = "Edit Notes",
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    IconButton(onClick = onFavorite) {
                        Icon(
                            imageVector = if (server.isFavorite) Icons.Filled.Favorite else Icons.Outlined.FavoriteBorder,
                            contentDescription = if (server.isFavorite) "Remove Favorite" else "Add Favorite",
                            tint = if (server.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(22.dp)
                        )                    }
                    IconButton(onClick = onDelete) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = "Delete Server",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(22.dp)
                        )
                    }
                    if (isConnecting) {
                        CircularProgressIndicator(
                            progress = { connectionProgress / 100f },
                            modifier = Modifier.size(32.dp),
                            strokeWidth = 2.dp
                        )
                    } else {
                        IconButton(onClick = onConnect) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = "Connect to Server",
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }
            }
            if (isConnecting) {
                LinearProgressIndicator(
                    progress = { connectionProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }

    if (showNotesDialog) {
        var notes by remember { mutableStateOf(server.notes) }
        AlertDialog(
            onDismissRequest = { showNotesDialog = false },
            title = { Text("Edit Notes") },
            text = {
                OutlinedTextField(
                    value = notes,
                    onValueChange = { notes = it },
                    label = { Text("Notes") },
                    singleLine = false,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    onEditNotes(notes)
                    showNotesDialog = false
                }) { Text("Save") }
            },
            dismissButton = {
                TextButton(onClick = { showNotesDialog = false }) { Text("Cancel") }
            }
        )
    }

    if (showDeviceTypeDialog) {
        AlertDialog(
            onDismissRequest = { showDeviceTypeDialog = false },
            title = { Text("Device Type") },
            text = {
                Column {
                    DeviceType.entries.forEach { type ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onEditDeviceType(type)
                                    showDeviceTypeDialog = false
                                }
                                .padding(vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(type.icon, fontSize = 20.sp)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text(type.displayName)
                        }
                        HorizontalDivider()
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showDeviceTypeDialog = false }) { Text("Close") }
            }
        )
    }
}

@Composable
fun HistoryItem(history: ConnectionHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (history.success)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        if (history.success) "✅" else "❌",
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(history.ip, fontFamily = FontFamily.Monospace, fontWeight = FontWeight.Medium)
                    Text(":${history.port}", fontFamily = FontFamily.Monospace)
                }
                Text(
                    SimpleDateFormat("MMM dd, HH:mm:ss", Locale.getDefault()).format(history.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                if (history.duration > 0) {
                    Text(
                        "Duration: ${history.duration}ms",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                // FIXED: Local variable for smart cast
                val errorMsg = history.errorMessage
                if (errorMsg != null) {
                    Text(
                        errorMsg,
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
            if (history.success) {
                Icon(
                    imageVector = Icons.Default.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(20.dp)
                )
            } else {
                Icon(
                    imageVector = Icons.Default.Error,
                    contentDescription = "Failed",
                    tint = MaterialTheme.colorScheme.error,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}

@Composable
fun StatusChip(isReachable: Boolean, ping: Int) {
    val (color, text) = when {
        !isReachable -> Color(0xFFF44336) to "Offline"
        ping < 50 -> Color(0xFF4CAF50) to "Excellent"
        ping < 100 -> Color(0xFFFFC107) to "Good"
        else -> Color(0xFFF44336) to "Slow"
    }
    Surface(
        shape = CircleShape,
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text,
            fontSize = 9.sp,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
            color = color
        )
    }
}

@Composable
fun SignalStrengthBar(strength: Int) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        repeat(4) { index ->
            Box(
                modifier = Modifier
                    .width(3.dp)
                    .height(((index + 1) * 3).dp) // FIXED: Proper Dp calculation
                    .clip(RoundedCornerShape(1.dp))
                    .background(
                        if (index < strength / 25)
                            Color(0xFF4CAF50)
                        else
                            Color.Gray.copy(alpha = 0.3f)
                    )
                    .padding(horizontal = 0.5.dp)
            )
        }
    }
}

@Composable
fun ManualConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit,
    initialIp: String,
    initialPort: String
) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Manual Connection") },
        text = {
            Column {
                OutlinedTextField(
                    value = ip,
                    onValueChange = { ip = it },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it },
                    label = { Text("Port") },
                    placeholder = { Text("8080") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(12.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Enter the IP address and port of your Air Mouse server",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val portInt = port.toIntOrNull() ?: 8080
                    if (ip.isNotBlank()) onConnect(ip, portInt)
                },
                enabled = ip.isNotBlank()
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun EmptyStateCard(title: String, message: String, icon: ImageVector, onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.height(8.dp))
            Text(message, fontSize = 14.sp, textAlign = TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = onRetry, shape = RoundedCornerShape(12.dp)) {
                Text("Try Again")
            }
        }
    }
}