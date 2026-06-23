package com.airmouse.presentation.ui.edge

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.VolumeDown
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EdgeGesturesScreen(
    navigationActions: NavigationActions,
    viewModel: EdgeGesturesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    var showStatsDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Edge Gestures",
                        fontWeight = FontWeight.Bold
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { showStatsDialog = true }) {
                        Icon(Icons.Default.Assessment, contentDescription = "Statistics")
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
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    EnableCard(
                        isEnabled = uiState.isEnabled,
                        onEnableChanged = viewModel::setEnabled
                    )
                }

                if (uiState.isEnabled) {
                    item {
                        GestureActionCard(
                            title = "Volume Up",
                            icon = Icons.AutoMirrored.Filled.VolumeUp,
                            currentAction = uiState.volumeUpAction,
                            onActionSelected = viewModel::setVolumeUpAction,
                            onConfigure = { viewModel.startGestureConfiguration(uiState.volumeUpAction) }
                        )
                    }

                    item {
                        GestureActionCard(
                            title = "Volume Down",
                            icon = Icons.AutoMirrored.Filled.VolumeDown,
                            currentAction = uiState.volumeDownAction,
                            onActionSelected = viewModel::setVolumeDownAction,
                            onConfigure = { viewModel.startGestureConfiguration(uiState.volumeDownAction) }
                        )
                    }

                    item {
                        GestureActionCard(
                            title = "Long Press",
                            icon = Icons.Default.TouchApp,
                            currentAction = uiState.longPressAction,
                            onActionSelected = viewModel::setLongPressAction,
                            onConfigure = { viewModel.startGestureConfiguration(uiState.longPressAction) }
                        )
                    }

                    item {
                        GestureActionCard(
                            title = "Double Press",
                            icon = Icons.Default.Cached,
                            currentAction = uiState.doublePressAction,
                            onActionSelected = viewModel::setDoublePressAction,
                            onConfigure = { viewModel.startGestureConfiguration(uiState.doublePressAction) }
                        )
                    }

                    item {
                        VibrationCard(
                            isEnabled = uiState.vibrationFeedback,
                            onEnabledChanged = viewModel::setVibrationFeedback
                        )
                    }

                    item {
                        SensitivityCard(
                            sensitivity = uiState.screenEdgeSensitivity,
                            onSensitivityChanged = viewModel::setScreenEdgeSensitivity
                        )
                    }

                    if (uiState.lastDetectedGesture != null) {
                        item {
                            LastDetectedCard(
                                gesture = uiState.lastDetectedGesture!!,
                                progress = uiState.gestureDetectionProgress
                            )
                        }
                    }

                    item {
                        StatsPreviewCard(
                            stats = stats,
                            onClick = { showStatsDialog = true }
                        )
                    }

                    item {
                        ResetButton(
                            onReset = { viewModel.resetToDefaults() }
                        )
                    }
                }
            }

            if (uiState.isConfiguring && uiState.configuringAction != null) {
                ConfigurationOverlay(
                    action = uiState.configuringAction!!,
                    onCancel = { viewModel.cancelConfiguration() },
                    onGestureDetected = { type ->
                        scope.launch {
                            viewModel.simulateGesture(type)
                            viewModel.cancelConfiguration()
                        }
                    }
                )
            }

            if (showStatsDialog) {
                StatisticsDialog(
                    stats = stats,
                    onDismiss = { showStatsDialog = false },
                    onReset = { viewModel.resetStats() }
                )
            }
        }
    }
}

@Composable
fun EnableCard(isEnabled: Boolean, onEnableChanged: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isEnabled)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Enable Edge Gestures",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "Use volume buttons for quick actions",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnableChanged,
                thumbContent = {
                    if (isEnabled) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureActionCard(
    title: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    currentAction: EdgeAction,
    onActionSelected: (EdgeAction) -> Unit,
    onConfigure: () -> Unit
) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Icon(
                    icon,
                    contentDescription = title,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(28.dp)
                )
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "Action when $title is pressed",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Spacer(modifier = Modifier.height(8.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ExposedDropdownMenuBox(
                    expanded = expanded,
                    onExpandedChange = { expanded = it }
                ) {
                    OutlinedTextField(
                        value = currentAction.displayName,
                        onValueChange = {},
                        readOnly = true,
                        trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(expanded = expanded) },
                        modifier = Modifier
                            .weight(1f)
                            .menuAnchor(MenuAnchorType.PrimaryNotEditable, enabled = true),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = MaterialTheme.colorScheme.primary
                        )
                    )

                    ExposedDropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                        modifier = Modifier.background(MaterialTheme.colorScheme.surface)
                    ) {
                        EdgeAction.entries.forEach { action ->
                            DropdownMenuItem(
                                text = {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            action.icon,
                                            contentDescription = action.displayName,
                                            modifier = Modifier.size(18.dp),
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(action.displayName)
                                    }
                                },
                                onClick = {
                                    onActionSelected(action)
                                    expanded = false
                                },
                                trailingIcon = {
                                    if (action == currentAction) {
                                        Icon(Icons.Default.Check, contentDescription = "Selected")
                                    }
                                }
                            )
                        }
                    }
                }

                OutlinedButton(
                    onClick = onConfigure,
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(Icons.Default.GraphicEq, contentDescription = "Configure", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Learn")
                }
            }
        }
    }
}

@Composable
fun VibrationCard(isEnabled: Boolean, onEnabledChanged: (Boolean) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "Vibration Feedback",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
                Text(
                    text = "Vibrate when gesture is detected",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Switch(
                checked = isEnabled,
                onCheckedChange = onEnabledChanged
            )
        }
    }
}

@Composable
fun SensitivityCard(sensitivity: Float, onSensitivityChanged: (Float) -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Edge Sensitivity",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Text(
                text = "How sensitive the gesture detection should be",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Less Sensitive", fontSize = 12.sp)
                Slider(
                    value = sensitivity,
                    onValueChange = onSensitivityChanged,
                    valueRange = 0.05f..0.5f,
                    modifier = Modifier.weight(1f).padding(horizontal = 12.dp),
                    colors = SliderDefaults.colors(
                        thumbColor = MaterialTheme.colorScheme.primary,
                        activeTrackColor = MaterialTheme.colorScheme.primary
                    )
                )
                Text("More Sensitive", fontSize = 12.sp)
            }
        }
    }
}

@Composable
fun LastDetectedCard(gesture: String, progress: Float) {
    val infiniteTransition = rememberInfiniteTransition(label = "LastDetectedAnimation")
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "ScaleAnimation"
    )

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(scale),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF4CAF50).copy(alpha = 0.1f)
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
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
                    Text(
                        text = "Gesture Detected!",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF4CAF50)
                    )
                    Text(
                        text = gesture.replace("_", " "),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
            LinearProgressIndicator(
                progress = { progress },
                modifier = Modifier
                    .width(60.dp)
                    .height(4.dp)
                    .clip(RoundedCornerShape(2.dp)),
                color = Color(0xFF4CAF50),
                trackColor = Color(0xFF4CAF50).copy(alpha = 0.3f)
            )
        }
    }
}

@Composable
fun StatsPreviewCard(stats: EdgeGesturesStats, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Assessment, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Statistics",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
            }
            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total", stats.totalDetections.toString())
                StatItem("Success", stats.successfulExecutions.toString())
                StatItem("Failed", stats.failedExecutions.toString())
            }
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Volume ↑", stats.volumeUpCount.toString())
                StatItem("Volume ↓", stats.volumeDownCount.toString())
                StatItem("Long Press", stats.longPressCount.toString())
                StatItem("Double", stats.doublePressCount.toString())
            }
        }
    }
}

@Composable
fun StatItem(label: String, value: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun ResetButton(onReset: () -> Unit) {
    Button(
        onClick = onReset,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Icon(Icons.Default.Restore, contentDescription = "Reset")
        Spacer(modifier = Modifier.width(8.dp))
        Text("Reset to Defaults")
    }
}

@Composable
fun ConfigurationOverlay(
    action: EdgeAction,
    onCancel: () -> Unit,
    onGestureDetected: (GestureType) -> Unit
) {
    var timeLeft by remember { mutableIntStateOf(5) }

    LaunchedEffect(Unit) {
        while (timeLeft > 0) {
            delay(1000)
            timeLeft--
        }
        onCancel()
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = 0.7f))
            .clickable(enabled = true, onClick = {})
    ) {
        Card(
            modifier = Modifier
                .align(Alignment.Center)
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(
                containerColor = MaterialTheme.colorScheme.surface
            ),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = "Configure ${action.displayName}",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Press the ${action.displayName.lowercase()} gesture now",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(modifier = Modifier.height(24.dp))

                Box(
                    modifier = Modifier
                        .size(100.dp)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                    Color.Transparent
                                )
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mic,
                        contentDescription = "Listening",
                        modifier = Modifier.size(48.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = "Listening... ${timeLeft}s",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        onClick = { onGestureDetected(GestureType.VOLUME_UP) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Volume ↑")
                    }
                    OutlinedButton(
                        onClick = { onGestureDetected(GestureType.LONG_PRESS) },
                        modifier = Modifier.weight(1f)
                    ) {
                        Text("Long Press")
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))

                TextButton(onClick = onCancel) {
                    Text("Cancel")
                }
            }
        }
    }
}

@Composable
fun StatisticsDialog(
    stats: EdgeGesturesStats,
    onDismiss: () -> Unit,
    onReset: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Edge Gestures Statistics") },
        text = {
            Column {
                StatisticsRow("Total Detections", stats.totalDetections.toString())
                HorizontalDivider()
                StatisticsRow("Volume Up", stats.volumeUpCount.toString())
                HorizontalDivider()
                StatisticsRow("Volume Down", stats.volumeDownCount.toString())
                HorizontalDivider()
                StatisticsRow("Long Press", stats.longPressCount.toString())
                HorizontalDivider()
                StatisticsRow("Double Press", stats.doublePressCount.toString())
                HorizontalDivider()
                StatisticsRow("Successful Executions", stats.successfulExecutions.toString())
                HorizontalDivider()
                StatisticsRow("Failed Executions", stats.failedExecutions.toString())
                HorizontalDivider()

                val successRate = if (stats.successfulExecutions + stats.failedExecutions > 0) {
                    (stats.successfulExecutions * 100f / (stats.successfulExecutions + stats.failedExecutions))
                } else 0f

                StatisticsRow("Success Rate", String.format(java.util.Locale.US, "%.1f%%", successRate))

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(
                    onClick = onReset,
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Reset Statistics")
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}

@Composable
fun StatisticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
    }
}



@Suppress("unused")
@Composable
fun RadarAnimation(isActive: Boolean, size: Int) {
    val transition = rememberInfiniteTransition(label = "Radar")
    val radiusRatio by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Radius"
    )
    val opacity by transition.animateFloat(
        initialValue = 0.5f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Opacity"
    )

    Box(
        modifier = Modifier.size(size.dp),
        contentAlignment = Alignment.Center
    ) {
        if (isActive) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .scale(radiusRatio)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = opacity))
            )
        }
        Icon(
            Icons.Default.Radar,
            contentDescription = null,
            tint = if (isActive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Suppress("unused")
@Composable
fun GestureWaveform(
    dataPoints: List<Float>,
    color: Color,
    animated: Boolean
) {
    val strokeWidth = 3.dp
    Canvas(
        modifier = Modifier
            .fillMaxWidth()
            .height(100.dp)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f), RoundedCornerShape(8.dp))
    ) {
        if (dataPoints.isEmpty()) return@Canvas
        val waveformColor = if (animated) color else color.copy(alpha = 0.7f)
        val path = Path()
        val widthStep = size.width / (dataPoints.size - 1).coerceAtLeast(1)
        val midY = size.height / 2

        dataPoints.forEachIndexed { index, value ->
            val x = index * widthStep
            val y = midY + (value * midY)
            if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = waveformColor,
            style = Stroke(width = strokeWidth.toPx())
        )
    }
}

@Suppress("unused")
@Composable
fun AnimatedSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    label: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Suppress("unused")
@Composable
fun NotificationBadge(count: Int) {
    if (count > 0) {
        Badge(
            containerColor = MaterialTheme.colorScheme.error,
            contentColor = MaterialTheme.colorScheme.onError
        ) {
            Text("$count Conflicts")
        }
    }
}

@Suppress("unused")
@Composable
fun SlideUpPanel(
    isVisible: Boolean,
    onDismiss: () -> Unit,
    content: @Composable () -> Unit
) {
    AnimatedVisibility(
        visible = isVisible,
        enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
        exit = slideOutVertically(targetOffsetY = { it }) + fadeOut()
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(topStart = 24.dp, topEnd = 24.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Box(
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .size(width = 40.dp, height = 4.dp)
                        .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.4f), CircleShape)
                        .clickable { onDismiss() }
                )
                Spacer(modifier = Modifier.height(16.dp))
                content()
            }
        }
    }
}
