package com.airmouse.presentation.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.airmouse.network.ConnectionManager

@Composable
fun ConnectionStatusBadge(
    status: ConnectionManager.ConnectionStatus,
    onRetry: () -> Unit = {},
    onDisconnect: () -> Unit = {}
) {
    val label = when (status) {
        ConnectionManager.ConnectionStatus.CONNECTED -> "Connected"
        ConnectionManager.ConnectionStatus.CONNECTING -> "Connecting"
        ConnectionManager.ConnectionStatus.DISCONNECTED -> "Disconnected"
        ConnectionManager.ConnectionStatus.RECONNECTING -> "Reconnecting"
        ConnectionManager.ConnectionStatus.ERROR -> "Error"
    }

    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Surface(
            shape = RoundedCornerShape(999.dp),
            color = MaterialTheme.colorScheme.surfaceVariant
        ) {
            Text(text = label, modifier = Modifier.clip(RoundedCornerShape(999.dp)))
        }

        when (status) {
            ConnectionManager.ConnectionStatus.CONNECTED -> {
                IconButton(onClick = onDisconnect) {
                    Icon(Icons.Default.Close, contentDescription = "Disconnect")
                }
            }
            else -> {
                IconButton(onClick = onRetry) {
                    Icon(Icons.Default.Refresh, contentDescription = "Retry")
                }
            }
        }
    }
}
