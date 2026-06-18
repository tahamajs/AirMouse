package com.airmouse.presentation.ui.about

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.Chat
import androidx.compose.material.icons.automirrored.filled.OpenInNew
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.accessibility.GlassCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigationActions: NavigationActions,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val infiniteTransition = rememberInfiniteTransition(label = "logoScale")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "scale"
    )

    var showContributors by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About", fontWeight = FontWeight.Bold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.shareApp() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { viewModel.rateApp() }) {
                        Icon(Icons.Default.Star, contentDescription = "Rate")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f)
                )
            )
        },
        floatingActionButton = {
            if (uiState.isUpdateAvailable) {
                FloatingActionButton(
                    onClick = { viewModel.checkForUpdates() },
                    containerColor = Color(0xFF4CAF50),
                    modifier = Modifier.size(56.dp)
                ) {
                    Icon(Icons.Default.SystemUpdate, contentDescription = "Update")
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primary.copy(alpha = 0.05f),
                            Color.Transparent
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    modifier = Modifier
                        .size(120.dp)
                        .scale(logoScale),
                    shape = CircleShape,
                    color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f),
                    shadowElevation = 8.dp
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            Icons.Default.Computer,
                            contentDescription = "Logo",
                            modifier = Modifier.size(60.dp),
                            tint = MaterialTheme.colorScheme.primary
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text(
                    text = uiState.appName,
                    fontSize = 28.sp,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.primary
                )

                Spacer(modifier = Modifier.height(8.dp))

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primaryContainer
                ) {
                    Text(
                        text = "Version ${uiState.versionName} (${uiState.versionCode})",
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.primary
                    )
                }

                if (uiState.buildDate.isNotEmpty()) {
                    Text(
                        text = "Built: ${uiState.buildDate}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(top = 4.dp)
                    )
                }

                Spacer(modifier = Modifier.height(32.dp))

                Text(
                    text = "Turn your Android phone into a wireless mouse using motion sensors, gestures, and voice commands.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(horizontal = 16.dp)
                )

                Spacer(modifier = Modifier.height(32.dp))

                GlassCard {
                    Column(
                        modifier = Modifier.padding(20.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("📊 Quick Stats", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            StatItem(value = uiState.totalDownloads.toString(), label = "Downloads", icon = Icons.Default.Download)
                            StatItem(value = uiState.totalUsers.toString(), label = "Active Users", icon = Icons.Default.People)
                            StatItem(value = uiState.totalGestures.toString(), label = "Gestures", icon = Icons.Default.Gesture)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("✨ Features", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        FeatureItem("🎯", "Motion Control", "Move cursor by rotating your phone")
                        FeatureItem("👆", "Click Detection", "Quick flick for left click")
                        FeatureItem("✌️", "Double Click", "Two quick flicks")
                        FeatureItem("👉", "Right Click", "Hold tilt for right click")
                        FeatureItem("📜", "Scrolling", "Fast vertical movement")
                        FeatureItem("🎙️", "Voice Commands", "Say 'click', 'scroll', and more")
                        FeatureItem("🎨", "Custom Gestures", "Train your own gestures")
                        FeatureItem("🔒", "Proximity Lock", "Auto-lock when walking away")
                        FeatureItem("🌐", "Multi-Protocol", "WebSocket, TCP, UDP support")
                        FeatureItem("📱", "Edge Gestures", "Quick actions from screen edge")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("🛠️ Tech Stack", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        TechStackItem("Android", "Kotlin, Jetpack Compose", Color(0xFF3DDC84))
                        TechStackItem("Networking", "OkHttp, WebSocket", Color(0xFF00BCD4))
                        TechStackItem("AI/ML", "TensorFlow Lite", Color(0xFFFF6D00))
                        TechStackItem("DI", "Hilt/Dagger", Color(0xFF2196F3))
                        TechStackItem("Storage", "DataStore, Room", Color(0xFF4CAF50))
                        TechStackItem("Server", "Go, WebSocket, TCP", Color(0xFF00ADD8))
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("👥 Team", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showContributors = !showContributors }) {
                                Text(if (showContributors) "Show Less" else "Show More")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        TeamMember("Arian Firoozi", "Lead Developer", "Full-stack & AI/ML")
                        TeamMember("Arsalan Talaee", "Core Developer", "Android & Networking")

                        AnimatedVisibility(visible = showContributors) {
                            Column {
                                TeamMember("Dr. Mohsen Shokri", "Instructor", "Embedded Systems")
                                TeamMember("Dr. Mehdi Kargahi", "Instructor", "Real-time Systems")
                                Spacer(modifier = Modifier.height(8.dp))
                                Text("University of Tehran", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("Faculty of Electrical and Computer Engineering", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("📚 Open Source Libraries", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                            TextButton(onClick = { showLicenses = !showLicenses }) {
                                Text(if (showLicenses) "Hide Licenses" else "View Licenses")
                            }
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        LibraryItem("Jetpack Compose", "UI Toolkit", "Apache 2.0")
                        LibraryItem("OkHttp", "Networking", "Apache 2.0")
                        LibraryItem("Coroutines", "Async Programming", "Apache 2.0")
                        LibraryItem("Hilt", "Dependency Injection", "Apache 2.0")
                        LibraryItem("TensorFlow Lite", "ML Inference", "Apache 2.0")
                        LibraryItem("Room", "Database", "Apache 2.0")
                        LibraryItem("DataStore", "Preferences", "Apache 2.0")

                        AnimatedVisibility(visible = showLicenses) {
                            Column {
                                Spacer(modifier = Modifier.height(8.dp))
                                HorizontalDivider()
                                Spacer(modifier = Modifier.height(12.dp))
                                Text(
                                    text = "Full license information available in the app's LICENSE file.",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("🔗 Links", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))

                        LinkItem("GitHub Repository", Icons.Default.Code, "github.com/airmouse") { viewModel.openUrl("https://github.com/airmouse") }
                        LinkItem("Website", Icons.Default.Language, "www.airmouse.io") { viewModel.openUrl("https://www.airmouse.io") }
                        LinkItem("Documentation", Icons.Default.Description, "docs.airmouse.io") { viewModel.openUrl("https://docs.airmouse.io") }
                        LinkItem("Support", Icons.Default.SupportAgent, "support@airmouse.io") { viewModel.openUrl("mailto:support@airmouse.io") }
                        LinkItem("Discord Community", Icons.AutoMirrored.Filled.Chat, "discord.gg/airmouse") { viewModel.openUrl("https://discord.gg/airmouse") }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                GlassCard {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Text("📄 License", fontSize = 18.sp, fontWeight = FontWeight.Bold)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("MIT License", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                        Text("Copyright (c) 2024-2025 Air Mouse Team", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Permission is hereby granted, free of charge, to any person obtaining a copy of this software...",
                            fontSize = 11.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    OutlinedButton(
                        onClick = { viewModel.checkForUpdates() },
                        modifier = Modifier.weight(1f),
                        enabled = !uiState.isLoading
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(modifier = Modifier.size(20.dp), strokeWidth = 2.dp)
                        } else {
                            Icon(Icons.Default.SystemUpdate, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Check Updates")
                        }
                    }

                    Button(
                        onClick = { navigationActions.navigateBack() },
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Back")
                    }
                }

                Spacer(modifier = Modifier.height(24.dp))

                Text("© 2024-2025 Air Mouse Team", fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center)
                Text("All rights reserved. Made with ❤️ at University of Tehran", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, textAlign = TextAlign.Center, modifier = Modifier.padding(top = 4.dp))
                Spacer(modifier = Modifier.height(32.dp))
            }
        }
    }
}

@Composable
fun StatItem(value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(24.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(value, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
        Text(label, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FeatureItem(emoji: String, title: String, description: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, fontSize = 22.sp, modifier = Modifier.width(36.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TechStackItem(tech: String, description: String, color: Color) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(modifier = Modifier.size(8.dp).clip(CircleShape).background(color))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = tech, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TeamMember(name: String, role: String, expertise: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primary.copy(alpha = 0.1f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(text = name.take(1).uppercase(), fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            }
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = role, fontSize = 12.sp, color = MaterialTheme.colorScheme.primary)
            Text(text = expertise, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LibraryItem(name: String, description: String, license: String) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = "•", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = name, fontSize = 13.sp, fontWeight = FontWeight.Medium)
            Text(text = "$description • $license", fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LinkItem(title: String, icon: androidx.compose.ui.graphics.vector.ImageVector, url: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = title, modifier = Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = url, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = "Open", modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}