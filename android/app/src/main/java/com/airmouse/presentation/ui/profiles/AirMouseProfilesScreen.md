# 📘 Air Mouse Profiles Screen – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.profiles` package contains the **User Profiles screen** for the Air Mouse application. This screen provides a comprehensive interface for managing multiple user profiles, each with their own settings, preferences, and customization options.

```
com.airmouse.presentation.ui.profiles/
├── ProfilesScreen.kt              # Main profiles UI
├── ProfilesViewModel.kt           # Profiles ViewModel
├── ProfilesUiState.kt             # Profiles state models
├── ProfileSettings.kt             # Profile settings data class
├── ProfileComponents.kt           # Reusable profile UI components
├── ProfileCard.kt                 # Profile card component
├── ProfileEditor.kt               # Profile editing dialog
└── ProfileSort.kt                 # Profile sorting options
```

---

## 🎯 1. ProfilesScreen – Complete Documentation

### Purpose
Provides a **comprehensive interface** for managing user profiles. Users can create, edit, delete, favorite, and switch between profiles with different sensitivity and gesture settings.

### Key Features

| Feature | Description |
|---------|-------------|
| **CRUD Operations** | Create, read, update, delete profiles |
| **View Modes** | List, Grid, Compact views |
| **Sorting** | By name, date, usage, favorites |
| **Favorites** | Mark profiles as favorites |
| **Default Profile** | Set a profile as default |
| **Usage Statistics** | Track usage count and last used |
| **Search** | Search profiles by name or tags |
| **Export/Import** | Profile data import/export |
| **Color Coding** | Each profile has a unique color |
| **Tags** | Categorize profiles with tags |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    navigationActions: NavigationActions,
    onNavigateBack: () -> Unit = { navigationActions.navigateBack() },
    viewModel: ProfilesViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredProfiles by viewModel.filteredProfiles.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            ProfilesTopBar(
                sortBy = uiState.sortBy,
                viewMode = uiState.viewMode,
                onSortChange = { viewModel.setSortBy(it) },
                onViewModeChange = { viewModel.setViewMode(it) },
                onSearch = { viewModel.setSearchQuery(it) },
                onNavigateBack = onNavigateBack
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
                    LoadingState()
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
                        onDelete = { viewModel.showDeleteDialog(true); viewModel.selectProfile(it.id) }
                    )
                }
            }
        }
    }

    // Dialogs
    if (uiState.showCreateDialog) {
        CreateProfileDialog(uiState, viewModel)
    }
    if (uiState.showDeleteDialog) {
        DeleteProfileDialog(uiState, viewModel)
    }
    if (uiState.showEditDialog) {
        EditProfileDialog(uiState, viewModel)
    }
    if (uiState.showDetailsDialog && uiState.selectedProfile != null) {
        ProfileDetailsDialog(uiState.selectedProfile!!, viewModel)
    }
}
```

---

## 🎯 2. ProfilesViewModel

### Purpose
Manages **profile state, CRUD operations, sorting, filtering, and persistence**.

### Key State Properties

| Property | Type | Description |
|----------|------|-------------|
| `uiState` | `StateFlow<ProfileUiState>` | Complete UI state |
| `filteredProfiles` | `StateFlow<List<UserProfile>>` | Filtered profile list |
| `stats` | `StateFlow<ProfileStats>` | Profile statistics |

### Key Methods

| Method | Purpose |
|--------|---------|
| `loadProfiles()` | Load all profiles from repository |
| `createProfile(name)` | Create a new profile |
| `updateProfile(profile)` | Update an existing profile |
| `deleteProfile(id)` | Delete a profile |
| `setDefaultProfile(id)` | Set a profile as default |
| `toggleFavorite(id)` | Toggle favorite status |
| `selectProfile(id)` | Select a profile for details |
| `setSortBy(sortBy)` | Set sort order |
| `setViewMode(viewMode)` | Set view mode |
| `setSearchQuery(query)` | Set search query |
| `showCreateDialog(show)` | Show/hide create dialog |
| `showDeleteDialog(show)` | Show/hide delete dialog |
| `showEditDialog(show)` | Show/hide edit dialog |
| `showDetailsDialog(show)` | Show/hide details dialog |
| `updateNewProfileName(name)` | Update new profile name |
| `clearError()` | Clear error message |
| `clearSuccess()` | Clear success message |

### Implementation

```kotlin
@HiltViewModel
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
                val entities = profileRepository.getAllProfiles()
                allProfiles.clear()
                allProfiles.addAll(entities)

                val default = entities.find { it.isDefault }
                val favorites = entities.filter { it.isFavorite }
                val totalUsage = allProfiles.sumOf { it.usageCount }
                val mostUsed = allProfiles.maxByOrNull { it.usageCount }

                _stats.value = ProfileStats(
                    totalProfiles = entities.size,
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

    fun createProfile(name: String) {
        viewModelScope.launch {
            try {
                val profile = UserProfile(name = name)
                val id = profileRepository.createProfile(profile)
                if (allProfiles.isEmpty()) {
                    profileRepository.setDefaultProfile(id)
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

    fun deleteProfile(id: String) {
        viewModelScope.launch {
            try {
                profileRepository.deleteProfile(id)
                val remaining = profileRepository.getAllProfiles()
                if (remaining.isNotEmpty() && remaining.none { it.isDefault }) {
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

    fun applyFilters() {
        var filtered = allProfiles.toList()

        if (_uiState.value.searchQuery.isNotEmpty()) {
            filtered = filtered.filter {
                it.name.contains(_uiState.value.searchQuery, ignoreCase = true) ||
                it.tags.any { tag -> tag.contains(_uiState.value.searchQuery, ignoreCase = true) }
            }
        }

        filtered = when (_uiState.value.sortBy) {
            ProfileSort.NAME -> filtered.sortedBy { it.name.lowercase() }
            ProfileSort.DATE_CREATED -> filtered.sortedByDescending { it.createdAt }
            ProfileSort.LAST_USED -> filtered.sortedByDescending { it.updatedAt }
            ProfileSort.FAVORITE -> filtered.sortedByDescending { it.isFavorite }
            ProfileSort.USAGE_COUNT -> filtered.sortedByDescending { it.usageCount }
        }

        _filteredProfiles.value = filtered
    }
}
```

---

## 🎯 3. ProfilesUiState

### Purpose
Defines the **complete state model** for the profiles screen.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `profiles` | `List<UserProfile>` | All profiles |
| `filteredProfiles` | `List<UserProfile>` | Filtered profiles |
| `selectedProfile` | `UserProfile?` | Currently selected profile |
| `sortBy` | `ProfileSort` | Current sort order |
| `viewMode` | `ViewMode` | Current view mode |
| `searchQuery` | `String` | Search query |
| `isLoading` | `Boolean` | Whether loading |
| `showCreateDialog` | `Boolean` | Show create dialog |
| `showDeleteDialog` | `Boolean` | Show delete dialog |
| `showEditDialog` | `Boolean` | Show edit dialog |
| `showDetailsDialog` | `Boolean` | Show details dialog |
| `newProfileName` | `String` | New profile name |
| `errorMessage` | `String?` | Error message |
| `successMessage` | `String?` | Success message |

### Enums

```kotlin
enum class ProfileSort(val displayName: String) {
    NAME("Name"),
    DATE_CREATED("Date Created"),
    LAST_USED("Last Used"),
    FAVORITE("Favorite"),
    USAGE_COUNT("Usage Count")
}

enum class ViewMode {
    LIST,
    GRID,
    COMPACT
}
```

---

## 🧩 4. UI Components

### ProfilesTopBar

```kotlin
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
                        unfocusedBorderColor = Color(0xFF2B3341)
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
            } else {
                Text("User Profiles", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 20.sp)
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
            IconButton(onClick = {
                showSearch = !showSearch
                if (!showSearch) {
                    searchQuery = ""
                    onSearch("")
                }
            }) {
                Icon(Icons.Default.Search, contentDescription = "Search", tint = Color.White)
            }
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
            IconButton(onClick = { showSortMenu = true }) {
                Icon(Icons.Default.Sort, contentDescription = "Sort Profiles", tint = Color.White)
            }
        }
    )

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
```

### ProfileListContent

```kotlin
@Composable
fun ProfileListContent(
    profiles: List<UserProfile>,
    viewMode: ViewMode,
    onProfileSelect: (UserProfile) -> Unit,
    onToggleFavorite: (UserProfile) -> Unit,
    onDelete: (UserProfile) -> Unit
) {
    AnimatedContent(
        targetState = viewMode,
        transitionSpec = {
            fadeIn(tween(200)) togetherWith fadeOut(tween(200))
        }
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
```

### ProfileListCard

```kotlin
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
```

### CreateProfileDialog

```kotlin
@Composable
fun CreateProfileDialog(
    uiState: ProfileUiState,
    viewModel: ProfilesViewModel
) {
    AlertDialog(
        onDismissRequest = { viewModel.showCreateDialog(false) },
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
                Text("Enter a name for your new profile", color = Color(0xFF96A0AE), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = uiState.newProfileName,
                    onValueChange = { viewModel.updateNewProfileName(it) },
                    label = { Text("Profile Name", color = Color(0xFF96A0AE)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF6366F1),
                        unfocusedBorderColor = Color(0xFF2B3341)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                    isError = uiState.newProfileName.isNotBlank() && 
                              uiState.profiles.any { it.name == uiState.newProfileName.trim() }
                )
                if (uiState.newProfileName.isNotBlank() && 
                    uiState.profiles.any { it.name == uiState.newProfileName.trim() }) {
                    Text(
                        "A profile with this name already exists",
                        color = Color(0xFFEF4444),
                        fontSize = 12.sp,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { viewModel.createProfile(uiState.newProfileName) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                enabled = uiState.newProfileName.isNotBlank() && 
                          !uiState.profiles.any { it.name == uiState.newProfileName.trim() }
            ) {
                Text("Create", color = Color.White)
            }
        },
        dismissButton = {
            TextButton(onClick = { viewModel.showCreateDialog(false) }) {
                Text("Cancel", color = Color(0xFF96A0AE))
            }
        }
    )
}
```

### ProfileDetailsDialog

```kotlin
@Composable
fun ProfileDetailsDialog(
    profile: UserProfile,
    viewModel: ProfilesViewModel
) {
    Dialog(
        onDismissRequest = { viewModel.showDetailsDialog(false) },
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

                HorizontalDivider(color = Color(0xFF2B3341))

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

                HorizontalDivider(color = Color(0xFF2B3341))

                Spacer(modifier = Modifier.height(16.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { viewModel.showEditDialog(true) },
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1)),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.Edit, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(4.dp))
                        Text("Edit")
                    }
                    Button(
                        onClick = { viewModel.toggleFavorite(profile.id) },
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
                            onClick = { viewModel.setDefaultProfile(profile.id) },
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

                TextButton(onClick = { viewModel.showDetailsDialog(false) }) {
                    Text("Close", color = Color(0xFF96A0AE))
                }
            }
        }
    }
}
```

---

## 📊 Profiles Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                        PROFILES FLOW                                   │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. LOAD PROFILES                                                      │
│     ├── Repository.getAllProfiles()                                   │
│     ├── Calculate statistics                                          │
│     ├── Apply filters                                                 │
│     └── Update UI                                                     │
│                                                                         │
│  2. CREATE PROFILE                                                     │
│     ├── Enter name                                                     │
│     ├── Repository.createProfile()                                    │
│     ├── If first profile → set as default                             │
│     └── Refresh list                                                  │
│                                                                         │
│  3. EDIT PROFILE                                                       │
│     ├── Select profile                                                │
│     ├── Modify name/color/tags                                        │
│     ├── Repository.updateProfile()                                    │
│     └── Refresh list                                                  │
│                                                                         │
│  4. DELETE PROFILE                                                     │
│     ├── Confirm deletion                                              │
│     ├── Repository.deleteProfile()                                    │
│     ├── If default → set new default                                 │
│     └── Refresh list                                                  │
│                                                                         │
│  5. FAVORITE PROFILE                                                   │
│     ├── Toggle favorite                                                │
│     ├── Repository.toggleFavorite()                                   │
│     └── Refresh list                                                  │
│                                                                         │
│  6. SET DEFAULT                                                        │
│     ├── Select profile                                                │
│     ├── Repository.setDefaultProfile()                                │
│     └── Refresh list                                                  │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📋 Profile Statistics

| Statistic | Description |
|-----------|-------------|
| `totalProfiles` | Total number of profiles |
| `defaultProfileName` | Name of the default profile |
| `favoriteCount` | Number of favorite profiles |
| `totalUsage` | Total usage count across all profiles |
| `mostUsedProfile` | Most frequently used profile |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **CRUD Operations** | Complete create, read, update, delete |
| **Multiple Views** | List, grid, compact modes |
| **Sorting & Filtering** | Sort by name, date, usage, favorites |
| **Search** | Search by name or tags |
| **Favorites** | Mark profiles as favorites |
| **Default Profile** | One profile is always default |
| **Usage Tracking** | Tracks usage count and last used |
| **Persistence** | Profiles saved to Room/Preferences |

---

**The Profiles Screen provides a comprehensive interface for managing multiple user profiles, allowing users to customize their Air Mouse experience for different scenarios and preferences.**