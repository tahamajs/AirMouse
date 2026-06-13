// app/src/main/java/com/airmouse/presentation/ui/network/NetworkDiscoveryScreen.kt
package com.airmouse.presentation.ui.network

import androidx.compose.animation.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun NetworkDiscoveryScreen(
    navigationActions: NavigationActions,
    viewModel: NetworkDiscoveryViewModel = hiltViewModel(),
    onServerSelected: (String, Int) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Discovery") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    IconButton(onClick = { viewModel.refreshScan() }) {
                        Icon(Icons.Default.Refresh, contentDescription = "Refresh")
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
            // Sort dropdown
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                SortBy.values().forEach { sortBy ->
                    DropdownMenuItem(
                        text = { Text(sortBy.displayName) },
                        onClick = {
                            viewModel.setSortBy(sortBy)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (uiState.sortBy == sortBy) {
                                Icon(Icons.Default.Check, contentDescription = null)
                            }
                        }
                    )
                }
            }

            // Search / filter
            OutlinedTextField(
                value = uiState.filterText,
                onValueChange = viewModel::setFilterText,
                label = { Text("Filter servers...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                trailingIcon = {
                    if (uiState.filterText.isNotEmpty()) {
                        IconButton(onClick = { viewModel.setFilterText("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Network info card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text("Your IP Address", style = MaterialTheme.typography.labelMedium)
                        Text(
                            viewModel.getLocalIpAddress() ?: "Unknown",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                    if (viewModel.isWifiConnected()) {
                        Icon(Icons.Default.Wifi, contentDescription = "WiFi", tint = Color(0xFF4CAF50))
                    } else {
                        Icon(Icons.Default.WifiOff, contentDescription = "No WiFi", tint = Color(0xFFF44336))
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Scan button
            Button(
                onClick = { viewModel.scanNetwork() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isScanning
            ) {
                if (uiState.isScanning) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp), color = MaterialTheme.colorScheme.onPrimary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scanning... ${uiState.scanProgress}%")
                } else {
                    Icon(Icons.Default.Search, contentDescription = "Scan")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Scan Network")
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isScanning) {
                LinearProgressIndicator(
                    progress = uiState.scanProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            Text(
                text = uiState.status,
                fontSize = 12.sp,
                color = if (uiState.errorMessage != null) MaterialTheme.colorScheme.error
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
            if (uiState.errorMessage != null) {
                Spacer(modifier = Modifier.height(8.dp))
                TextButton(onClick = { viewModel.clearErrorMessage() }) {
                    Text("Dismiss")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Tabs
            TabRow(
                selectedTabIndex = if (uiState.activeTab == DiscoveryTab.DISCOVERED) 0 else 1,
                containerColor = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Tab(
                    selected = uiState.activeTab == DiscoveryTab.DISCOVERED,
                    onClick = { viewModel.setActiveTab(DiscoveryTab.DISCOVERED) },
                    text = { Text("Discovered") }
                )
                Tab(
                    selected = uiState.activeTab == DiscoveryTab.SAVED,
                    onClick = { viewModel.setActiveTab(DiscoveryTab.SAVED) },
                    text = { Text("Saved") }
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Content based on active tab
            when (uiState.activeTab) {
                DiscoveryTab.DISCOVERED -> {
                    if (uiState.discoveredServers.isNotEmpty()) {
                        Text(
                            text = "Discovered Servers (${uiState.discoveredServers.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(uiState.discoveredServers) { server ->
                                    DiscoveredServerItem(
                                        server = server,
                                        onConnect = {
                                            viewModel.connectToServer(server)
                                            onServerSelected(server.ip, server.port)
                                        },
                                        onSave = { viewModel.addServerToSaved(server) }
                                    )
                                    Divider()
                                }
                            }
                        }
                    } else if (!uiState.isScanning) {
                        EmptyStateCard(onRetry = { viewModel.scanNetwork() })
                    }
                }
                DiscoveryTab.SAVED -> {
                    if (uiState.savedServers.isNotEmpty()) {
                        Text(
                            text = "Saved Servers (${uiState.savedServers.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            LazyColumn(
                                modifier = Modifier.fillMaxSize(),
                                contentPadding = PaddingValues(8.dp)
                            ) {
                                items(uiState.savedServers) { server ->
                                    SavedServerItem(
                                        server = server,
                                        onConnect = {
                                            viewModel.connectToServer(server)
                                            onServerSelected(server.ip, server.port)
                                        },
                                        onDelete = { viewModel.removeSavedServer(server.id) },
                                        onFavorite = { viewModel.toggleFavorite(server.id) },
                                        onEditNotes = { notes -> viewModel.updateServerNotes(server.id, notes) }
                                    )
                                    Divider()
                                }
                            }
                        }
                    } else {
                        Card(
                            modifier = Modifier.fillMaxSize(),
                            colors = CardDefaults.cardColors(
                                containerColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                        ) {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .padding(32.dp),
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Center
                            ) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = "No saved servers",
                                    modifier = Modifier.size(64.dp),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                                Spacer(modifier = Modifier.height(16.dp))
                                Text("No Saved Servers", style = MaterialTheme.typography.titleLarge)
                                Text(
                                    "Discover servers and tap the save icon to add them here",
                                    textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun DiscoveredServerItem(
    server: DiscoveredServer,
    onConnect: () -> Unit,
    onSave: () -> Unit
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
                Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (server.isReachable) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f)
                ) {
                    Text(if (server.isReachable) "Online" else "Offline", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${server.ip}:${server.port}", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (server.ping > 0) {
                Text("Ping: ${server.ping}ms", fontSize = 10.sp, color = when { server.ping < 50 -> Color(0xFF4CAF50); server.ping < 100 -> Color(0xFFFFC107); else -> Color(0xFFF44336) })
            }
            Text("Version: ${server.version}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row {
            IconButton(onClick = onSave) { Icon(Icons.Default.BookmarkAdd, contentDescription = "Save") }
            IconButton(onClick = onConnect) { Icon(Icons.Default.ArrowForward, contentDescription = "Connect", tint = MaterialTheme.colorScheme.primary) }
        }
    }
}

@Composable
fun SavedServerItem(
    server: DiscoveredServer,
    onConnect: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onEditNotes: (String) -> Unit
) {
    var showNotesDialog by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(server.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                if (server.isFavorite) {
                    Icon(Icons.Default.Favorite, contentDescription = "Favorite", tint = Color(0xFFFFC107), modifier = Modifier.size(16.dp))
                }
                Surface(
                    shape = MaterialTheme.shapes.small,
                    color = if (server.isReachable) Color(0xFF4CAF50).copy(alpha = 0.2f) else Color(0xFFF44336).copy(alpha = 0.2f)
                ) {
                    Text(if (server.isReachable) "Online" else "Offline", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                }
            }
            Spacer(modifier = Modifier.height(4.dp))
            Text("${server.ip}:${server.port}", fontSize = 12.sp, fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace, color = MaterialTheme.colorScheme.onSurfaceVariant)
            if (server.notes.isNotEmpty()) {
                Text("📝 ${server.notes}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Row {
            IconButton(onClick = { showNotesDialog = true }) { Icon(Icons.Default.EditNote, contentDescription = "Add note") }
            IconButton(onClick = onFavorite) { Icon(if (server.isFavorite) Icons.Filled.Favorite else Icons.Outlined.Favorite, contentDescription = "Favorite", tint = if (server.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant) }
            IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            IconButton(onClick = onConnect) { Icon(Icons.Default.ArrowForward, contentDescription = "Connect", tint = MaterialTheme.colorScheme.primary) }
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
                    modifier = Modifier.fillMaxWidth()
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
}

@Composable
fun EmptyStateCard(onRetry: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxSize(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(Icons.Default.Wifi, contentDescription = "No servers", modifier = Modifier.size(64.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            Text("No Servers Found", style = MaterialTheme.typography.titleLarge)
            Text("Make sure the Air Mouse server is running and both devices are on the same network", textAlign = androidx.compose.ui.text.style.TextAlign.Center, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(16.dp))
            OutlinedButton(onClick = onRetry) { Text("Scan Again") }
        }
    }
}