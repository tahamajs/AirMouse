package com.airmouse.presentation.ui.network

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkDiscoveryScreen(
    viewModel: NetworkDiscoveryViewModel = hiltViewModel(),
    onServerSelected: (String, Int) -> Unit = { _, _ -> }
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Network Discovery") }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Button(
                    onClick = { viewModel.scanNetwork() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !uiState.isScanning
                ) {
                    if (uiState.isScanning) CircularProgressIndicator(modifier = Modifier.size(24.dp))
                    else Text("Scan Network")
                }
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.status)
            }
            if (uiState.discoveredServers.isNotEmpty()) {
                item {
                    Card(modifier = Modifier.fillMaxWidth()) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text("Discovered Servers", style = MaterialTheme.typography.titleMedium)
                            uiState.discoveredServers.forEach { server ->
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onServerSelected(server.ip, server.port) }
                                        .padding(vertical = 8.dp)
                                ) {
                                    Text("${server.ip}:${server.port}")
                                }
                                Divider()
                            }
                        }
                    }
                }
            }
        }
    }
}