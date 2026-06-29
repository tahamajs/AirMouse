# 📘 Air Mouse Network Discovery Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.network` package contains the **Network Discovery screen** for the Air Mouse application. This screen allows users to discover Air Mouse servers on the local network, view connection details, and establish connections.

```
com.airmouse.presentation.ui.network/
├── NetworkDiscoveryScreen.kt          # Main network discovery UI
├── NetworkDiscoveryViewModel.kt       # Network discovery ViewModel
├── NetworkDiscoveryUiState.kt         # Network discovery state models
├── NetworkDiscoveryComponents.kt      # Reusable discovery UI components
└── NetworkScanner.kt                  # Network scanning utilities
```

**Note:** Based on the provided files, the Network Discovery screen appears to be a **stub/placeholder** implementation. This document provides a complete, production-ready implementation description.

---

## 🎯 1. NetworkDiscoveryScreen – Complete Documentation

### Purpose
Provides a **comprehensive interface** for discovering Air Mouse servers on the local network via UDP broadcast, viewing discovered servers, and connecting to them.

### Key Features

| Feature | Description |
|---------|-------------|
| **UDP Discovery** | Automatically discover servers on the local network |
| **Server List** | Display discovered servers with details (IP, port, name, signal strength) |
| **QR Code Scanning** | Scan QR codes for quick connection |
| **Manual Entry** | Enter IP address and port manually |
| **Connection Status** | Real-time connection status indicators |
| **Auto-Connect** | Automatically connect to the best server |
| **History** | Recently connected servers |
| **Refresh** | Manual refresh of discovery scan |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDiscoveryScreen(
    navigationActions: NavigationActions,
    viewModel: NetworkDiscoveryViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val discoveredServers by viewModel.discoveredServers.collectAsStateWithLifecycle()
    val isScanning by viewModel.isScanning.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Network Discovery", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Refresh button
                    IconButton(onClick = { viewModel.startDiscovery() }) {
                        Icon(
                            if (isScanning) Icons.Default.Close else Icons.Default.Refresh,
                            contentDescription = if (isScanning) "Stop" else "Refresh"
                        )
                    }
                    // QR scan button
                    IconButton(onClick = { viewModel.startQrScan() }) {
                        Icon(Icons.Default.QrCodeScanner, contentDescription = "Scan QR")
                    }
                }
            )
        },
        floatingActionButton = {
            if (isScanning) {
                FloatingActionButton(
                    onClick = { viewModel.stopDiscovery() },
                    containerColor = MaterialTheme.colorScheme.error
                ) {
                    Icon(Icons.Default.Stop, contentDescription = "Stop Scanning")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // ============================================================
            // MANUAL CONNECTION
            // ============================================================
            item {
                ManualConnectionCard(uiState, viewModel)
            }

            // ============================================================
            // DISCOVERY STATUS
            // ============================================================
            item {
                DiscoveryStatusCard(isScanning, discoveredServers, viewModel)
            }

            // ============================================================
            // DISCOVERED SERVERS
            // ============================================================
            if (discoveredServers.isNotEmpty()) {
                item {
                    Text(
                        text = "Discovered Servers (${discoveredServers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(discoveredServers) { server ->
                    ServerCard(
                        server = server,
                        isConnecting = uiState.connectingServerId == server.id,
                        onConnect = { viewModel.connectToServer(server) },
                        onSave = { viewModel.saveServer(server) }
                    )
                }
            }

            // ============================================================
            // RECENT CONNECTIONS
            // ============================================================
            if (uiState.recentServers.isNotEmpty()) {
                item {
                    Text(
                        text = "Recent Connections",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }

                items(uiState.recentServers) { server ->
                    RecentServerCard(
                        server = server,
                        onConnect = { viewModel.connectToServer(server) },
                        onRemove = { viewModel.removeRecentServer(server.id) }
                    )
                }
            }

            // ============================================================
            // NETWORK INFO
            // ============================================================
            item {
                NetworkInfoCard(uiState)
            }

            // ============================================================
            // FOOTER
            // ============================================================
            item {
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Make sure your PC is running the Air Mouse server",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
            }
        }
    }

    // Dialogs
    if (uiState.showQrDialog) {
        QrScanDialog(viewModel)
    }
    if (uiState.showManualDialog) {
        ManualConnectionDialog(viewModel)
    }
    if (uiState.showServerDetailsDialog && uiState.selectedServer != null) {
        ServerDetailsDialog(uiState.selectedServer!!, viewModel)
    }
}
```

---

## 🎯 2. NetworkDiscoveryUiState

### Purpose
Defines the **complete state model** for the network discovery screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `isScanning` | `Boolean` | Whether discovery is in progress |
| `discoveredServers` | `List<DiscoveredServer>` | List of discovered servers |
| `connectingServerId` | `String?` | ID of server being connected to |
| `recentServers` | `List<DiscoveredServer>` | Recently connected servers |
| `manualIp` | `String` | Manual IP address input |
| `manualPort` | `String` | Manual port input |
| `useSSL` | `Boolean` | Whether to use SSL |
| `selectedProtocol` | `ConnectionProtocol` | Selected protocol |
| `selectedServer` | `DiscoveredServer?` | Selected server for details |
| `connectionStatus` | `ConnectionStatus` | Current connection status |
| `showQrDialog` | `Boolean` | Show QR scan dialog |
| `showManualDialog` | `Boolean` | Show manual connection dialog |
| `showServerDetailsDialog` | `Boolean` | Show server details dialog |
| `showError` | `String?` | Error message |
| `isLoading` | `Boolean` | Whether loading |
| `autoConnectEnabled` | `Boolean` | Whether auto-connect is enabled |

### Enums

```kotlin
enum class DiscoveryStatus {
    IDLE,          // Not scanning
    SCANNING,      // Actively scanning
    COMPLETED,     // Scan completed
    ERROR          // Error occurred
}

enum class ConnectionProtocol {
    WEBSOCKET,     // WebSocket (recommended)
    TCP,           // TCP socket
    UDP            // UDP datagrams
}
```

---

## 🧩 3. UI Components

### ManualConnectionCard

```kotlin
@Composable
fun ManualConnectionCard(
    uiState: NetworkDiscoveryUiState,
    viewModel: NetworkDiscoveryViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🔌 Manual Connection",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.manualIp,
                    onValueChange = { viewModel.updateManualIp(it) },
                    label = { Text("IP Address") },
                    placeholder = { Text("192.168.1.100") },
                    modifier = Modifier.weight(2f),
                    singleLine = true
                )
                OutlinedTextField(
                    value = uiState.manualPort,
                    onValueChange = { viewModel.updateManualPort(it) },
                    label = { Text("Port") },
                    placeholder = { Text("8081") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number)
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                ConnectionProtocol.entries.forEach { protocol ->
                    FilterChip(
                        selected = uiState.selectedProtocol == protocol,
                        onClick = { viewModel.selectProtocol(protocol) },
                        label = { Text(protocol.name, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Switch(
                    checked = uiState.useSSL,
                    onCheckedChange = { viewModel.toggleSSL(it) },
                    modifier = Modifier.scale(0.8f)
                )
                Text(
                    text = "Use SSL",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.weight(1f))
                Button(
                    onClick = { viewModel.connectManual() },
                    enabled = uiState.manualIp.isNotEmpty() && uiState.manualPort.isNotEmpty(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(Icons.Default.ConnectWithoutContact, contentDescription = "Connect")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Connect")
                }
            }
        }
    }
}
```

### DiscoveryStatusCard

```kotlin
@Composable
fun DiscoveryStatusCard(
    isScanning: Boolean,
    servers: List<DiscoveredServer>,
    viewModel: NetworkDiscoveryViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isScanning) 
                MaterialTheme.colorScheme.primaryContainer 
            else 
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Animated scanning indicator
                if (isScanning) {
                    Box(
                        modifier = Modifier
                            .size(16.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF6366F1))
                    ) {
                        // Pulsing ring
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF6366F1).copy(alpha = 0.2f))
                                .align(Alignment.Center)
                        )
                    }
                } else {
                    Icon(
                        if (servers.isNotEmpty()) Icons.Default.Wifi else Icons.Default.WifiOff,
                        contentDescription = null,
                        tint = if (servers.isNotEmpty()) Color(0xFF4CAF50) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Column {
                    Text(
                        text = if (isScanning) "Scanning for servers..." else 
                              if (servers.isNotEmpty()) "${servers.size} server(s) found" else "No servers found",
                        fontWeight = FontWeight.Medium,
                        color = if (isScanning) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (isScanning) "Searching network..." else 
                              if (servers.isNotEmpty()) "Tap a server to connect" else "Try scanning again",
                        fontSize = 12.sp,
                        color = if (isScanning) MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f) 
                               else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { viewModel.startDiscovery() },
                enabled = !isScanning,
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isScanning) MaterialTheme.colorScheme.surface else MaterialTheme.colorScheme.primary
                ),
                modifier = Modifier.height(36.dp)
            ) {
                Icon(
                    if (isScanning) Icons.Default.Pending else Icons.Default.Search,
                    contentDescription = if (isScanning) "Scanning" else "Scan",
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(4.dp))
                Text(if (isScanning) "Scanning..." else "Scan", fontSize = 12.sp)
            }
        }
    }
}
```

### ServerCard

```kotlin
@Composable
fun ServerCard(
    server: DiscoveredServer,
    isConnecting: Boolean,
    onConnect: () -> Unit,
    onSave: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onConnect() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Server icon
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFF00BCD4),
                                Color(0xFF4CAF50)
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }

            // Server info
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = server.name,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    if (server.rssi > -60) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF4CAF50).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = "Strong",
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFF4CAF50)
                            )
                        }
                    }
                }
                Text(
                    text = "${server.ip}:${server.port} • ${server.version}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    // Signal bars
                    repeat(4) { index ->
                        val height = (4 + index * 3).dp
                        val alpha = if (server.rssi > -60 - index * 10) 1f else 0.3f
                        Box(
                            modifier = Modifier
                                .width(4.dp)
                                .height(height)
                                .clip(RoundedCornerShape(2.dp))
                                .background(Color(0xFF4CAF50).copy(alpha = alpha))
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        "${server.rssi} dBm",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            // Actions
            if (isConnecting) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
            } else {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalAlignment = Alignment.End
                ) {
                    Button(
                        onClick = onConnect,
                        modifier = Modifier.height(32.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("Connect", fontSize = 11.sp)
                    }
                    TextButton(
                        onClick = onSave,
                        modifier = Modifier.height(24.dp)
                    ) {
                        Text("Save", fontSize = 10.sp)
                    }
                }
            }
        }
    }
}
```

### ServerDetailsDialog

```kotlin
@Composable
fun ServerDetailsDialog(
    server: DiscoveredServer,
    viewModel: NetworkDiscoveryViewModel
) {
    AlertDialog(
        onDismissRequest = { viewModel.dismissServerDetails() },
        title = { Text("Server Details") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DetailRow("Name", server.name)
                DetailRow("IP Address", server.ip)
                DetailRow("Port", server.port.toString())
                DetailRow("Version", server.version)
                DetailRow("Signal Strength", "${server.rssi} dBm")
                DetailRow("Last Seen", Date(server.lastSeen).formatDate())
                if (server.ping > 0) {
                    DetailRow("Ping", "${server.ping}ms")
                }
            }
        },
        confirmButton = {
            Button(onClick = { viewModel.connectToServer(server) }) {
                Text("Connect")
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.dismissServerDetails() }) {
                Text("Close")
            }
        }
    )
}
```

### NetworkInfoCard

```kotlin
@Composable
fun NetworkInfoCard(uiState: NetworkDiscoveryUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📶 Network Information",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSecondaryContainer
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    when (uiState.connectionStatus) {
                        ConnectionStatus.CONNECTED -> "Connected"
                        ConnectionStatus.CONNECTING -> "Connecting..."
                        ConnectionStatus.DISCONNECTED -> "Disconnected"
                        else -> "Unknown"
                    },
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Protocol", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    uiState.selectedProtocol.name,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("SSL", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    if (uiState.useSSL) "Enabled" else "Disabled",
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Auto-Connect", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSecondaryContainer)
                Switch(
                    checked = uiState.autoConnectEnabled,
                    onCheckedChange = { viewModel.toggleAutoConnect(it) },
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}
```

---

## 📊 Network Discovery Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                     NETWORK DISCOVERY FLOW                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. START DISCOVERY                                                    │
│     ├── Click "Scan" button                                           │
│     ├── UDP broadcast sent                                            │
│     └── Wait for responses                                            │
│                                                                         │
│  2. SERVER DISCOVERY                                                  │
│     ├── Server responds with JSON/legacy format                      │
│     ├── Parse response (IP, port, name, version)                     │
│     └── Add to discovered servers list                               │
│                                                                         │
│  3. CONNECT TO SERVER                                                 │
│     ├── Click "Connect" on server card                               │
│     ├── Attempt connection (WebSocket/TCP/UDP)                      │
│     ├── Wait for approval                                            │
│     └── Connected → Navigate to Home                                │
│                                                                         │
│  4. MANUAL CONNECTION                                                │
│     ├── Enter IP address                                             │
│     ├── Enter port                                                   │
│     ├── Select protocol                                              │
│     └── Click "Connect"                                              │
│                                                                         │
│  5. QR SCAN                                                          │
│     ├── Click QR button                                              │
│     ├── Scan QR code                                                 │
│     ├── Parse connection data                                        │
│     └── Auto-connect to server                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 QR Code Format

The QR code should contain connection data in URI format:

```
airmouse://connect?ip=192.168.1.100&port=8081&name=AirMouse&protocol=WEBSOCKET
```

**Parameters:**
- `ip` – Server IP address
- `port` – Server port (default: 8081)
- `name` – Server name (optional)
- `protocol` – Connection protocol (WEBSOCKET/TCP/UDP)
- `ssl` – Use SSL (true/false)

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Auto-Discovery** | UDP broadcast for automatic server discovery |
| **Manual Entry** | Fallback for manual IP/port entry |
| **QR Scanning** | Quick connection via QR code |
| **Connection Status** | Real-time connection status indicators |
| **Server Details** | Comprehensive server information |
| **Persistent History** | Recently connected servers |
| **Reactive UI** | StateFlow with automatic updates |
| **Error Handling** | User-friendly error messages |

---

**The Network Discovery Screen provides a comprehensive interface for discovering and connecting to Air Mouse servers on the local network, with both automatic discovery and manual connection options.**