package com.airmouse.presentation.ui.edge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeGesturesScreen(
    navigationActions: NavigationActions,
    viewModel: EdgeGesturesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val actions = listOf("Click", "Double Click", "Right Click", "Scroll Up", "Scroll Down", "Next Track", "Previous Track", "Volume Up", "Volume Down")

    Scaffold(
        topBar = { TopAppBar(title = { Text("Edge Gestures") }, navigationIcon = { BackButton(navigationActions) }) }
    ) { paddingValues ->
        LazyColumn(modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Row(modifier = Modifier.fillMaxWidth().padding(16.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text("Enable Edge Gestures")
                        Switch(checked = uiState.isEnabled, onCheckedChange = viewModel::setEnabled)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Volume Up (long press)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(actions.size) { idx ->
                                val action = actions[idx]
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(action)
                                    RadioButton(selected = uiState.volumeUpAction == action, onClick = { viewModel.setVolumeUpAction(action) })
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("Volume Down (long press)", style = MaterialTheme.typography.titleMedium)
                        Spacer(modifier = Modifier.height(8.dp))
                        LazyColumn {
                            items(actions.size) { idx ->
                                val action = actions[idx]
                                Row(horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
                                    Text(action)
                                    RadioButton(selected = uiState.volumeDownAction == action, onClick = { viewModel.setVolumeDownAction(action) })
                                }
                            }
                        }
                    }
                }
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
