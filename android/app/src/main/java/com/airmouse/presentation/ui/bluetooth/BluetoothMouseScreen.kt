package com.airmouse.presentation.ui.bluetooth

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Bluetooth
import androidx.compose.material.icons.filled.BluetoothConnected
import androidx.compose.material.icons.filled.Computer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airmouse.presentation.ui.components.SweepGradientGlowRing
import com.airmouse.presentation.ui.components.GlassmorphicNeonProgressBar
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.ConnectionProtocol
import com.airmouse.domain.model.ConnectionStatus
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.home.HomeViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothMouseScreen(
    navigationActions: NavigationActions,
    viewModel: HomeViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isConnected = uiState.connectionStatus == ConnectionStatus.CONNECTED
    val isConnecting = uiState.isConnecting

    val context = androidx.compose.ui.platform.LocalContext.current

    val requiredPermissions = remember {
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.S) {
            arrayOf(
                android.Manifest.permission.BLUETOOTH_CONNECT,
                android.Manifest.permission.BLUETOOTH_SCAN,
                android.Manifest.permission.BLUETOOTH_ADVERTISE
            )
        } else {
            arrayOf(
                android.Manifest.permission.BLUETOOTH,
                android.Manifest.permission.BLUETOOTH_ADMIN,
                android.Manifest.permission.ACCESS_FINE_LOCATION
            )
        }
    }

    var hasPermissions by remember {
        mutableStateOf(
            requiredPermissions.all {
                androidx.core.content.ContextCompat.checkSelfPermission(context, it) == android.content.pm.PackageManager.PERMISSION_GRANTED
            }
        )
    }

    val permissionLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        hasPermissions = requiredPermissions.all { results[it] == true }
    }

    val bluetoothAdapter = remember { android.bluetooth.BluetoothAdapter.getDefaultAdapter() }
    var isBluetoothEnabled by remember { mutableStateOf(bluetoothAdapter?.isEnabled == true) }

    // Register a BroadcastReceiver to listen to Bluetooth state changes
    DisposableEffect(context) {
        val receiver = object : android.content.BroadcastReceiver() {
            override fun onReceive(context: android.content.Context, intent: android.content.Intent) {
                if (intent.action == android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED) {
                    val state = intent.getIntExtra(
                        android.bluetooth.BluetoothAdapter.EXTRA_STATE,
                        android.bluetooth.BluetoothAdapter.ERROR
                    )
                    isBluetoothEnabled = state == android.bluetooth.BluetoothAdapter.STATE_ON
                }
            }
        }
        val filter = android.content.IntentFilter(android.bluetooth.BluetoothAdapter.ACTION_STATE_CHANGED)
        context.registerReceiver(receiver, filter)
        onDispose {
            context.unregisterReceiver(receiver)
        }
    }

    val discoverableLauncher = androidx.activity.compose.rememberLauncherForActivityResult(
        contract = androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult()
    ) {
        viewModel.connect()
    }

    // Force Protocol to BLUETOOTH when entering this screen
    LaunchedEffect(Unit) {
        viewModel.updateConnectionProtocol(ConnectionProtocol.BLUETOOTH)
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Native Bluetooth Mouse", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { padding ->
        if (!hasPermissions) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(120.dp)
                        .background(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Bluetooth,
                        contentDescription = "Permissions Required",
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
                Spacer(modifier = Modifier.height(32.dp))
                Text(
                    text = "Bluetooth Permissions Required",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text(
                    text = "Air Mouse Pro requires Bluetooth permissions to scan, pair, and connect directly as a native wireless mouse hardware peripheral.",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )
                Spacer(modifier = Modifier.height(40.dp))
                Button(
                    onClick = { permissionLauncher.launch(requiredPermissions) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(64.dp),
                    shape = RoundedCornerShape(24.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.primary
                    )
                ) {
                    Text("Grant Permissions", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                
                // Animation for Bluetooth Icon
                val infiniteTransition = rememberInfiniteTransition(label = "bt_pulse")
                val pulseScale by infiniteTransition.animateFloat(
                    initialValue = 0.9f,
                    targetValue = 1.15f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(1200, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "pulseScale"
                )

                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .clip(CircleShape)
                        .background(
                            if (isConnected) Color(0xFF10B981).copy(alpha = 0.15f) 
                            else MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(110.dp)
                            .scale(if (isConnecting) pulseScale else 1f)
                            .clip(CircleShape)
                            .background(
                                if (isConnected) Color(0xFF10B981).copy(alpha = 0.25f)
                                else MaterialTheme.colorScheme.primary.copy(alpha = 0.2f)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = if (isConnected) Icons.Default.BluetoothConnected else Icons.Default.Bluetooth,
                            contentDescription = "Bluetooth Status",
                            modifier = Modifier.size(56.dp),
                            tint = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = when {
                        isConnected -> "Connected to PC"
                        isConnecting -> "Advertising as Mouse..."
                        else -> "Ready to Broadcast"
                    },
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.Bold,
                    color = if (isConnected) Color(0xFF10B981) else MaterialTheme.colorScheme.onBackground,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                Text(
                    text = when {
                        isConnected -> "Your phone is now acting as a native Bluetooth mouse. You can switch to the Touchpad or Games tab to start controlling your PC."
                        isConnecting -> "Go to your computer's Bluetooth settings and look for 'Air Mouse'. Click 'Connect' or 'Pair' to establish the connection."
                        else -> "Make your phone appear as a standard wireless mouse to any computer without needing a server app."
                    },
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(48.dp))

                if (bluetoothAdapter == null) {
                    Text(
                        text = "Bluetooth is not supported on this device.",
                        color = MaterialTheme.colorScheme.error,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(16.dp),
                        style = MaterialTheme.typography.bodyLarge,
                        fontWeight = FontWeight.Bold
                    )
                } else if (!isBluetoothEnabled) {
                    Button(
                        onClick = {
                            try {
                                val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_ENABLE)
                                context.startActivity(intent)
                            } catch (e: Exception) {
                                val intent = android.content.Intent(android.provider.Settings.ACTION_BLUETOOTH_SETTINGS)
                                context.startActivity(intent)
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Icon(Icons.Default.Bluetooth, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Turn On Bluetooth", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                } else if (!isConnected) {
                    Button(
                        onClick = {
                            val intent = android.content.Intent(android.bluetooth.BluetoothAdapter.ACTION_REQUEST_DISCOVERABLE).apply {
                                putExtra(android.bluetooth.BluetoothAdapter.EXTRA_DISCOVERABLE_DURATION, 120)
                            }
                            try {
                                discoverableLauncher.launch(intent)
                            } catch (e: Exception) {
                                viewModel.connect()
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(24.dp),
                        enabled = !isConnecting,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        if (isConnecting) {
                            SweepGradientGlowRing(
                                modifier = Modifier.size(24.dp),
                                strokeWidth = 2.5.dp,
                                glowRadius = 4.dp,
                                colors = listOf(
                                    Color(0xFF6366F1),
                                    Color(0xFF3B82F6),
                                    Color(0xFF6366F1)
                                )
                            )
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Advertising...", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        } else {
                            Icon(Icons.Default.Bluetooth, contentDescription = null)
                            Spacer(modifier = Modifier.width(12.dp))
                            Text("Enable & Broadcast", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                } else {
                    // Connected state: show disconnect button prominently
                    Button(
                        onClick = { viewModel.disconnect() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(64.dp),
                        shape = RoundedCornerShape(24.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFDC2626),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Default.BluetoothConnected, contentDescription = null)
                        Spacer(modifier = Modifier.width(12.dp))
                        Text("Disconnect Completely", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                // Connection status details card
                if (isConnected) {
                    Card(
                        colors = CardDefaults.cardColors(containerColor = Color(0xFF10B981).copy(alpha = 0.1f)),
                        shape = RoundedCornerShape(16.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(Color(0xFF10B981), CircleShape)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Text(
                                    text = "Active HID Connection",
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF10B981)
                                )
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Column {
                                    Text("Protocol", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Bluetooth HID", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                                Column(horizontalAlignment = Alignment.End) {
                                    Text("Device Name", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                    Text("Air Mouse", style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(16.dp))
                }

                Card(
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(16.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Computer,
                            contentDescription = "PC Setup",
                            tint = MaterialTheme.colorScheme.primary
                        )
                        Spacer(modifier = Modifier.width(16.dp))
                        Text(
                            text = "Works on Windows, macOS, and Linux out-of-the-box. No software needed on the PC.",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
