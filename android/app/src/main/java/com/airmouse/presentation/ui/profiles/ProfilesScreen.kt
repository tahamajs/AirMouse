package com.airmouse.presentation.ui.profiles

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    navigationActions: NavigationActions,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredProfiles by viewModel.filteredProfiles.collectAsStateWithLifecycle()
    val configuration = LocalConfiguration.current
    val isLandscape = configuration.orientation == android.content.res.Configuration.ORIENTATION_LANDSCAPE
    val coroutineScope = rememberCoroutineScope()
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    // Sort button
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort")
                    }
                    // View mode toggle
                    IconButton(onClick = {
                        val newMode = when (uiState.viewMode) {
                            ViewMode.LIST -> ViewMode.GRID
                            ViewMode.GRID -> ViewMode.COMPACT
                            ViewMode.COMPACT -> ViewMode.LIST
                        }
                        viewModel.setViewMode(newMode)
                    }) {
                        Icon(
                            when (uiState.viewMode) {
                                ViewMode.LIST -> Icons.Default.GridView
                                ViewMode.GRID -> Icons.Default.ViewAgenda
                                ViewMode.COMPACT -> Icons.Default.List
                            },
                            contentDescription = "View mode"
                        )
                    }
                    // Export button
                    IconButton(onClick = { viewModel.showExportDialog(true) }) {
                        Icon(Icons.Default.Share, contentDescription = "Export")
                    }
                    // Import button
                    IconButton(onClick = { viewModel.showImportDialog(true) }) {
                        Icon(Icons.Default.FileUpload, contentDescription = "Import")
                    }
                    // Create button
                    IconButton(onClick = { showCreateDialog = true }) {
                        Icon(Icons.Default.Add, contentDescription = "Create")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = MaterialTheme.colorScheme.primary
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Profile")
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp)
        ) {
            // Sort dropdown
            DropdownMenu(
                expanded = showSortMenu,
                onDismissRequest = { showSortMenu = false }
            ) {
                ProfileSort.values().forEach { sortBy ->
                    DropdownMenuItem(
                        text = { Text(sortBy.displayName) },
                        onClick = {
                            viewModel.setSortBy(sortBy)
                            showSortMenu = false
                        },
                        trailingIcon = {
                            if (uiState.sortBy == sortBy) {
                                Icon(Icons.Default.Check, contentDescription = "Selected")
                            }
                        }
                    )
                }
            }

            // Create Profile Dialog
            if (showCreateDialog) {
                CreateProfileDialog(
                    onDismiss = { showCreateDialog = false },
                    onCreate = { name ->
                        viewModel.saveProfile()
                        showCreateDialog = false
                    },
                    profileName = uiState.newProfileName,
                    onNameChange = viewModel::updateNewProfileName
                )
            }

            // Search and Filter Row
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedTextField(
                    value = uiState.searchQuery,
                    onValueChange = viewModel::setSearchQuery,
                    label = { Text("Search profiles...") },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                    modifier = Modifier.weight(1f),
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp)
                )
                
                // Tag filter chip
                if (uiState.selectedTags.isNotEmpty()) {
                    AssistChip(
                        onClick = { viewModel.clearTagFilters() },
                        label = { Text("Clear filters (${uiState.selectedTags.size})") },
                        leadingIcon = { Icon(Icons.Default.Close, contentDescription = "Clear") },
                        modifier = Modifier.align(Alignment.CenterVertically)
                    )
                }
            }
            
            // Tag filter row
            AnimatedVisibility(visible = true) {
                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(vertical = 8.dp)
                ) {
                    items(uiState.availableTags) { tag ->
                        FilterChip(
                            selected = uiState.selectedTags.contains(tag),
                            onClick = { viewModel.toggleTagFilter(tag) },
                            label = { Text(tag) },
                            modifier = Modifier.animateContentSize()
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Active Profile Card
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                    Column {
                        Text(
                            text = "ACTIVE PROFILE",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.primary
                        )
                        Text(
                            text = uiState.selectedProfile?.name ?: "Default",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                        if (uiState.selectedProfile?.tags?.isNotEmpty() == true) {
                            Row(
                                modifier = Modifier.padding(top = 4.dp),
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                uiState.selectedProfile!!.tags.take(3).forEach { tag ->
                                    AssistChip(
                                        onClick = {},
                                        label = { Text(tag, fontSize = 10.sp) },
                                        modifier = Modifier.height(24.dp)
                                    )
                                }
                            }
                        }
                    }
                    if (uiState.selectedProfile?.isFavorite == true) {
                        Icon(
                            Icons.Default.Star,
                            contentDescription = "Favorite",
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset to default button
            OutlinedButton(
                onClick = { viewModel.resetToDefault() },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(Icons.Default.Restore, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Default Settings")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profiles count and info
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "${filteredProfiles.size} profiles",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "${filteredProfiles.count { it.isFavorite }} favorites",
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Profiles list/grid
            when (uiState.viewMode) {
                ViewMode.LIST -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProfiles, key = { it.id }) { profile ->
                            ProfileCard(
                                profile = profile,
                                isSelected = uiState.selectedProfile?.id == profile.id,
                                onSelect = { viewModel.selectProfile(profile) },
                                onEdit = { viewModel.showEditDialog(true) },
                                onDelete = { viewModel.showDeleteDialog(true) },
                                onFavorite = { viewModel.toggleFavorite(profile) },
                                onDuplicate = { viewModel.duplicateProfile(profile) },
                                onDetails = { viewModel.showDetailsDialog(true) }
                            )
                        }
                    }
                }
                ViewMode.GRID -> {
                    val columns = if (isLandscape) 3 else 2
                    LazyVerticalGrid(
                        columns = GridCells.Fixed(columns),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProfiles, key = { it.id }) { profile ->
                            ProfileGridItem(
                                profile = profile,
                                isSelected = uiState.selectedProfile?.id == profile.id,
                                onSelect = { viewModel.selectProfile(profile) },
                                onFavorite = { viewModel.toggleFavorite(profile) }
                            )
                        }
                    }
                }
                ViewMode.COMPACT -> {
                    LazyColumn(
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxSize()
                    ) {
                        items(filteredProfiles, key = { it.id }) { profile ->
                            ProfileCompactItem(
                                profile = profile,
                                isSelected = uiState.selectedProfile?.id == profile.id,
                                onSelect = { viewModel.selectProfile(profile) },
                                onFavorite = { viewModel.toggleFavorite(profile) }
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
        }
    }

    // Dialogs
    if (uiState.showDeleteDialog && uiState.selectedProfile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete '${uiState.selectedProfile?.name}'? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = { viewModel.deleteProfile() },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showDeleteDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showEditDialog && uiState.selectedProfile != null) {
        var editedName by remember { mutableStateOf(uiState.selectedProfile?.name ?: "") }
        var selectedColor by remember { mutableStateOf(uiState.selectedProfile?.color ?: "#6366F1") }
        val colors = listOf("#6366F1", "#10B981", "#F59E0B", "#EF4444", "#8B5CF6", "#EC4899")

        AlertDialog(
            onDismissRequest = { viewModel.showEditDialog(false) },
            title = { Text("Edit Profile") },
            text = {
                Column {
                    OutlinedTextField(
                        value = editedName,
                        onValueChange = { editedName = it },
                        label = { Text("Profile Name") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("Profile Color", style = MaterialTheme.typography.labelMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        colors.forEach { color ->
                            Box(
                                modifier = Modifier
                                    .size(40.dp)
                                    .clip(CircleShape)
                                    .background(Color(android.graphics.Color.parseColor(color)))
                                    .clickable { selectedColor = color }
                                    .then(
                                        if (selectedColor == color) Modifier
                                            .border(3.dp, MaterialTheme.colorScheme.primary, CircleShape)
                                        else Modifier
                                    )
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            viewModel.updateSelectedProfileName(editedName)
                            uiState.selectedProfile?.let { viewModel.updateProfileColor(it, selectedColor) }
                        }
                        viewModel.showEditDialog(false)
                    }
                ) {
                    Text("Save")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showEditDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showDetailsDialog && uiState.selectedProfile != null) {
        val profile = uiState.selectedProfile!!
        AlertDialog(
            onDismissRequest = { viewModel.showDetailsDialog(false) },
            title = { Text(profile.name) },
            text = {
                Column {
                    DetailRow("Created", viewModel.formatDate(profile.createdAt))
                    DetailRow("Last Used", viewModel.formatDate(profile.lastUsedAt))
                    DetailRow("Usage Count", "${profile.usageCount} times")
                    DetailRow("Sensitivity", "${profile.settings.sensitivity}")
                    DetailRow("Theme", profile.settings.theme)
                    DetailRow("AI Smoothing", if (profile.settings.aiSmoothing) "Enabled" else "Disabled")
                    DetailRow("Predictive Movement", if (profile.settings.predictiveMovement) "Enabled" else "Disabled")
                    if (profile.tags.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Tags:", style = MaterialTheme.typography.labelMedium, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(4.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                            profile.tags.forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag, fontSize = 11.sp) },
                                    modifier = Modifier.height(28.dp)
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { viewModel.showDetailsDialog(false) }) {
                    Text("Close")
                }
            }
        )
    }

    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showExportDialog(false) },
            title = { Text("Export Profiles") },
            text = { Text("Export all profiles to a JSON file? This will save all your profile settings.") },
            confirmButton = {
                TextButton(onClick = { viewModel.exportProfiles() }) {
                    Text("Export")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showExportDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    if (uiState.showImportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showImportDialog(false) },
            title = { Text("Import Profiles") },
            text = { Text("Import profiles from a JSON file. This will merge with existing profiles.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        // File picker would go here
                        viewModel.showImportDialog(false)
                    }
                ) {
                    Text("Import")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.showImportDialog(false) }) {
                    Text("Cancel")
                }
            }
        )
    }

    // Success/Error messages
    if (uiState.successMessage != null) {
        Snackbar(
            modifier = Modifier.padding(8.dp),
            shape = RoundedCornerShape(8.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onPrimary)
                }
            }
        ) {
            Text(uiState.successMessage!!)
        }
    }

    if (uiState.errorMessage != null) {
        Snackbar(
            modifier = Modifier.padding(8.dp),
            containerColor = MaterialTheme.colorScheme.error,
            shape = RoundedCornerShape(8.dp),
            action = {
                TextButton(onClick = { viewModel.clearError() }) {
                    Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                }
            }
        ) {
            Text(uiState.errorMessage!!)
        }
    }
}

@Composable
fun CreateProfileDialog(
    onDismiss: () -> Unit,
    onCreate: () -> Unit,
    profileName: String,
    onNameChange: (String) -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Create New Profile") },
        text = {
            Column {
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onNameChange,
                    label = { Text("Profile Name") },
                    placeholder = { Text("e.g., Gaming, Work, Presentation") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This will create a new profile with current settings.",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onCreate,
                enabled = profileName.isNotBlank()
            ) {
                Text("Create")
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
fun ProfileCard(
    profile: UserProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onDuplicate: () -> Unit,
    onDetails: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Profile icon with color
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(profile.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = profile.name.take(1).uppercase(),
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp
                    )
                }

                Column {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = profile.name,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        if (profile.isFavorite) {
                            Spacer(modifier = Modifier.width(4.dp))
                            Icon(
                                Icons.Default.Star,
                                contentDescription = "Favorite",
                                modifier = Modifier.size(16.dp),
                                tint = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Text(
                        text = if (profile.isDefault) "Default profile" else "Last used: ${java.text.SimpleDateFormat("MMM dd", java.util.Locale.getDefault()).format(java.util.Date(profile.lastUsedAt))}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    if (profile.tags.isNotEmpty()) {
                        Row(
                            modifier = Modifier.padding(top = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(4.dp)
                        ) {
                            profile.tags.take(2).forEach { tag ->
                                AssistChip(
                                    onClick = {},
                                    label = { Text(tag, fontSize = 10.sp) },
                                    modifier = Modifier.height(22.dp)
                                )
                            }
                            if (profile.tags.size > 2) {
                                Text("+${profile.tags.size - 2}", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!profile.isDefault) {
                    IconButton(onClick = onFavorite, modifier = Modifier.size(36.dp)) {
                        Icon(
                            if (profile.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite",
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    IconButton(onClick = onDuplicate, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onEdit, modifier = Modifier.size(36.dp)) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit", modifier = Modifier.size(20.dp))
                    }
                    IconButton(onClick = onDelete, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = "Delete",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                }
                IconButton(onClick = onDetails, modifier = Modifier.size(36.dp)) {
                    Icon(Icons.Default.Info, contentDescription = "Details", modifier = Modifier.size(20.dp))
                }
            }
        }
    }
}

@Composable
fun ProfileGridItem(
    profile: UserProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .animateContentSize(),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(profile.color))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 24.sp
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            if (profile.isFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(14.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
fun ProfileCompactItem(
    profile: UserProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onFavorite: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() }
            .background(
                if (isSelected) MaterialTheme.colorScheme.primaryContainer
                else MaterialTheme.colorScheme.surface
            )
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(32.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(profile.color))),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = profile.name.take(1).uppercase(),
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.bodyMedium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
        if (!profile.isDefault) {
            IconButton(onClick = onFavorite, modifier = Modifier.size(32.dp)) {
                Icon(
                    if (profile.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun DetailRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodySmall, fontWeight = FontWeight.Medium)
    }
}