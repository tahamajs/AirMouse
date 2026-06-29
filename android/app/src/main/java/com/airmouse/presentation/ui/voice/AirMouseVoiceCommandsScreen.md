# 📘 Air Mouse Voice Commands Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.voice` package contains the **Voice Commands screen** for the Air Mouse application. This screen provides a comprehensive interface for voice command control, allowing users to control the cursor, execute actions, and create custom voice commands.

```
com.airmouse.presentation.ui.voice/
├── VoiceCommandsScreen.kt          # Main voice commands UI
├── VoiceCommandsViewModel.kt       # Voice commands ViewModel
├── VoiceCommandsUiState.kt         # Voice commands state models
├── VoiceCommandsComponents.kt      # Reusable voice UI components
└── VoiceCommandsConstants.kt       # Voice commands constants
```

---

## 🎯 1. VoiceCommandsScreen

### Purpose
Provides a **comprehensive voice command interface** for controlling the Air Mouse using speech recognition, custom commands, and voice settings.

### Key Features

| Feature | Description |
|---------|-------------|
| **Start/Stop Listening** | Toggle voice recognition |
| **Wake Word Detection** | "Hey Air Mouse" wake word |
| **Built-in Commands** | Click, scroll, volume, media control |
| **Custom Commands** | Create, edit, delete custom voice commands |
| **Command History** | Recent command executions with confidence |
| **Settings** | Sensitivity, continuous listening, voice feedback |
| **Voice Feedback** | Text-to-speech responses |
| **Sound Effects** | Beep on start/stop |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VoiceCommandsScreen(
    navigationActions: NavigationActions,
    viewModel: VoiceCommandsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // Check microphone permission
    val hasPermission = ContextCompat.checkSelfPermission(
        context,
        Manifest.permission.RECORD_AUDIO
    ) == PackageManager.PERMISSION_GRANTED

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Voice Commands") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
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
            // Permission Warning
            if (!hasPermission && !uiState.microphonePermissionGranted) {
                item {
                    PermissionWarningCard(viewModel)
                }
            }

            // Voice Control Card
            item {
                VoiceControlCard(uiState, viewModel)
            }

            // Status Card
            item {
                StatusCard(uiState)
            }

            // Available Commands
            item {
                SectionHeader("Available Commands")
            }
            items(uiState.availableCommands) { command ->
                CommandCard(command)
            }

            // Custom Commands
            if (uiState.customCommands.isNotEmpty()) {
                item {
                    SectionHeader("Custom Commands")
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
                    SectionHeader("Command History")
                }
                items(uiState.commandHistory.take(10)) { history ->
                    HistoryCard(history)
                }
            }
        }
    }
}
```

---

## 🎯 2. VoiceCommandsUiState

### Purpose
Defines the **complete state model** for the voice commands screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `microphonePermissionGranted` | Boolean | Whether microphone permission is granted |
| `isListening` | Boolean | Whether voice recognition is active |
| `isProcessing` | Boolean | Whether voice is being processed |
| `wakeWordEnabled` | Boolean | Whether wake word detection is enabled |
| `wakeWord` | String | Custom wake word ("Hey Air Mouse") |
| `sensitivity` | Float | Voice recognition sensitivity (0.1-1.0) |
| `language` | String | Language for voice recognition (en-US) |
| `continuousListening` | Boolean | Whether to keep listening after commands |
| `voiceFeedback` | Boolean | Whether to provide voice confirmation |
| `soundEffects` | Boolean | Whether to play sound effects |
| `status` | String | Current status message |
| `statusColor` | Int | Status color code |
| `lastCommand` | String? | Last recognized command |
| `lastConfidence` | Float | Confidence of last command |
| `availableCommands` | List<VoiceCommand> | Built-in commands |
| `customCommands` | List<CustomVoiceCommand> | User-created commands |
| `commandHistory` | List<VoiceCommandHistory> | Command execution history |

### Enums and Data Classes

```kotlin
data class VoiceCommand(
    val keyword: String,
    val action: String,
    val description: String,
    val icon: String
)

data class CustomVoiceCommand(
    val id: String,
    val phrase: String,
    val action: String,
    val enabled: Boolean
)

data class VoiceCommandHistory(
    val id: String = UUID.randomUUID().toString(),
    val command: String,
    val timestamp: Long,
    val confidence: Float,
    val success: Boolean
)
```

---

## 🧩 3. UI Components

### VoiceControlCard

```kotlin
@Composable
fun VoiceControlCard(
    uiState: VoiceCommandsUiState,
    viewModel: VoiceCommandsViewModel
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
            // Microphone Icon with Animation
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
                    Box(
                        modifier = Modifier
                            .size(80.dp)
                            .clip(CircleShape)
                            .background(Color(0xFFF44336).copy(alpha = 0.1f))
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Start/Stop Button
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

            // Wake Word Indicator
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
```

### StatusCard

```kotlin
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
                        uiState.lastCommand,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                    Text("${(uiState.lastConfidence * 100).toInt()}%", fontSize = 12.sp)
                }
            }
        }
    }
}
```

### CommandCard

```kotlin
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
                    text = command.keyword.replaceFirstChar { 
                        if (it.isLowerCase()) it.titlecase(Locale.getDefault()) else it.toString() 
                    },
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
```

### CustomCommandCard

```kotlin
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
                    color = if (command.enabled) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = command.action,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Switch(
                    checked = command.enabled,
                    onCheckedChange = onToggle
                )
                IconButton(onClick = onDelete) {
                    Icon(
                        Icons.Default.Delete,
                        contentDescription = "Delete",
                        tint = MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}
```

### AddCustomCommandButton

```kotlin
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
                        text = "Action examples: click:left, scroll:5, move:100:50, right-click",
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
```

### SettingsSection

```kotlin
@Composable
fun SettingsSection(
    viewModel: VoiceCommandsViewModel,
    uiState: VoiceCommandsUiState
) {
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
                HorizontalDivider()
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
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
                        formatValue = { String.format(Locale.getDefault(), "%.1f", it) }
                    )
                }
            }
        }
    }
}
```

### HistoryCard

```kotlin
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
                    color = if (history.success) 
                        MaterialTheme.colorScheme.onSurface 
                    else 
                        MaterialTheme.colorScheme.error
                )
                Text(
                    text = SimpleDateFormat("HH:mm:ss", Locale.getDefault())
                        .format(Date(history.timestamp)),
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
```

### PermissionWarningCard

```kotlin
@Composable
fun PermissionWarningCard(viewModel: VoiceCommandsViewModel) {
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
```

### SwitchSetting

```kotlin
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
```

### SliderSetting

```kotlin
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
```

---

## 🎯 4. VoiceCommandsViewModel

### Key Methods

| Method | Purpose |
|--------|---------|
| `startListening()` | Start voice recognition |
| `stopListening()` | Stop voice recognition |
| `processVoiceInput(text, confidence)` | Process recognized speech |
| `executeCommand(command)` | Execute a voice command |
| `executeCustomAction(action)` | Execute a custom action |
| `addCustomCommand(phrase, action)` | Add custom command |
| `removeCustomCommand(id)` | Remove custom command |
| `updateCustomCommand(id, enabled)` | Toggle custom command |
| `clearHistory()` | Clear command history |
| `updateWakeWord(word)` | Update wake word |
| `updateSensitivity(value)` | Update sensitivity |
| `toggleWakeWord()` | Toggle wake word detection |
| `speakResponse(text)` | Text-to-speech response |

### Built-in Commands

```kotlin
private val availableCommandsList = listOf(
    VoiceCommand("click", "CLICK", "Perform left click", "👆"),
    VoiceCommand("double click", "DOUBLE_CLICK", "Perform double click", "👆👆"),
    VoiceCommand("right click", "RIGHT_CLICK", "Perform right click", "👉"),
    VoiceCommand("scroll up", "SCROLL_UP", "Scroll up", "⬆️"),
    VoiceCommand("scroll down", "SCROLL_DOWN", "Scroll down", "⬇️"),
    VoiceCommand("left click", "CLICK", "Perform left click", "👈"),
    VoiceCommand("select", "CLICK", "Select item", "✅"),
    VoiceCommand("back", "BACK", "Go back", "🔙"),
    VoiceCommand("home", "HOME", "Go to desktop", "🏠"),
    VoiceCommand("calibrate", "CALIBRATE", "Start calibration", "🔧"),
    VoiceCommand("connect", "CONNECT", "Connect to server", "🔌"),
    VoiceCommand("disconnect", "DISCONNECT", "Disconnect from server", "🔌❌"),
    VoiceCommand("stop", "STOP", "Stop Air Mouse", "⏹️"),
    VoiceCommand("start", "START", "Start Air Mouse", "▶️")
)
```

### Custom Action Parsing

```kotlin
private fun executeCustomAction(action: String) {
    viewModelScope.launch {
        when {
            action.startsWith("move:") -> {
                val parts = action.split(":")
                if (parts.size >= 3) {
                    val x = parts[1].toFloatOrNull() ?: 0f
                    val y = parts[2].toFloatOrNull() ?: 0f
                    connectionManager.sendMove(x, y)
                }
            }
            action.startsWith("scroll:") -> {
                val delta = action.substringAfter(":").toIntOrNull() ?: 1
                connectionManager.sendScroll(delta)
            }
            action.startsWith("click:") -> {
                val button = action.substringAfter(":")
                connectionManager.sendClick(button)
            }
            else -> {
                connectionManager.sendControl(action)
            }
        }
    }
    vibrate(30)
}
```

---

## 📊 Voice Commands Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    VOICE COMMANDS FLOW                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User says wake word or taps "Start Listening"                      │
│         │                                                               │
│         ▼                                                               │
│  2. Microphone captures audio                                          │
│         │                                                               │
│         ▼                                                               │
│  3. Speech recognition (Google Speech API)                             │
│         │                                                               │
│         ▼                                                               │
│  4. Text is processed                                                 │
│         │                                                               │
│         ▼                                                               │
│  5. Matches against:                                                   │
│     ├── Built-in commands (click, scroll, volume, media)              │
│     ├── Custom commands (user-defined)                                │
│     └── Wake word (if enabled)                                        │
│                                                                         │
│  6. Command is executed:                                               │
│     ├── ConnectionManager.sendClick()                                 │
│     ├── ConnectionManager.sendScroll()                                │
│     ├── ConnectionManager.sendControl()                               │
│     └── ConnectionManager.sendMove()                                  │
│                                                                         │
│  7. Feedback:                                                          │
│     ├── Voice confirmation (if enabled)                               │
│     ├── Sound effects (if enabled)                                    │
│     ├── Haptic feedback                                               │
│     └── UI updates                                                    │
│                                                                         │
│  8. Command logged to history                                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Command Actions Reference

| Voice Command | Action | Server Command |
|---------------|--------|----------------|
| "click" | Left Click | `sendClick("left")` |
| "double click" | Double Click | `sendDoubleClick()` |
| "right click" | Right Click | `sendRightClick()` |
| "scroll up" | Scroll Up | `sendScroll(3)` |
| "scroll down" | Scroll Down | `sendScroll(-3)` |
| "volume up" | Volume Up | `sendControl("volume_up")` |
| "volume down" | Volume Down | `sendControl("volume_down")` |
| "mute" | Mute | `sendControl("mute")` |
| "play" | Play/Pause | `sendControl("play_pause")` |
| "pause" | Play/Pause | `sendControl("play_pause")` |
| "next" | Next Track | `sendControl("next_track")` |
| "previous" | Previous Track | `sendControl("prev_track")` |
| "stop" | Stop | `sendControl("stop")` |
| "connect" | Connect | `connectionManager.connect()` |
| "disconnect" | Disconnect | `connectionManager.disconnect()` |
| "calibrate" | Calibrate | `sendControl("calibrate")` |
| "back" | Back | `sendControl("back")` |
| "home" | Home | `sendControl("home")` |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Hands-Free Control** | Voice commands for cursor and media control |
| **Wake Word** | "Hey Air Mouse" activation |
| **Custom Commands** | User-defined voice commands |
| **Voice Feedback** | Text-to-speech confirmation |
| **Sound Effects** | Beep on start/stop |
| **Command History** | Recent commands with confidence |
| **Sensitivity Control** | Adjustable recognition sensitivity |
| **Reactive UI** | StateFlow with automatic updates |

---

**The Voice Commands Screen provides comprehensive voice control functionality, allowing users to control the Air Mouse hands-free with built-in and custom voice commands.**