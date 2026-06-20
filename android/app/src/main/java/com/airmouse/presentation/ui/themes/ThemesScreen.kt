// app/src/main/java/com/airmouse/presentation/ui/themes/ThemesScreen.kt
package com.airmouse.presentation.ui.themes

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = colors.primaryContainer,
                    titleContentColor = colors.onPrimaryContainer,
                    navigationIconContentColor = colors.onPrimaryContainer,
                    actionIconContentColor = colors.onPrimaryContainer
                )
            )
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            contentColor = colors.onSurface
        ) {
            // Current Theme Indicator
            item {
                CurrentThemeCard(uiState, viewModel, colors)
            }

            // Theme Options
            item {
                Text(
                    text = "Themes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
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

            // Accent Colors Section
            if (uiState.isCustomizing) {
                item {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Accent Colors",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold,
                        color = colors.onSurface,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }

                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = colors.surfaceVariant
                        )
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Text(
                                text = "Choose your accent color",
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.onSurface
                            )
                            Spacer(modifier = Modifier.height(12.dp))

                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(AccentColor.values()) { accentColor ->
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
            }

            // Premium Section
            item {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "Premium Themes",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = colors.onSurface,
                    modifier = Modifier.padding(horizontal = 8.dp)
                )
            }

            items(themes.filter { it.isPremium }) { theme ->
                PremiumThemeCard(
                    theme = theme,
                    isSelected = uiState.currentTheme == theme.id,
                    onSelect = { viewModel.setTheme(theme.id) },
                    colors = colors
                )
            }

            // Info Section
            item {
                Spacer(modifier = Modifier.height(16.dp))
                InfoCard(colors)
            }
        }
    }

    // Success/Error messages
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

            // Color preview
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

            // Preview button
            AnimatedVisibility(visible = !isPreviewing) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End
                ) {
                    TextButton(
                        onClick = onPreview,
                        modifier = Modifier.padding(top = 8.dp),
                        colors = TextButtonDefaults.textButtonColors(
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

    // Preview overlay
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