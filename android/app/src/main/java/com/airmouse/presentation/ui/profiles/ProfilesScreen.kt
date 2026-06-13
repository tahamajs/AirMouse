package com.airmouse.presentation.ui.profiles

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import java.util.*

// Data Classes
data class ProfileSettings(
    val sensitivity: Float = 0.5f,
    val clickThreshold: Float = 8f,
    val doubleClickInterval: Long = 300L,
    val scrollThreshold: Float = 6f,
    val rightClickTilt: Float = 45f,
    val hapticFeedback: Boolean = true,
    val aiSmoothing: Boolean = false,
    val predictiveMovement: Boolean = false,
    val invertX: Boolean = false,
    val invertY: Boolean = false,
    val accelerationEnabled: Boolean = true,
    val accelerationFactor: Float = 1.5f,
    val smoothingEnabled: Boolean = true,
    val smoothingFactor: Float = 0.5f
)

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val settings: ProfileSettings = ProfileSettings(),
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false
)

enum class ProfileSort(val displayName: String) {
    NAME("Name"),
    DATE("Date Created"),
    LAST_USED("Last Used"),
    FAVORITE("Favorite")
}

enum class ViewMode {
    LIST, GRID
}

data class ProfilesUiState(
    val profiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val newProfileName: String = "",
    val isLoading: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val isEditing: Boolean = false,
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val exportPath: String? = null,
    val searchQuery: String = "",
    val sortBy: ProfileSort = ProfileSort.NAME,
    val viewMode: ViewMode = ViewMode.LIST
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    navigationActions: NavigationActions,
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredProfiles = viewModel.getFilteredProfiles()
    var showSortMenu by remember { mutableStateOf(false) }
    var showCreateDialog by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Profiles") },
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
                        viewModel.setViewMode(if (uiState.viewMode == ViewMode.LIST) ViewMode.GRID else ViewMode.LIST)
                    }) {
                        Icon(
                            if (uiState.viewMode == ViewMode.LIST) Icons.Default.GridView else Icons.Default.List,
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
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
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

            // Search
            OutlinedTextField(
                value = uiState.searchQuery,
                onValueChange = viewModel::setSearchQuery,
                label = { Text("Search profiles...") },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search") },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Current Profile Indicator
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
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
                        Text("Active Profile", style = MaterialTheme.typography.labelMedium)
                        Text(
                            uiState.selectedProfile?.name ?: "Default",
                            style = MaterialTheme.typography.headlineSmall,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    if (uiState.selectedProfile?.isFavorite == true) {
                        Icon(Icons.Default.Star, contentDescription = "Favorite", tint = MaterialTheme.colorScheme.primary)
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Reset to default button
            OutlinedButton(
                onClick = { viewModel.resetToDefault() },
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(Icons.Default.Restore, contentDescription = "Reset")
                Spacer(modifier = Modifier.width(8.dp))
                Text("Reset to Default")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Profiles list/grid
            if (uiState.viewMode == ViewMode.LIST) {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProfiles) { profile ->
                        ProfileCard(
                            profile = profile,
                            isSelected = uiState.selectedProfile?.id == profile.id,
                            onSelect = { viewModel.selectProfile(profile) },
                            onEdit = { viewModel.showEditDialog(true) },
                            onDelete = { viewModel.showDeleteDialog(true) },
                            onFavorite = { viewModel.toggleFavorite(profile) },
                            onDuplicate = { viewModel.duplicateProfile(profile) }
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(filteredProfiles) { profile ->
                        ProfileGridItem(
                            profile = profile,
                            isSelected = uiState.selectedProfile?.id == profile.id,
                            onSelect = { viewModel.selectProfile(profile) },
                            onFavorite = { viewModel.toggleFavorite(profile) }
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Create new profile section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Create New Profile",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = uiState.newProfileName,
                        onValueChange = viewModel::updateNewProfileName,
                        label = { Text("Profile Name") },
                        placeholder = { Text("e.g., Gaming, Work, Presentation") },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true
                    )

                    Spacer(modifier = Modifier.height(12.dp))

                    Button(
                        onClick = { viewModel.saveProfile() },
                        modifier = Modifier.fillMaxWidth(),
                        enabled = uiState.newProfileName.isNotBlank()
                    ) {
                        Icon(Icons.Default.Save, contentDescription = "Save")
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Save Profile")
                    }
                }
            }

            // Success/Error messages
            if (uiState.successMessage != null) {
                Snackbar(
                    modifier = Modifier.padding(8.dp),
                    action = {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("Dismiss")
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
                    action = {
                        TextButton(onClick = { viewModel.clearErrorMessage() }) {
                            Text("Dismiss", color = MaterialTheme.colorScheme.onError)
                        }
                    }
                ) {
                    Text(uiState.errorMessage!!)
                }
            }
        }
    }

    // Dialogs
    if (uiState.showDeleteDialog && uiState.selectedProfile != null) {
        AlertDialog(
            onDismissRequest = { viewModel.showDeleteDialog(false) },
            title = { Text("Delete Profile") },
            text = { Text("Are you sure you want to delete '${uiState.selectedProfile?.name}'?") },
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

        AlertDialog(
            onDismissRequest = { viewModel.showEditDialog(false) },
            title = { Text("Edit Profile") },
            text = {
                OutlinedTextField(
                    value = editedName,
                    onValueChange = { editedName = it },
                    label = { Text("Profile Name") },
                    singleLine = true
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (editedName.isNotBlank()) {
                            viewModel.updateSelectedProfileName(editedName)
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

    if (uiState.showExportDialog) {
        AlertDialog(
            onDismissRequest = { viewModel.showExportDialog(false) },
            title = { Text("Export Profiles") },
            text = { Text("Export all profiles to JSON file?") },
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
            text = { Text("Import profiles from JSON file?") },
            confirmButton = {
                TextButton(onClick = {
                    // File picker would go here
                    viewModel.showImportDialog(false)
                }) {
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
}

@Composable
fun ProfileCard(
    profile: UserProfile,
    isSelected: Boolean,
    onSelect: () -> Unit,
    onEdit: () -> Unit,
    onDelete: () -> Unit,
    onFavorite: () -> Unit,
    onDuplicate: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                if (profile.isFavorite) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = "Favorite",
                        tint = MaterialTheme.colorScheme.primary
                    )
                }

                Column {
                    Text(
                        text = profile.name,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (profile.isDefault) {
                        Text(
                            text = "Default profile",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    } else {
                        Text(
                            text = "Created: ${java.text.SimpleDateFormat("MMM dd, yyyy", java.util.Locale.getDefault()).format(java.util.Date(profile.createdAt))}",
                            fontSize = 10.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Row(
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                if (!profile.isDefault) {
                    IconButton(onClick = onFavorite) {
                        Icon(
                            if (profile.isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = "Favorite"
                        )
                    }
                    IconButton(onClick = onDuplicate) {
                        Icon(Icons.Default.ContentCopy, contentDescription = "Duplicate")
                    }
                    IconButton(onClick = onEdit) {
                        Icon(Icons.Default.Edit, contentDescription = "Edit")
                    }
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
            .clickable { onSelect() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected)
                MaterialTheme.colorScheme.primaryContainer
            else
                MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.Person,
                contentDescription = profile.name,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = profile.name,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                overflow = androidx.compose.ui.text.style.TextOverflow.Ellipsis
            )
            if (profile.isFavorite) {
                Icon(
                    Icons.Default.Star,
                    contentDescription = "Favorite",
                    modifier = Modifier.size(16.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}