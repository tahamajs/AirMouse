package com.airmouse.presentation.ui.network

import android.util.Patterns
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Brush
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

/**
 * Screen for discovering Air Mouse servers on the local network.
 * Supports UDP broadcast discovery, manual IP entry, and saving favorite servers.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NetworkDiscoveryScreen(
    navigationActions: NavigationActions,
    viewModel: NetworkDiscoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val focusManager = androidx.compose.ui.platform.LocalFocusManager.current
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
                            contentDescription = "Sort",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.refreshScan() }) {
                        Icon(
                            imageVector = if (uiState.isScanning) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (uiState.isScanning) "Stop Scan" else "Refresh",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { showManualConnectDialog = true }) {
                        Icon(
                            imageVector = Icons.Default.AddLink,
                            contentDescription = "Manual Connect",
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            if (uiState.isConnecting) {
                FloatingActionButton(
                    onClick = { viewModel.disconnect() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Close,
                        contentDescription = "Cancel Connection",
                        tint = MaterialTheme.colorScheme.onError
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
            // Sort menu
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

            // Manual connect dialog
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

            // Search / filter field
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                shape = RoundedCornerShape(24.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                tonalElevation = 2.dp
            ) {
                OutlinedTextField(
                    value = uiState.filterText,
                    onValueChange = viewModel::setFilterText,
                    placeholder = { Text("Search by name or IP...") },
                    leadingIcon = {
                        Icon(
                            imageVector = Icons.Default.Search,
                            contentDescription = "Search",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    },
                    trailingIcon = {
                        if (uiState.filterText.isNotEmpty()) {
                            IconButton(onClick = { viewModel.setFilterText("") }) {
                                Icon(
                                    imageVector = Icons.Default.Close,
                                    contentDescription = "Clear",
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    shape = RoundedCornerShape(24.dp),
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        imeAction = androidx.compose.ui.text.input.ImeAction.Search
                    ),
                    keyboardActions = androidx.compose.foundation.text.KeyboardActions(
                        onSearch = {
                            focusManager.clearFocus()
                        }
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = Color.Transparent,
                        unfocusedBorderColor = Color.Transparent,
                        disabledBorderColor = Color.Transparent,
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Local IP address card
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp),
                colors = CardDefaults.cardColors(containerColor = Color.Transparent),
                shape = RoundedCornerShape(16.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            Brush.linearGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primaryContainer,
                                    MaterialTheme.colorScheme.tertiaryContainer
                                )
                            )
                        )
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column {
                            Text(
                                "Local IP Address",
                                style = MaterialTheme.typography.labelMedium,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                viewModel.getLocalIpAddress() ?: "Not connected",
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                fontFamily = FontFamily.Monospace,
                                color = MaterialTheme.colorScheme.onPrimaryContainer
                            )
                            if (uiState.lastScanTime != null) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    "Last scan: ${SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(uiState.lastScanTime)}",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.6f)
                                )
                            }
                        }
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (viewModel.isWifiConnected()) {
                                Surface(
                                    shape = CircleShape,
                                    color = Color(0xFF4CAF50).copy(alpha = 0.15f)
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Wifi,
                                            contentDescription = "WiFi Connected",
                                            tint = Color(0xFF2E7D32),
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Online",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = Color(0xFF2E7D32)
                                        )
                                    }
                                }
                            } else {
                                Surface(
                                    shape = CircleShape,
                                    color = MaterialTheme.colorScheme.errorContainer
                                ) {
                                    Row(
                                        verticalAlignment = Alignment.CenterVertically,
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.WifiOff,
                                            contentDescription = "No WiFi",
                                            tint = MaterialTheme.colorScheme.onErrorContainer,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            "Offline",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = MaterialTheme.colorScheme.onErrorContainer
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Status card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.secondaryContainer
                ),
                shape = RoundedCornerShape(12.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(14.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text("Nearby device message", style = MaterialTheme.typography.labelSmall)
                        Text(
                            uiState.status,
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    Icon(
                        imageVector = if (uiState.isScanning) Icons.Default.GraphicEq else Icons.Default.Notifications,
                        contentDescription = "Discovery message",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Scan button
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

            // Error message display
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
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs: Discovered, Saved, History
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
                                tint = MaterialTheme.colorScheme.onSurface,
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
                                tint = MaterialTheme.colorScheme.onSurface,
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
                                tint = MaterialTheme.colorScheme.onSurface,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("History")
                        }
                    }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on active tab
            when (uiState.activeTab) {
                DiscoveryTab.DISCOVERED -> {
                    val filteredServers = uiState.discoveredServers.filter { server ->
                        uiState.filterText.isEmpty() ||
                                server.name.contains(uiState.filterText, ignoreCase = true) ||
                                server.ip.contains(uiState.filterText, ignoreCase = true)
                    }
                    if (uiState.discoveredServers.isNotEmpty()) {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            color = MaterialTheme.colorScheme.tertiaryContainer
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.MarkEmailUnread,
                                    contentDescription = "Nearby devices",
                                    tint = MaterialTheme.colorScheme.onTertiaryContainer
                                )
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        "Nearby devices found",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                    Text(
                                        "Tap Request Pairing to open the connection request flow.",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onTertiaryContainer
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }
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
                    val sortedSavedServers = viewModel.sortServers(
                        uiState.savedServers.filter { server ->
                            uiState.filterText.isEmpty() ||
                                    server.name.contains(uiState.filterText, ignoreCase = true) ||
                                    server.ip.contains(uiState.filterText, ignoreCase = true)
                        },
                        uiState.sortBy
                    ).sortedByDescending { it.isFavorite }
                    if (sortedSavedServers.isNotEmpty()) {
                        LazyColumn(
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxSize()
                        ) {
                            items(sortedSavedServers, key = { it.id }) { server ->
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
                                    onFavorite = { viewModel.toggleFavorite(server.id) }
                                )
                            }
                        }
                    } else {
                        EmptyStateCard(
                            title = "No Saved Servers",
                            message = "Discover servers and tap the save icon to add them here",
                            icon = Icons.Default.BookmarkAdd,
                            actionLabel = "Discover Servers",
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
                            icon = Icons.Default.History
                        )
                    }
                }
            }
        }
    }
}

/**
 * Item representing a discovered server in the list.
 */
@Composable
fun DiscoveredServerItem(
    server: DiscoveredServer,
    isConnecting: Boolean,
    connectionProgress: Int,
    onConnect: () -> Unit,
    onSave: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isConnecting)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = if (isConnecting) 8.dp else 2.dp,
        tonalElevation = 2.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                Surface(
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                        Icon(
                            imageVector = when (server.protocol) {
                                Protocol.WEBSOCKET -> Icons.Default.Wifi
                                Protocol.UDP -> Icons.Default.SettingsEthernet
                                Protocol.TCP -> Icons.Default.Computer
                            },
                        contentDescription = "Protocol",
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(10.dp)
                    )
                }
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        Text(server.deviceType.icon, fontSize = 18.sp)
                        Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "${server.ip}:${server.port}",
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                        StatusChip(isReachable = server.isReachable, ping = server.ping.toLong())
                        if (server.ping > 0) {
                            Text(
                                "Ping ${server.ping}ms",
                                fontSize = 11.sp,
                                color = Color(server.pingColor.toInt())
                            )
                        }
                        Text(
                            "v${server.version}",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = server.protocol.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
                    }
                }
            }
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "Tap Request Pairing to open the connection request flow.",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                FilledTonalButton(onClick = onConnect, enabled = !isConnecting) {
                    if (isConnecting) {
                        CircularProgressIndicator(
                            progress = { connectionProgress / 100f },
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Requesting...")
                    } else {
                        Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "Request Pairing")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Request Pairing")
                    }
                }
                OutlinedButton(onClick = onSave) {
                    Icon(imageVector = Icons.Default.BookmarkAdd, contentDescription = "Save Server")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Save")
                }
            }
            if (isConnecting) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { connectionProgress / 100f },
                    modifier = Modifier.fillMaxWidth(),
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

/**
 * Item representing a saved server in the list.
 */
@Composable
fun SavedServerItem(
    server: DiscoveredServer,
    isConnecting: Boolean,
    connectionProgress: Int,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        shape = RoundedCornerShape(16.dp),
        color = if (isConnecting)
            MaterialTheme.colorScheme.primaryContainer
        else
            MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shadowElevation = if (isConnecting) 8.dp else 2.dp,
        tonalElevation = 2.dp
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
                        StatusChip(isReachable = server.isReachable, ping = server.ping.toLong())
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.secondaryContainer,
                            modifier = Modifier.padding(horizontal = 4.dp)
                        ) {
                            Text(
                                text = server.protocol.name,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSecondaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                            )
                        }
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

/**
 * Status chip indicating online/offline state.
 */
@Composable
fun StatusChip(isReachable: Boolean, ping: Long) {
    val color = if (isReachable) Color(0xFF4CAF50) else Color(0xFFF44336)
    val text = if (isReachable) "Online" else "Offline"
    val pingText = if (ping > 0) " • ${ping}ms" else ""
    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.2f)
    ) {
        Text(
            text = text + pingText,
            fontSize = 10.sp,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}

/**
 * Signal strength bar indicator.
 */
@Composable
fun SignalStrengthBar(strength: Int) {
    Row {
        repeat(4) { i ->
            Box(
                modifier = Modifier
                    .size(4.dp, 12.dp)
                    .padding(end = 1.dp)
                    .background(
                        color = if (i < strength) Color(0xFF4CAF50) else Color(0xFFE0E0E0),
                        shape = RoundedCornerShape(1.dp)
                    )
            )
        }
    }
}

/**
 * Empty state card with a retry button.
 */
@Composable
fun EmptyStateCard(
    title: String,
    message: String,
    icon: ImageVector,
    actionLabel: String = "Retry",
    onRetry: (() -> Unit)? = null
) {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            if (onRetry != null) {
                Spacer(modifier = Modifier.height(16.dp))
                Button(onClick = onRetry) {
                    Text(actionLabel)
                }
            }
        }
    }
}

/**
 * History item showing a past connection.
 */
@Composable
fun HistoryItem(history: ConnectionHistoryItem) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
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
                Text(
                    text = history.serverName,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${history.ip}:${history.port}",
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = history.status,
                    fontSize = 11.sp,
                    color = if (history.status == "Connected") Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(history.timestamp),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Dialog for manually entering server IP and port.
 */
@Composable
fun ManualConnectDialog(
    onDismiss: () -> Unit,
    onConnect: (String, Int) -> Unit,
    initialIp: String,
    initialPort: Int
) {
    var ip by remember { mutableStateOf(initialIp) }
    var port by remember { mutableStateOf(initialPort.toString()) }
    var isValidIp by remember { mutableStateOf(true) }

    val portInt = port.toIntOrNull()
    val isValidPort = portInt != null && portInt in 1..65535

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Manual Connect", fontWeight = FontWeight.Bold)
        },
        text = {
            Column {
                Text("Enter the IP address and port of the Air Mouse server:")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = ip,
                    onValueChange = {
                        ip = it
                        isValidIp = it.matches("^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$".toRegex())
                    },
                    label = { Text("IP Address") },
                    isError = !isValidIp && ip.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValidIp && ip.isNotEmpty()) {
                    Text(
                        "Invalid IP address",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = port,
                    onValueChange = { port = it.filter { c -> c.isDigit() } },
                    label = { Text("Port") },
                    isError = !isValidPort && port.isNotEmpty(),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                if (!isValidPort && port.isNotEmpty()) {
                    Text(
                        "Port must be between 1 and 65535",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    if (ip.isNotEmpty() && isValidIp && isValidPort && portInt != null) {
                        onConnect(ip, portInt)
                    }
                },
                enabled = isValidIp && ip.isNotEmpty() && isValidPort
            ) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
