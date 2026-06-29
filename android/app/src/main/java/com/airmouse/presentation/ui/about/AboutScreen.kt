// app/src/main/java/com/airmouse/presentation/ui/about/AboutScreen.kt
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
import androidx.compose.ui.platform.LocalContext
import android.content.Intent
import android.net.Uri
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigationActions: NavigationActions,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val effect by viewModel.effect.collectAsStateWithLifecycle()
    val context = LocalContext.current

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

    fun shareApp() {
        runCatching {
            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                type = "text/plain"
                putExtra(Intent.EXTRA_SUBJECT, "Air Mouse Pro")
                putExtra(Intent.EXTRA_TEXT, "Try Air Mouse Pro: https://github.com/airmouse")
            }
            context.startActivity(Intent.createChooser(shareIntent, "Share Air Mouse Pro"))
        }.onFailure {
            android.widget.Toast.makeText(context, "Sharing is unavailable", android.widget.Toast.LENGTH_SHORT).show()
        }
    }

    LaunchedEffect(effect) {
        when (val currentEffect = effect) {
            is AboutEffect.OpenUrl -> {
                openUrl(currentEffect.url)
            }
            is AboutEffect.ShowToast -> {
                android.widget.Toast.makeText(context, currentEffect.message, android.widget.Toast.LENGTH_SHORT).show()
            }
            is AboutEffect.NavigateBack -> {
                navigationActions.navigateBack()
            }
            is AboutEffect.ShowUpdateDialog -> {
                android.widget.Toast.makeText(context, "Update available", android.widget.Toast.LENGTH_SHORT).show()
            }
            null -> { /* No effect */ }
        }
        if (effect != null) {
            viewModel.clearEffect()
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "logoScale")
    val logoScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "scale"
    )

    var showContributors by remember { mutableStateOf(false) }
    var showLicenses by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("About Air Mouse", fontWeight = FontWeight.SemiBold, fontSize = 20.sp) },
                navigationIcon = {
                    IconButton(onClick = { viewModel.onEvent(AboutEvent.NavigateBack) }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { shareApp() }) {
                        Icon(Icons.Default.Share, contentDescription = "Share")
                    }
                    IconButton(onClick = { openUrl("market://details?id=com.airmouse.app") }) {
                        Icon(Icons.Default.Star, contentDescription = "Rate")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface
                )
            )
        },
        floatingActionButton = {
            AnimatedVisibility(
                visible = uiState.isUpdateAvailable,
                enter = scaleIn() + fadeIn(),
                exit = scaleOut() + fadeOut()
            ) {
                ExtendedFloatingActionButton(
                    onClick = { viewModel.onEvent(AboutEvent.CheckForUpdates) },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary,
                    icon = { Icon(Icons.Default.SystemUpdate, contentDescription = "Update") },
                    text = { Text("Update Available", fontWeight = FontWeight.Bold) }
                )
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.3f),
                            MaterialTheme.colorScheme.background,
                            MaterialTheme.colorScheme.background
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Hero Section
                Spacer(modifier = Modifier.height(16.dp))
                HeroSection(
                    appName = uiState.appName,
                    versionName = uiState.versionName,
                    versionCode = uiState.versionCode,
                    buildDate = uiState.buildDate,
                    logoScale = logoScale
                )

                Spacer(modifier = Modifier.height(40.dp))

                // Stats Row
                StatsSection(uiState)

                Spacer(modifier = Modifier.height(24.dp))

                // Premium Expandable Sections
                var expandedSection by remember { mutableStateOf<String?>("Features") }

                PremiumExpandableCard(
                    title = "✨ Core Features",
                    isExpanded = expandedSection == "Features",
                    onClick = { expandedSection = if (expandedSection == "Features") null else "Features" }
                ) {
                    FeatureGrid()
                }

                Spacer(modifier = Modifier.height(16.dp))

                PremiumExpandableCard(
                    title = "🛠️ Technology Stack",
                    isExpanded = expandedSection == "Tech",
                    onClick = { expandedSection = if (expandedSection == "Tech") null else "Tech" }
                ) {
                    TechStackList()
                }

                Spacer(modifier = Modifier.height(16.dp))

                PremiumExpandableCard(
                    title = "👥 Meet the Team",
                    isExpanded = expandedSection == "Team",
                    onClick = { expandedSection = if (expandedSection == "Team") null else "Team" }
                ) {
                    TeamList(showContributors = showContributors, onToggleContributors = { showContributors = !showContributors })
                }

                Spacer(modifier = Modifier.height(16.dp))

                PremiumExpandableCard(
                    title = "📚 Open Source Libraries",
                    isExpanded = expandedSection == "Libraries",
                    onClick = { expandedSection = if (expandedSection == "Libraries") null else "Libraries" }
                ) {
                    LibrariesList(showLicenses = showLicenses, onToggleLicenses = { showLicenses = !showLicenses })
                }

                Spacer(modifier = Modifier.height(16.dp))

                // Quick Links
                QuickLinksCard(onOpenUrl = ::openUrl)

                Spacer(modifier = Modifier.height(32.dp))

                // Footer
                FooterSection()
                Spacer(modifier = Modifier.height(48.dp))
            }
        }
    }
}

// ==========================================
// SUB-COMPONENTS
// ==========================================

@Composable
fun HeroSection(appName: String, versionName: String, versionCode: Int, buildDate: String, logoScale: Float) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(
            modifier = Modifier
                .size(140.dp)
                .scale(logoScale),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 12.dp
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                Color.Transparent
                            )
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.Computer,
                    contentDescription = "Logo",
                    modifier = Modifier.size(72.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(28.dp))

        Text(
            text = appName,
            fontSize = 32.sp,
            fontWeight = FontWeight.ExtraBold,
            color = MaterialTheme.colorScheme.onBackground,
            letterSpacing = (-0.5).sp
        )

        Spacer(modifier = Modifier.height(12.dp))

        Surface(
            shape = RoundedCornerShape(20.dp),
            color = MaterialTheme.colorScheme.secondaryContainer.copy(alpha = 0.7f),
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Info, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSecondaryContainer)
                Text(
                    text = "Version $versionName ($versionCode)",
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }

        if (buildDate.isNotEmpty()) {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Built on $buildDate",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
            )
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Transform your Android device into a powerful, precise wireless mouse using advanced motion sensors and AI.",
            fontSize = 15.sp,
            textAlign = TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            lineHeight = 22.sp,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
    }
}

@Composable
fun StatsSection(uiState: AboutUiState) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        StatCard(modifier = Modifier.weight(1f), value = uiState.totalDownloads.toString(), label = "Downloads", icon = Icons.Default.Download)
        StatCard(modifier = Modifier.weight(1f), value = uiState.totalUsers.toString(), label = "Users", icon = Icons.Default.People)
        StatCard(modifier = Modifier.weight(1f), value = uiState.totalGestures.toString(), label = "Gestures", icon = Icons.Default.Gesture)
    }
}

@Composable
fun StatCard(modifier: Modifier = Modifier, value: String, label: String, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(
        modifier = modifier.aspectRatio(1f),
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.3f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))
    ) {
        Column(
            modifier = Modifier.fillMaxSize().padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(icon, contentDescription = label, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(28.dp))
            Spacer(modifier = Modifier.height(12.dp))
            Text(value, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(label, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun PremiumExpandableCard(
    title: String,
    isExpanded: Boolean,
    onClick: () -> Unit,
    content: @Composable () -> Unit
) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(24.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surface.copy(alpha = 0.7f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.3f)),
        shadowElevation = if (isExpanded) 8.dp else 2.dp
    ) {
        Column(
            modifier = Modifier.padding(20.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Icon(
                    imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                    contentDescription = "Toggle",
                    tint = MaterialTheme.colorScheme.primary
                )
            }
            
            AnimatedVisibility(
                visible = isExpanded,
                enter = expandVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeIn(),
                exit = shrinkVertically(animationSpec = spring(stiffness = Spring.StiffnessLow)) + fadeOut()
            ) {
                Column {
                    Spacer(modifier = Modifier.height(20.dp))
                    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.2f))
                    Spacer(modifier = Modifier.height(20.dp))
                    content()
                }
            }
        }
    }
}

@Composable
fun FeatureGrid() {
    val features = listOf(
        "🎯" to "Motion Control" to "Move cursor by rotating phone",
        "👆" to "Click Detection" to "Quick flick for left click",
        "✌️" to "Double Click" to "Two quick flicks",
        "👉" to "Right Click" to "Hold tilt for right click",
        "📜" to "Scrolling" to "Fast vertical movement",
        "🎙️" to "Voice Cmds" to "Say 'click', 'scroll'",
        "🎨" to "Custom Gestures" to "Train your own gestures",
        "🔒" to "Proximity Lock" to "Auto-lock when away"
    )
    
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        features.chunked(2).forEach { row ->
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                row.forEach { (header, desc) ->
                    val (emoji, title) = header
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(emoji, fontSize = 18.sp)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(title, fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                        }
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(desc, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, lineHeight = 16.sp)
                    }
                }
                if (row.size == 1) Spacer(modifier = Modifier.weight(1f))
            }
        }
    }
}

@Composable
fun TechStackList() {
    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
        TechItem("Android & UI", "Kotlin, Jetpack Compose, Material 3", Color(0xFF3DDC84))
        TechItem("Architecture", "Clean Architecture, MVI, Hilt", Color(0xFF2196F3))
        TechItem("Networking", "WebSockets, TCP, UDP, OkHttp", Color(0xFF00BCD4))
        TechItem("AI & Sensors", "TensorFlow Lite, SensorFusion", Color(0xFFFF6D00))
        TechItem("Backend Server", "Go (Golang), Cross-platform", Color(0xFF00ADD8))
    }
}

@Composable
fun TechItem(title: String, subtitle: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(modifier = Modifier.size(10.dp).clip(CircleShape).background(color))
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun TeamList(showContributors: Boolean, onToggleContributors: () -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
        TeamProfile("Arian Firoozi", "Lead Developer", "Full-stack & AI/ML", true)
        TeamProfile("Arsalan Talaee", "Core Developer", "Android & Networking", true)
        
        TextButton(onClick = onToggleContributors, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (showContributors) "Hide Advisors" else "Show Advisors")
        }
        
        AnimatedVisibility(visible = showContributors) {
            Column(verticalArrangement = Arrangement.spacedBy(20.dp)) {
                TeamProfile("Dr. Mohsen Shokri", "Instructor", "Embedded Systems", false)
                TeamProfile("Dr. Mehdi Kargahi", "Instructor", "Real-time Systems", false)
                
                Surface(
                    modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                    shape = RoundedCornerShape(12.dp),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.5f)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text("University of Tehran", fontWeight = FontWeight.Bold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onPrimaryContainer)
                        Text("Faculty of Electrical & Computer Engineering", fontSize = 12.sp, color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
fun TeamProfile(name: String, role: String, focus: String, isPrimary: Boolean) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Surface(
            modifier = Modifier.size(48.dp),
            shape = CircleShape,
            color = if (isPrimary) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text(
                    text = name.take(1), 
                    fontSize = 20.sp, 
                    fontWeight = FontWeight.Bold, 
                    color = if (isPrimary) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSecondaryContainer
                )
            }
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column {
            Text(name, fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
            Text(role, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary)
            Text(focus, fontSize = 12.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun LibrariesList(showLicenses: Boolean, onToggleLicenses: () -> Unit) {
    val libs = listOf(
        "Jetpack Compose" to "UI Toolkit",
        "Kotlin Coroutines" to "Async",
        "Dagger Hilt" to "DI",
        "OkHttp" to "Network",
        "TensorFlow Lite" to "ML"
    )
    
    Column {
        libs.forEach { (name, desc) ->
            Row(
                modifier = Modifier.fillMaxWidth().padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(name, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurface)
                Text(desc, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        
        Spacer(modifier = Modifier.height(12.dp))
        TextButton(onClick = onToggleLicenses, modifier = Modifier.align(Alignment.CenterHorizontally)) {
            Text(if (showLicenses) "Hide Details" else "View License Details")
        }
        
        AnimatedVisibility(visible = showLicenses) {
            Text(
                "All libraries are licensed under Apache 2.0. Full details in the app's LICENSE file.",
                fontSize = 12.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp)
            )
        }
    }
}

@Composable
fun QuickLinksCard(onOpenUrl: (String) -> Unit) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.4f),
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = 0.2f))
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Text("🔗 Connect", fontSize = 18.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onPrimaryContainer)
            Spacer(modifier = Modifier.height(16.dp))
            
            QuickLinkItem(Icons.Default.Code, "GitHub", "View Source", { onOpenUrl("https://github.com/airmouse") })
            QuickLinkItem(Icons.Default.Language, "Website", "airmouse.io", { onOpenUrl("https://www.airmouse.io") })
            QuickLinkItem(Icons.AutoMirrored.Filled.Chat, "Discord", "Join Community", { onOpenUrl("https://discord.gg/airmouse") })
        }
    }
}

@Composable
fun QuickLinkItem(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Surface(
            modifier = Modifier.size(40.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.surface
        ) {
            Icon(icon, contentDescription = null, modifier = Modifier.padding(8.dp), tint = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(16.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, fontSize = 15.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
            Text(subtitle, fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Icon(Icons.AutoMirrored.Filled.OpenInNew, contentDescription = null, modifier = Modifier.size(16.dp), tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun FooterSection() {
    Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
        Text("MIT License", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)
        Spacer(modifier = Modifier.height(8.dp))
        Text(
            "© 2024-2025 Air Mouse Team\nMade with ❤️ at University of Tehran",
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            lineHeight = 18.sp
        )
    }
}

// ==========================================
// PREVIEWS
// ==========================================

@Preview(showBackground = true, name = "Light Mode")
@Composable
fun AboutScreenPreviewLight() {
    MaterialTheme {
        AboutScreen(
            navigationActions = object : NavigationActions {
                override fun navigateTo(route: String) = Unit
                override fun navigateBack() = Unit
                override fun navigateToHome() = Unit
                override fun navigateToSettings() = Unit
                override fun navigateToCalibration() = Unit
                override fun navigateToCalibrationResult(quality: String) = Unit
                override fun navigateToStatistics() = Unit
                override fun navigateToHelp() = Unit
                override fun navigateToAbout() = Unit
                override fun navigateToProfiles() = Unit
                override fun navigateToTouchpad() = Unit
                override fun navigateToGamingMode() = Unit
                override fun navigateToScreenMirroring() = Unit
                override fun navigateToSyncStatus() = Unit
                override fun navigateToNotificationsCenter() = Unit
                override fun navigateToGestureStudio() = Unit
                override fun navigateToNetworkDiscovery() = Unit
                override fun navigateToProximity() = Unit
                override fun navigateToVoiceCommands() = Unit
                override fun navigateToEdgeGestures() = Unit
                override fun navigateToSensorVisualizer() = Unit
                override fun navigateToServerLogs() = Unit
                override fun navigateToThemes() = Unit
                override fun navigateToBattery() = Unit
                override fun navigateToAccessibility() = Unit
                override fun navigateToOnboarding() = Unit
                override fun navigateToTouchpadSettings() = Unit
            }
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode", uiMode = android.content.res.Configuration.UI_MODE_NIGHT_YES)
@Composable
fun AboutScreenPreviewDark() {
    MaterialTheme {
        AboutScreen(
            navigationActions = object : NavigationActions {
                override fun navigateTo(route: String) = Unit
                override fun navigateBack() = Unit
                override fun navigateToHome() = Unit
                override fun navigateToSettings() = Unit
                override fun navigateToCalibration() = Unit
                override fun navigateToCalibrationResult(quality: String) = Unit
                override fun navigateToStatistics() = Unit
                override fun navigateToHelp() = Unit
                override fun navigateToAbout() = Unit
                override fun navigateToProfiles() = Unit
                override fun navigateToTouchpad() = Unit
                override fun navigateToGamingMode() = Unit
                override fun navigateToScreenMirroring() = Unit
                override fun navigateToSyncStatus() = Unit
                override fun navigateToNotificationsCenter() = Unit
                override fun navigateToGestureStudio() = Unit
                override fun navigateToNetworkDiscovery() = Unit
                override fun navigateToProximity() = Unit
                override fun navigateToVoiceCommands() = Unit
                override fun navigateToEdgeGestures() = Unit
                override fun navigateToSensorVisualizer() = Unit
                override fun navigateToServerLogs() = Unit
                override fun navigateToThemes() = Unit
                override fun navigateToBattery() = Unit
                override fun navigateToAccessibility() = Unit
                override fun navigateToOnboarding() = Unit
                override fun navigateToTouchpadSettings() = Unit
            }
        )
    }
}
