package com.airmouse.presentation.ui.voice

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

// Data Classes
data class VoiceCommand(
    val keyword: String,
    val action: String,
    val description: String,
    val icon: String = "🎤"
)

data class CustomVoiceCommand(
    val id: String,
    val phrase: String,
    val action: String,
    val enabled: Boolean = true
)

data class VoiceCommandHistory(
    val timestamp: Long,
    val command: String,
    val confidence: Float,
    val success: Boolean
)

data class VoiceCommandsUiState(
    val isListening: Boolean = false,
    val isProcessing: Boolean = false,
    val lastCommand: String? = null,
    val lastConfidence: Float = 0f,
    val status: String = "Ready",
    val statusColor: Long = 0xFF4CAF50,
    val availableCommands: List<VoiceCommand> = emptyList(),
    val commandHistory: List<VoiceCommandHistory> = emptyList(),
    val microphonePermissionGranted: Boolean = false,
    val wakeWordEnabled: Boolean = false,
    val wakeWord: String = "Hey Air Mouse",
    val sensitivity: Float = 0.5f,
    val language: String = "en-US",
    val continuousListening: Boolean = false,
    val voiceFeedback: Boolean = true,
    val soundEffects: Boolean = true,
    val customCommands: List<CustomVoiceCommand> = emptyList()
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandsScreen(
    navigationActions: NavigationActions,
    viewModel: VoiceCommandsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    var isAnimating by remember { mutableStateOf(false) }

    // Check microphone permission
    val hasPermission = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED
    } else true

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Commands") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
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
            // Microphone Permission Warning
            if (!hasPermission && !uiState.microphonePermissionGranted) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.errorContainer
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(16.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "Microphone permission required",
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                            Button(
                                onClick = { viewModel.requestMicrophonePermission() },
                                colors = ButtonDefaults.buttonColors(
                                    containerColor = MaterialTheme.colorScheme.error
                                )
                            ) {
                                Text("Grant Permission")
                            }
                        }
                    }
                }
            }

            // Voice Control Card
            item {
                VoiceControlCard(uiState, viewModel, isAnimating)
            }

            // Status Card
            item {
                StatusCard(uiState)
            }

            // Available Commands Section
            item {
                Text(
                    text = "Available Commands",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, top = 8.dp)
                )
            }

            items(uiState.availableCommands) { command ->
                CommandCard(command)
            }

            // Custom Commands Section
            if (uiState.customCommands.isNotEmpty()) {
                item {
                    Text(
                        text = "Custom Commands",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, top = 8.dp)
                    )
                }

                items(uiState.customCommands) { customCommand ->
                    CustomCommandCard(
                        command = customCommand,
                        onToggle = { viewModel.updateCustomCommand(customCommand.id, it) },
                        onDelete = { viewModel.removeCustomCommand(customCommand.id) }
                    )
                }
            }

            // Add Custom Command Button
            item {
                AddCustomCommandButton(viewModel)
            }

            // Settings Section
            item {
                SettingsSection(viewModel, uiState)
            }

            // Command History
            if (uiState.commandHistory.isNotEmpty()) {
                item {
                    Text(
                        text = "Command History",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 8.dp, top = 8.dp)
                    )
                }

                items(uiState.commandHistory.take(10)) { history ->
                    HistoryCard(history)
                }
            }
        }
    }
}

@Composable
fun VoiceControlCard(
    uiState: VoiceCommandsUiState,
    viewModel: VoiceCommandsViewModel,
    isAnimating: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (uiState.isListening)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Animated microphone icon
            Box(
                modifier = Modifier
                    .size(100.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isListening)
                            Color(0xFFF44336).copy(alpha = 0.2f)
                        else
                            Color(0xFF4CAF50).copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Mic,
                    contentDescription = "Microphone",
                    modifier = Modifier.size(48.dp),
                    tint = if (uiState.isListening) Color(0xFFF44336) else Color(0xFF4CAF50)
                )
                if (uiState.isListening) {
                    // Pulsing animation effect
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336).copy(alpha = 0.1f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            Button(
                onClick = {
                    if (uiState.isListening) viewModel.stopListening()
                    else viewModel.startListening()
                },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isListening)
                        MaterialTheme.colorScheme.error
                    else
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (uiState.isListening) Icons.Default.Stop else Icons.Default.Mic,
                    contentDescription = if (uiState.isListening) "Stop" else "Start"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isListening) "Stop Listening" else "Start Listening")
            }

            if (uiState.wakeWordEnabled) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Wake word: \"${uiState.wakeWord}\"",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
fun StatusCard(uiState: VoiceCommandsUiState) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(uiState.statusColor).copy(alpha = 0.1f)
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(10.dp)
                        .clip(CircleShape)
                        .background(Color(uiState.statusColor))
                )
                Text(
                    text = uiState.status,
                    fontWeight = FontWeight.Medium,
                    color = Color(uiState.statusColor)
                )
            }

            if (uiState.lastCommand != null) {
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text("Last command:", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(
                        uiState.lastCommand!!,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${(uiState.lastConfidence * 100).toInt()}%", fontSize = 12.sp)
                }
            }
        }
    }
}

@Composable
fun CommandCard(command: VoiceCommand) {
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
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(command.icon, fontSize = 28.sp)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.keyword.replaceFirstChar { it.uppercase() },
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = command.description,
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Text(
                text = command.action,
                fontSize = 11.sp,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
fun CustomCommandCard(
    command: CustomVoiceCommand,
    onToggle: (Boolean) -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (command.enabled)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = command.phrase,
                    fontWeight = FontWeight.Bold,
                    color = if (command.enabled) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = command.action,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Switch(
                    checked = command.enabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", tint = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun AddCustomCommandButton(viewModel: VoiceCommandsViewModel) {
    var showDialog by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { showDialog = true },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, contentDescription = "Add")
            Spacer(modifier = Modifier.width(8.dp))
            Text("Add Custom Command")
        }
    }

    if (showDialog) {
        var phrase by remember { mutableStateOf("") }
        var action by remember { mutableStateOf("") }

        AlertDialog(
            onDismissRequest = { showDialog = false },
            title = { Text("Add Custom Command") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = phrase,
                        onValueChange = { phrase = it },
                        label = { Text("Phrase to recognize") },
                        placeholder = { Text("e.g., volume up") },
                        singleLine = true
                    )
                    OutlinedTextField(
                        value = action,
                        onValueChange = { action = it },
                        label = { Text("Action") },
                        placeholder = { Text("e.g., scroll:5 or click:left") },
                        singleLine = true
                    )
                    Text(
                        text = "Action examples: click:left, scroll:5, move:100:50, rightclick",
                        fontSize = 10.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (phrase.isNotBlank() && action.isNotBlank()) {
                            viewModel.addCustomCommand(phrase, action)
                            showDialog = false
                        }
                    }
                ) {
                    Text("Add")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

@Composable
fun SettingsSection(viewModel: VoiceCommandsViewModel, uiState: VoiceCommandsUiState) {
    var expanded by remember { mutableStateOf(false) }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { expanded = !expanded }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Voice Settings", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Icon(
                    if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = if (expanded) "Collapse" else "Expand"
                )
            }

            if (expanded) {
                Divider()
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    SwitchSetting(
                        label = "Wake Word Detection",
                        description = "Listen for \"${uiState.wakeWord}\" to activate",
                        checked = uiState.wakeWordEnabled,
                        onCheckedChange = viewModel::updateWakeWordEnabled
                    )

                    if (uiState.wakeWordEnabled) {
                        OutlinedTextField(
                            value = uiState.wakeWord,
                            onValueChange = viewModel::updateWakeWord,
                            label = { Text("Wake Word") },
                            modifier = Modifier.fillMaxWidth()
                        )
                    }

                    SwitchSetting(
                        label = "Continuous Listening",
                        description = "Keep listening after each command",
                        checked = uiState.continuousListening,
                        onCheckedChange = viewModel::updateContinuousListening
                    )

                    SwitchSetting(
                        label = "Voice Feedback",
                        description = "Voice confirmation of commands",
                        checked = uiState.voiceFeedback,
                        onCheckedChange = viewModel::updateVoiceFeedback
                    )

                    SwitchSetting(
                        label = "Sound Effects",
                        description = "Play sounds on voice events",
                        checked = uiState.soundEffects,
                        onCheckedChange = viewModel::updateSoundEffects
                    )

                    SliderSetting(
                        label = "Sensitivity",
                        value = uiState.sensitivity,
                        onValueChange = viewModel::updateSensitivity,
                        valueRange = 0.1f..1.0f,
                        formatValue = { String.format("%.1f", it) }
                    )
                }
            }
        }
    }
}

@Composable
fun HistoryCard(history: VoiceCommandHistory) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (history.success)
                MaterialTheme.colorScheme.surfaceVariant
            else
                MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.5f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = history.command,
                    fontWeight = FontWeight.Bold,
                    color = if (history.success) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.error
                )
                Text(
                    text = java.text.SimpleDateFormat("HH:mm:ss", java.util.Locale.getDefault())
                        .format(java.util.Date(history.timestamp)),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${(history.confidence * 100).toInt()}%",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (history.success) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
                Text(
                    text = if (history.success) "Executed" else "Failed",
                    fontSize = 10.sp
                )
            }
        }
    }
}

@Composable
fun SwitchSetting(
    label: String,
    description: String = "",
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            if (description.isNotEmpty()) {
                Text(description, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
fun SliderSetting(
    label: String,
    value: Float,
    onValueChange: (Float) -> Unit,
    valueRange: ClosedFloatingPointRange<Float>,
    formatValue: (Float) -> String
) {
    Column {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(label, style = MaterialTheme.typography.bodyLarge)
            Text(formatValue(value), style = MaterialTheme.typography.bodyMedium, fontWeight = FontWeight.Bold)
        }
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = valueRange,
            modifier = Modifier.fillMaxWidth()
        )
    }
}