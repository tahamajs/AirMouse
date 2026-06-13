// app/src/main/java/com/airmouse/presentation/ui/components/ConnectionStatusBadge.kt
package com.airmouse.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.network.ConnectionManager

@Composable
fun ConnectionStatusBadge(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    val status by connectionManager.connectionStatus.collectAsStateWithLifecycle()
    val quality by connectionManager.connectionQuality.collectAsStateWithLifecycle()
    val currentIp by connectionManager.currentIp.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(status) {
        visible = status != ConnectionManager.ConnectionStatus.CONNECTED
        if (status == ConnectionManager.ConnectionStatus.CONNECTED) {
            delay(3000)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Card(
            modifier = modifier
                .padding(8.dp)
                .wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> Icons.Default.Wifi
                        ConnectionManager.ConnectionStatus.CONNECTING -> Icons.Default.Sync
                        ConnectionManager.ConnectionStatus.RECONNECTING -> Icons.Default.Autorenew
                        else -> Icons.Default.WifiOff
                    },
                    contentDescription = "Status",
                    tint = when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                        ConnectionManager.ConnectionStatus.CONNECTING -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = when (status) {
                            ConnectionManager.ConnectionStatus.CONNECTED -> "Connected to $currentIp"
                            ConnectionManager.ConnectionStatus.CONNECTING -> "Connecting..."
                            ConnectionManager.ConnectionStatus.RECONNECTING -> "Reconnecting..."
                            ConnectionManager.ConnectionStatus.ERROR -> "Connection error"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (status == ConnectionManager.ConnectionStatus.CONNECTED) {
                        Text(
                            text = "Signal: ${quality.signalStrength.name} (${quality.ping}ms)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}// app/src/main/java/com/airmouse/presentation/ui/components/ConnectionStatusBadge.kt
package com.airmouse.presentation.ui.components

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.network.ConnectionManager

@Composable
fun ConnectionStatusBadge(
    connectionManager: ConnectionManager,
    modifier: Modifier = Modifier
) {
    val status by connectionManager.connectionStatus.collectAsStateWithLifecycle()
    val quality by connectionManager.connectionQuality.collectAsStateWithLifecycle()
    val currentIp by connectionManager.currentIp.collectAsStateWithLifecycle()

    var visible by remember { mutableStateOf(true) }
    LaunchedEffect(status) {
        visible = status != ConnectionManager.ConnectionStatus.CONNECTED
        if (status == ConnectionManager.ConnectionStatus.CONNECTED) {
            delay(3000)
            visible = false
        }
    }

    AnimatedVisibility(
        visible = visible,
        enter = fadeIn() + slideInVertically(),
        exit = fadeOut() + slideOutVertically()
    ) {
        Card(
            modifier = modifier
                .padding(8.dp)
                .wrapContentSize(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.9f)
            ),
            shape = MaterialTheme.shapes.medium
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> Icons.Default.Wifi
                        ConnectionManager.ConnectionStatus.CONNECTING -> Icons.Default.Sync
                        ConnectionManager.ConnectionStatus.RECONNECTING -> Icons.Default.Autorenew
                        else -> Icons.Default.WifiOff
                    },
                    contentDescription = "Status",
                    tint = when (status) {
                        ConnectionManager.ConnectionStatus.CONNECTED -> Color(0xFF4CAF50)
                        ConnectionManager.ConnectionStatus.CONNECTING -> Color(0xFFFFC107)
                        else -> MaterialTheme.colorScheme.error
                    },
                    modifier = Modifier.size(24.dp)
                )
                Column {
                    Text(
                        text = when (status) {
                            ConnectionManager.ConnectionStatus.CONNECTED -> "Connected to $currentIp"
                            ConnectionManager.ConnectionStatus.CONNECTING -> "Connecting..."
                            ConnectionManager.ConnectionStatus.RECONNECTING -> "Reconnecting..."
                            ConnectionManager.ConnectionStatus.ERROR -> "Connection error"
                            else -> "Disconnected"
                        },
                        style = MaterialTheme.typography.bodySmall
                    )
                    if (status == ConnectionManager.ConnectionStatus.CONNECTED) {
                        Text(
                            text = "Signal: ${quality.signalStrength.name} (${quality.ping}ms)",
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}