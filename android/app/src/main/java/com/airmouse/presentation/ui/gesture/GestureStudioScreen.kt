
package com.airmouse.presentation.ui.gesture

import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.duration
import com.airmouse.presentation.ui.components.GestureTrainingCenter
import com.airmouse.presentation.navigation.NavigationActions
import java.text.SimpleDateFormat
import java.util.*
import kotlin.math.abs
import kotlin.math.max


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureStudioScreen(
    navigationActions: NavigationActions,
    viewModel: GestureStudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val gestureName by viewModel.gestureName.collectAsStateWithLifecycle()
    val dateFormat = remember { SimpleDateFormat("MMM dd, yyyy", Locale.getDefault()) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "🎯 Gesture Studio",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.showExportDialog() }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    IconButton(onClick = { viewModel.showImportDialog() }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import")
                    }
                    IconButton(onClick = { viewModel.showTrainDialog() }) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Train All")
                    }
                    IconButton(onClick = { viewModel.showAddDialog() }) {
                        Icon(Icons.Default.Add, contentDescription = "Add Gesture")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    actionIconContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        containerColor = Color.Transparent,
        floatingActionButton = {
            if (uiState.savedGestures.isNotEmpty()) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.showAddDialog() },
                    icon = { Icon(Icons.Default.Add, contentDescription = "Add") },
                    text = { Text("New Gesture") }
                )
            }
        }
    ) { paddingValues ->
        PremiumDashboardBackground {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                
                item {
                    RecordingCard(
                        uiState = uiState,
                        onNameChange = { viewModel.updateGestureName(it) },
                        onStartRecording = { viewModel.startRecording() },
                        onStopRecording = { viewModel.stopRecording() },
                        onToggleWaveform = { viewModel.toggleWaveform() },
                        onSensorSelected = { viewModel.setSelectedSensor(it) }
                    )
                }

                
                if (uiState.isTraining) {
                    item {
                        TrainingProgressCard(
                            progress = uiState.trainingProgress,
                            currentGesture = uiState.trainingCurrentGesture,
                            totalGestures = uiState.trainingTotalGestures
                        )
                    }
                }

                
                if (uiState.lastRecognizedGesture != null && uiState.lastRecognizedGesture != "none") {
                    item {
                        RecognitionResultCard(
                            gesture = uiState.lastRecognizedGesture!!,
                            confidence = uiState.lastRecognitionConfidence,
                            onDismiss = { viewModel.clearRecognition() }
                        )
                    }
                }

                
                if (uiState.savedGestures.isNotEmpty()) {
                    item {
                        StatisticsHeader(stats = uiState.trainingStats ?: GestureTrainingStats())
                    }
                    item {
                        SearchFilterCard(
                            searchQuery = uiState.searchQuery,
                            onSearchQueryChange = { viewModel.updateSearchQuery(it) },
                            selectedFilterType = uiState.filterType,
                            onFilterTypeChange = { viewModel.updateFilterType(it) },
                            sortBy = uiState.sortBy,
                            onSortChange = { viewModel.updateSort(it) },
                            showFavoritesOnly = uiState.showFavoritesOnly,
                            onToggleFavoritesOnly = { viewModel.toggleFavoritesOnly() }
                        )
                    }
                }

                
                item {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "📋 Your Gestures (${uiState.savedGestures.size})",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        if (uiState.savedGestures.isNotEmpty()) {
                            TextButton(onClick = { viewModel.showTrainDialog() }) {
                                Text("Train All", color = Color(0xFF6366F1), fontWeight = FontWeight.Bold)
                            }
                        }
                    }
                }

                
                val gesturesToDisplay = uiState.filteredGestures
                if (uiState.savedGestures.isEmpty()) {
                    item {
                        EmptyGesturesCard(
                            onAddClick = { viewModel.showAddDialog() }
                        )
                    }
                } else if (gesturesToDisplay.isEmpty()) {
                    item {
                        GlassmorphicCard(
                            modifier = Modifier.fillMaxWidth(),
                            cornerRadius = 20.dp
                        ) {
                            Column(
                                modifier = Modifier.padding(24.dp).fillMaxWidth(),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(48.dp))
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("No matching gestures found", fontWeight = FontWeight.Bold)
                                Text("Try adjusting your search query or filters", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                } else {
                    items(gesturesToDisplay, key = { it.id }) { gesture ->
                        GestureCard(
                            gesture = gesture,
                            dateFormat = dateFormat,
                            onTrain = { viewModel.trainGesture(gesture.id) },
                            onDelete = { viewModel.showDeleteDialog(gesture.id) },
                            onEdit = { viewModel.showEditDialog(gesture) },
                            onDetails = { viewModel.showDetailsDialog(gesture) },
                            onPlayback = { viewModel.showPlaybackDialog(gesture) },
                            onToggleFavorite = { viewModel.toggleFavorite(gesture.id) },
                            onToggleEnabled = { viewModel.toggleGesture(gesture.id) }
                        )
                    }
                }
            }

            

            
            if (uiState.showAddGestureDialog) {
                AddGestureDialog(
                    name = uiState.newGestureName,
                    action = uiState.newGestureAction,
                    onNameChange = { viewModel.updateNewGestureName(it) },
                    onActionChange = { viewModel.updateNewGestureAction(it) },
                    onConfirm = {
                        viewModel.addGesture()
                    },
                    onDismiss = { viewModel.closeAllDialogs() },
                    isAdding = uiState.isLoading
                )
            }

            
            if (uiState.showEditGestureDialog && uiState.editGesture != null) {
                EditGestureDialog(
                    gesture = uiState.editGesture!!,
                    name = uiState.newGestureName,
                    action = uiState.newGestureAction,
                    onNameChange = { viewModel.updateNewGestureName(it) },
                    onActionChange = { viewModel.updateNewGestureAction(it) },
                    onConfirm = {
                        viewModel.updateGesture(
                            uiState.editGesture!!.copy(
                                name = uiState.newGestureName,
                                action = uiState.newGestureAction
                            )
                        )
                    },
                    onDismiss = { viewModel.closeAllDialogs() },
                    isUpdating = uiState.isLoading
                )
            }

            
            if (uiState.showDeleteDialog && uiState.deleteGestureId != null) {
                val gesture = uiState.savedGestures.find { it.id == uiState.deleteGestureId }
                DeleteGestureDialog(
                    gestureName = gesture?.name ?: "Gesture",
                    onConfirm = {
                        viewModel.deleteGesture(uiState.deleteGestureId!!)
                    },
                    onDismiss = { viewModel.closeAllDialogs() }
                )
            }

            
            if (uiState.showTrainDialog) {
                TrainAllDialog(
                    gestureCount = uiState.savedGestures.size,
                    onConfirm = {
                        viewModel.startTraining()
                        viewModel.closeAllDialogs()
                    },
                    onDismiss = { viewModel.closeAllDialogs() }
                )
            }

            
            if (uiState.showExportDialog) {
                ExportDialog(
                    initialFormat = uiState.exportFormat,
                    onConfirm = { format ->
                        viewModel.exportGestures(format)
                    },
                    onDismiss = { viewModel.closeAllDialogs() }
                )
            }

            
            if (uiState.showImportDialog) {
                ImportDialog(
                    onConfirm = { path ->
                        viewModel.importGestures(path)
                    },
                    onDismiss = { viewModel.closeAllDialogs() },
                    isImporting = uiState.isImporting,
                    importProgress = uiState.importProgress
                )
            }

            
            if (uiState.showDetailsDialog && uiState.selectedGesture != null) {
                GestureDetailsDialog(
                    gesture = uiState.selectedGesture!!,
                    onDismiss = { viewModel.closeAllDialogs() }
                )
            }

            
            if (uiState.showPlaybackDialog && uiState.selectedGesture != null) {
                PlaybackDialog(
                    gesture = uiState.selectedGesture!!,
                    speed = uiState.playbackSpeed,
                    onSpeedChange = { viewModel.setPlaybackSpeed(it) },
                    onDismiss = { viewModel.closeAllDialogs() }
                )
            }

            
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

            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    action = {
                        TextButton(onClick = { viewModel.clearMessages() }) {
                            Text("OK")
                        }
                    }
                ) {
                    Text(uiState.successMessage!!)
                }
            }
        }
    }
}



@Composable
fun RecordingCard(
    uiState: GestureStudioUiState,
    onNameChange: (String) -> Unit,
    onStartRecording: () -> Unit,
    onStopRecording: () -> Unit,
    onToggleWaveform: () -> Unit,
    onSensorSelected: (SensorType) -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 24.dp,
        backgroundAlpha = if (uiState.isRecording) 0.6f else 0.45f
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = if (uiState.isRecording) "🔴 Recording Mode" else "🎯 Create Gesture",
                        fontWeight = FontWeight.Bold,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = if (uiState.isRecording) "Step 1 → Recording  Step 2 → Preview  Step 3 → Save"
                               else "Name your gesture, then hit Record",
                        fontSize = 10.sp,
                        color = Color.White.copy(alpha = 0.5f)
                    )
                }
                IconButton(onClick = onToggleWaveform) {
                    Icon(
                        if (uiState.showWaveform) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                        contentDescription = "Toggle Waveform",
                        tint = Color(0xFF6366F1)
                    )
                }
            }
            Spacer(modifier = Modifier.height(8.dp))

            if (uiState.isRecording) {
                AnimatedRecordingIndicator()
                Spacer(modifier = Modifier.height(12.dp))

                
                // Pulsing neon ring around timer when recording
                val neonPulse by rememberInfiniteTransition(label = "NeonRing").animateFloat(
                    initialValue = 0.6f,
                    targetValue = 1f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, easing = FastOutSlowInEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "NeonBorderAlpha"
                )
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(100.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = neonPulse * 0.7f),
                                radius = size.minDimension / 2f,
                                style = Stroke(width = 4.dp.toPx())
                            )
                            drawCircle(
                                color = Color(0xFFEF4444).copy(alpha = neonPulse * 0.25f),
                                radius = size.minDimension / 2f + 8.dp.toPx(),
                                style = Stroke(width = 2.dp.toPx())
                            )
                        }
                ) {
                    Text(
                        text = formatTime(uiState.recordingTime),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFF6B6B)
                    )
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "${uiState.samplesCollected} samples collected",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.6f)
                )
                Spacer(modifier = Modifier.height(8.dp))
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

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                SensorType.entries.forEach { sensor ->
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

            
            if (uiState.showWaveform && uiState.waveformData.samples.isNotEmpty()) {
                WaveformVisualizer(
                    data = uiState.waveformData,
                    sensorType = uiState.selectedSensor,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(80.dp)
                )
                Spacer(modifier = Modifier.height(8.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "Peak: ${String.format(Locale.US, "%.2f", uiState.waveformData.maxValue)}",
                        fontSize = 10.sp,
                        color = Color(0xFF10B981)
                    )
                    Text(
                        "Rate: ${uiState.waveformData.samplingRate}Hz",
                        fontSize = 10.sp,
                        color = Color(0xFF6366F1)
                    )
                }
            }

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
                shape = RoundedCornerShape(16.dp),
                enabled = if (uiState.isRecording) true else uiState.gestureName.isNotBlank()
            ) {
                Icon(
                    if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isRecording) "Stop Recording" else "Start Recording")
            }

            if (uiState.progress > 0 && uiState.progress < 100) {
                Spacer(modifier = Modifier.height(12.dp))
                LinearProgressIndicator(
                    progress = { uiState.progress / 100f },
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
    val animatedAlpha by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(500),
        label = "Alpha"
    )

    Canvas(modifier = modifier) {
        val width = size.width
        val height = size.height
        val centerY = height / 2

        if (data.samples.isEmpty()) return@Canvas

        val stepX = width / data.samples.size
        val maxAbs = max(abs(data.maxValue), abs(data.minValue)).coerceAtLeast(0.1f)

        val color = when (sensorType) {
            SensorType.GYROSCOPE -> Color(0xFF2196F3)     // Blue
            SensorType.ACCELEROMETER -> Color(0xFF4CAF50) // Green
            SensorType.MAGNETOMETER -> Color(0xFFFFC107)  // Amber
            SensorType.ALL -> Color(0xFF9C27B0)          // Purple
        }

        for (i in 1..3) {
            val y = (height / 4) * i
            drawLine(
                color = Color.White.copy(alpha = 0.05f),
                start = Offset(0f, y.toFloat()),
                end = Offset(width, y.toFloat()),
                strokeWidth = 1f
            )
        }

        val path = Path()
        var isFirst = true

        for (i in data.samples.indices) {
            val x = i * stepX
            val normalizedY = (data.samples[i] / maxAbs).coerceIn(-1f, 1f)
            val y = centerY - (normalizedY * centerY * 0.8f)

            if (isFirst) {
                path.moveTo(x, y)
                isFirst = false
            } else {
                path.lineTo(x, y)
            }
        }

        drawPath(
            path = path,
            color = color.copy(alpha = animatedAlpha),
            style = Stroke(width = 2.5f, cap = StrokeCap.Round)
        )

        val fillPath = Path().apply {
            val firstX = 0f
            val lastX = width
            moveTo(firstX, height)

            for (i in data.samples.indices) {
                val x = i * stepX
                val normalizedY = (data.samples[i] / maxAbs).coerceIn(-1f, 1f)
                val y = centerY - (normalizedY * centerY * 0.8f)
                lineTo(x, y)
            }

            lineTo(lastX, height)
            close()
        }

        val brush = Brush.verticalGradient(
            colors = listOf(color.copy(alpha = 0.25f), Color.Transparent),
            startY = 0f,
            endY = height
        )

        drawPath(
            path = fillPath,
            brush = brush
        )

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
    val infiniteTransition = rememberInfiniteTransition(label = "Pulse")
    val pulse by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "IndicatorScale"
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
        Text(
            text = "●",
            color = Color.Red,
            fontSize = 8.sp,
            modifier = Modifier.animateContentSize()
        )
    }
}



@Composable
fun StatisticsHeader(stats: GestureTrainingStats) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem("Total", stats.totalGestures.toString(), "🎯")
            StatItem("Custom", stats.customGestureUsage.size.toString(), "✋")
            StatItem("Best", stats.mostUsedGesture.ifEmpty { "—" }, "🏆")
            StatItem("Confidence", "${(stats.averageConfidence * 100).toInt()}%", "📊")
        }
    }
}

@Composable
fun StatItem(label: String, value: String, icon: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(icon, fontSize = 22.sp)
        Text(
            value,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp,
            color = Color.White
        )
        Text(
            label,
            fontSize = 10.sp,
            color = Color.White.copy(alpha = 0.55f)
        )
    }
}



@Composable
fun TrainingProgressCard(progress: Int, currentGesture: String, totalGestures: Int) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 16.dp
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                com.airmouse.presentation.ui.components.SweepGradientGlowRing(
                    size = 24.dp,
                    strokeWidth = 2.dp
                )
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text("🧠 Training Model...", fontWeight = FontWeight.Bold, color = Color.White)
                    if (currentGesture.isNotEmpty()) {
                        Text("Current: $currentGesture", fontSize = 12.sp, color = Color.White.copy(alpha = 0.8f))
                        Text("${totalGestures} gestures total", fontSize = 11.sp, color = Color.White.copy(alpha = 0.55f))
                    }
                }
            }
            Spacer(modifier = Modifier.height(8.dp))
            LinearProgressIndicator(
                progress = { progress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = Color(0xFF6366F1),
                trackColor = Color.White.copy(alpha = 0.1f)
            )
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("$progress%", fontSize = 12.sp, color = Color.White)
                Text("${progress / 10}/10 epochs", fontSize = 12.sp, color = Color.White.copy(alpha = 0.55f))
            }
        }
    }
}



@Composable
fun RecognitionResultCard(gesture: String, confidence: Float, onDismiss: () -> Unit) {
    var isTargetReached by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { isTargetReached = true }

    val scale by animateFloatAsState(
        targetValue = if (isTargetReached) 1f else 0.8f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy),
        label = "EntranceScale"
    )

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        cornerRadius = 16.dp
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
                    Text("🎯 Gesture Recognized!", fontWeight = FontWeight.Bold, color = Color(0xFF10B981))
                    Text(
                        gesture.replace("_", " "),
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    Text(
                        "Confidence: ${(confidence * 100).toInt()}%",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
            IconButton(onClick = onDismiss) {
                Icon(Icons.Default.Close, contentDescription = "Dismiss")
            }
        }
    }
}



@Composable
fun EmptyGesturesCard(onAddClick: () -> Unit) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Gesture,
                contentDescription = null,
                modifier = Modifier.size(72.dp),
                tint = Color(0xFF6366F1)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                "No Custom Gestures Yet",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Text(
                "Record your first gesture above",
                fontSize = 13.sp,
                color = Color.White.copy(alpha = 0.6f)
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                "Try gestures like: ThumbsUp, CircleCW, SwipeLeft",
                fontSize = 11.sp,
                color = Color.White.copy(alpha = 0.45f)
            )
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = onAddClick,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Icon(Icons.Default.Add, contentDescription = null, tint = Color.White)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create First Gesture", color = Color.White, fontWeight = FontWeight.Bold)
            }
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
    onPlayback: () -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleEnabled: () -> Unit,
    onEdit: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        backgroundAlpha = if (gesture.isEnabled) 0.45f else 0.25f
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
                        fontWeight = FontWeight.Bold,
                        color = if (gesture.isEnabled) Color.White else Color.White.copy(alpha = 0.45f)
                    )
                    if (gesture.usageCount > 0) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFF6366F1).copy(alpha = 0.2f)
                        ) {
                            Text(
                                "Used ${gesture.usageCount}x",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFF818CF8)
                            )
                        }
                    }
                    if (!gesture.isEnabled) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = Color(0xFFEF4444).copy(alpha = 0.15f)
                        ) {
                            Text(
                                "Disabled",
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                                color = Color(0xFFEF4444)
                            )
                        }
                    }
                }
                Text(
                    "⚡ Action: ${gesture.action}",
                    fontSize = 12.sp,
                    color = Color(0xFF10B981)
                )
                Text(
                    "🗓 Created: ${dateFormat.format(Date(gesture.createdAt))}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
                Text(
                    "🎯 Confidence: ${(gesture.confidence * 100).toInt()}% • ${gesture.type.name}",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.5f)
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Row {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (gesture.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(18.dp),
                            tint = if (gesture.isFavorite) Color(0xFFFF6B6B) else Color.White.copy(alpha = 0.4f)
                        )
                    }
                    IconButton(
                        onClick = onToggleEnabled,
                        modifier = Modifier.size(32.dp)
                    ) {
                        Icon(
                            if (gesture.isEnabled) Icons.Default.Visibility else Icons.Default.VisibilityOff,
                            contentDescription = "Toggle",
                            modifier = Modifier.size(18.dp),
                            tint = if (gesture.isEnabled) Color(0xFF10B981) else Color.White.copy(alpha = 0.35f)
                        )
                    }
                }
                Row {
                    IconButton(onClick = onPlayback, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.PlayArrow, contentDescription = "Playback", modifier = Modifier.size(18.dp), tint = Color(0xFF38BDF8))
                    }
                    IconButton(onClick = onTrain, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.AutoAwesome, contentDescription = "Train", modifier = Modifier.size(18.dp), tint = Color(0xFFF59E0B))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp), tint = Color(0xFF818CF8))
                    }
                    IconButton(onClick = onDetails, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Info, contentDescription = "Details", modifier = Modifier.size(18.dp), tint = Color.White.copy(alpha = 0.5f))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(32.dp)) {
                        Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp), tint = Color(0xFFEF4444))
                    }
                }
            }
        }
    }
}



@Composable
fun AddGestureDialog(
    name: String,
    action: String,
    onNameChange: (String) -> Unit,
    onActionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isAdding: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✋ Add New Gesture") },
        text = {
            Column {
                Text("Create a custom gesture that will be recognized by the system.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Gesture Name") },
                    placeholder = { Text("e.g., ThumbsUp") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isAdding
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = action,
                    onValueChange = onActionChange,
                    label = { Text("Action") },
                    placeholder = { Text("e.g., play_pause") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isAdding
                )
                if (isAdding) {
                    Spacer(modifier = Modifier.height(8.dp))
                    com.airmouse.presentation.ui.components.SweepGradientGlowRing(size = 24.dp, strokeWidth = 2.dp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank() && action.isNotBlank() && !isAdding
            ) {
                Text("Add")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isAdding) { Text("Cancel") }
        }
    )
}

@Composable
fun EditGestureDialog(
    gesture: CustomGestureTemplate,
    name: String,
    action: String,
    onNameChange: (String) -> Unit,
    onActionChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    isUpdating: Boolean
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("✏️ Edit Gesture: ${gesture.name.replace('_', ' ')}") },
        text = {
            Column {
                Text("Update the name and action for this gesture.")
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = name,
                    onValueChange = onNameChange,
                    label = { Text("Gesture Name") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )
                Spacer(modifier = Modifier.height(8.dp))
                OutlinedTextField(
                    value = action,
                    onValueChange = onActionChange,
                    label = { Text("Action") },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    enabled = !isUpdating
                )
                if (isUpdating) {
                    Spacer(modifier = Modifier.height(8.dp))
                    com.airmouse.presentation.ui.components.SweepGradientGlowRing(size = 24.dp, strokeWidth = 2.dp)
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                enabled = name.isNotBlank() && action.isNotBlank() && !isUpdating
            ) {
                Text("Save")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss, enabled = !isUpdating) { Text("Cancel") }
        }
    )
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
        title = { Text("🧠 Train All Gestures") },
        text = {
            Column {
                Text("Train the classifier with all $gestureCount saved gestures.")
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "This will improve recognition accuracy and may take a few seconds.",
                    fontSize = 13.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
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
fun ExportDialog(
    initialFormat: ExportFormat,
    onConfirm: (ExportFormat) -> Unit,
    onDismiss: () -> Unit
) {
    var selectedFormat by remember { mutableStateOf(initialFormat) }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📤 Export Dataset") },
        text = {
            Column {
                Text("Export all gesture data to a file.")
                Spacer(modifier = Modifier.height(12.dp))
                Text("Select Format:", style = MaterialTheme.typography.labelMedium)
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ExportFormat.entries.forEach { format ->
                        FilterChip(
                            selected = selectedFormat == format,
                            onClick = { selectedFormat = format },
                            label = { Text(format.displayName) }
                        )
                    }
                }
                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    "The file will be saved to the application's external storage folder.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = { onConfirm(selectedFormat) }) { Text("Export") }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text("Cancel") }
        }
    )
}

@Composable
fun ImportDialog(
    onConfirm: (String) -> Unit,
    onDismiss: () -> Unit,
    isImporting: Boolean,
    importProgress: Int
) {
    var filePath by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("📥 Import Gestures") },
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
                    LinearProgressIndicator(
                        progress = { importProgress / 100f },
                        modifier = Modifier.fillMaxWidth()
                    )
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
                DetailsRow("Type", gesture.type.name)
                DetailsRow("Action", gesture.action)
                DetailsRow("Confidence", "${(gesture.confidence * 100).toInt()}%")
                DetailsRow("Created", SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(gesture.createdAt)))
                DetailsRow("Updated", SimpleDateFormat("MMM dd, yyyy HH:mm:ss", Locale.getDefault()).format(Date(gesture.updatedAt)))
                DetailsRow("Usage", "${gesture.usageCount} times")
                DetailsRow("Enabled", if (gesture.isEnabled) "✅ Yes" else "❌ No")
                DetailsRow("Favorite", if (gesture.isFavorite) "⭐ Yes" else "No")
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
        modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, color = Color.White.copy(alpha = 0.55f), fontSize = 13.sp)
        Text(value, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = Color.White)
    }
}

@Composable
fun PlaybackDialog(
    gesture: CustomGestureTemplate,
    speed: Float,
    onSpeedChange: (Float) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("▶️ Playback: ${gesture.name}") },
        text = {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text("Simulating gesture motion playback...")
                Spacer(modifier = Modifier.height(16.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 32.sp)
                        Text("X", fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("↕️", fontSize = 32.sp)
                        Text("Y", fontSize = 12.sp)
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text("🔄", fontSize = 32.sp)
                        Text("Z", fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                Text("Speed", fontSize = 12.sp)
                Slider(
                    value = speed,
                    onValueChange = onSpeedChange,
                    valueRange = 0.5f..2.0f,
                    modifier = Modifier.fillMaxWidth()
                )
                Text("${String.format(Locale.US, "%.1f", speed)}x", fontSize = 12.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    "Duration: ~${String.format(Locale.US, "%.1f", gesture.duration)}s",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Done") }
        }
    )
}



private fun formatTime(millis: Int): String {
    val seconds = millis / 1000
    val minutes = seconds / 60
    val remainingSeconds = seconds % 60
    return String.format(Locale.US, "%02d:%02d", minutes, remainingSeconds)
}

@Composable
fun SearchFilterCard(
    searchQuery: String,
    onSearchQueryChange: (String) -> Unit,
    selectedFilterType: com.airmouse.domain.model.GestureType?,
    onFilterTypeChange: (com.airmouse.domain.model.GestureType?) -> Unit,
    sortBy: GestureSort,
    onSortChange: (GestureSort) -> Unit,
    showFavoritesOnly: Boolean,
    onToggleFavoritesOnly: () -> Unit
) {
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            
            OutlinedTextField(
                value = searchQuery,
                onValueChange = onSearchQueryChange,
                label = { Text("Search gestures...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(16.dp),
                trailingIcon = {
                    if (searchQuery.isNotEmpty()) {
                        IconButton(onClick = { onSearchQueryChange("") }) {
                            Icon(Icons.Default.Close, contentDescription = "Clear")
                        }
                    }
                }
            )

            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                
                FilterChip(
                    selected = showFavoritesOnly,
                    onClick = onToggleFavoritesOnly,
                    label = { Text("⭐ Favorites Only", fontSize = 11.sp) },
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = MaterialTheme.colorScheme.primaryContainer
                    )
                )

                
                var showSortMenu by remember { mutableStateOf(false) }
                Box {
                    AssistChip(
                        onClick = { showSortMenu = true },
                        label = { Text("Sort: ${sortBy.displayName}", fontSize = 11.sp) },
                        leadingIcon = { Icon(Icons.AutoMirrored.Filled.Sort, contentDescription = null, modifier = Modifier.size(16.dp)) }
                    )
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false }
                    ) {
                        GestureSort.entries.forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(sortOption.displayName) },
                                onClick = {
                                    onSortChange(sortOption)
                                    showSortMenu = false
                                }
                            )
                        }
                    }
                }
            }

            
            Text("Filter by Type:", style = MaterialTheme.typography.labelMedium, color = Color.White.copy(alpha = 0.7f), fontWeight = FontWeight.SemiBold)
            androidx.compose.foundation.lazy.LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item {
                    FilterChip(
                        selected = selectedFilterType == null,
                        onClick = { onFilterTypeChange(null) },
                        label = { Text("All", fontSize = 11.sp) }
                    )
                }
                items(com.airmouse.domain.model.GestureType.entries.filter { it != com.airmouse.domain.model.GestureType.NONE }) { type ->
                    FilterChip(
                        selected = selectedFilterType == type,
                        onClick = { onFilterTypeChange(type) },
                        label = { Text(type.name.replace("_", " "), fontSize = 11.sp) }
                    )
                }
            }
        }
    }
}
