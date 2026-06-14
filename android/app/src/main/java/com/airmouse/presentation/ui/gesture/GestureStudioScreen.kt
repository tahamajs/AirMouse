package com.airmouse.presentation.ui.gesture

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontFamily
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
    val waveformData by viewModel.waveformData.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        "Gesture Studio",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showExportDialog(true) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.showImportDialog(true) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import")
                    }
                    IconButton(onClick = { viewModel.showTrainDialog(true) }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Train All")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
                    RecordingCard(
                        uiState = uiState,
                        waveformData = waveformData,
                        onNameChange = viewModel::updateGestureName,
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() },
                        onToggleWaveform = { viewModel.toggleWaveform() },
                        onSensorSelected = { viewModel.setSelectedSensor(it) }
                    )
                }

                // Training progress card
                if (uiState.isTraining) {
                    item {
                        TrainingProgressCard(
                            progress = uiState.trainingProgress,
                            currentGesture = uiState.trainingCurrentGesture,
                            totalGestures = uiState.trainingTotalGestures
                        )
                    }
                }

                // Gesture recognition result
                if (uiState.lastRecognizedGesture != null && uiState.lastRecognizedGesture != "none") {
                    item {
                        RecognitionResultCard(
                            gesture = uiState.lastRecognizedGesture!!,
                            confidence = uiState.lastRecognitionConfidence,
                            onDismiss = { viewModel.resetRecognition() }
                        )
                    }
                }

                // Gestures list header
                item {
                    Text(
                        text = "🎯 Your Custom Gestures",
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
                            },
                            onDetails = {
                                viewModel.selectGesture(gesture)
                                viewModel.showDetailsDialog(true)
                            },
                            onPlayback = {
                                viewModel.selectGesture(gesture)
                                viewModel.showPlaybackDialog(true)
                            }
                        )
                    }
                }
            }

            // Dialogs
            if (uiState.showDeleteDialog && uiState.selectedGesture != null) {
                DeleteGestureDialog(
                    gestureName = uiState.selectedGesture?.name ?: "",
                    onConfirm = { viewModel.deleteGesture(uiState.selectedGesture!!.id) },
                    onDismiss = { viewModel.showDeleteDialog(false) }
                )
            }

            if (uiState.showTrainDialog) {
                TrainAllDialog(
                    gestureCount = uiState.savedGestures.filter { !it.isTrained }.size,
                    onConfirm = { viewModel.trainAllGestures() },
                    onDismiss = { viewModel.showTrainDialog(false) }
                )
            }

            if (uiState.showExportDialog) {
                ExportDialog(
                    onConfirm = { viewModel.exportDataset() },
                    onDismiss = { viewModel.showExportDialog(false) }
                )
            }

            if (uiState.showImportDialog) {
                ImportDialog(
                    onConfirm = { path -> viewModel.importGestures(path) },
                    onDismiss = { viewModel.showImportDialog(false) },
                    isImporting = uiState.isImporting,
                    importProgress = uiState.importProgress
                )
            }

            if (uiState.showDetailsDialog && uiState.selectedGesture != null) {
                GestureDetailsDialog(
                    gesture = uiState.selectedGesture!!,
                    onDismiss = { viewModel.showDetailsDialog(false) }
                )
            }

            if (uiState.showPlaybackDialog && uiState.selectedGesture != null) {
                PlaybackDialog(
                    gesture = uiState.selectedGesture!!,
                    speed = uiState.playbackSpeed,
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    onDismiss = { viewModel.showPlaybackDialog(false) }
                )
            }

            // Error/Success Snackbar
            if (uiState.errorMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("Dismiss")
                        }
                    }
                ) {
                    Text(uiState.errorMessage!!)
                }
            }
        }
    }
}

@Composable
fun RecordingCard(
    uiState: GestureStudioUiState,
    waveformData: GestureWaveformData,
    onNameChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleWaveform: () -> Unit,
    onSensorSelected: (SensorType) -> Unit
) {
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
                Spacer(modifier = Modifier.height(12.dp))
            }

            OutlinedTextField(
                value = uiState.gestureName,
                onValueChange = onNameChange,
                label = { Text("Gesture Name") },
                placeholder = { Text("e.g., ThumbsUp, CircleCW, SwipeLeft") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRecording,
                singleLine = true,
                shape = RoundedCornerShape(16.dp)
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Sensor type selector
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorType.values().forEach { sensor ->
                    FilterChip(
                        selected = uiState.selectedSensor == sensor,
                        onClick = { onSensorSelected(sensor) },
                        label = { Text("${sensor.icon} ${sensor.displayName}", fontSize = 11.sp) },
                        modifier = Modifier.weight(1f),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Waveform visualization
            if (uiState.showWaveform && waveformData.samples.isNotEmpty()) {
                WaveformVisualizer(
                    data = waveformData,
                    sensorType = uiState.selectedSensor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(100.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Recording button
            Button(
                onClick = {
                    if (uiState.isRecording) onStopRecording()
                    else onStartRecording()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isRecording)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Icon(
                    if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isRecording) "Stop Recording" else "Start Recording")
            }

            // Progress and quality indicators
            if (uiState.progress > 0 && uiState.progress < 100) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = uiState.progress / 100f,
                    modifier = Modifier.fillMaxWidth(),
                    color = when (uiState.recordingQuality) {
                        RecordingQuality.EXCELLENT -> Color(0xFF4CAF50)
                        RecordingQuality.GOOD -> Color(0xFF8BC34A)
                        RecordingQuality.FAIR -> Color(0xFFFFC107)
                        else -> Color(0xFFF44336)
                    }
                )
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("${uiState.progress}%", fontSize = 12.sp)
                    Text(
                        uiState.recordingQuality.displayName,
                        fontSize = 12.sp,
                        color = uiState.recordingQuality.color
                    )
                }
            }

            if (uiState.status.isNotEmpty() && !uiState.isRecording) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = uiState.status,
                    fontSize = 13.sp,
                    color = uiState.statusColor
                )
            }
        }
    }
}

@Composable
fun WaveformVisualizer(data: GestureWaveformData, sensorType: SensorType, modifier: Modifier = Modifier) {
    val animatedAlpha by animateFloatAsState(targetValue = 1f, animationSpec = tween(500))
    
    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2
        
        if (data.samples.isEmpty()) return@Canvas
        
        val stepX = width / data.samples.size
        val maxAbs = max(abs(data.maxValue), abs(data.minValue)).coerceAtLeast(0.1f)
        
        val colors = listOf(
            Color(0xFFFF5722), // Gyro X
            Color(0xFF4CAF50), // Gyro Y
            Color(0xFF2196F3), // Gyro Z
            Color(0xFFFFC107), // Accel X
            Color(0xFF9C27B0), // Accel Y
            Color(0xFF00BCD4)  // Accel Z
        )
        
        val indices = when (sensorType) {
            SensorType.GYROSCOPE -> 0..2
            SensorType.ACCELEROMETER -> 3..5
            SensorType.MAGNETOMETER -> 6..8
            SensorType.ALL -> 0..5
        }
        
        for (channel in indices) {
            if (channel >= data.samples.size) continue
            
            val path = Path()
            var isFirst = true
            
            for (i in data.samples.indices) {
                val x = i * stepX
                val normalizedY = (data.samples[i] / maxAbs).coerceIn(-1f, 1f)
                val y = centerY - (normalizedY * centerY)
                
                if (isFirst) {
                    path.moveTo(x, y)
                    isFirst = false
                } else {
                    path.lineTo(x, y)
                }
            }
            
            drawPath(
                path = path,
                color = colors[channel % colors.size].copy(alpha = animatedAlpha),
                style = Stroke(width = 2f, cap = StrokeCap.Round)
            )
        }
        
        // Draw zero line
        drawLine(
            color = Color.White.copy(alpha = 0.2f),
            start = Offset(0f, centerY),
            end = Offset(width, centerY),
            strokeWidth = 1f,
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f))
        )
    }
}

@Composable
fun AnimatedRecordingIndicator() {
    val infiniteTransition = rememberInfiniteTransition()
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )
    
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(12.dp)
                .scale(pulse)
                .clip(CircleShape)
                .background(Color.Red)
        )
        Text("RECORDING", color = Color.Red, fontWeight = FontWeight.Bold, fontSize = 12.sp)
    }
}

@Composable
fun TrainingProgressCard(progress: Int, currentGesture: String, totalGestures: Int) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CircularProgressIndicator(modifier = Modifier.size(24.dp), strokeWidth = 2.dp)
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Training Model...", fontWeight = FontWeight.Bold)
                    if (currentGesture.isNotEmpty()) {
                        Text("Current: $currentGesture", fontSize = 12.sp)
                        Text("${totalGestures} gestures total", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = progress / 100f,
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
            Text("$progress%", fontSize = 12.sp, modifier = Modifier.padding(top = 4.dp))
        }
    }
}

@Composable
fun RecognitionResultCard(gesture: String, confidence: Float, onDismiss: () -> Unit) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy)
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.15f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .background(Color(0xFF4CAF50).copy(alpha = 0.2f)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF4CAF50))
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("Gesture Recognized!", fontWeight = FontWeight.Bold, color = Color(0xFF4CAF50))
                    Text(gesture.replace("_", " "), fontSize = 18.sp, fontWeight = FontWeight.Bold)
                    Text("Confidence: ${(confidence * 100).toInt()}%", fontSize = 12.sp)
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}

@Composable
fun EmptyGesturesCard() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Gesture,
                contentDescription = null,
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text("No Custom Gestures", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Medium)
            Text("Record your first gesture above", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Try gestures like: ThumbsUp, CircleCW, SwipeLeft",
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun GestureCard(
    gesture: CustomGestureTemplate,
    dateFormat: SimpleDateFormat,
    onTrain: () -> Unit,
    onDelete: () -> Unit,
    onDetails: () -> Unit,
    onPlayback: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(20.dp)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        gesture.name.replace("_", " "),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (gesture.isTrained) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.primaryContainer
                        ) {
                            Text("Trained", fontSize = 10.sp, modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp))
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = when (gesture.quality) {
                            "EXCELLENT" -> Color(0xFF4CAF50).copy(alpha = 0.2f)
                            "GOOD" -> Color(0xFF8BC34A).copy(alpha = 0.2f)
                            "FAIR" -> Color(0xFFFFC107).copy(alpha = 0.2f)
                            else -> Color(0xFFF44336).copy(alpha = 0.2f)
                        }
                    ) {
                        Text(
                            gesture.quality.takeIf { it.isNotEmpty() } ?: "UNKNOWN",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = when (gesture.quality) {
                                "EXCELLENT" -> Color(0xFF4CAF50)
                                "GOOD" -> Color(0xFF8BC34A)
                                "FAIR" -> Color(0xFFFFC107)
                                else -> Color(0xFFF44336)
                            }
                        )
                    }
                }
                Text(
                    "Created: ${dateFormat.format(Date(gesture.createdAt))}",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    "${gesture.sampleCount} samples • ${gesture.duration}s duration",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Row {
                IconButton(onClick = onPlayback) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Playback", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onTrain) {
                    Icon(Icons.Default.AutoAwesome, contentDescription = "Train", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onDetails) {
                    Icon(Icons.Default.Info, contentDescription = "Details", modifier = Modifier.size(22.dp))
                }
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(22.dp), tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun DeleteGestureDialog(gestureName: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Delete Gesture") },
        text = { Text("Delete '$gestureName' permanently? This action cannot be undone.") },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                colors = ButtonDefaults.textButtonColors(contentColor = MaterialTheme.colorScheme.error)
            ) {
                Text("Delete")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun TrainAllDialog(gestureCount: Int, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Train All Gestures") },
        text = { 
            Text("Train the classifier with all $gestureCount saved gestures. This will improve recognition accuracy and may take a few seconds.")
        },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Train All") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ExportDialog(onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Export Dataset") },
        text = { Text("Export all gesture data to a CSV file. The file will be saved to your device storage.") },
        confirmButton = {
            TextButton(onClick = onConfirm) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportDialog(onConfirm: (String) -> Unit, onDismiss: () -> Unit, isImporting: Boolean, importProgress: Int) {
    var filePath by remember { mutableStateOf("") }
    
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Import Gestures") },
        text = {
            Column {
                Text("Import gestures from a CSV file.")
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = filePath,
                    onValueChange = { filePath = it },
                    label = { Text("File Path") },
                    placeholder = { Text("/storage/emulated/0/gesture_export.csv") },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = !isImporting,
                    singleLine = true
                )
                if (isImporting) {
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(progress = importProgress / 100f, modifier = Modifier.fillMaxWidth())
                    Text("Importing... $importProgress%", fontSize = 11.sp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = { if (filePath.isNotBlank()) onConfirm(filePath) },
                enabled = !isImporting && filePath.isNotBlank()
            ) {
                Text("Import")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isImporting) { Text("Cancel") }
        }
    )
}

@Composable
fun GestureDetailsDialog(gesture: CustomGestureTemplate, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(gesture.name.replace("_", " ")) },
        text = {
            Column {
                DetailsRow("ID", gesture.id.take(8))
                DetailsRow("Created", SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(gesture.createdAt)))
                DetailsRow("Samples", "${gesture.sampleCount}")
                DetailsRow("Duration", "${gesture.duration}s")
                DetailsRow("Quality", gesture.quality)
                DetailsRow("Trained", if (gesture.isTrained) "Yes" else "No")
                if (gesture.features != null) {
                    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
                    Text("Features:", fontWeight = FontWeight.Bold, fontSize = 12.sp)
                    gesture.features?.forEach { (key, value) ->
                        DetailsRow(key, String.format("%.3f", value))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun PlaybackDialog(gesture: CustomGestureTemplate, speed: Float, onSpeedChange: (Float) -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Playback: ${gesture.name.replace("_", " ")}") },
        text = {
            Column {
                Text("Speed: ${speed}x", fontSize = 12.sp)
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.5f..2f,
                    steps = 3
                )
                Spacer(modifier = Modifier.height(12.dp))
                Text("Samples: ${gesture.sampleCount}", fontSize = 12.sp)
                Text("Duration: ${gesture.duration}s", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(12.dp))
                
                // Mini waveform preview
                if (gesture.samples.isNotEmpty()) {
                    val previewData = gesture.samples.take(50).map { it[0] }
                    Canvas(modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color.Black.copy(alpha = 0.3f))
                    ) {
                        val width = size.width
                        val height = size.height
                        val centerY = height / 2
                        val stepX = width / previewData.size
                        val maxVal = previewData.maxOrNull()?.let { maxOf(abs(it), 0.1f) } ?: 0.1f
                        
                        val path = Path()
                        previewData.forEachIndexed { i, value ->
                            val x = i * stepX
                            val y = centerY - (value / maxVal) * centerY
                            if (i == 0) path.moveTo(x, y)
                            else path.lineTo(x, y)
                        }
                        drawPath(path, Color(0xFFFF5722), style = Stroke(width = 2f))
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun DetailsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, fontSize = 12.sp, fontWeight = FontWeight.Medium, fontFamily = FontFamily.Monospace)
    }
}