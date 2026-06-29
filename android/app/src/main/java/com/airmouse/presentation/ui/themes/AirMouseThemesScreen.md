# 📘 Air Mouse Themes Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.themes` package contains the **Themes screen** for the Air Mouse application. This screen provides a comprehensive interface for selecting and customizing themes, accent colors, and visual styles for the entire application.

```
com.airmouse.presentation.ui.themes/
├── ThemesScreen.kt                # Main themes UI
├── ThemesViewModel.kt             # Themes ViewModel
├── ThemesState.kt                 # Themes state models
├── ThemesComponents.kt            # Reusable themes UI components
└── ThemesConstants.kt             # Themes constants
```

---

## 🎯 1. ThemesScreen

### Purpose
Provides a **comprehensive themes interface** for selecting, previewing, and customizing the application's visual appearance, including themes and accent colors.

### Key Features

| Feature | Description |
|---------|-------------|
| **Theme Selection** | 20+ themes including light, dark, pure black, and premium themes |
| **Accent Colors** | 14+ accent color options |
| **Live Preview** | Preview themes before applying |
| **Premium Themes** | Paid themes with special designs |
| **Reset** | Reset to default theme |
| **Customization** | Accent color customization |
| **Preview Mode** | Hover preview of themes |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ThemesScreen(
    navigationActions: NavigationActions,
    viewModel: ThemesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val themes = viewModel.themeOptions
    val colors = LocalThemeColors.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "Themes",
                        color = colors.onSurface
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = colors.onSurface
                        )
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.resetToDefault() }) {
                        Icon(
                            Icons.Default.Restore,
                            contentDescription = "Reset",
                            tint = colors.onSurface
                        )
                    }
                    IconButton(onClick = { viewModel.toggleCustomization() }) {
                        Icon(
                            if (uiState.isCustomizing) Icons.Filled.Close else Icons.Filled.Brush,
                            contentDescription = "Customize",
                            tint = colors.onSurface
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
            // Current Theme Card
            item {
                CurrentThemeCard(uiState, viewModel, colors)
            }

            // Themes Section
            item {
                SectionHeader("Themes")
            }
            items(themes.filter { !it.isPremium }) { theme ->
                ThemeOptionCard(
                    theme = theme,
                    isSelected = uiState.currentTheme == theme.id,
                    isPreviewing = uiState.previewTheme == theme.id,
                    onSelect = { viewModel.setTheme(theme.id) },
                    onPreview = { viewModel.previewTheme(theme.id) },
                    onPreviewEnd = { viewModel.clearPreview() },
                    colors = colors
                )
            }

            // Accent Colors (when customizing)
            if (uiState.isCustomizing) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    SectionHeader("Accent Colors")
                }
                item {
                    AccentColorsCard(uiState, viewModel, colors)
                }
            }

            // Premium Themes
            item {
                Spacer(modifier = Modifier.height(8.dp))
                SectionHeader("Premium Themes")
            }
            items(themes.filter { it.isPremium }) { theme ->
                PremiumThemeCard(
                    theme = theme,
                    isSelected = uiState.currentTheme == theme.id,
                    onSelect = { viewModel.setTheme(theme.id) },
                    colors = colors
                )
            }

            // Info Card
            item {
                InfoCard(colors)
            }
        }
    }

    // Snackbar for success/error messages
    if (uiState.success != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = colors.primary,
            contentColor = colors.onPrimary,
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss", color = colors.onPrimary)
                }
            }
        ) {
            Text(uiState.success!!)
        }
    }

    if (uiState.error != null) {
        Snackbar(
            modifier = Modifier.padding(16.dp),
            containerColor = colors.error,
            contentColor = colors.onError,
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss", color = colors.onError)
                }
            }
        ) {
            Text(uiState.error!!)
        }
    }
}
```

---

## 🎯 2. ThemesState

### Purpose
Defines the **complete state model** for the themes screen, including all theme options, accent colors, and UI state.

### ThemesUiState

| Property | Type | Description |
|----------|------|-------------|
| `currentTheme` | String | Currently selected theme ID |
| `accentColor` | `AccentColor` | Currently selected accent color |
| `isCustomizing` | Boolean | Whether accent color customization is open |
| `previewTheme` | String? | Theme ID currently being previewed |
| `isLoading` | Boolean | Whether loading data |
| `error` | String? | Error message if any |
| `success` | String? | Success message if any |
| `themeApplied` | Boolean | Whether theme has been applied |

### AccentColor Enum

```kotlin
enum class AccentColor(
    val displayName: String,
    val colorCode: Long,
    val lightColor: Long,
    val darkColor: Long
) {
    ORANGE("Orange", 0xFFFF5722, 0xFFFF8A65, 0xFFBF360C),
    BLUE("Blue", 0xFF2196F3, 0xFF64B5F6, 0xFF0D47A1),
    GREEN("Green", 0xFF4CAF50, 0xFF81C784, 0xFF1B5E20),
    PURPLE("Purple", 0xFF9C27B0, 0xFFCE93D8, 0xFF4A148C),
    PINK("Pink", 0xFFE91E63, 0xFFF06292, 0xFF880E4F),
    RED("Red", 0xFFF44336, 0xFFEF9A9A, 0xFFB71C1C),
    TEAL("Teal", 0xFF009688, 0xFF4DB6AC, 0xFF004D40),
    INDIGO("Indigo", 0xFF3F51B5, 0xFF7986CB, 0xFF1A237E),
    CYAN("Cyan", 0xFF00BCD4, 0xFF4DD0E1, 0xFF006064),
    AMBER("Amber", 0xFFFFC107, 0xFFFFD54F, 0xFFFF6F00),
    ROSE("Rose", 0xFFE91E63, 0xFFF48FB1, 0xFF880E4F),
    LIME("Lime", 0xFFCDDC39, 0xFFD4E157, 0xFF827717),
    BROWN("Brown", 0xFF795548, 0xFFA1887F, 0xFF3E2723),
    GREY("Grey", 0xFF607D8B, 0xFF90A4AE, 0xFF263238)
}
```

### ThemeOption

```kotlin
data class ThemeOption(
    val id: String,
    val name: String,
    val description: String,
    val previewColors: List<Long>,
    val isPremium: Boolean = false,
    val isSystem: Boolean = false
)
```

### ThemeDefinitions

```kotlin
object ThemeDefinitions {
    val themes = listOf(
        ThemeOption(
            id = "system",
            name = "System Default",
            description = "Follows system theme",
            previewColors = listOf(0xFF607D8B, 0xFF90A4AE, 0xFFCFD8DC),
            isSystem = true
        ),
        ThemeOption(
            id = "light",
            name = "Light",
            description = "Clean bright interface",
            previewColors = listOf(0xFFFFFFFF, 0xFFF5F5F5, 0xFFE0E0E0)
        ),
        ThemeOption(
            id = "dark",
            name = "Dark",
            description = "Easy on the eyes",
            previewColors = listOf(0xFF1E1E1E, 0xFF2D2D2D, 0xFF424242)
        ),
        ThemeOption(
            id = "pure_black",
            name = "Pure Black",
            description = "AMOLED friendly",
            previewColors = listOf(0xFF000000, 0xFF0D0D0D, 0xFF1A1A1A)
        ),
        // ... 15+ more themes
    )
}
```

---

## 🧩 3. UI Components

### CurrentThemeCard

```kotlin
@Composable
fun CurrentThemeCard(
    uiState: ThemesUiState,
    viewModel: ThemesViewModel,
    colors: ThemeColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.primaryContainer
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
                    text = "Current Theme",
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.onPrimaryContainer
                )
                Text(
                    text = ThemeDefinitions.getTheme(uiState.currentTheme)?.name?.uppercase()
                        ?: uiState.currentTheme.uppercase(),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.onPrimaryContainer
                )
                Text(
                    text = "Accent: ${uiState.accentColor.displayName}",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.onPrimaryContainer.copy(alpha = 0.8f)
                )
            }
            if (uiState.isCustomizing) {
                Icon(
                    Icons.Default.Brush,
                    contentDescription = "Customizing",
                    tint = colors.onPrimaryContainer
                )
            }
        }
    }
}
```

### ThemeOptionCard

```kotlin
@Composable
fun ThemeOptionCard(
    theme: ThemeOption,
    isSelected: Boolean,
    isPreviewing: Boolean,
    onSelect: () -> Unit,
    onPreview: () -> Unit,
    onPreviewEnd: () -> Unit,
    colors: ThemeColorScheme
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                colors.primaryContainer
            else
                colors.surfaceVariant
        ),
        onClick = onSelect
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant
                    )
                }

                if (theme.isPremium) {
                    Surface(
                        shape = RoundedCornerShape(4.dp),
                        color = Color(0xFFFFC107).copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = "PREMIUM",
                            fontSize = 10.sp,
                            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            color = Color(0xFFFFC107)
                        )
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }

                RadioButton(
                    selected = isSelected,
                    onClick = onSelect,
                    colors = RadioButtonDefaults.colors(
                        selectedColor = colors.primary,
                        unselectedColor = colors.onSurfaceVariant
                    )
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Color Preview
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                theme.previewColors.forEach { color ->
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp))
                            .background(Color(color))
                    )
                }
            }

            // Preview Button
            AnimatedVisibility(visible = !isPreviewing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onPreview,
                        modifier = Modifier.padding(top = 8.dp),
                        colors = ButtonDefaults.textButtonColors(
                            contentColor = colors.primary
                        )
                    ) {
                        Icon(
                            Icons.Default.Visibility,
                            contentDescription = "Preview",
                            modifier = Modifier.size(16.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Preview")
                    }
                }
            }
        }
    }

    // Preview Card
    AnimatedVisibility(visible = isPreviewing) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            colors = CardDefaults.cardColors(
                containerColor = getPreviewColor(theme.id)
            ),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp)
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Text(
                        text = "Preview: ${theme.name}",
                        style = MaterialTheme.typography.headlineSmall,
                        color = Color.White
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Text appears in theme colors",
                        color = Color.White.copy(alpha = 0.8f)
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(
                        onClick = onPreviewEnd,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.White,
                            contentColor = getPreviewColor(theme.id)
                        )
                    ) {
                        Text("Close Preview")
                    }
                }
            }
        }
    }
}
```

### PremiumThemeCard

```kotlin
@Composable
fun PremiumThemeCard(
    theme: ThemeOption,
    isSelected: Boolean,
    onSelect: () -> Unit,
    colors: ThemeColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                colors.primaryContainer
            else
                colors.surfaceVariant
        ),
        onClick = onSelect
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(getPreviewColor(theme.id))
                )

                Column {
                    Text(
                        text = theme.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (isSelected) colors.onPrimaryContainer else colors.onSurface
                    )
                    Text(
                        text = theme.description,
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isSelected) colors.onPrimaryContainer.copy(alpha = 0.8f) else colors.onSurfaceVariant
                    )
                }
            }

            RadioButton(
                selected = isSelected,
                onClick = onSelect,
                colors = RadioButtonDefaults.colors(
                    selectedColor = colors.primary,
                    unselectedColor = colors.onSurfaceVariant
                )
            )
        }
    }
}
```

### AccentColorsCard

```kotlin
@Composable
fun AccentColorsCard(
    uiState: ThemesUiState,
    viewModel: ThemesViewModel,
    colors: ThemeColorScheme
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "Choose your accent color",
                style = MaterialTheme.typography.titleMedium,
                color = colors.onSurface
            )

            // 14+ accent color options in a grid
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(AccentColor.entries) { accentColor ->
                    AccentColorOption(
                        accentColor = accentColor,
                        isSelected = uiState.accentColor == accentColor,
                        onClick = { viewModel.setAccentColor(accentColor) },
                        colors = colors
                    )
                }
            }
        }
    }
}
```

### AccentColorOption

```kotlin
@Composable
fun AccentColorOption(
    accentColor: AccentColor,
    isSelected: Boolean,
    onClick: () -> Unit,
    colors: ThemeColorScheme
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier
            .width(60.dp)
            .clickable { onClick() }
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(CircleShape)
                .background(Color(accentColor.colorCode))
                .then(
                    if (isSelected) Modifier.border(3.dp, colors.primary, CircleShape)
                    else Modifier
                )
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = accentColor.displayName,
            fontSize = 10.sp,
            color = if (isSelected) colors.primary else colors.onSurfaceVariant
        )
    }
}
```

### InfoCard

```kotlin
@Composable
fun InfoCard(colors: ThemeColorScheme) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = colors.secondaryContainer
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Info,
                    contentDescription = "Info",
                    modifier = Modifier.size(20.dp),
                    tint = colors.onSecondaryContainer
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "Theme Tips",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSecondaryContainer
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "• Pure Black theme saves battery on AMOLED screens\n" +
                        "• High Contrast mode helps with visibility\n" +
                        "• Premium themes require in-app purchase\n" +
                        "• Themes sync across all your devices",
                style = MaterialTheme.typography.bodySmall,
                color = colors.onSecondaryContainer
            )
        }
    }
}
```

### SectionHeader

```kotlin
@Composable
fun SectionHeader(
    text: String,
    modifier: Modifier = Modifier
) {
    Text(
        text = text,
        style = MaterialTheme.typography.titleLarge,
        fontWeight = FontWeight.Bold,
        color = MaterialTheme.colorScheme.onSurface,
        modifier = modifier.padding(horizontal = 8.dp)
    )
}
```

### Preview Color Helper

```kotlin
private fun getPreviewColor(themeId: String): Color {
    return when (themeId) {
        "light" -> Color(0xFFF5F5F5)
        "dark" -> Color(0xFF1E1E1E)
        "pure_black" -> Color(0xFF000000)
        "ocean" -> Color(0xFF1976D2)
        "sunset" -> Color(0xFFFF5722)
        "forest" -> Color(0xFF2E7D32)
        "purple_haze" -> Color(0xFF6A1B9A)
        "cherry" -> Color(0xFFAD1457)
        "neon" -> Color(0xFF00BCD4)
        "lavender" -> Color(0xFF4527A0)
        "mint" -> Color(0xFF00695C)
        "peach" -> Color(0xFFD84315)
        "sky" -> Color(0xFF1565C0)
        "midnight" -> Color(0xFF0A0A0A)
        "gold" -> Color(0xFF4E342E)
        "matrix" -> Color(0xFF003300)
        "cotton_candy" -> Color(0xFF4A148C)
        "coffee" -> Color(0xFF3E2723)
        else -> Color(0xFF1E1E1E)
    }
}
```

---

## 🎯 4. ThemesViewModel

### Key Methods

| Method | Purpose |
|--------|---------|
| `setTheme(themeId)` | Apply a theme |
| `setAccentColor(accentColor)` | Set accent color |
| `previewTheme(themeId)` | Preview a theme |
| `clearPreview()` | Clear theme preview |
| `toggleCustomization()` | Toggle accent color customization |
| `resetToDefault()` | Reset to default theme |
| `clearError()` | Clear error message |

### Theme Persistence

```kotlin
private fun loadSettings() {
    val savedTheme = prefs.getString("theme", "system")
    val savedAccent = prefs.getString("accent_color", "ORANGE")
    val accentColor = try {
        AccentColor.valueOf(savedAccent)
    } catch (e: Exception) {
        AccentColor.ORANGE
    }

    _uiState.update {
        it.copy(
            currentTheme = savedTheme,
            accentColor = accentColor,
            themeApplied = true
        )
    }
}

fun setTheme(themeId: String) {
    viewModelScope.launch {
        try {
            _uiState.update { it.copy(isLoading = true, error = null) }
            prefs.putString("theme", themeId)
            _uiState.update {
                it.copy(
                    currentTheme = themeId,
                    isLoading = false,
                    success = "Theme applied: ${getThemeName(themeId)}",
                    themeApplied = true
                )
            }
            delay(3000)
            _uiState.update { it.copy(success = null) }
        } catch (e: Exception) {
            _uiState.update {
                it.copy(
                    isLoading = false,
                    error = "Failed to apply theme: ${e.message}"
                )
            }
        }
    }
}
```

---

## 📋 Theme List

| Theme ID | Name | Description | Premium |
|----------|------|-------------|---------|
| `system` | System Default | Follows system theme | No |
| `light` | Light | Clean bright interface | No |
| `dark` | Dark | Easy on the eyes | No |
| `pure_black` | Pure Black | AMOLED friendly | No |
| `ocean` | Ocean Blue | Calm blue tones | No |
| `sunset` | Sunset | Warm orange hues | No |
| `forest` | Forest Green | Natural green tones | No |
| `purple_haze` | Purple Haze | Mystical purple | No |
| `cherry` | Cherry Blossom | Soft pink tones | No |
| `neon` | Neon Cyber | Vibrant cyberpunk | No |
| `lavender` | Lavender | Gentle purple tones | No |
| `mint` | Mint Fresh | Cool mint green | No |
| `peach` | Peach | Warm peach tones | No |
| `sky` | Sky Blue | Bright sky blue | No |
| `midnight` | Midnight | Deep night theme | Yes |
| `gold` | Golden Luxe | Elegant gold accents | Yes |
| `matrix` | Matrix | Green matrix style | Yes |
| `cotton_candy` | Cotton Candy | Sweet pastel pink/blue | Yes |
| `coffee` | Coffee | Warm coffee tones | Yes |

---

## 🎨 Accent Color Preview

| Color | Name | Hex Code |
|-------|------|----------|
| 🟠 | Orange | #FF5722 |
| 🔵 | Blue | #2196F3 |
| 🟢 | Green | #4CAF50 |
| 🟣 | Purple | #9C27B0 |
| 🩷 | Pink | #E91E63 |
| 🔴 | Red | #F44336 |
| 🔷 | Teal | #009688 |
| 🟣 | Indigo | #3F51B5 |
| 🔵 | Cyan | #00BCD4 |
| 🟡 | Amber | #FFC107 |
| 🩷 | Rose | #E91E63 |
| 🟢 | Lime | #CDDC39 |
| 🟤 | Brown | #795548 |
| ⚪ | Grey | #607D8B |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Live Preview** | Preview themes before applying |
| **Accent Colors** | 14+ accent color options |
| **Premium Themes** | Paid themes with special designs |
| **Persistence** | Themes saved to PreferencesManager |
| **Visual Feedback** | Success/error messages |
| **System Theme** | Follow system theme option |
| **AMOLED Friendly** | Pure Black theme for AMOLED screens |
| **Responsive** | Adapts to screen size |

---

**The Themes Screen provides comprehensive theme customization, allowing users to personalize the Air Mouse application with a wide range of themes and accent colors.**