// app/src/main/java/com/airmouse/presentation/ui/network/NetworkDiscoveryScreen.kt
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
import androidx.compose.material.icons.outlined.FavoriteBorder
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

            // Status and Error
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

            // Content Switching
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
                        )
                    }
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
        }
    }
}

// Placeholder layouts for remaining downstream structural UI dependencies
@Composable fun StatusChip(isReachable: Boolean, ping: Long) {}
@Composable fun SignalStrengthBar(strength: Int) {}
@Composable fun EmptyStateCard(title: String, message: String, icon: ImageVector, onRetry: () -> Unit) {}
@Composable fun HistoryItem(history: Any) {}
@Composable fun ManualConnectDialog(onDismiss: () -> Unit, onConnect: (String, Int) -> Unit, initialIp: String, initialPort: Int) {}

enum class DiscoveryTab { DISCOVERED, SAVED, HISTORY }
enum class SortBy(val displayName: String) { NAME("Name"), IP("IP Address"), PING("Latency") }
enum class DeviceType(val icon: String) { PC("💻"), MAC("🖥️"), LINUX("🐧") }
data class DiscoveredServer(
    val id: String, val name: String, val ip: String, val port: Int, val isReachable: Boolean,
    val ping: Long, val pingColor: Long, val version: String, val signalStrength: Int,
    val isFavorite: Boolean, val notes: String, val deviceType: DeviceType
)