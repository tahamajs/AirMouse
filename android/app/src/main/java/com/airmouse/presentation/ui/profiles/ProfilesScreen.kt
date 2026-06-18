package com.airmouse.presentation.ui.profiles

import androidx.compose.animation.*
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    navigationActions: NavigationActions,
    modifier: Modifier = Modifier
) {
    // --- State Handlers (Mocking ViewModel state layers locally) ---
    var profilesList by remember {
        mutableStateOf(
            listOf(
                UserProfile(name = "Default Setup", isDefault = true, color = "#00BCD4"),
                UserProfile(name = "FPS Gaming Arena", isFavorite = true, color = "#9C27B0", usageCount = 42),
                UserProfile(name = "Office & Productivity", color = "#4CAF50", usageCount = 15),
                UserProfile(name = "Precise CAD Design", color = "#FF9800", usageCount = 8)
            )
        )
    }

    var selectedSort by remember { mutableStateOf(ProfileSort.LAST_USED) }
    var currentViewMode by remember { mutableStateOf(ViewMode.LIST) }
    var showCreateDialog by remember { mutableStateOf(false) }
    var newProfileName by remember { mutableStateOf("") }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        "User Profiles",
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateToHome() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back", tint = Color.White)
                    }
                },
                actions = {
                    // View Mode Switcher
                    IconButton(
                        onClick = {
                            currentViewMode = when (currentViewMode) {
                                ViewMode.LIST -> ViewMode.GRID
                                ViewMode.GRID -> ViewMode.COMPACT
                                ViewMode.COMPACT -> ViewMode.LIST
                            }
                        }
                    ) {
                        Icon(
                            imageVector = when (currentViewMode) {
                                ViewMode.LIST -> Icons.Default.ViewModule
                                ViewMode.GRID -> Icons.Default.ViewStream
                                ViewMode.COMPACT -> Icons.Default.ViewList
                            },
                            contentDescription = "Change View Mode",
                            tint = Color.White
                        )
                    }

                    // Sort Menu Button
                    var showSortMenu by remember { mutableStateOf(false) }
                    IconButton(onClick = { showSortMenu = true }) {
                        Icon(Icons.Default.Sort, contentDescription = "Sort Profiles", tint = Color.White)
                    }
                    DropdownMenu(
                        expanded = showSortMenu,
                        onDismissRequest = { showSortMenu = false },
                        modifier = Modifier.background(Color(0xFF1A1D24))
                    ) {
                        ProfileSort.values().forEach { sortOption ->
                            DropdownMenuItem(
                                text = { Text(sortOption.displayName, color = Color.White) },
                                onClick = {
                                    selectedSort = sortOption
                                    showSortMenu = false
                                    profilesList = when (sortOption) {
                                        ProfileSort.NAME -> profilesList.sortedBy { it.name }
                                        ProfileSort.DATE_CREATED -> profilesList.sortedByDescending { it.createdAt }
                                        ProfileSort.LAST_USED -> profilesList.sortedByDescending { it.lastUsedAt }
                                        ProfileSort.FAVORITE -> profilesList.sortedByDescending { it.isFavorite }
                                        ProfileSort.USAGE_COUNT -> profilesList.sortedByDescending { it.usageCount }
                                    }
                                }
                            )
                        }
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFF1A1D24))
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showCreateDialog = true },
                containerColor = Color(0xFF00BCD4),
                contentColor = Color.White,
                shape = CircleShape
            ) {
                Icon(Icons.Default.Add, contentDescription = "Create Profile")
            }
        },
        containerColor = Color(0xFF0F1115)
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            if (profilesList.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No profiles found. Tap + to create one.", color = Color(0xFF96A0AE))
                }
            } else {
                AnimatedContent(
                    targetState = currentViewMode,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "ViewModeAnimation"
                ) { viewMode ->
                    when (viewMode) {
                        ViewMode.LIST -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                items(profilesList, key = { it.id }) { profile ->
                                    ProfileListCard(
                                        profile = profile,
                                        onSelect = {
                                            profilesList = profilesList.map {
                                                it.copy(isDefault = it.id == profile.id).incrementUsage()
                                            }
                                        },
                                        onToggleFavorite = {
                                            profilesList = profilesList.map {
                                                if (it.id == profile.id) it.copy(isFavorite = !it.isFavorite) else it
                                            }
                                        },
                                        onDelete = {
                                            profilesList = profilesList.filter { it.id != profile.id }
                                        }
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
                                items(profilesList, key = { it.id }) { profile ->
                                    ProfileGridCard(
                                        profile = profile,
                                        onSelect = {
                                            profilesList = profilesList.map {
                                                it.copy(isDefault = it.id == profile.id).incrementUsage()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                        ViewMode.COMPACT -> {
                            LazyColumn(
                                contentPadding = PaddingValues(16.dp),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(profilesList, key = { it.id }) { profile ->
                                    ProfileCompactRow(
                                        profile = profile,
                                        onSelect = {
                                            profilesList = profilesList.map {
                                                it.copy(isDefault = it.id == profile.id).incrementUsage()
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }

    // --- Create Profile Dialog Dialog Builder ---
    if (showCreateDialog) {
        AlertDialog(
            onDismissRequest = { showCreateDialog = false },
            containerColor = Color(0xFF1A1D24),
            title = { Text("New Profile", color = Color.White, fontWeight = FontWeight.Bold) },
            text = {
                OutlinedTextField(
                    value = newProfileName,
                    onValueChange = { newProfileName = it },
                    label = { Text("Profile Name", color = Color(0xFF96A0AE)) },
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color(0xFF00BCD4),
                        unfocusedBorderColor = Color(0xFF2B3341)
                    ),
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                Button(
                    onClick = {
                        if (newProfileName.isNotBlank()) {
                            val colors = listOf("#6366F1", "#EC4899", "#10B981", "#F59E0B", "#3B82F6")
                            profilesList = profilesList + UserProfile(
                                name = newProfileName.trim(),
                                color = colors.random()
                            )
                            newProfileName = ""
                            showCreateDialog = false
                        }
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF00BCD4))
                ) {
                    Text("Create", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showCreateDialog = false }) {
                    Text("Cancel", color = Color(0xFF96A0AE))
                }
            }
        )
    }
}

// ==========================================
// Sub-Components & Layout Variants
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
            ),
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1A1D24))
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(profile.color))),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (profile.isDefault) Icons.Default.CheckCircle else Icons.Default.SettingsInputComponent,
                    contentDescription = null,
                    tint = Color.White
                )
            }

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
                            label = { Text("Active", fontSize = 10.sp) },
                            colors = SuggestionChipDefaults.suggestionChipColors(
                                labelColor = Color(0xFF00BCD4),
                                containerColor = Color(0xFF00BCD4).copy(alpha = 0.15f)
                            ),
                            border = null,
                            modifier = Modifier.height(20.dp)
                        )
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Used: ${profile.formattedLastUsed} • Count: ${profile.usageCount}",
                    color = Color(0xFF96A0AE),
                    fontSize = 12.sp
                )
            }

            IconButton(onClick = onToggleFavorite) {
                Icon(
                    imageVector = if (profile.isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = "Favorite Toggle",
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
            .height(140.dp),
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
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(android.graphics.Color.parseColor(profile.color))),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(Icons.Default.Mouse, contentDescription = null, tint = Color.White, modifier = Modifier.size(18.dp))
                }
                if (profile.isFavorite) {
                    Icon(Icons.Default.Favorite, contentDescription = null, tint = Color(0xFFEC4899), modifier = Modifier.size(18.dp))
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
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${profile.usageCount} uses",
                    color = Color(0xFF96A0AE),
                    fontSize = 11.sp
                )
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
            modifier = Modifier.padding(symmetric = PaddingValues(horizontal = 12.dp, vertical = 8.dp)),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(12.dp)
                    .clip(CircleShape)
                    .background(Color(android.graphics.Color.parseColor(profile.color)))
            )
            Text(
                text = profile.name,
                color = if (profile.isDefault) Color(0xFF00BCD4) else Color.White,
                fontWeight = if (profile.isDefault) FontWeight.Bold else FontWeight.Normal,
                fontSize = 14.sp,
                modifier = Modifier.weight(1f),
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (profile.isDefault) {
                Icon(Icons.Default.Check, contentDescription = null, tint = Color(0xFF00BCD4), modifier = Modifier.size(16.dp))
            }
        }
    }
}

// Extension helper function for localized compact configuration padding rules
private fun Modifier.padding(symmetric: PaddingValues): Modifier = this.padding(symmetric)