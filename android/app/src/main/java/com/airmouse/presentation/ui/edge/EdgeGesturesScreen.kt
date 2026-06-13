package com.airmouse.presentation.ui.edge

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Edge Gestures") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(
                            imageVector = androidx.compose.material.icons.Icons.Default.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Enable/Disable
            item {
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
                            Text(
                                text = "Enable Edge Gestures",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "Use volume buttons for quick actions",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Switch(
                            checked = uiState.isEnabled,
                            onCheckedChange = viewModel::setEnabled
                        )
                    }
                }
            }

            if (uiState.isEnabled) {
                // Volume Up Action
                item {
                    ActionCard(
                        title = "Volume Up",
                        currentAction = uiState.volumeUpAction,
                        actions = uiState.availableActions,
                        onActionSelected = viewModel::setVolumeUpAction
                    )
                }

                // Volume Down Action
                item {
                    ActionCard(
                        title = "Volume Down",
                        currentAction = uiState.volumeDownAction,
                        actions = uiState.availableActions,
                        onActionSelected = viewModel::setVolumeDownAction
                    )
                }

                // Long Press Action
                item {
                    ActionCard(
                        title = "Long Press",
                        currentAction = uiState.longPressAction,
                        actions = uiState.availableActions,
                        onActionSelected = viewModel::setLongPressAction
                    )
                }

                // Double Press Action
                item {
                    ActionCard(
                        title = "Double Press",
                        currentAction = uiState.doublePressAction,
                        actions = uiState.availableActions,
                        onActionSelected = viewModel::setDoublePressAction
                    )
                }

                // Vibration Feedback
                item {
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
                                Text(
                                    text = "Vibration Feedback",
                                    style = MaterialTheme.typography.titleMedium
                                )
                                Text(
                                    text = "Vibrate when gesture is detected",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            Switch(
                                checked = uiState.vibrationFeedback,
                                onCheckedChange = viewModel::setVibrationFeedback
                            )
                        }
                    }
                }

                // Sensitivity Slider
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp)
                        ) {
                            Text(
                                text = "Edge Sensitivity",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "How sensitive the gesture detection should be",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Spacer(modifier = Modifier.height(8.dp))

                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Less Sensitive")
                                Slider(
                                    value = uiState.screenEdgeSensitivity,
                                    onValueChange = viewModel::setScreenEdgeSensitivity,
                                    valueRange = 0.05f..0.5f,
                                    modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                                )
                                Text("More Sensitive")
                            }
                        }
                    }
                }

                // Reset Button
                item {
                    Button(
                        onClick = { viewModel.resetToDefaults() },
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.error,
                            contentColor = MaterialTheme.colorScheme.onError
                        )
                    ) {
                        Text("Reset to Defaults")
                    }
                }
            }
        }
    }
}

@Composable
fun ActionCard(
    title: String,
    currentAction: String,
    actions: List<String>,
    onActionSelected: (String) -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )

            Spacer(modifier = Modifier.height(8.dp))

            ExposedDropdownMenuBox(
                expanded = expanded,
                onExpandedChange = { expanded = !expanded }
            ) {
                OutlinedTextField(
                    value = currentAction,
                    onValueChange = {},
                    readOnly = true,
                    trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                    modifier = Modifier.menuAnchor(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = MaterialTheme.colorScheme.primary
                    )
                )

                ExposedDropdownMenu(
                    expanded = expanded,
                    onDismissRequest = { expanded = false }
                ) {
                    actions.forEach { action ->
                        DropdownMenuItem(
                            text = { Text(action) },
                            onClick = {
                                onActionSelected(action)
                                expanded = false
                            }
                        )
                    }
                }
            }
        }
    }
}