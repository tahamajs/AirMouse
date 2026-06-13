// app/src/main/java/com/airmouse/presentation/ui/gesture/GestureStudioScreen.kt
package com.airmouse.presentation.ui.gesture

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureStudioScreen(
    navigationActions: NavigationActions,
    viewModel: GestureStudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Gesture Studio") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showExportDialog(true) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.showTrainDialog(true) }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Train All")
                    }
                }
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize().padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Recording Card
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = if (uiState.isRecording)
                                MaterialTheme.colorScheme.errorContainer
                            else
                                MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(24.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(20.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            if (uiState.isRecording) {
                                AnimatedRecordingIndicator()
                                Spacer(Modifier.height(8.dp))
                            }

                            OutlinedTextField(
                                value = uiState.gestureName,
                                onValueChange = viewModel::updateGestureName,
                                label = { Text("Gesture Name") },
                                placeholder = { Text("e.g., ThumbsUp, CircleCW") },
                                modifier = Modifier.fillMaxWidth(),
                                enabled = !uiState.isRecording,
                                singleLine = true
                            )

                            Spacer(Modifier.height(16.dp))

                            Button(
                                onClick = {
                                    if (uiState.isRecording) viewModel.stopRecording()
                                    else viewModel.startRecording()
                                },
                                modifier = Modifier.fillMaxWidth(),
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = if (uiState.isRecording)
                                        MaterialTheme.colorScheme.error
                                    else
                                        MaterialTheme.colorScheme.primary
                                )
                            ) {
                                Icon(
                                    if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                                    contentDescription = null
                                )
                                Spacer(Modifier.width(8.dp))
                                Text(if (uiState.isRecording) "Stop Recording" else "Start Recording")
                            }

                            if (uiState.progress > 0 && uiState.progress < 100) {
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(progress = uiState.progress / 100f, modifier = Modifier.fillMaxWidth())
                                Text("${uiState.progress}%", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }

                            if (uiState.status.isNotEmpty()) {
                                Spacer(Modifier.height(8.dp))
                                Text(
                                    text = uiState.status,
                                    fontSize = 14.sp,
                                    color = Color(uiState.statusColor)
                                )
                            }
                        }
                    }
                }

                // Training progress card
                if (uiState.isTraining) {
                    item {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                        ) {
                            Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                                Text("Training model...", fontWeight = FontWeight.Bold)
                                Spacer(Modifier.height(8.dp))
                                LinearProgressIndicator(progress = uiState.trainingProgress / 100f, modifier = Modifier.fillMaxWidth())
                                Text("${uiState.trainingProgress}%", fontSize = 12.sp)
                            }
                        }
                    }
                }

                // Gestures list header
                item {
                    Text(
                        text = "Your Custom Gestures",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(top = 8.dp)
                    )
                }

                if (uiState.savedGestures.isEmpty()) {
                    item {
                        EmptyGesturesCard()
                    }
                } else {
                    items(uiState.savedGestures) { gesture ->
                        GestureCard(
                            gesture = gesture,
                            dateFormat = dateFormat,
                            onTrain = { viewModel.trainGesture(gesture.id) },
                            onDelete = {
                                viewModel.selectGesture(gesture)
                                viewModel.showDeleteDialog(true)
                            }
                        )
                    }
                }
            }

            // Dialogs
            if (uiState.showDeleteDialog && uiState.selectedGesture != null) {
                AlertDialog(
                    onDismissRequest = { viewModel.showDeleteDialog(false) },
                    title = { Text("Delete Gesture") },
                    text = { Text("Delete '${uiState.selectedGesture?.name}' permanently?") },
                    confirmButton = {
                        TextButton(
                            onClick = { uiState.selectedGesture?.let { viewModel.deleteGesture(it.id) } },
                            colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
                        ) { Text("Delete") }
                    },
                    dismissButton = { TextButton(onClick = { viewModel.showDeleteDialog(false) }) { Text("Cancel") } }
                )
            }

            if (uiState.showExportDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showExportDialog(false) },
                    title = { Text("Export Dataset") },
                    text = { Text("Export all recorded gestures to a CSV file?") },
                    confirmButton = { TextButton(onClick = { viewModel.exportDataset() }) { Text("Export") } },
                    dismissButton = { TextButton(onClick = { viewModel.showExportDialog(false) }) { Text("Cancel") } }
                )
            }

            if (uiState.showTrainDialog) {
                AlertDialog(
                    onDismissRequest = { viewModel.showTrainDialog(false) },
                    title = { Text("Train All Gestures") },
                    text = { Text("Retrain the classifier with all saved gestures. This may take a few seconds.") },
                    confirmButton = {
                        TextButton(onClick = {
                            viewModel.showTrainDialog(false)
                            uiState.savedGestures.forEach { viewModel.trainGesture(it.id) }
                        }) { Text("Train All") }
                    },
                    dismissButton = { TextButton(onClick = { viewModel.showTrainDialog(false) }) { Text("Cancel") } }
                )
            }
        }
    }
}

@Composable
fun AnimatedRecordingIndicator() {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .clip(CircleShape)
                .background(Color.Red)
                .animateContentSize()
        )
        Text("RECORDING", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun EmptyGesturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(Icons.Default.Gesture, contentDescription = null, modifier = Modifier.size(48.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.height(8.dp))
            Text("No Custom Gestures", style = MaterialTheme.typography.titleMedium)
            Text("Record your first gesture above", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun GestureCard(
    gesture: com.airmouse.domain.model.CustomGestureTemplate,
    dateFormat: SimpleDateFormat,
    onTrain: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(gesture.name, style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                    if (gesture.isTrained) {
                        Surface(shape = RoundedCornerShape(4.dp), color = MaterialTheme.colorScheme.primaryContainer) {
                            Text("Trained", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                }
                Text("Created: ${dateFormat.format(Date(gesture.createdAt))}", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                if (gesture.confidence > 0) {
                    Text("Confidence: ${(gesture.confidence * 100).toInt()}%", fontSize = 10.sp, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row {
                IconButton(onClick = onTrain) { Icon(Icons.Default.AutoAwesome, contentDescription = "Train", tint = MaterialTheme.colorScheme.primary) }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error) }
            }
        }
    }
}