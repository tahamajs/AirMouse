package com.airmouse.presentation.ui.help

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.ContactSupport
import androidx.compose.material.icons.automirrored.filled.Help
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.ParticleBackground
import android.content.Intent
import android.net.Uri
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navigationActions: NavigationActions,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredSections = viewModel.getFilteredSections()
    val context = LocalContext.current
    val infiniteTransition = rememberInfiniteTransition(label = "SearchScale")
    val searchBarScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.02f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "Scale"
    )

    var showContactDialog by remember { mutableStateOf(false) }
    var showFeedbackDialog by remember { mutableStateOf(false) }

    fun openUrl(url: String) {
        runCatching {
            context.startActivity(
                Intent(Intent.ACTION_VIEW, Uri.parse(url)).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
            )
        }.onFailure {
            android.widget.Toast.makeText(context, "Cannot open link", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    fun sendEmail(to: String, subject: String, body: String = "") {
        runCatching {
            val intent = Intent(Intent.ACTION_SENDTO).apply {
                data = Uri.parse("mailto:")
                putExtra(Intent.EXTRA_EMAIL, arrayOf(to))
                putExtra(Intent.EXTRA_SUBJECT, subject)
                putExtra(Intent.EXTRA_TEXT, body)
            }
            context.startActivity(intent)
        }.onFailure {
            android.widget.Toast.makeText(context, "Email app unavailable", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "Help & Support",
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.toggleShowFavoritesOnly() }) {
                        Icon(
                            imageVector = if (uiState.showFavoritesOnly) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorites",
                            tint = if (uiState.showFavoritesOnly) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { showContactDialog = true }) {
                        Icon(Icons.AutoMirrored.Filled.ContactSupport, contentDescription = "Contact")
                    }
                    IconButton(onClick = { showFeedbackDialog = true }) {
                        Icon(Icons.Default.Feedback, contentDescription = "Feedback")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.03f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            ParticleBackground(particleCount = 15)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp)
                        .scale(searchBarScale)
                ) {
                    OutlinedTextField(
                        value = uiState.searchQuery,
                        onValueChange = viewModel::updateSearchQuery,
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
                                {
                                    Icon(
                                        Icons.Default.Check,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            } else null
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != HelpCategory.ALL) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 16.dp, vertical = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "${filteredSections.size} results found",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = {
                            viewModel.updateSearchQuery("")
                            viewModel.selectCategory(HelpCategory.ALL)
                        }) {
                            Text("Clear filters", fontSize = 12.sp)
                        }
                    }
                }

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    if (filteredSections.isEmpty()) {
                        item {
                            EmptyHelpState(
                                onClearFilters = {
                                    viewModel.updateSearchQuery("")
                                    viewModel.selectCategory(HelpCategory.ALL)
                                    if (uiState.showFavoritesOnly) viewModel.toggleShowFavoritesOnly()
                                }
                            )
                        }
                    } else {
                        items(filteredSections, key = { it.id }) { section ->
                            HelpSectionCard(
                                section = section,
                                isExpanded = uiState.expandedSections.contains(section.id),
                                isFavorite = uiState.favoriteSections.contains(section.id),
                                onToggle = { viewModel.toggleSection(section.id) },
                                onToggleFavorite = { viewModel.toggleFavorite(section.id) },
                                onRelatedTopicClick = { topic ->
                                    when {
                                        topic.contains("Connection", ignoreCase = true) -> navigationActions.navigateToNetworkDiscovery()
                                        topic.contains("Calibration", ignoreCase = true) -> navigationActions.navigateToCalibration()
                                        topic.contains("Gesture", ignoreCase = true) -> navigationActions.navigateToGestureStudio()
                                        topic.contains("Voice", ignoreCase = true) -> navigationActions.navigateToVoiceCommands()
                                        topic.contains("Accessibility", ignoreCase = true) -> navigationActions.navigateToAccessibility()
                                        topic.contains("Troubleshooting", ignoreCase = true) -> viewModel.selectCategory(HelpCategory.TROUBLESHOOTING)
                                        else -> viewModel.selectCategory(HelpCategory.ALL)
                                    }
                                }
                            )
                        }
                    }

                    item {
                        Spacer(modifier = Modifier.height(32.dp))
                    }
                }
            }
        }
    }

    if (showContactDialog) {
        ContactDialog(
            onDismiss = { showContactDialog = false },
            onEmail = { sendEmail("support@airmouse.io", "Air Mouse Pro Support") },
            onDiscord = { openUrl("https://discord.gg/airmouse") },
            onGithub = { openUrl("https://github.com/airmouse") },
            onWebsite = { openUrl("https://www.airmouse.io") }
        )
    }

    if (showFeedbackDialog) {
        FeedbackDialog(
            onDismiss = { showFeedbackDialog = false },
            onSubmit = { rating, message ->
                val body = buildString {
                    appendLine("Rating: $rating/5")
                    appendLine()
                    appendLine(message.ifBlank { "No message provided." })
                }
                sendEmail("support@airmouse.io", "Air Mouse Pro Feedback", body)
            }
        )
    }
}

@Composable
fun HelpSectionCard(
    section: HelpSection,
    isExpanded: Boolean,
    isFavorite: Boolean,
    onToggle: () -> Unit,
    onToggleFavorite: () -> Unit,
    onRelatedTopicClick: (String) -> Unit
) {
    val context = LocalContext.current
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() }
                    .padding(16.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 4.dp)
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
                            IconButton(
                                onClick = {
                                    android.widget.Toast.makeText(context, "Thanks for the feedback", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ThumbUp, contentDescription = "Yes", modifier = Modifier.size(18.dp))
                            }
                            IconButton(
                                onClick = {
                                    android.widget.Toast.makeText(context, "We’ll improve this article", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                modifier = Modifier.size(32.dp)
                            ) {
                                Icon(Icons.Default.ThumbDown, contentDescription = "No", modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyHelpState(onClearFilters: () -> Unit) {
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
                imageVector = Icons.AutoMirrored.Filled.Help,
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
            OutlinedButton(onClick = onClearFilters) {
                Text("Clear All Filters")
            }
        }
    }
}

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
                    icon = Icons.AutoMirrored.Filled.Chat,
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
            TextButton(onClick = onDismiss) {
                Text("Close")
            }
        }
    )
}

@Composable
fun ContactOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = MaterialTheme.colorScheme.primary)
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium)
                Text(description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, message: String) -> Unit
) {
    var rating by remember { mutableIntStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Send Feedback") },
        text = {
            Column {
                Text("How would you rate your experience?", style = MaterialTheme.typography.bodyMedium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Rating ${index + 1}",
                                tint = if (index < rating) Color(0xFFFFC107) else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Tell us how we can improve...") },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(rating, feedbackText)
                    onDismiss()
                }
            ) {
                Text("Submit")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun getCategoryColor(category: HelpCategory): Color {
    return when (category) {
        HelpCategory.GETTING_STARTED -> Color(0xFF00BCD4)
        HelpCategory.CONNECTION -> Color(0xFF4CAF50)
        HelpCategory.MOUSE_CONTROL -> Color(0xFF3F51B5)
        HelpCategory.GESTURES -> Color(0xFFFF9800)
        HelpCategory.CALIBRATION -> Color(0xFF9C27B0)
        HelpCategory.TROUBLESHOOTING -> Color(0xFFF44336)
        HelpCategory.SERVER_SHORTCUTS -> Color(0xFF795548)
        HelpCategory.BLUETOOTH -> Color(0xFF0288D1)
        HelpCategory.BACKGROUND_MODE -> Color(0xFF00897B)
        HelpCategory.SETTINGS -> Color(0xFF546E7A)
        HelpCategory.ADVANCED -> Color(0xFF2196F3)
        HelpCategory.ACCESSIBILITY -> Color(0xFFE91E63)
        HelpCategory.FAQ -> Color(0xFF607D8B)
        else -> MaterialTheme.colorScheme.primary
    }
}
