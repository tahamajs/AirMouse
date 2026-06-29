# 📘 Air Mouse Gesture Studio Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.gesture` package contains the **Gesture Studio screen** for the Air Mouse application. This screen provides a comprehensive interface for creating, training, testing, and managing custom gestures.

```
com.airmouse.presentation.ui.gesture/
├── GestureStudioScreen.kt          # Main gesture studio UI
├── GestureStudioViewModel.kt       # Gesture studio ViewModel
├── GestureStudioUiState.kt         # Gesture studio state models
├── GestureTraining.kt              # Training components
├── GestureVisualizer.kt            # Gesture visualization
└── GestureTemplates.kt             # Template management components
```

**Note:** Based on the provided files, the Gesture Studio screen appears to be a **stub/placeholder** implementation. This document provides a complete, production-ready implementation description.

---

## 🎯 1. GestureStudioScreen – Complete Documentation

### Purpose
Provides a **comprehensive interface** for creating, training, testing, and managing custom gestures. Users can record new gestures, train the recognition system, test gesture detection, and manage gesture templates.

### Key Features

| Feature | Description |
|---------|-------------|
| **Gesture Recording** | Record new gestures using device sensors |
| **Gesture Training** | Train the system with multiple samples |
| **Gesture Testing** | Test gesture recognition in real-time |
| **Template Management** | Create, edit, delete, and favorite gestures |
| **Gesture Visualization** | Visual feedback of gesture data |
| **Performance Metrics** | Confidence scores, success rates |
| **Export/Import** | Share gesture templates |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GestureStudioScreen(
    navigationActions: NavigationActions,
    viewModel: GestureStudioViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    var selectedTab by remember { mutableStateOf(GestureStudioTab.TRAINING) }

    Scaffold(
        topBar = { /* TopAppBar with title, actions */ }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Tab Row
            ScrollableTabRow(
                selectedTabIndex = selectedTab.ordinal,
                containerColor = Color.Transparent,
                edgePadding = 16.dp
            ) {
                GestureStudioTab.entries.forEach { tab ->
                    LeadingIconTab(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        text = { Text(tab.title) },
                        icon = { Icon(tab.icon, contentDescription = tab.title) }
                    )
                }
            }

            // Content
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                when (selectedTab) {
                    GestureStudioTab.TRAINING -> TrainingTab(uiState, viewModel)
                    GestureStudioTab.TESTING -> TestingTab(uiState, viewModel)
                    GestureStudioTab.TEMPLATES -> TemplatesTab(uiState, viewModel)
                    GestureStudioTab.STATS -> StatsTab(uiState, viewModel)
                }
            }
        }
    }
}
```

---

## 🎯 2. GestureStudioUiState

### Purpose
Defines the **complete state model** for the gesture studio screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `templates` | `List<CustomGestureTemplate>` | All gesture templates |
| `selectedTemplate` | `CustomGestureTemplate?` | Currently selected template |
| `filteredTemplates` | `List<CustomGestureTemplate>` | Filtered templates |
| `searchQuery` | `String` | Search query |
| `selectedFilter` | `GestureFilter` | Current filter |
| `showAddDialog` | `Boolean` | Show add gesture dialog |
| `showEditDialog` | `Boolean` | Show edit gesture dialog |
| `showDeleteDialog` | `Boolean` | Show delete confirmation dialog |
| `isRecording` | `Boolean` | Whether recording a gesture |
| `isTraining` | `Boolean` | Whether training is in progress |
| `isTesting` | `Boolean` | Whether testing is in progress |
| `trainingProgress` | `Int` | Training progress percentage |
| `trainingStatus` | `String` | Training status message |
| `trainingSamples` | `Int` | Number of training samples collected |
| `minTrainingSamples` | `Int` | Minimum samples needed for training |
| `testResult` | `GestureDetectionResult?` | Current test result |
| `newGestureName` | `String` | Name for new gesture |
| `newGestureAction` | `String` | Action for new gesture |
| `detectedGesture` | `String` | Last detected gesture name |
| `detectedConfidence` | `Float` | Confidence of last detection |
| `gestureStats` | `GestureTrainingStats` | Training statistics |
| `isLoading` | `Boolean` | Whether loading data |
| `errorMessage` | `String?` | Error message if any |
| `successMessage` | `String?` | Success message if any |

### Enums

```kotlin
enum class GestureStudioTab(
    val title: String,
    val icon: ImageVector
) {
    TRAINING("Training", Icons.Default.School),
    TESTING("Testing", Icons.Default.Science),
    TEMPLATES("Templates", Icons.Default.GridView),
    STATS("Statistics", Icons.Default.BarChart)
}

enum class GestureFilter(
    val displayName: String
) {
    ALL("All"),
    CUSTOM("Custom"),
    SYSTEM("System"),
    FAVORITES("Favorites"),
    ENABLED("Enabled"),
    DISABLED("Disabled")
}

enum class TrainingStatus(
    val displayName: String,
    val color: Color
) {
    IDLE("Ready", Color(0xFF64748B)),
    RECORDING("Recording...", Color(0xFFEF4444)),
    PROCESSING("Processing...", Color(0xFFF59E0B)),
    TRAINING("Training...", Color(0xFF6366F1)),
    COMPLETE("Complete!", Color(0xFF10B981)),
    ERROR("Error", Color(0xFFEF4444))
}
```

---

## 🧩 3. UI Components

### Training Tab

```kotlin
@Composable
fun TrainingTab(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    // Training status card
    TrainingStatusCard(uiState, viewModel)

    // Sample collection
    TrainingSamplesCard(uiState, viewModel)

    // Action buttons
    TrainingActionsCard(uiState, viewModel)

    // Live sensor preview
    SensorPreviewCard()
}

@Composable
fun TrainingStatusCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(12.dp)
                        .clip(CircleShape)
                        .background(
                            when (uiState.trainingStatus) {
                                TrainingStatus.RECORDING -> Color(0xFFEF4444)
                                TrainingStatus.TRAINING -> Color(0xFF6366F1)
                                TrainingStatus.COMPLETE -> Color(0xFF10B981)
                                TrainingStatus.ERROR -> Color(0xFFEF4444)
                                else -> Color(0xFF64748B)
                            }
                        )
                )
                Text(
                    text = uiState.trainingStatus.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }

            Text(
                text = "Samples collected: ${uiState.trainingSamples} / ${uiState.minTrainingSamples} minimum",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            if (uiState.isTraining) {
                LinearProgressIndicator(
                    progress = uiState.trainingProgress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
fun TrainingSamplesCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📝 Training Samples",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            OutlinedTextField(
                value = uiState.newGestureName,
                onValueChange = { viewModel.updateNewGestureName(it) },
                label = { Text("Gesture Name") },
                placeholder = { Text("e.g., "Circle", "Wave"") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRecording && !uiState.isTraining
            )

            OutlinedTextField(
                value = uiState.newGestureAction,
                onValueChange = { viewModel.updateNewGestureAction(it) },
                label = { Text("Action") },
                placeholder = { Text("e.g., "volume_up", "play_pause"") },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isRecording && !uiState.isTraining
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Button(
                    onClick = { viewModel.startRecording() },
                    modifier = Modifier.weight(1f),
                    enabled = !uiState.isTraining && 
                             uiState.newGestureName.isNotBlank() &&
                             !uiState.isRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (uiState.isRecording) 
                            MaterialTheme.colorScheme.error 
                        else 
                            MaterialTheme.colorScheme.primary
                    )
                ) {
                    Icon(
                        if (uiState.isRecording) Icons.Default.Stop else Icons.Default.FiberManualRecord,
                        contentDescription = if (uiState.isRecording) "Stop" else "Record"
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(if (uiState.isRecording) "Stop Recording" else "Start Recording")
                }

                Button(
                    onClick = { viewModel.trainGesture() },
                    modifier = Modifier.weight(1f),
                    enabled = uiState.trainingSamples >= uiState.minTrainingSamples && !uiState.isRecording,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = MaterialTheme.colorScheme.secondary
                    )
                ) {
                    Icon(Icons.Default.School, contentDescription = "Train")
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Train")
                }
            }
        }
    }
}

@Composable
fun TrainingActionsCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
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
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            OutlinedButton(
                onClick = { viewModel.clearSamples() },
                modifier = Modifier.weight(1f),
                enabled = uiState.trainingSamples > 0
            ) {
                Icon(Icons.Default.Delete, contentDescription = "Clear")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Clear Samples")
            }

            OutlinedButton(
                onClick = { viewModel.saveTemplate() },
                modifier = Modifier.weight(1f),
                enabled = uiState.trainingStatus == TrainingStatus.COMPLETE
            ) {
                Icon(Icons.Default.Save, contentDescription = "Save")
                Spacer(modifier = Modifier.width(4.dp))
                Text("Save Template")
            }
        }
    }
}
```

### Testing Tab

```kotlin
@Composable
fun TestingTab(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    // Live detection card
    LiveDetectionCard(uiState, viewModel)

    // Test controls
    TestControlsCard(uiState, viewModel)

    // Detection history
    DetectionHistoryCard(uiState, viewModel)
}

@Composable
fun LiveDetectionCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "🎯 Live Detection",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onPrimaryContainer
            )

            Box(
                modifier = Modifier
                    .size(120.dp)
                    .clip(CircleShape)
                    .background(
                        if (uiState.isTesting) 
                            Color(0xFF6366F1).copy(alpha = 0.2f) 
                        else 
                            MaterialTheme.colorScheme.surface
                    ),
                contentAlignment = Alignment.Center
            ) {
                if (uiState.isTesting) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = uiState.detectedGesture.ifEmpty { "Listening..." },
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        if (uiState.detectedConfidence > 0) {
                            Text(
                                text = "${(uiState.detectedConfidence * 100).toInt()}% confidence",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                            )
                        }
                    }
                } else {
                    Text(
                        text = "Start Testing",
                        fontSize = 14.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Button(
                onClick = { viewModel.toggleTesting() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (uiState.isTesting) 
                        MaterialTheme.colorScheme.error 
                    else 
                        MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(
                    if (uiState.isTesting) Icons.Default.Stop else Icons.Default.PlayArrow,
                    contentDescription = if (uiState.isTesting) "Stop" else "Start"
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(if (uiState.isTesting) "Stop Testing" else "Start Testing")
            }
        }
    }
}

@Composable
fun TestControlsCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⚙️ Test Controls",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.testGesture("click") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Click")
                }
                OutlinedButton(
                    onClick = { viewModel.testGesture("swipe") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Swipe")
                }
                OutlinedButton(
                    onClick = { viewModel.testGesture("circle") },
                    modifier = Modifier.weight(1f)
                ) {
                    Text("Test Circle")
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = { viewModel.runBatchTest() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Batch Test")
                }
                OutlinedButton(
                    onClick = { viewModel.exportTestResults() },
                    modifier = Modifier.weight(1f)
                ) {
                    Icon(Icons.Default.Share, contentDescription = null)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Export")
                }
            }
        }
    }
}

@Composable
fun DetectionHistoryCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "📜 Detection History",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            if (uiState.detectionHistory.isEmpty()) {
                Text(
                    text = "No detections yet. Start testing to see results.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(vertical = 16.dp)
                )
            } else {
                uiState.detectionHistory.takeLast(10).forEach { detection ->
                    DetectionHistoryItem(detection)
                }
            }

            if (uiState.detectionHistory.isNotEmpty()) {
                TextButton(
                    onClick = { viewModel.clearHistory() },
                    modifier = Modifier.align(Alignment.End)
                ) {
                    Text("Clear History")
                }
            }
        }
    }
}
```

### Templates Tab

```kotlin
@Composable
fun TemplatesTab(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    // Template controls
    TemplateControls(uiState, viewModel)

    // Template grid/list
    if (uiState.filteredTemplates.isEmpty()) {
        EmptyTemplatesState(uiState, viewModel)
    } else {
        TemplateList(uiState, viewModel)
    }
}

@Composable
fun TemplateControls(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = { viewModel.updateSearchQuery(it) },
                    placeholder = { Text("Search gestures...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f),
                    singleLine = true
                )
                FilterChip(
                    selected = uiState.selectedFilter != GestureFilter.ALL,
                    onClick = { /* Show filter dialog */ },
                    label = { Text("Filter") }
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                GestureFilter.entries.forEach { filter ->
                    FilterChip(
                        selected = uiState.selectedFilter == filter,
                        onClick = { viewModel.setFilter(filter) },
                        label = { Text(filter.displayName, fontSize = 11.sp) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }

            Button(
                onClick = { viewModel.showAddDialog(true) },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Add, contentDescription = "Add")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create New Gesture")
            }
        }
    }
}

@Composable
fun TemplateList(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    uiState.filteredTemplates.forEach { template ->
        TemplateCard(
            template = template,
            onSelect = { viewModel.selectTemplate(it) },
            onEdit = { viewModel.showEditDialog(true); viewModel.selectTemplate(it) },
            onDelete = { viewModel.showDeleteDialog(true); viewModel.selectTemplate(it) },
            onToggleFavorite = { viewModel.toggleFavorite(it.id) },
            onToggleEnabled = { viewModel.toggleEnabled(it.id) }
        )
    }
}

@Composable
fun TemplateCard(
    template: CustomGestureTemplate,
    onSelect: (CustomGestureTemplate) -> Unit,
    onEdit: (CustomGestureTemplate) -> Unit,
    onDelete: (CustomGestureTemplate) -> Unit,
    onToggleFavorite: () -> Unit,
    onToggleEnabled: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect(template) }
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (template.isEnabled) 
                MaterialTheme.colorScheme.surfaceVariant 
            else 
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(8.dp))
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = template.icon ?: "✋",
                    fontSize = 20.sp
                )
            }

            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = template.name,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium,
                        color = if (template.isEnabled) 
                            MaterialTheme.colorScheme.onSurface 
                        else 
                            MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (template.isFavorite) {
                        Icon(
                            Icons.Default.Favorite,
                            contentDescription = "Favorite",
                            tint = Color(0xFFFFC107),
                            modifier = Modifier.size(14.dp)
                        )
                    }
                    if (!template.isEnabled) {
                        Surface(
                            shape = RoundedCornerShape(4.dp),
                            color = MaterialTheme.colorScheme.errorContainer
                        ) {
                            Text(
                                text = "Disabled",
                                fontSize = 8.sp,
                                modifier = Modifier.padding(horizontal = 4.dp, vertical = 2.dp),
                                color = MaterialTheme.colorScheme.onErrorContainer
                            )
                        }
                    }
                }
                Text(
                    text = "Action: ${template.action} • Used: ${template.usageCount} times",
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                IconButton(onClick = { onEdit(template) }) {
                    Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = { onDelete(template) }) {
                    Icon(Icons.Default.Delete, contentDescription = "Delete", modifier = Modifier.size(18.dp))
                }
                IconButton(onClick = onToggleFavorite) {
                    Icon(
                        if (template.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                        contentDescription = "Favorite",
                        modifier = Modifier.size(18.dp),
                        tint = if (template.isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = template.isEnabled,
                    onCheckedChange = { onToggleEnabled() },
                    modifier = Modifier.scale(0.8f)
                )
            }
        }
    }
}
```

### Stats Tab

```kotlin
@Composable
fun StatsTab(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    // Overview stats
    StatsOverviewCard(uiState, viewModel)

    // Performance chart
    PerformanceChart(uiState, viewModel)

    // Gesture breakdown
    GestureBreakdownCard(uiState, viewModel)
}

@Composable
fun StatsOverviewCard(
    uiState: GestureStudioUiState,
    viewModel: GestureStudioViewModel
) {
    val stats = uiState.gestureStats

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Statistics Overview",
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                StatsItem(
                    value = stats.totalGestures.toString(),
                    label = "Total Gestures",
                    icon = Icons.Default.Gesture,
                    modifier = Modifier.weight(1f)
                )
                StatsItem(
                    value = "${(stats.successRate * 100).toInt()}%",
                    label = "Success Rate",
                    icon = Icons.Default.CheckCircle,
                    modifier = Modifier.weight(1f)
                )
                StatsItem(
                    value = "${(stats.averageConfidence * 100).toInt()}%",
                    label = "Avg Confidence",
                    icon = Icons.Default.TrendingUp,
                    modifier = Modifier.weight(1f)
                )
                StatsItem(
                    value = stats.totalTrainingSessions.toString(),
                    label = "Sessions",
                    icon = Icons.Default.School,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
```

---

## 📊 Gesture Studio Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    GESTURE STUDIO FLOW                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. CREATE GESTURE                                                     │
│     ├── Enter gesture name                                            │
│     ├── Enter action                                                  │
│     └── Click "Start Recording"                                       │
│                                                                         │
│  2. RECORD SAMPLES                                                     │
│     ├── Perform gesture motion                                        │
│     ├── Sensor data collected                                         │
│     └── Samples added to training set                                 │
│                                                                         │
│  3. TRAIN GESTURE                                                      │
│     ├── Collect minimum samples (5+)                                  │
│     ├── Click "Train"                                                 │
│     ├── ML model processes data                                      │
│     └── Training complete                                            │
│                                                                         │
│  4. SAVE TEMPLATE                                                      │
│     ├── Review training results                                       │
│     ├── Click "Save Template"                                         │
│     └── Template added to library                                    │
│                                                                         │
│  5. TEST GESTURE                                                       │
│     ├── Start testing mode                                            │
│     ├── Perform gesture                                               │
│     ├── System detects and matches                                   │
│     └── Results displayed                                            │
│                                                                         │
│  6. MANAGE TEMPLATES                                                   │
│     ├── View all templates                                            │
│     ├── Edit/Delete templates                                         │
│     ├── Toggle favorites                                              │
│     └── Enable/Disable templates                                      │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **User-Centric** | Intuitive workflow for creating gestures |
| **Real-time Feedback** | Live detection during testing |
| **Visual Feedback** | Animated indicators and progress |
| **Template Management** | Comprehensive CRUD operations |
| **Performance Metrics** | Success rates, confidence scores |
| **Export/Import** | Share gesture templates |
| **Persistent Storage** | Templates saved to Room/Preferences |
| **Reactive UI** | StateFlow with automatic updates |

---

**The Gesture Studio provides a powerful interface for creating, training, testing, and managing custom gestures, allowing users to personalize their Air Mouse experience.**