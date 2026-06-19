package com.airmouse.presentation.ui.profiles

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.automirrored.filled.ViewList
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

// ==========================================
// DATA MODELS
// ==========================================

data class UserProfile(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val color: String = "#6366F1",
    val isDefault: Boolean = false,
    val isFavorite: Boolean = false,
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis(),
    val lastUsedAt: Long = System.currentTimeMillis(),
    val tags: List<String> = emptyList()
) {
    val formattedLastUsed: String
        get() = when {
            lastUsedAt == 0L -> "Never"
            System.currentTimeMillis() - lastUsedAt < 3600000 -> "Just now"
            System.currentTimeMillis() - lastUsedAt < 86400000 -> "Today"
            else -> SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(lastUsedAt))
        }

    val daysSinceLastUsed: Long
        get() = (System.currentTimeMillis() - lastUsedAt) / (1000 * 60 * 60 * 24)

    val isRecentlyUsed: Boolean get() = daysSinceLastUsed < 7

    val usageLabel: String get() = when {
        usageCount == 0 -> "Never used"
        usageCount == 1 -> "Used once"
        usageCount < 10 -> "Used $usageCount times"
        else -> "Used $usageCount times"
    }

    val initials: String get() {
        return name.split(" ")
            .filter { it.isNotEmpty() }
            .map { it[0].uppercase() }
            .take(2)
            .joinToString("")
    }

    fun incrementUsage(): UserProfile {
        return this.copy(
            usageCount = usageCount + 1,
            lastUsedAt = System.currentTimeMillis()
        )
    }

    fun toggleFavorite(): UserProfile {
        return this.copy(isFavorite = !isFavorite)
    }

    fun getColorInt(): Int {
        return try {
            android.graphics.Color.parseColor(color)
        } catch (e: Exception) {
            android.graphics.Color.parseColor("#6366F1")
        }
    }
}

enum class ProfileSort(val displayName: String) {
    NAME("Name"),
    DATE_CREATED("Date Created"),
    LAST_USED("Last Used"),
    FAVORITE("Favorite"),
    USAGE_COUNT("Usage Count")
}

enum class ViewMode {
    LIST, GRID, COMPACT
}

data class ProfileStats(
    val totalProfiles: Int = 0,
    val defaultProfileName: String? = null,
    val favoriteCount: Int = 0,
    val totalUsage: Int = 0,
    val mostUsedProfile: String? = null
)

// ==========================================
// MAIN SCREEN
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    viewModel: ProfilesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsState()
    val filteredProfiles by viewModel.filteredProfiles.collectAsState()
    val stats by viewModel.stats.collectAsState()

    val scope = rememberCoroutineScope()
    val snackbarHostState = remember { SnackbarHostState() }

    // Handle events
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearError()
        }
    }

    LaunchedEffect(uiState.successMessage) {
        uiState.successMessage?.let {
            snackbarHostState.showSnackbar(it, duration = SnackbarDuration.Short)
            viewModel.clearSuccess()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            ProfilesTopBar(
                sortBy = uiState.sortBy,
                viewMode = uiState.viewMode,
                onSortChange = { viewModel.setSortBy(it) },
                onViewModeChange = { viewModel.setViewMode(it) },
                onSearch = { viewModel.setSearchQuery(it) },
                onNavigateBack = { /* Navigate back */ }
            )
        },
        floatingActionButton = {
            if (!uiState.showCreateDialog) {
                FloatingActionButton(
                    onClick = { viewModel.showCreateDialog(true) },
                    containerColor = Color(0xFF6366F1),
                    contentColor = Color.White,
                    shape = CircleShape,
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.Add, contentDescription = "Create Profile")
                }
            }
        },
        containerColor = Color(0xFF0F1115)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                uiState.isLoading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = Color(0xFF6366F1))
                    }
                }
                filteredProfiles.isEmpty() && uiState.searchQuery.isNotEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.Search,
                        title = "No Results Found",
                        message = "No profiles match your search query",
                        actionText = "Clear Search",
                        onAction = { viewModel.setSearchQuery("") }
                    )
                }
                filteredProfiles.isEmpty() -> {
                    EmptyState(
                        icon = Icons.Default.PersonAdd,
                        title = "No Profiles Yet",
                        message = "Create your first profile to get started",
                        actionText = "Create Profile",
                        onAction = { viewModel.showCreateDialog(true) }
                    )
                }
                else -> {
                    ProfileListContent(
                        profiles = filteredProfiles,
                        viewMode = uiState.viewMode,
                        onProfileSelect = { viewModel.selectProfile(it.id) },
                        onToggleFavorite = { viewModel.toggleFavorite(it.id) },
                        onDelete = { viewModel.showDeleteDialog(true) },
                        onLongPress = { viewModel.selectProfile(it.id) }
                    )
                }
            }
        }
    }

    // ==========================================
    // DIALOGS
    // ==========================================

    // Create Profile Dialog
    if (uiState.showCreateDialog) {
        CreateProfileDialog(
            profileName = uiState.newProfileName,
            onNameChange = { viewModel.updateNewProfileName(it) },
            onDismiss = { viewModel.showCreateDialog(false) },
            onConfirm = {
                viewModel.createProfile(uiState.newProfileName)
                viewModel.showCreateDialog(false)
            },
            existingNames = uiState.profiles.map { it.name }
        )
    }

    // Delete Profile Dialog
    if (uiState.showDeleteDialog) {
        DeleteProfileDialog(
            profile = uiState.selectedProfile,
            onDismiss = { viewModel.showDeleteDialog(false) },
            onConfirm = {
                viewModel.deleteProfile(uiState.selectedProfile?.id ?: "")
                viewModel.showDeleteDialog(false)
            }
        )
    }

    // Edit Profile Dialog
    if (uiState.showEditDialog) {
        EditProfileDialog(
            profile = uiState.selectedProfile,
            onDismiss = { viewModel.showEditDialog(false) },
            onConfirm = { updatedProfile ->
                viewModel.updateProfile(updatedProfile)
                viewModel.showEditDialog(false)
            }
        )
    }

    // Details Dialog
    if (uiState.showDetailsDialog && uiState.selectedProfile != null) {
        ProfileDetailsDialog(
            profile = uiState.selectedProfile!!,
            onDismiss = { viewModel.showDetailsDialog(false) },
            onEdit = {
                viewModel.showDetailsDialog(false)
                viewModel.showEditDialog(true)
            },
            onToggleFavorite = {
                viewModel.toggleFavorite(uiState.selectedProfile!!.id)
                viewModel.showDetailsDialog(false)
            },
            onSetDefault = {
                viewModel.setDefaultProfile(uiState.selectedProfile!!.id)
                viewModel.showDetailsDialog(false)
            }
        )
    }
}

// ==========================================
// TOP BAR
// ==========================================

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesTopBar(
    sortBy: ProfileSort,
    viewMode: ViewMode,
    onSortChange: (ProfileSort) -> Unit,
    onViewModeChange: (ViewMode) -> Unit,
    onSearch: (String) -> Unit,
    onNavigateBack: () -> Unit
) {
    var showSortMenu by remember { mutableStateOf(false) }
    var showSearch by remember { mutableStateOf(false) }
    var searchQuery by remember { mutableStateOf("") }

    TopAppBar(
        title = {
            if (showSearch) {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = {
                        searchQuery = it
                        onSearch(it)
                    },
                    placeholder = { Text("Search profiles...", color = Color(0xFF96A0AE)) },
                    singleLine = true,
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF2B3341),
                        focusedPlaceholderColor = Color(0xFF96A0AE)
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                    keyboardActions = KeyboardActions(onSearch = { showSearch = false })
                )
            } else {
                Text(
                    "User Profiles",
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    fontSize = 20.sp
                )
            }
        },
        navigationIcon = {
            IconButton(onClick = { if (showSearch) showSearch = false else onNavigateBack() }) {
                Icon(
                    if (showSearch) Icons.Default.Close else Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = if (showSearch) "Close Search" else "Back",
                    tint = Color.White
                )
            }
        },
        actions = {
            // Search Button
            IconButton(onClick = {
                showSearch = !showSearch
                if (!showSearch) {
                    searchQuery = ""
                    onSearch("")
                }
            }) {
                Icon(
                    Icons.Default.Search,
                    contentDescription = "Search",
                    tint = Color.White
                )
            }

            // View Mode Switcher
            IconButton(onClick = {
                val newMode = when (viewMode) {
                    ViewMode.LIST -> ViewMode.GRID
                    ViewMode.GRID -> ViewMode.COMPACT
                    ViewMode.COMPACT -> ViewMode.LIST
                }
                onViewModeChange(newMode)
            }) {
                Icon(
                    imageVector = when (viewMode) {
                        ViewMode.LIST -> Icons.Default.ViewModule
                        ViewMode.GRID -> Icons.Default.ViewStream
                        ViewMode.COMPACT -> Icons.AutoMirrored.Filled.ViewList
                    },
                    contentDescription = "Change View Mode",
                    tint = Color.White
                )
            }

            // Sort Button
            IconButton(onClick = { showSortMenu = true }) {
                Icon(
                    Icons.AutoMirrored.Filled.Sort,
                    contentDescription = "Sort Profiles",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF1A1D24)
        )
    )

    // Sort Dropdown
    DropdownMenu(
        expanded = showSortMenu,
        onDismissRequest = { showSortMenu = false },
        modifier = Modifier.background(Color(0xFF1A1D24))
    ) {
        ProfileSort.entries.forEach { sortOption ->
            DropdownMenuItem(
                text = {
                    Text(
                        sortOption.displayName,
                        color = if (sortBy == sortOption) Color(0xFF6366F1) else Color.White
                    )
                },
                onClick = {
                    onSortChange(sortOption)
                    showSortMenu = false
                }
            )
        }
    }
}

// ==========================================
// PROFILE LIST CONTENT
// ==========================================

@Composable
fun ProfileListContent(
    profiles: List<UserProfile>,
    viewMode: ViewMode,
    onProfileSelect: (UserProfile) -> Unit,
    onToggleFavorite: (UserProfile) -> Unit,
    onDelete: (UserProfile) -> Unit,
    onLongPress: (UserProfile) -> Unit
) {
    AnimatedContent(
        targetState = viewMode,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        },
        label = "ViewModeAnimation"
    ) { mode ->
        when (mode) {
            ViewMode.LIST -> {
                LazyColumn(
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileListCard(
                            profile = profile,
                            onSelect = { onProfileSelect(profile) },
                            onToggleFavorite = { onToggleFavorite(profile) },
                            onDelete = { onDelete(profile) }
                        )
                    }
                }
            }
            ViewMode.GRID -> {
                LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    contentPadding = PaddingValues(16.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileGridCard(
                            profile = profile,
                            onSelect = { onProfileSelect(profile) }
                        )
                    }
                }
            }
            ViewMode.COMPACT -> {
                LazyColumn(
                    contentPadding = PaddingValues(8.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    items(profiles, key = { it.id }) { profile ->
                        ProfileCompactRow(
                            profile = profile,
                            onSelect = { onProfileSelect(profile) }
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// PROFILE CARDS
// ==========================================

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ProfileListCard(
    profile: UserProfile,
    onSelect: () -> Unit,
    onToggleFavorite: () -> Unit,
    onDelete: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .combinedClickable(
                onClick = onSelect,
                onLongClick = { if (!profile.isDefault) onDelete() }
            )
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            ProfileAvatar(
                name = profile.initials,
                color = profile.color,
                size = 48.dp
            )

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = profile.name,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (profile.isDefault) {
                        Spacer(modifier = Modifier.width(8.dp))
                        SuggestionChip(
                            onClick = {},
                            label = { Text("Active", fontSize = 10.sp, color = Color(0xFF6366F1)) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                containerColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                            ),
                            border = null,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = profile.usageLabel,
                        color = Color(0xFF96A0AE),
                        fontSize = 12.sp
                    )
                    if (profile.isRecentlyUsed) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(Color(0xFF10B981))
                        )
                        Text(
                            text = "Active",
                            color = Color(0xFF10B981),
                            fontSize = 11.sp
                        )
                    }
                }
                if (profile.tags.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(4.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        profile.tags.take(2).forEach { tag ->
                            AssistChip(
                                onClick = {},
                                label = { Text(tag, fontSize = 10.sp, color = Color(0xFF96A0AE)) },
                                colors = AssistChipDefaults.assistChipColors(
                                    containerColor = Color(0xFF2B3341)
                                ),
                                modifier = Modifier.height(20.dp)
                            )
                        }
                        if (profile.tags.size > 2) {
                            Text(
                                text = "+${profile.tags.size - 2}",
                                color = Color(0xFF96A0AE),
                                fontSize = 10.sp
                            )
                        }
                    }
                }
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (profile.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite",
                    tint = if (profile.isFavorite) Color(0xFFEC4899) else Color(0xFF96A0AE)
                )
            }
        }
    }
}

@Composable
fun ProfileGridCard(
    profile: UserProfile,
    onSelect: () -> Unit
) {
    Card(
        onClick = onSelect,
        modifier = Modifier
            .fillMaxWidth()
            .height(160.dp)
            .shadow(4.dp, RoundedCornerShape(16.dp)),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(14.dp)
                .fillMaxSize(),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(10.dp))
                        .background(Color(profile.getColorInt())),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Mouse,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                if (profile.isFavorite) {
                    Icon(
                        Icons.Default.Favorite,
                        contentDescription = null,
                        tint = Color(0xFFEC4899),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Column {
                Text(
                    text = profile.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = profile.usageLabel,
                    color = Color(0xFF96A0AE),
                    fontSize = 11.sp
                )
                if (profile.isDefault) {
                    Spacer(modifier = Modifier.height(4.dp))
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Default", fontSize = 9.sp, color = Color(0xFF6366F1)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                        ),
                        border = null,
                        modifier = Modifier.height(16.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun ProfileCompactRow(
    profile: UserProfile,
    onSelect: () -> Unit
) {
    Surface(
        onClick = onSelect,
        modifier = Modifier.fillMaxWidth(),
        color = Color(0xFF1A1D24),
        shape = RoundedCornerShape(8.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(horizontal = 12.dp, vertical = 8.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(Color(profile.getColorInt()))
            )
            Text(
                text = profile.name,
                color = if (profile.isDefault) Color(0xFF6366F1) else Color.White,
                fontWeight = if (profile.isDefault) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (profile.isDefault) {
                Icon(
                    Icons.Default.Check,
                    contentDescription = null,
                    tint = Color(0xFF6366F1),
                    modifier = Modifier.size(14.dp)
                )
            }
            if (profile.isFavorite) {
                Icon(
                    Icons.Default.Favorite,
                    contentDescription = null,
                    tint = Color(0xFFEC4899),
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = profile.usageLabel,
                color = Color(0xFF96A0AE),
                fontSize = 10.sp,
                maxLines = 1
            )
        }
    }
}

// ==========================================
// PROFILE AVATAR
// ==========================================

@Composable
fun ProfileAvatar(
    name: String,
    color: String,
    size: androidx.compose.ui.unit.Dp = 48.dp
) {
    Box(
        modifier = Modifier
            .size(size)
            .clip(CircleShape)
            .background(Color(android.graphics.Color.parseColor(color))),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = name.take(2).uppercase(),
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = size.value / 2.5.sp
        )
    }
}

// ==========================================
// DIALOGS
// ==========================================

@Composable
fun CreateProfileDialog(
    profileName: String,
    onNameChange: (String) -> Unit,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit,
    existingNames: List<String>
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D24),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.PersonAdd, contentDescription = null, tint = Color(0xFF6366F1))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Create Profile", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                Text(
                    text = "Enter a name for your new profile",
                    color = Color(0xFF96A0AE),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = profileName,
                    onValueChange = onNameChange,
                    label = { Text("Profile Name", color = Color(0xFF96A0AE)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF2B3341),
                        focusedLabelColor = Color(0xFF6366F1)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = profileName.isNotBlank() && existingNames.contains(profileName.trim())
                )
                if (profileName.isNotBlank() && existingNames.contains(profileName.trim())) {
                    Text(
                        text = "A profile with this name already exists",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                enabled = profileName.isNotBlank() && !existingNames.contains(profileName.trim())
            ) {
                Text("Create", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF96A0AE))
            }
        }
    )
}

@Composable
fun DeleteProfileDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D24),
        icon = {
            Icon(
                Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFEF4444),
                modifier = Modifier.size(48.dp)
            )
        },
        title = {
            Text(
                "Delete Profile?",
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column {
                Text(
                    text = "Are you sure you want to delete '${profile?.name ?: "this profile"}'?",
                    color = Color(0xFF96A0AE)
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "This action cannot be undone.",
                    color = Color(0xFFEF4444),
                    fontSize = 12.sp
                )
            }
        },
        confirmButton = {
            Button(
                onClick = onConfirm,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
            ) {
                Text("Delete", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF96A0AE))
            }
        }
    )
}

@Composable
fun EditProfileDialog(
    profile: UserProfile?,
    onDismiss: () -> Unit,
    onConfirm: (UserProfile) -> Unit
) {
    var name by remember { mutableStateOf(profile?.name ?: "") }
    var color by remember { mutableStateOf(profile?.color ?: "#6366F1") }
    var tags by remember { mutableStateOf(profile?.tags?.joinToString(", ") ?: "") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF1A1D24),
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Default.Edit, contentDescription = null, tint = Color(0xFF6366F1))
                Spacer(modifier = Modifier.width(8.dp))
                Text("Edit Profile", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Column {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("Name", color = Color(0xFF96A0AE)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF2B3341)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(12.dp))
                ColorPicker(
                    selectedColor = color,
                    onColorSelected = { color = it }
                )
                Spacer(modifier = Modifier.height(12.dp))
                OutlinedTextField(
                    value = tags,
                    onValueChange = { tags = it },
                    label = { Text("Tags (comma separated)", color = Color(0xFF96A0AE)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF2B3341)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    profile?.let {
                        val updatedProfile = it.copy(
                            name = name,
                            color = color,
                            tags = tags.split(",").map { it.trim() }.filter { it.isNotEmpty() }
                        )
                        onConfirm(updatedProfile)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
            ) {
                Text("Save", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color(0xFF96A0AE))
            }
        }
    )
}

@Composable
fun ProfileDetailsDialog(
    profile: UserProfile,
    onDismiss: () -> Unit,
    onEdit: () -> Unit,
    onToggleFavorite: () -> Unit,
    onSetDefault: () -> Unit
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .wrapContentHeight(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
            shape = RoundedCornerShape(24.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Avatar
                ProfileAvatar(
                    name = profile.initials,
                    color = profile.color,
                    size = 80.dp
                )

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = profile.name,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 20.sp
                )

                if (profile.isDefault) {
                    SuggestionChip(
                        onClick = {},
                        label = { Text("Default Profile", fontSize = 12.sp, color = Color(0xFF6366F1)) },
                        colors = SuggestionChipDefaults.suggestionChipColors(
                            containerColor = Color(0xFF6366F1).copy(alpha = 0.15f)
                        )
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color(0xFF2B3341))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    ProfileStatItem(
                        icon = Icons.Default.Timer,
                        value = profile.usageCount.toString(),
                        label = "Uses"
                    )
                    ProfileStatItem(
                        icon = Icons.Default.CalendarToday,
                        value = profile.formattedLastUsed,
                        label = "Last Used"
                    )
                    ProfileStatItem(
                        icon = Icons.Default.Favorite,
                        value = if (profile.isFavorite) "Yes" else "No",
                        label = "Favorite"
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))

                Divider(color = Color(0xFF2B3341))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = onEdit,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = onToggleFavorite,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (profile.isFavorite) Color(0xFFEC4899) else Color(0xFF2B3341)
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(
                            if (profile.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    if (!profile.isDefault) {
                        Button(
                            onClick = onSetDefault,
                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF10B981)),
                            modifier = Modifier.weight(1f)
                        ) {
                            Icon(Icons.Default.Check, contentDescription = null, modifier = Modifier.size(18.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text("Default")
                        }
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                TextButton(onClick = onDismiss) {
                    Text("Close", color = Color(0xFF96A0AE))
                }
            }
        }
    }
}

@Composable
fun ProfileStatItem(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    value: String,
    label: String
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = null, tint = Color(0xFF6366F1), modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = value,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 14.sp
        )
        Text(
            text = label,
            color = Color(0xFF96A0AE),
            fontSize = 11.sp
        )
    }
}

// ==========================================
// COLOR PICKER
// ==========================================

@Composable
fun ColorPicker(
    selectedColor: String,
    onColorSelected: (String) -> Unit
) {
    val colors = listOf(
        "#6366F1", "#8B5CF6", "#A855F7", "#D946EF",
        "#EC4899", "#F43F5E", "#EF4444", "#F97316",
        "#F59E0B", "#EAB308", "#84CC16", "#22C55E",
        "#10B981", "#14B8A6", "#06B6D4", "#0EA5E9",
        "#3B82F6", "#6366F1", "#8B5CF6", "#A855F7"
    )

    Column {
        Text(
            text = "Profile Color",
            color = Color(0xFF96A0AE),
            fontSize = 12.sp,
            modifier = Modifier.padding(bottom = 8.dp)
        )

        FlowRow(
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            colors.forEach { color ->
                val isSelected = color == selectedColor
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(Color(android.graphics.Color.parseColor(color)))
                        .clickable { onColorSelected(color) }
                        .border(
                            width = if (isSelected) 3.dp else 0.dp,
                            color = Color.White,
                            shape = CircleShape
                        )
                ) {
                    if (isSelected) {
                        Box(
                            modifier = Modifier
                                .size(36.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.2f))
                        )
                    }
                }
            }
        }
    }
}

// ==========================================
// EMPTY STATE
// ==========================================

@Composable
fun EmptyState(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    message: String,
    actionText: String,
    onAction: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = Color(0xFF6366F1),
            modifier = Modifier.size(64.dp)
        )
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = title,
            color = Color.White,
            fontWeight = FontWeight.Bold,
            fontSize = 20.sp
        )
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            text = message,
            color = Color(0xFF96A0AE),
            fontSize = 14.sp,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onAction,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
        ) {
            Text(actionText, color = Color.White)
        }
    }
}

// ==========================================
// STATS BAR
// ==========================================

@Composable
fun StatsBar(
    stats: ProfileStats,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24)),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatsBarItem(
                value = stats.totalProfiles.toString(),
                label = "Profiles",
                color = Color(0xFF6366F1)
            )
            StatsBarItem(
                value = stats.favoriteCount.toString(),
                label = "Favorites",
                color = Color(0xFFEC4899)
            )
            StatsBarItem(
                value = stats.totalUsage.toString(),
                label = "Total Uses",
                color = Color(0xFF10B981)
            )
            StatsBarItem(
                value = stats.mostUsedProfile?.take(2) ?: "-",
                label = "Best Profile",
                color = Color(0xFFF59E0B)
            )
        }
    }
}

@Composable
fun StatsBarItem(
    value: String,
    label: String,
    color: Color
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            color = color,
            fontWeight = FontWeight.Bold,
            fontSize = 16.sp
        )
        Text(
            text = label,
            color = Color(0xFF96A0AE),
            fontSize = 10.sp
        )
    }
}

// ==========================================
// VIEW MODEL (Example)
// ==========================================

class ProfilesViewModel @Inject constructor(
    private val profileRepository: IProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    private val _filteredProfiles = MutableStateFlow<List<UserProfile>>(emptyList())
    val filteredProfiles: StateFlow<List<UserProfile>> = _filteredProfiles.asStateFlow()

    private val _stats = MutableStateFlow(ProfileStats())
    val stats: StateFlow<ProfileStats> = _stats.asStateFlow()

    private val allProfiles = mutableListOf<UserProfile>()

    init {
        loadProfiles()
    }

    fun loadProfiles() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val profiles = profileRepository.getAllProfiles()
                allProfiles.clear()
                allProfiles.addAll(profiles.map { mapToUiProfile(it) })

                // Calculate stats
                val default = profiles.find { it.isDefault }
                val favorites = profiles.filter { it.isFavorite }
                val totalUsage = profiles.sumOf { it.usageCount }
                val mostUsed = profiles.maxByOrNull { it.usageCount }

                _stats.value = ProfileStats(
                    totalProfiles = profiles.size,
                    defaultProfileName = default?.name,
                    favoriteCount = favorites.size,
                    totalUsage = totalUsage,
                    mostUsedProfile = mostUsed?.name
                )

                applyFilters()
                _uiState.value = _uiState.value.copy(isLoading = false)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message
                )
            }
        }
    }

    fun setSortBy(sortBy: ProfileSort) {
        _uiState.value = _uiState.value.copy(sortBy = sortBy)
        applyFilters()
    }

    fun setViewMode(viewMode: ViewMode) {
        _uiState.value = _uiState.value.copy(viewMode = viewMode)
    }

    fun setSearchQuery(query: String) {
        _uiState.value = _uiState.value.copy(searchQuery = query)
        applyFilters()
    }

    private fun applyFilters() {
        val state = _uiState.value
        var filtered = allProfiles.toList()

        // Apply search filter
        if (state.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(state.searchQuery, ignoreCase = true) ||
                        it.tags.any { tag -> tag.contains(state.searchQuery, ignoreCase = true) }
            }
        }

        // Apply sort
        filtered = when (state.sortBy) {
            ProfileSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            ProfileSort.DATE_CREATED -> filtered.sortedByDescending { it.createdAt }
            ProfileSort.LAST_USED -> filtered.sortedByDescending { it.lastUsedAt }
            ProfileSort.FAVORITE -> filtered.sortedByDescending { it.isFavorite }
            ProfileSort.USAGE_COUNT -> filtered.sortedByDescending { it.usageCount }
        }

        _filteredProfiles.value = filtered
    }

    fun createProfile(name: String) {
        viewModelScope.launch {
            try {
                val profile = UserProfile(name = name)
                val entity = mapToEntity(profile)
                profileRepository.createProfile(entity)

                // If first profile, make it default
                if (allProfiles.isEmpty()) {
                    profileRepository.setDefaultProfile(entity.id)
                }

                loadProfiles()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Profile '$name' created successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                val entity = mapToEntity(profile)
                profileRepository.updateProfile(entity)
                loadProfiles()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Profile updated successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(id)

                // If deleted profile was default, set another as default
                val remaining = profileRepository.getAllProfiles()
                if (remaining.isEmpty()) {
                    // No profiles left
                } else if (remaining.none { it.isDefault }) {
                    profileRepository.setDefaultProfile(remaining.first().id)
                }

                loadProfiles()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Profile deleted successfully"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun toggleFavorite(id: String) {
        viewModelScope.launch {
            try {
                profileRepository.toggleFavorite(id)
                loadProfiles()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun setDefaultProfile(id: String) {
        viewModelScope.launch {
            try {
                profileRepository.setDefaultProfile(id)
                loadProfiles()
                _uiState.value = _uiState.value.copy(
                    successMessage = "Default profile updated"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message
                )
            }
        }
    }

    fun selectProfile(id: String) {
        val profile = allProfiles.find { it.id == id }
        profile?.let {
            _uiState.value = _uiState.value.copy(
                selectedProfile = profile,
                showDetailsDialog = true
            )
        }
    }

    fun showCreateDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(
            showCreateDialog = show,
            newProfileName = ""
        )
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDeleteDialog = show)
    }

    fun showEditDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showEditDialog = show)
    }

    fun showDetailsDialog(show: Boolean) {
        _uiState.value = _uiState.value.copy(showDetailsDialog = show)
    }

    fun updateNewProfileName(name: String) {
        _uiState.value = _uiState.value.copy(newProfileName = name)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun clearSuccess() {
        _uiState.value = _uiState.value.copy(successMessage = null)
    }

    // ==========================================
    // MAPPING FUNCTIONS
    // ==========================================

    private fun mapToUiProfile(entity: ProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            name = entity.name,
            color = "#6366F1",
            isDefault = entity.isDefault,
            isFavorite = entity.isFavorite,
            usageCount = 0,
            createdAt = entity.createdAt,
            lastUsedAt = entity.lastUsed,
            tags = entity.tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList()
        )
    }

    private fun mapToEntity(profile: UserProfile): ProfileEntity {
        return ProfileEntity(
            id = profile.id,
            name = profile.name,
            email = "",
            avatarUri = null,
            sensitivity = 1.0f,
            clickThreshold = 5.0f,
            doubleClickInterval = 400L,
            scrollThreshold = 8.0f,
            rightClickTilt = 45f,
            hapticEnabled = true,
            theme = "dark",
            aiSmoothing = false,
            predictiveMovement = true,
            invertX = false,
            invertY = false,
            accelerationEnabled = true,
            smoothingEnabled = true,
            edgeGesturesEnabled = false,
            voiceCommandsEnabled = false,
            isDefault = profile.isDefault,
            isFavorite = profile.isFavorite,
            tags = profile.tags.joinToString(","),
            iconRes = 0,
            createdAt = profile.createdAt,
            lastUsed = profile.lastUsedAt
        )
    }
}

// ==========================================
// UI STATE
// ==========================================

data class ProfileUiState(
    val profiles: List<UserProfile> = emptyList(),
    val filteredProfiles: List<UserProfile> = emptyList(),
    val selectedProfile: UserProfile? = null,
    val sortBy: ProfileSort = ProfileSort.NAME,
    val viewMode: ViewMode = ViewMode.LIST,
    val searchQuery: String = "",
    val isLoading: Boolean = false,
    val showCreateDialog: Boolean = false,
    val showDeleteDialog: Boolean = false,
    val showEditDialog: Boolean = false,
    val showDetailsDialog: Boolean = false,
    val newProfileName: String = "",
    val errorMessage: String? = null,
    val successMessage: String? = null
)
