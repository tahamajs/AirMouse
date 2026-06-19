package com.airmouse.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.network.ConnectionManager
import kotlinx.coroutines.delay

@Composable
fun ConnectionStatusBadge(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier,
    onDismiss: () -> Unit = {}
) {
    val status by connectionManager.connectionStatus.collectAsStateWithLifecycle()
    val quality by connectionManager.connectionQuality.collectAsStateWithLifecycle()
    val currentIp by connectionManager.currentIp.collectAsStateWithLifecycle()
    val serverName by connectionManager.serverName.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(true) }
    
    LaunchedEffect(status) {
        visible = status != ConnectionManager.ConnectionStatus.CONNECTED
        if (status == ConnectionManager.ConnectionStatus.CONNECTED) {
            delay(5000)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(
            initialOffsetY = { -it },
            animationSpec = tween(300)
        ),
        exit = fadeOut() + slideOutVertically(
            targetOffsetY = { -it },
            animationSpec = tween(300)
        )
    ) {
        Card(
            modifier = modifier
                .padding(16.dp)
                .fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.95f)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    // Animated status icon
                    Box {
                        com.airmouse.ui.components.AnimatedConnectionStatus(
                            isConnected = status == ConnectionManager.ConnectionStatus.CONNECTED,
                            signalStrength = quality.level(),
                            ping = quality.ping,
                            modifier = Modifier.size(48.dp),
                            showDetails = false
                        )
                        
                        // Connecting spinner overlay
                        if (status == ConnectionManager.ConnectionStatus.CONNECTING || 
                            status == ConnectionManager.ConnectionStatus.RECONNECTING) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(48.dp),
                                color = Color(0xFF00BCD4),
                                strokeWidth = 2.dp
                            )
                        }
                    }
                    
                    Column {
                        Text(
                            text = when (status) {
                                ConnectionManager.ConnectionStatus.CONNECTED -> "Connected"
                                ConnectionManager.ConnectionStatus.CONNECTING -> "Connecting..."
                                ConnectionManager.ConnectionStatus.RECONNECTING -> "Reconnecting..."
                                ConnectionManager.ConnectionStatus.ERROR -> "Connection Error"
                                else -> "Disconnected"
                            },
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = androidx.compose.ui.text.font.FontWeight.Bold
                        )
                        
                        when (status) {
                            ConnectionManager.ConnectionStatus.CONNECTED -> {
                                Text(
                                    text = "$serverName • ${currentIp.split(":").first()}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = Color(0xFF4CAF50)
                                )
                                Text(
                                    text = "Ping: ${quality.ping}ms • Signal: ${quality.signalStrength.name.lowercase().replaceFirstChar { it.uppercase() }}",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ConnectionManager.ConnectionStatus.CONNECTING -> {
                                Text(
                                    text = "Attempting to connect to $currentIp",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            ConnectionManager.ConnectionStatus.ERROR -> {
                                Text(
                                    text = "Failed to connect. Check your connection.",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.error
                                )
                            }
                            else -> {
                                Text(
                                    text = "No active connection",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
                
                // Action buttons
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> {
                            IconButton(
                                onClick = { connectionManager.disconnect() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Default.Close,
                                    contentDescription = "Disconnect",
                                    tint = MaterialTheme.colorScheme.error,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        ConnectionManager.ConnectionStatus.ERROR,
                        ConnectionManager.ConnectionStatus.DISCONNECTED,
                        ConnectionManager.ConnectionStatus.CONNECTING,
                        ConnectionManager.ConnectionStatus.RECONNECTING -> {
                            IconButton(
                                onClick = { connectionManager.reconnect() },
                                modifier = Modifier
                                    .size(36.dp)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
                            ) {
                                Icon(
                                    Icons.Default.Refresh,
                                    contentDescription = "Retry",
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
                    IconButton(
                        onClick = { onDismiss() },
                        modifier = Modifier
                            .size(36.dp)
                            .clip(RoundedCornerShape(8.dp))
                            .background(MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Icon(
                            Icons.Default.Close,
                            contentDescription = "Dismiss",
                            modifier = Modifier.size(18.dp),
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
