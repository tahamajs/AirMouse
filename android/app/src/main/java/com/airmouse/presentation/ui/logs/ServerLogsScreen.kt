package com.airmouse.presentation.ui.logs

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
fun ServerLogsScreen(
    viewModel: ServerLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Server Logs") }) }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.filter,
                    onValueChange = viewModel::setFilter,
                    label = { Text("Filter") },
                    modifier = Modifier.weight(1f)
                )
                Button(onClick = viewModel::clearLogs) { Text("Clear") }
                Button(onClick = viewModel::exportLogs) { Text("Export") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row {
                listOf("All", "Info", "Warn", "Error").forEach { level ->
                    FilterChip(
                        selected = uiState.level == level,
                        onClick = { viewModel.setLevel(level) },
                        label = { Text(level) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
            }
            Spacer(modifier = Modifier.height(16.dp))
            LazyColumn {
                items(uiState.logs) { log ->
                    Text(log, modifier = Modifier.padding(vertical = 4.dp), fontSize = 12.sp)
                }
            }
        }
    }
}