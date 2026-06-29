# 📘 Air Mouse Help Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.help` package contains the **Help & Support screen** for the Air Mouse application. This screen provides a comprehensive help center with searchable articles, categories, favorites, and contact options.

```
com.airmouse.presentation.ui.help/
├── HelpScreen.kt              # Main help UI
├── HelpViewModel.kt           # Help ViewModel
├── HelpModels.kt              # Help data models
├── HelpUiState.kt             # Help state models
├── HelpComponents.kt          # Reusable help UI components
└── HelpData.kt                # Static help content
```

---

## 🎯 1. HelpScreen – Complete Documentation

### Purpose
Provides a **comprehensive help center** where users can find answers to common questions, learn about features, troubleshoot issues, and contact support.

### Key Features

| Feature | Description |
|---------|-------------|
| **Search** | Search help articles by keyword |
| **Categories** | Filter articles by category |
| **Favorites** | Save frequently referenced articles |
| **Expandable Content** | Expand articles to view full content |
| **Contact Support** | Email, Discord, GitHub, Website |
| **Feedback** | Rate articles and provide feedback |
| **Related Topics** | Cross-linked related content |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navigationActions: NavigationActions,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredSections = viewModel.getFilteredSections()

    Scaffold(
        topBar = { /* TopAppBar with title, search, favorites toggle */ }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Search bar
            SearchBar(uiState, viewModel)

            // Category chips
            CategoryChips(uiState, viewModel)

            // Results count
            if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != HelpCategory.ALL) {
                ResultsCount(uiState, filteredSections)
            }

            // Help sections list
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (filteredSections.isEmpty()) {
                    item { EmptyHelpState(uiState, viewModel) }
                } else {
                    items(filteredSections, key = { it.id }) { section ->
                        HelpSectionCard(
                            section = section,
                            isExpanded = uiState.expandedSections.contains(section.id),
                            isFavorite = uiState.favoriteSections.contains(section.id),
                            onToggle = { viewModel.toggleSection(section.id) },
                            onToggleFavorite = { viewModel.toggleFavorite(section.id) },
                            onRelatedTopicClick = { topic -> viewModel.filterByTopic(topic) }
                        )
                    }
                }
            }
        }
    }

    // Dialogs
    if (uiState.showContactDialog) {
        ContactDialog(
            onDismiss = { viewModel.showContactDialog(false) },
            onEmail = { viewModel.openEmail("support@airmouse.io") },
            onDiscord = { viewModel.openDiscord() },
            onGithub = { viewModel.openGithub() },
            onWebsite = { viewModel.openWebsite() }
        )
    }

    if (uiState.showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { viewModel.showFeedbackDialog(false) },
            onSubmit = { rating, message ->
                viewModel.submitFeedback(rating, message)
            }
        )
    }
}
```

---

## 🎯 2. HelpModels – Data Models

### HelpUiState

```kotlin
data class HelpUiState(
    val searchQuery: String = "",
    val selectedCategory: HelpCategory = HelpCategory.ALL,
    val expandedSections: Set<String> = emptySet(),
    val favoriteSections: Set<String> = emptySet(),
    val showFavoritesOnly: Boolean = false,
    val showContactDialog: Boolean = false,
    val showFeedbackDialog: Boolean = false,
    val isLoading: Boolean = false,
    val error: String? = null,
    val success: String? = null
)
```

### HelpCategory

```kotlin
enum class HelpCategory(val displayName: String) {
    ALL("All"),
    GETTING_STARTED("Getting Started"),
    CONNECTION("Connection"),
    GESTURES("Gestures"),
    CALIBRATION("Calibration"),
    TROUBLESHOOTING("Troubleshooting"),
    ADVANCED("Advanced"),
    ACCESSIBILITY("Accessibility"),
    FAQ("FAQ")
}
```

### HelpSection

```kotlin
data class HelpSection(
    val id: String,
    val title: String,
    val content: String,
    val category: HelpCategory,
    val steps: List<String> = emptyList(),
    val tips: List<String> = emptyList(),
    val relatedTopics: List<String> = emptyList()
)
```

---

## 🎯 3. HelpViewModel

### Purpose
Manages **help content**, search, filtering, favorites, and expanded sections.

### Key State Properties

| Property | Type | Description |
|----------|------|-------------|
| `uiState` | `StateFlow<HelpUiState>` | Complete UI state |
| `helpSections` | `List<HelpSection>` | All help articles |
| `filteredSections` | `List<HelpSection>` | Filtered articles |

### Key Methods

| Method | Purpose |
|--------|---------|
| `toggleSection(sectionId: String)` | Expand/collapse an article |
| `toggleFavorite(sectionId: String)` | Add/remove from favorites |
| `toggleShowFavoritesOnly()` | Toggle favorites filter |
| `updateSearchQuery(query: String)` | Update search query |
| `selectCategory(category: HelpCategory)` | Filter by category |
| `filterByTopic(topic: String)` | Filter by related topic |
| `getFilteredSections()` | Apply all filters |
| `showContactDialog(show: Boolean)` | Show contact dialog |
| `showFeedbackDialog(show: Boolean)` | Show feedback dialog |
| `openEmail(email: String)` | Open email client |
| `openDiscord()` | Open Discord invite |
| `openGithub()` | Open GitHub repo |
| `openWebsite()` | Open website |
| `submitFeedback(rating: Int, message: String)` | Submit user feedback |
| `clearFilters()` | Reset search and category |
| `resetToDefaults()` | Reset all help preferences |

### Filtering Logic

```kotlin
fun getFilteredSections(): List<HelpSection> {
    var sections = helpSections

    // Filter by category
    if (_uiState.value.selectedCategory != HelpCategory.ALL) {
        sections = sections.filter { 
            it.category == _uiState.value.selectedCategory 
        }
    }

    // Filter by favorites
    if (_uiState.value.showFavoritesOnly) {
        sections = sections.filter { 
            _uiState.value.favoriteSections.contains(it.id) 
        }
    }

    // Filter by search query
    if (_uiState.value.searchQuery.isNotEmpty()) {
        val query = _uiState.value.searchQuery.lowercase()
        sections = sections.filter {
            it.title.lowercase().contains(query) ||
            it.content.lowercase().contains(query) ||
            it.steps.any { step -> step.lowercase().contains(query) } ||
            it.tips.any { tip -> tip.lowercase().contains(query) } ||
            it.relatedTopics.any { topic -> topic.lowercase().contains(query) }
        }
    }

    return sections
}
```

### Persistence

```kotlin
// Favorites are persisted in PreferencesManager
private fun loadFavorites() {
    viewModelScope.launch {
        val favorites = prefs.getString("help_favorites", "")
        val favoriteSet = if (favorites.isNotEmpty()) 
            favorites.split(",").toSet() 
        else 
            emptySet()
        _uiState.update { it.copy(favoriteSections = favoriteSet) }
    }
}

private fun saveFavorites() {
    val favorites = _uiState.value.favoriteSections.joinToString(",")
    prefs.putString("help_favorites", favorites)
}

// Expanded sections are persisted in PreferencesManager
private fun loadExpandedSections() {
    viewModelScope.launch {
        val expanded = prefs.getString("help_expanded", "")
        val expandedSet = if (expanded.isNotEmpty()) 
            expanded.split(",").toSet() 
        else 
            emptySet()
        _uiState.update { it.copy(expandedSections = expandedSet) }
    }
}

private fun saveExpandedSections() {
    val expanded = _uiState.value.expandedSections.joinToString(",")
    prefs.putString("help_expanded", expanded)
}
```

---

## 📖 4. Help Content

### Predefined Help Sections

```kotlin
val helpSections = listOf(
    HelpSection(
        id = "getting_started",
        title = "Getting Started",
        content = "Air Mouse Pro turns your phone into a wireless mouse. Follow these steps to get started:",
        category = HelpCategory.GETTING_STARTED,
        steps = listOf(
            "Install the Air Mouse app on your Android phone",
            "Download and run the Air Mouse server on your PC",
            "Ensure both devices are on the same WiFi network",
            "Open the app and enter your PC's IP address",
            "Tap 'Connect' to establish connection",
            "Calibrate sensors for best accuracy",
            "Start moving your phone to control the cursor"
        ),
        tips = listOf(
            "Keep your phone steady during calibration",
            "Hold the phone with screen facing you",
            "Start with medium sensitivity settings"
        ),
        relatedTopics = listOf("Connection Guide", "Calibration Guide")
    ),
    HelpSection(
        id = "connection",
        title = "Connection Guide",
        content = "How to connect your phone to the PC server:",
        category = HelpCategory.CONNECTION,
        steps = listOf(
            "Run the Air Mouse server on your PC",
            "Note the IP address displayed in the server window",
            "On your phone, enter the IP address and port (default: 8080)",
            "Alternatively, scan the QR code shown on the server",
            "Tap 'Connect' - you should see 'Connected' status",
            "The cursor will now follow your phone's movement"
        ),
        tips = listOf(
            "Both devices must be on the same WiFi network",
            "Check firewall settings if connection fails",
            "Use 5GHz WiFi for lower latency"
        ),
        relatedTopics = listOf("Troubleshooting", "Network Discovery")
    ),
    HelpSection(
        id = "gestures",
        title = "Gesture Controls",
        content = "Learn the basic gestures to control your PC:",
        category = HelpCategory.GESTURES,
        steps = listOf(
            "• Move cursor: Rotate phone around X and Z axes",
            "• Left click: Quick flick/rotation around Y axis",
            "• Right click: Hold tilt for 0.5 seconds",
            "• Double click: Two quick flicks in succession",
            "• Scroll up: Fast upward movement",
            "• Scroll down: Fast downward movement",
            "• Custom gestures: Create your own in Gesture Studio"
        ),
        tips = listOf(
            "Adjust gesture sensitivity in Settings",
            "Practice gestures for better accuracy",
            "Use voice commands as alternative"
        ),
        relatedTopics = listOf("Gesture Studio", "Voice Commands")
    ),
    HelpSection(
        id = "calibration",
        title = "Calibration Guide",
        content = "Proper calibration ensures accurate cursor tracking:",
        category = HelpCategory.CALIBRATION,
        steps = listOf(
            "Open Calibration from the main menu",
            "Gyroscope: Place phone flat and keep still for 10 seconds",
            "Accelerometer: Hold phone in 6 different positions",
            "Magnetometer: Move phone in figure-8 pattern",
            "Test the cursor movement after calibration",
            "Re-calibrate if you notice drift or inaccuracy"
        ),
        tips = listOf(
            "Calibrate on a flat, stable surface",
            "Recalibrate when switching holding hands",
            "Calibration data persists across app restarts"
        ),
        relatedTopics = listOf("Sensor Settings", "Troubleshooting")
    ),
    HelpSection(
        id = "troubleshooting",
        title = "Troubleshooting",
        content = "Common issues and solutions:",
        category = HelpCategory.TROUBLESHOOTING,
        steps = listOf(
            "Cannot connect:",
            "  • Check if both devices are on same WiFi",
            "  • Disable firewall temporarily",
            "  • Try the QR code method",
            "",
            "Laggy cursor:",
            "  • Reduce sensitivity in Settings",
            "  • Enable predictive movement",
            "  • Use 5GHz WiFi if available",
            "",
            "Gestures not detected:",
            "  • Recalibrate sensors",
            "  • Increase gesture sensitivity",
            "  • Practice the movement pattern"
        ),
        tips = listOf(
            "Restart the app if issues persist",
            "Clear app cache in Android settings",
            "Update to latest version"
        ),
        relatedTopics = listOf("Connection Guide", "Calibration Guide")
    ),
    HelpSection(
        id = "advanced",
        title = "Advanced Features",
        content = "Explore additional features:",
        category = HelpCategory.ADVANCED,
        steps = listOf(
            "Voice Commands: Say 'click', 'scroll', 'right click'",
            "Custom Gestures: Train your own gestures in Gesture Studio",
            "Proximity Lock: Auto-locks PC when you walk away",
            "Edge Gestures: Use volume buttons for quick actions",
            "Multiple Profiles: Save different sensitivity settings",
            "AI Smoothing: Machine learning for smoother cursor",
            "Predictive Movement: Kalman filter reduces lag"
        ),
        tips = listOf(
            "Enable AI features for smoother experience",
            "Create profiles for different activities",
            "Use voice commands for hands-free control"
        ),
        relatedTopics = listOf("Voice Commands", "Proximity Lock")
    ),
    HelpSection(
        id = "accessibility",
        title = "Accessibility Features",
        content = "Accessibility options for all users:",
        category = HelpCategory.ACCESSIBILITY,
        steps = listOf(
            "High Contrast Mode: Better visibility",
            "Large Text: Increased font size",
            "Screen Reader: Voice feedback for actions",
            "Announce Movement: Voice announcements",
            "Reduce Motion: Minimize animations",
            "Color Blind Mode: Adjust colors for visibility"
        ),
        tips = listOf(
            "Enable features in Accessibility settings",
            "Customize to your preference",
            "Features work with system screen readers"
        ),
        relatedTopics = listOf("Display Settings", "Voice Feedback")
    ),
    HelpSection(
        id = "faq",
        title = "Frequently Asked Questions",
        content = "Answers to common questions:",
        category = HelpCategory.FAQ,
        steps = listOf(
            "Q: Does it work without internet?",
            "A: Yes, works on local WiFi network only",
            "",
            "Q: What Android versions are supported?",
            "A: Android 10 (API 29) and above",
            "",
            "Q: Does it drain battery?",
            "A: Moderate usage, optimized for efficiency",
            "",
            "Q: Can I use Bluetooth instead?",
            "A: Yes, Bluetooth HID mouse mode available",
            "",
            "Q: Is my data private?",
            "A: All communication is local - no cloud servers"
        ),
        tips = listOf(
            "Check GitHub for latest updates",
            "Report issues on GitHub",
            "Join our Discord for support"
        ),
        relatedTopics = listOf("Getting Started", "Troubleshooting")
    )
)
```

---

## 🧩 5. UI Components

### Search Bar

```kotlin
@Composable
fun SearchBar(
    uiState: HelpUiState,
    viewModel: HelpViewModel
) {
    val infiniteTransition = rememberInfiniteTransition()
    val scale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .scale(scale)
    ) {
        OutlinedTextField(
            value = uiState.searchQuery,
            onValueChange = { viewModel.updateSearchQuery(it) },
            placeholder = { Text("Search help articles...") },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
            trailingIcon = {
                if (uiState.searchQuery.isNotEmpty()) {
                    IconButton(onClick = { viewModel.updateSearchQuery("") }) {
                        Icon(Icons.Default.Close, contentDescription = "Clear")
                    }
                }
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true,
            shape = RoundedCornerShape(28.dp),
            colors = OutlinedTextFieldDefaults.colors(
                focusedBorderColor = MaterialTheme.colorScheme.primary,
                unfocusedBorderColor = MaterialTheme.colorScheme.outline
            )
        )
    }
}
```

### Category Chips

```kotlin
@Composable
fun CategoryChips(
    uiState: HelpUiState,
    viewModel: HelpViewModel
) {
    LazyRow(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(HelpCategory.entries.toTypedArray()) { category ->
            val isSelected = uiState.selectedCategory == category
            FilterChip(
                selected = isSelected,
                onClick = { viewModel.selectCategory(category) },
                label = { Text(category.displayName, fontSize = 13.sp) },
                modifier = Modifier.animateContentSize(),
                colors = FilterChipDefaults.filterChipColors(
                    selectedContainerColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                    selectedLabelColor = MaterialTheme.colorScheme.primary
                ),
                leadingIcon = if (isSelected) {
                    { Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(16.dp)) }
                } else null
            )
        }
    }
}
```

### Help Section Card

```kotlin
@Composable
fun HelpSectionCard(
    section: HelpSection,
    isExpanded: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRelatedTopicClick: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize()
            .clickable { onToggle() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column {
            // Header
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold
                        )
                        Surface(
                            shape = CircleShape,
                            color = getCategoryColor(section.category).copy(alpha = 0.2f)
                        ) {
                            Text(
                                text = section.category.displayName,
                                fontSize = 10.sp,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = getCategoryColor(section.category)
                            )
                        }
                    }
                    Text(
                        text = section.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Column(horizontalAlignment = Alignment.End) {
                    IconButton(onClick = onToggleFavorite) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            // Expanded content
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically() + fadeIn(),
                exit = shrinkVertically() + fadeOut()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 16.dp)
                ) {
                    // Steps
                    if (section.steps.isNotEmpty()) {
                        Text(
                            text = "📋 Steps",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        section.steps.forEach { step ->
                            if (step.isBlank()) {
                                Spacer(modifier = Modifier.height(8.dp))
                            } else {
                                Row(
                                    modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    Text("• ", style = MaterialTheme.typography.bodyMedium)
                                    Text(
                                        text = step,
                                        style = MaterialTheme.typography.bodyMedium,
                                        modifier = Modifier.weight(1f)
                                    )
                                }
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                    }

                    // Tips
                    if (section.tips.isNotEmpty()) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier.padding(bottom = 8.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.TipsAndUpdates,
                                contentDescription = "Tips",
                                modifier = Modifier.size(18.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = "💡 Tips",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold
                            )
                        }
                        section.tips.forEach { tip ->
                            Row(
                                modifier = Modifier.padding(start = 8.dp, bottom = 6.dp),
                                verticalAlignment = Alignment.Top
                            ) {
                                Text("• ", style = MaterialTheme.typography.bodyMedium)
                                Text(
                                    text = tip,
                                    style = MaterialTheme.typography.bodyMedium,
                                    modifier = Modifier.weight(1f)
                                )
                            }
                        }
                    }

                    // Related Topics
                    if (section.relatedTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "🔗 Related Topics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                        section.relatedTopics.forEach { topic ->
                            TextButton(
                                onClick = { onRelatedTopicClick(topic) },
                                modifier = Modifier.padding(start = 8.dp)
                            ) {
                                Text(topic, fontSize = 13.sp)
                            }
                        }
                    }

                    // Feedback
                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider()
                    Spacer(modifier = Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Was this helpful?", fontSize = 13.sp)
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            IconButton(onClick = { /* Positive feedback */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "Yes", modifier = Modifier.size(18.dp))
                            }
                            IconButton(onClick = { /* Negative feedback */ }, modifier = Modifier.size(32.dp)) {
                                Icon(Icons.Default.ThumbDown, contentDescription = "No", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}
```

### Contact Dialog

```kotlin
@Composable
fun ContactDialog(
    onDismiss: () -> Unit,
    onEmail: () -> Unit,
    onDiscord: () -> Unit,
    onGithub: () -> Unit,
    onWebsite: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Contact Support") },
        text = {
            Column {
                Text(
                    text = "Choose how you'd like to get support:",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ContactOption(
                    icon = Icons.Default.Email,
                    title = "Email Support",
                    description = "support@airmouse.io",
                    onClick = onEmail
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContactOption(
                    icon = Icons.Default.Chat,
                    title = "Discord Community",
                    description = "Join our Discord server",
                    onClick = onDiscord
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContactOption(
                    icon = Icons.Default.Code,
                    title = "GitHub Issues",
                    description = "Report bugs on GitHub",
                    onClick = onGithub
                )
                Spacer(modifier = Modifier.height(8.dp))
                ContactOption(
                    icon = Icons.Default.Language,
                    title = "Website",
                    description = "www.airmouse.io",
                    onClick = onWebsite
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) { Text("Close") }
        }
    )
}
```

### Empty State

```kotlin
@Composable
fun EmptyHelpState(
    uiState: HelpUiState,
    viewModel: HelpViewModel
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(48.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = "No results",
                modifier = Modifier.size(64.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(16.dp))
            Text(
                text = "No help articles found",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Try adjusting your search or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(24.dp))
            OutlinedButton(onClick = { viewModel.clearFilters() }) {
                Text("Clear All Filters")
            }
        }
    }
}
```

---

## 📊 Help Screen Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        HELP SCREEN FLOW                                │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. USER OPENS HELP SCREEN                                             │
│         │                                                               │
│         ▼                                                               │
│  2. LOAD HELP CONTENT                                                  │
│     ├── Load all help sections                                        │
│     ├── Load favorites from Preferences                              │
│     └── Load expanded sections from Preferences                      │
│                                                                         │
│  3. USER INTERACTS                                                    │
│     ├── Search → Filter articles by text                             │
│     ├── Category Chip → Filter by category                           │
│     ├── Favorite Toggle → Save/remove from favorites                 │
│     ├── Article Click → Expand/Collapse content                      │
│     └── Related Topic → Navigate to filtered view                    │
│                                                                         │
│  4. VIEW FILTERED RESULTS                                             │
│     ├── Apply all filters (search, category, favorites)              │
│     └── Display filtered articles                                    │
│                                                                         │
│  5. CONTACT SUPPORT                                                   │
│     ├── Click "Contact" button                                       │
│     ├── Dialog shows options                                         │
│     └── User chooses contact method                                  │
│                                                                         │
│  6. SUBMIT FEEDBACK                                                   │
│     ├── Click "Feedback" button                                      │
│     ├── Dialog with rating and message                               │
│     └── Submit feedback                                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Category Color Mapping

```kotlin
@Composable
fun getCategoryColor(category: HelpCategory): Color {
    return when (category) {
        HelpCategory.GETTING_STARTED -> Color(0xFF00BCD4)
        HelpCategory.CONNECTION -> Color(0xFF4CAF50)
        HelpCategory.GESTURES -> Color(0xFFFF9800)
        HelpCategory.CALIBRATION -> Color(0xFF9C27B0)
        HelpCategory.TROUBLESHOOTING -> Color(0xFFF44336)
        HelpCategory.ADVANCED -> Color(0xFF2196F3)
        HelpCategory.ACCESSIBILITY -> Color(0xFFE91E63)
        HelpCategory.FAQ -> Color(0xFF607D8B)
        else -> MaterialTheme.colorScheme.primary
    }
}
```

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **User-Centric** | Articles organized by categories |
| **Searchable** | Full-text search across all content |
| **Personalized** | Favorites for quick access |
| **Interactive** | Expandable content with animations |
| **Comprehensive** | Covers all features and common issues |
| **Support Integration** | Multiple contact options |
| **Feedback** | User feedback on articles |
| **Persistence** | Favorites and expanded sections saved |

---

**The Help Screen provides a comprehensive, searchable knowledge base that helps users quickly find answers and get support for the Air Mouse application.**