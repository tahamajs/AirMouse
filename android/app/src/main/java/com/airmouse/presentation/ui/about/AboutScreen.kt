// app/src/main/java/com/airmouse/presentation/ui/about/AboutScreen.kt
package com.airmouse.presentation.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigationActions: NavigationActions,
    viewModel: AboutViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Air Mouse Pro") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.isUpdateAvailable) {
                FloatingActionButton(
                    onClick = { /* Navigate to update screen */ },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Update", fontSize = 12.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon / Logo
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = uiState.appName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Version
            Text(
                text = "Version ${uiState.versionName} (${uiState.versionCode})",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (uiState.buildDate.isNotEmpty()) {
                Text(
                    text = "Built: ${uiState.buildDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Description Card
            InfoCard(title = "About") {
                Text(
                    text = "Turn your Android phone into a wireless mouse using motion sensors. " +
                            "Control your computer cursor with natural hand movements, gestures, and voice commands.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features Card
            InfoCard(title = "Features") {
                FeatureItem("🎯", "Motion Control", "Move cursor by rotating your phone")
                FeatureItem("👆", "Click Detection", "Quick flick for left click")
                FeatureItem("✌️", "Double Click", "Two quick flicks")
                FeatureItem("👉", "Right Click", "Hold tilt for right click")
                FeatureItem("📜", "Scrolling", "Fast vertical movement")
                FeatureItem("🎙️", "Voice Commands", "Say 'click', 'scroll', and more")
                FeatureItem("🎨", "Custom Gestures", "Train your own gestures")
                FeatureItem("🔒", "Proximity Lock", "Auto-lock when walking away")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Team Card
            InfoCard(title = "Developed by") {
                Text("Arian Firoozi", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Arsalan Talaee", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("University of Tehran", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Faculty of Electrical and Computer Engineering",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructors Card
            InfoCard(title = "Instructors") {
                Text("Dr. Mohsen Shokri", fontSize = 16.sp)
                Text("Dr. Mehdi Kargahi", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // License Card
            InfoCard(title = "License") {
                Text("MIT License", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Free to use, modify, and distribute.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Check for Updates Button
            OutlinedButton(
                onClick = { viewModel.checkForUpdates() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking...")
                } else {
                    Text("Check for Updates")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back Button
            Button(
                onClick = { navigationActions.navigateBack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Back", color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copyright
            Text(
                text = "© 2024-2025 Air Mouse Team. All rights reserved.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun FeatureItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}// app/src/main/java/com/airmouse/presentation/ui/about/AboutScreen.kt
package com.airmouse.presentation.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigationActions: NavigationActions,
    viewModel: AboutViewModel = hiltViewModel(),
    modifier: Modifier = Modifier
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Air Mouse Pro") },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer,
                    titleContentColor = MaterialTheme.colorScheme.onPrimaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.isUpdateAvailable) {
                FloatingActionButton(
                    onClick = { /* Navigate to update screen */ },
                    containerColor = MaterialTheme.colorScheme.primary
                ) {
                    Text("Update", fontSize = 12.sp)
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // App Icon / Logo
            Surface(
                modifier = Modifier.size(100.dp),
                shape = CircleShape,
                color = MaterialTheme.colorScheme.primaryContainer,
                shadowElevation = 4.dp
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        Icons.Default.Info,
                        contentDescription = "App Icon",
                        modifier = Modifier.size(60.dp),
                        tint = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // App Name
            Text(
                text = uiState.appName,
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                textAlign = TextAlign.Center
            )

            // Version
            Text(
                text = "Version ${uiState.versionName} (${uiState.versionCode})",
                fontSize = 16.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center
            )

            if (uiState.buildDate.isNotEmpty()) {
                Text(
                    text = "Built: ${uiState.buildDate}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 4.dp)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Description Card
            InfoCard(title = "About") {
                Text(
                    text = "Turn your Android phone into a wireless mouse using motion sensors. " +
                            "Control your computer cursor with natural hand movements, gestures, and voice commands.",
                    fontSize = 14.sp,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Features Card
            InfoCard(title = "Features") {
                FeatureItem("🎯", "Motion Control", "Move cursor by rotating your phone")
                FeatureItem("👆", "Click Detection", "Quick flick for left click")
                FeatureItem("✌️", "Double Click", "Two quick flicks")
                FeatureItem("👉", "Right Click", "Hold tilt for right click")
                FeatureItem("📜", "Scrolling", "Fast vertical movement")
                FeatureItem("🎙️", "Voice Commands", "Say 'click', 'scroll', and more")
                FeatureItem("🎨", "Custom Gestures", "Train your own gestures")
                FeatureItem("🔒", "Proximity Lock", "Auto-lock when walking away")
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Team Card
            InfoCard(title = "Developed by") {
                Text("Arian Firoozi", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Text("Arsalan Talaee", fontSize = 16.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Text("University of Tehran", fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(
                    "Faculty of Electrical and Computer Engineering",
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Instructors Card
            InfoCard(title = "Instructors") {
                Text("Dr. Mohsen Shokri", fontSize = 16.sp)
                Text("Dr. Mehdi Kargahi", fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // License Card
            InfoCard(title = "License") {
                Text("MIT License", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Text(
                    "Free to use, modify, and distribute.",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Check for Updates Button
            OutlinedButton(
                onClick = { viewModel.checkForUpdates() },
                modifier = Modifier.fillMaxWidth(),
                enabled = !uiState.isLoading
            ) {
                if (uiState.isLoading) {
                    CircularProgressIndicator(modifier = Modifier.size(20.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Checking...")
                } else {
                    Text("Check for Updates")
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Back Button
            Button(
                onClick = { navigationActions.navigateBack() },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
            ) {
                Text("Back", color = MaterialTheme.colorScheme.onPrimary)
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Copyright
            Text(
                text = "© 2024-2025 Air Mouse Team. All rights reserved.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(modifier = Modifier.height(32.dp))
        }
    }
}

@Composable
private fun InfoCard(
    title: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
            Spacer(modifier = Modifier.height(8.dp))
            content()
        }
    }
}

@Composable
private fun FeatureItem(
    emoji: String,
    title: String,
    description: String
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Text(text = emoji, fontSize = 20.sp, modifier = Modifier.width(32.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(text = title, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text(text = description, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}