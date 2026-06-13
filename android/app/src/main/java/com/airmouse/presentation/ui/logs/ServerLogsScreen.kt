package com.airmouse.presentation.ui.logs

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.sp
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ServerLogsScreen(
    navigationActions: NavigationActions,
    viewModel: ServerLogsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Server Logs") }, navigationIcon = { BackButton(navigationActions) }) }
    ) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp)) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                OutlinedTextField(value = uiState.filter, onValueChange = viewModel::setFilter, label = { Text("Filter") }, modifier = Modifier.weight(1f))
                Button(onClick = viewModel::clearLogs) { Text("Clear") }
                Button(onClick = viewModel::exportLogs) { Text("Export") }
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                FilterChip(selected = uiState.level == "All", onClick = { viewModel.setLevel("All") }, label = { Text("All") })
                FilterChip(selected = uiState.level == "Info", onClick = { viewModel.setLevel("Info") }, label = { Text("Info") })
                FilterChip(selected = uiState.level == "Warn", onClick = { viewModel.setLevel("Warn") }, label = { Text("Warn") })
                FilterChip(selected = uiState.level == "Error", onClick = { viewModel.setLevel("Error") }, label = { Text("Error") })
            }
            Spacer(modifier = Modifier.height(8.dp))
            LazyColumn {
                items(uiState.logs) { log -> Text(log, modifier = Modifier.padding(vertical = 4.dp), fontSize = 12.sp) }
            }
        }
    }
}

@Composable
private fun BackButton(navigationActions: NavigationActions) {
    IconButton(onClick = { navigationActions.navigateBack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}
