package com.airmouse.presentation.ui.help

import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.material3.AssistChip
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults

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

// Premium background and card imports
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard

// Canvas & interactive guide imports
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.foundation.border
import androidx.compose.ui.geometry.center

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HelpScreen(
    navigationActions: NavigationActions,
    viewModel: HelpViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val filteredSections = viewModel.getFilteredSections()
    val context = LocalContext.current

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

    PremiumDashboardBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            text = "Help & Support",
                            fontWeight = FontWeight.Bold,
                            fontSize = 20.sp,
                            color = Color.White
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigationActions.navigateBack() }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(onClick = { viewModel.toggleShowFavoritesOnly() }) {
                            Icon(
                                imageVector = if (uiState.showFavoritesOnly) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Favorites",
                                tint = if (uiState.showFavoritesOnly) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { showContactDialog = true }) {
                            Icon(
                                imageVector = Icons.AutoMirrored.Filled.ContactSupport,
                                contentDescription = "Contact",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                        IconButton(onClick = { showFeedbackDialog = true }) {
                            Icon(
                                imageVector = Icons.Default.Feedback,
                                contentDescription = "Feedback",
                                tint = Color.White.copy(alpha = 0.6f)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White,
                        actionIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent,
            modifier = Modifier.fillMaxSize()
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                ParticleBackground(particleCount = 10)

                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(top = 0.dp, bottom = 32.dp, start = 16.dp, end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        HelpHeader()
                    }

                    item {
                        InteractiveWalkthroughSection()
                    }

                    item {
                        PremiumSearchBar(
                            value = uiState.searchQuery,
                            onValueChange = viewModel::updateSearchQuery,
                            onClear = { viewModel.updateSearchQuery("") }
                        )
                    }

                    item {
                        LazyRow(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(HelpCategory.entries.toTypedArray()) { category ->
                                PremiumCategoryChip(
                                    category = category,
                                    isSelected = uiState.selectedCategory == category,
                                    onClick = { viewModel.selectCategory(category) }
                                )
                            }
                        }
                    }

                    item {
                        PopularTopicsRow(onTopicClick = { viewModel.updateSearchQuery(it) })
                    }

                    if (uiState.searchQuery.isNotEmpty() || uiState.selectedCategory != HelpCategory.ALL) {
                        item {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "${filteredSections.size} results found",
                                    fontSize = 12.sp,
                                    color = Color.White.copy(alpha = 0.6f)
                                )
                                TextButton(
                                    onClick = {
                                        viewModel.updateSearchQuery("")
                                        viewModel.selectCategory(HelpCategory.ALL)
                                    },
                                    colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00E5FF))
                                ) {
                                    Text("Clear filters", fontSize = 12.sp)
                                }
                            }
                        }
                    }

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
fun InteractiveWalkthroughSection() {
    var activeTab by remember { mutableStateOf(0) }

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        cornerRadius = 24.dp,
        backgroundAlpha = 0.5f,
        borderAlpha = 0.15f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "✨ Interactive Walkthroughs",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            // Tab Selector Chips
            LazyRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                contentPadding = PaddingValues(bottom = 16.dp)
            ) {
                val tabs = listOf(
                    Triple(0, "Mouse Movement", Icons.Default.Mouse),
                    Triple(1, "Gestures Guide", Icons.Default.TouchApp),
                    Triple(2, "Calibration", Icons.Default.Sensors),
                    Triple(3, "Wireless Pairing", Icons.Default.Wifi)
                )
                items(tabs) { (index, title, icon) ->
                    val isSelected = activeTab == index
                    val tabColor = if (isSelected) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f)

                    Surface(
                        modifier = Modifier
                            .clickable { activeTab = index }
                            .animateContentSize(),
                        shape = RoundedCornerShape(12.dp),
                        color = if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.15f) else Color.White.copy(alpha = 0.05f),
                        border = BorderStroke(
                            1.dp,
                            if (isSelected) Color(0xFF00E5FF).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.1f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Icon(
                                imageVector = icon,
                                contentDescription = title,
                                tint = tabColor,
                                modifier = Modifier.size(16.dp)
                            )
                            Text(
                                text = title,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.8f),
                                maxLines = 1,
                                softWrap = false
                            )
                        }
                    }
                }
            }

            // Content Card representing the walkthrough
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(200.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color(0xFF07090E))
                    .border(1.dp, Color.White.copy(alpha = 0.08f), RoundedCornerShape(16.dp)),
                contentAlignment = Alignment.Center
            ) {
                AnimatedContent(
                    targetState = activeTab,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) togetherWith fadeOut(animationSpec = tween(300))
                    },
                    label = "WalkthroughContent"
                ) { targetState ->
                    when (targetState) {
                        0 -> MouseMovementWalkthrough()
                        1 -> GesturesWalkthrough()
                        2 -> CalibrationWalkthrough()
                        3 -> WirelessPairingWalkthrough()
                    }
                }
            }
        }
    }
}

@Composable
fun MouseMovementWalkthrough() {
    var dragOffset by remember { mutableStateOf(Offset.Zero) }
    var isDragging by remember { mutableStateOf(false) }

    // Auto-animation when not dragging
    val infiniteTransition = rememberInfiniteTransition(label = "MouseMovementAuto")
    val autoAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "AutoAngle"
    )

    val autoRadius = 35f
    val currentOffset = if (isDragging) {
        dragOffset
    } else {
        val angleRad = Math.toRadians(autoAngle.toDouble())
        Offset(
            x = (Math.cos(angleRad) * autoRadius).toFloat(),
            y = (Math.sin(angleRad) * autoRadius).toFloat()
        )
    }

    // Compute cursor position inside screen bounds
    val cursorX = currentOffset.x * 2.5f
    val cursorY = currentOffset.y * 1.8f

    // Phone tilt representation
    val tiltX = currentOffset.x / 50f
    val tiltY = currentOffset.y / 50f

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (isDragging) "🎮 Drag phone to tilt/move cursor" else "🔄 Auto-Demo: Tilting phone moves cursor",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White.copy(alpha = 0.7f),
            fontSize = 11.sp,
            modifier = Modifier.padding(bottom = 6.dp)
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(
                modifier = Modifier
                    .fillMaxSize()
                    .pointerInput(Unit) {
                        detectDragGestures(
                            onDragStart = {
                                isDragging = true
                            },
                            onDragEnd = {
                                isDragging = false
                                dragOffset = Offset.Zero
                            },
                            onDragCancel = {
                                isDragging = false
                                dragOffset = Offset.Zero
                            },
                            onDrag = { change, dragAmount ->
                                change.consume()
                                val newX = (dragOffset.x + dragAmount.x).coerceIn(-60f, 60f)
                                val newY = (dragOffset.y + dragAmount.y).coerceIn(-60f, 60f)
                                dragOffset = Offset(newX, newY)
                            }
                        )
                    }
            ) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Draw Virtual Computer Screen (on the right side)
                val monitorWidth = 140f
                val monitorHeight = 90f
                val monitorLeft = center.x + 40f
                val monitorTop = center.y - 55f

                // Monitor Bezel
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(monitorLeft, monitorTop),
                    size = Size(monitorWidth, monitorHeight),
                    cornerRadius = CornerRadius(8f, 8f),
                    style = Stroke(width = 6f)
                )
                // Monitor Inner Screen (Glassmorphic look)
                drawRoundRect(
                    color = Color(0xFF0F172A).copy(alpha = 0.8f),
                    topLeft = Offset(monitorLeft + 3f, monitorTop + 3f),
                    size = Size(monitorWidth - 6f, monitorHeight - 6f),
                    cornerRadius = CornerRadius(6f, 6f)
                )
                // Monitor Stand
                drawPath(
                    path = Path().apply {
                        moveTo(monitorLeft + monitorWidth / 2 - 15f, monitorTop + monitorHeight)
                        lineTo(monitorLeft + monitorWidth / 2 - 25f, monitorTop + monitorHeight + 20f)
                        lineTo(monitorLeft + monitorWidth / 2 + 25f, monitorTop + monitorHeight + 20f)
                        lineTo(monitorLeft + monitorWidth / 2 + 15f, monitorTop + monitorHeight)
                        close()
                    },
                    color = Color(0xFF1E293B)
                )
                // Monitor Base
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(monitorLeft + monitorWidth / 2 - 35f, monitorTop + monitorHeight + 18f),
                    size = Size(70f, 6f),
                    cornerRadius = CornerRadius(2f, 2f)
                )

                // Draw Virtual Cursor inside Monitor
                val monitorCenter = Offset(monitorLeft + monitorWidth / 2, monitorTop + monitorHeight / 2)
                val activeCursorPos = Offset(
                    (monitorCenter.x + cursorX).coerceIn(monitorLeft + 10f, monitorLeft + monitorWidth - 10f),
                    (monitorCenter.y + cursorY).coerceIn(monitorTop + 10f, monitorTop + monitorHeight - 10f)
                )

                // Glow around cursor
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.4f),
                    radius = 12f,
                    center = activeCursorPos
                )
                drawCircle(
                    color = Color(0xFF00E5FF),
                    radius = 4f,
                    center = activeCursorPos
                )

                // Draw Virtual Phone (on the left side)
                val phoneWidth = 65f
                val phoneHeight = 110f
                val phoneLeft = center.x - 130f
                val phoneTop = center.y - 55f
                val phoneCenter = Offset(phoneLeft + phoneWidth / 2, phoneTop + phoneHeight / 2)

                // We rotate the phone graphic based on tilt
                rotate(
                    degrees = tiltX * 15f,
                    pivot = phoneCenter
                ) {
                    // Phone body shadow/glow
                    drawRoundRect(
                        color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                        topLeft = Offset(phoneLeft - 4f, phoneTop - 4f),
                        size = Size(phoneWidth + 8f, phoneHeight + 8f),
                        cornerRadius = CornerRadius(12f, 12f)
                    )

                    // Phone body border
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(phoneLeft, phoneTop),
                        size = Size(phoneWidth, phoneHeight),
                        cornerRadius = CornerRadius(10f, 10f),
                        style = Stroke(width = 4f)
                    )

                    // Phone body inner
                    drawRoundRect(
                        color = Color(0xFF1E293B),
                        topLeft = Offset(phoneLeft + 2f, phoneTop + 2f),
                        size = Size(phoneWidth - 4f, phoneHeight - 4f),
                        cornerRadius = CornerRadius(8f, 8f)
                    )

                    // Phone Notch/Screen elements
                    drawRoundRect(
                        color = Color(0xFF475569),
                        topLeft = Offset(phoneLeft + phoneWidth / 2 - 12f, phoneTop + 4f),
                        size = Size(24f, 6f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )

                    // Screen center touch point showing drag/rotation
                    drawCircle(
                        color = Color(0xFF00E5FF).copy(alpha = 0.7f),
                        radius = 8f,
                        center = Offset(phoneCenter.x, phoneCenter.y + tiltY * 15f)
                    )
                }

                // Draw connection stream/beam
                drawPath(
                    path = Path().apply {
                        moveTo(phoneCenter.x + 35f, phoneCenter.y)
                        quadraticTo(
                            (phoneCenter.x + activeCursorPos.x) / 2,
                            (phoneCenter.y + activeCursorPos.y) / 2 - 20f,
                            activeCursorPos.x,
                            activeCursorPos.y
                        )
                    },
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    style = Stroke(
                        width = 3f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(15f, 15f), autoAngle * 1.5f)
                    )
                )
            }
        }
    }
}

@Composable
fun GesturesWalkthrough() {
    var selectedGesture by remember { mutableStateOf(0) } // 0: Left Click, 1: Right Click, 2: Scroll, 3: Edge Swipe
    val gestures = listOf("Left Click", "Right Click", "2-Finger Scroll", "Edge Swipe")

    val infiniteTransition = rememberInfiniteTransition(label = "GesturesAnim")
    val time by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "Time"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp)
    ) {
        // Sub-tabs for gestures
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 8.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            gestures.forEachIndexed { idx, name ->
                val isSel = selectedGesture == idx
                Text(
                    text = name,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isSel) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.5f),
                    modifier = Modifier
                        .clickable { selectedGesture = idx }
                        .background(
                            if (isSel) Color(0xFF00E5FF).copy(alpha = 0.12f) else Color.Transparent,
                            RoundedCornerShape(6.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                )
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Draw Touchpad Surface
                val padW = 200f
                val padH = 110f
                val padLeft = center.x - padW / 2
                val padTop = center.y - padH / 2

                // Touchpad background border
                drawRoundRect(
                    color = Color(0xFF334155),
                    topLeft = Offset(padLeft, padTop),
                    size = Size(padW, padH),
                    cornerRadius = CornerRadius(10f, 10f),
                    style = Stroke(width = 3f)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B).copy(alpha = 0.8f),
                    topLeft = Offset(padLeft + 1f, padTop + 1f),
                    size = Size(padW - 2f, padH - 2f),
                    cornerRadius = CornerRadius(9f, 9f)
                )

                // Perform drawing based on the active gesture demo selection
                when (selectedGesture) {
                    0 -> { // Left Click: Single tap in center
                        val fingerProgress = time
                        val fingerAlpha = if (fingerProgress < 0.2f) fingerProgress / 0.2f else if (fingerProgress > 0.8f) (1f - fingerProgress) / 0.2f else 1f
                        val fingerY = if (fingerProgress < 0.4f) {
                            padTop + padH / 2 + (1f - fingerProgress / 0.4f) * 20f
                        } else if (fingerProgress > 0.6f) {
                            padTop + padH / 2 + ((fingerProgress - 0.6f) / 0.4f) * 20f
                        } else {
                            padTop + padH / 2
                        }

                        // Ripple
                        if (fingerProgress >= 0.4f) {
                            val rippleProgress = ((fingerProgress - 0.4f) / 0.4f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFF00E5FF).copy(alpha = 0.5f * (1f - rippleProgress)),
                                radius = 8f + rippleProgress * 25f,
                                center = Offset(center.x, padTop + padH / 2)
                            )
                        }

                        // Finger cursor
                        drawCircle(
                            color = Color.White.copy(alpha = fingerAlpha * 0.9f),
                            radius = 12f,
                            center = Offset(center.x, fingerY)
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF).copy(alpha = fingerAlpha),
                            radius = 6f,
                            center = Offset(center.x, fingerY)
                        )
                    }
                    1 -> { // Right Click: Two fingers tapping down
                        val fingerProgress = time
                        val fingerAlpha = if (fingerProgress < 0.2f) fingerProgress / 0.2f else if (fingerProgress > 0.8f) (1f - fingerProgress) / 0.2f else 1f
                        val fingerY = if (fingerProgress < 0.4f) {
                            padTop + padH / 2 + (1f - fingerProgress / 0.4f) * 20f
                        } else if (fingerProgress > 0.6f) {
                            padTop + padH / 2 + ((fingerProgress - 0.6f) / 0.4f) * 20f
                        } else {
                            padTop + padH / 2
                        }

                        val f1x = center.x - 20f
                        val f2x = center.x + 20f

                        // Two Ripples
                        if (fingerProgress >= 0.4f) {
                            val rippleProgress = ((fingerProgress - 0.4f) / 0.4f).coerceIn(0f, 1f)
                            drawCircle(
                                color = Color(0xFFFF5722).copy(alpha = 0.5f * (1f - rippleProgress)),
                                radius = 8f + rippleProgress * 22f,
                                center = Offset(f1x, padTop + padH / 2)
                            )
                            drawCircle(
                                color = Color(0xFFFF5722).copy(alpha = 0.5f * (1f - rippleProgress)),
                                radius = 8f + rippleProgress * 22f,
                                center = Offset(f2x, padTop + padH / 2)
                            )
                        }

                        // Two Fingers
                        drawCircle(
                            color = Color.White.copy(alpha = fingerAlpha * 0.9f),
                            radius = 10f,
                            center = Offset(f1x, fingerY)
                        )
                        drawCircle(
                            color = Color(0xFFFF5722).copy(alpha = fingerAlpha),
                            radius = 5f,
                            center = Offset(f1x, fingerY)
                        )
                        drawCircle(
                            color = Color.White.copy(alpha = fingerAlpha * 0.9f),
                            radius = 10f,
                            center = Offset(f2x, fingerY)
                        )
                        drawCircle(
                            color = Color(0xFFFF5722).copy(alpha = fingerAlpha),
                            radius = 5f,
                            center = Offset(f2x, fingerY)
                        )
                    }
                    2 -> { // 2-Finger Scroll: Two fingers sliding up/down
                        val animVal = time
                        val slideDown = animVal < 0.5f
                        val segmentProgress = if (slideDown) animVal / 0.5f else (animVal - 0.5f) / 0.5f

                        val startY = if (slideDown) padTop + 25f else padTop + padH - 25f
                        val endY = if (slideDown) padTop + padH - 25f else padTop + 25f
                        val currentY = startY + (endY - startY) * segmentProgress

                        val f1x = center.x - 15f
                        val f2x = center.x + 15f

                        // Draw vertical trails
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            start = Offset(f1x, startY),
                            end = Offset(f1x, currentY),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )
                        drawLine(
                            color = Color(0xFF00E5FF).copy(alpha = 0.2f),
                            start = Offset(f2x, startY),
                            end = Offset(f2x, currentY),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        // Two Fingers
                        drawCircle(
                            color = Color.White,
                            radius = 10f,
                            center = Offset(f1x, currentY)
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 5f,
                            center = Offset(f1x, currentY)
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 10f,
                            center = Offset(f2x, currentY)
                        )
                        drawCircle(
                            color = Color(0xFF00E5FF),
                            radius = 5f,
                            center = Offset(f2x, currentY)
                        )
                    }
                    3 -> { // Edge Swipe: Swipe from left edge
                        val progress = time
                        val startX = padLeft + 2f
                        val endX = padLeft + padW * 0.45f
                        val currentX = startX + (endX - startX) * progress
                        val currentY = padTop + padH / 2

                        // Draw edge indicator glow
                        drawLine(
                            color = Color(0xFFD500F9).copy(alpha = 0.6f * (1f - progress)),
                            start = Offset(padLeft, padTop + 10f),
                            end = Offset(padLeft, padTop + padH - 10f),
                            strokeWidth = 6f,
                            cap = StrokeCap.Round
                        )

                        // Draw trail line
                        if (progress > 0.05f) {
                            drawLine(
                                color = Color(0xFFD500F9).copy(alpha = 0.3f),
                                start = Offset(startX, currentY),
                                end = Offset(currentX, currentY),
                                strokeWidth = 5f,
                                cap = StrokeCap.Round
                            )
                        }

                        // Swipe back indicator arrow
                        if (progress > 0.3f) {
                            drawCircle(
                                color = Color(0xFFD500F9).copy(alpha = 0.2f * progress),
                                radius = 20f,
                                center = Offset(padLeft + 45f, currentY)
                            )
                        }

                        // Finger
                        drawCircle(
                            color = Color.White,
                            radius = 11f,
                            center = Offset(currentX, currentY)
                        )
                        drawCircle(
                            color = Color(0xFFD500F9),
                            radius = 5.5f,
                            center = Offset(currentX, currentY)
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun CalibrationWalkthrough() {
    var isCalibrating by remember { mutableStateOf(false) }
    var calibrationProgress by remember { mutableStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = calibrationProgress,
        animationSpec = tween(durationMillis = 3000, easing = LinearEasing),
        label = "CalibrationProgress"
    )

    // Virtual device position drift animation when not calibrating
    val infiniteTransition = rememberInfiniteTransition(label = "CalibrationDrift")
    val driftX by infiniteTransition.animateFloat(
        initialValue = -18f,
        targetValue = 18f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftX"
    )
    val driftY by infiniteTransition.animateFloat(
        initialValue = 12f,
        targetValue = -12f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "DriftY"
    )

    val devicePos = if (isCalibrating) {
        val factor = 1f - animatedProgress
        Offset(driftX * factor, driftY * factor)
    } else {
        Offset(driftX, driftY)
    }

    val radarAngle by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "RadarAngle"
    )

    LaunchedEffect(animatedProgress) {
        if (animatedProgress >= 1f) {
            isCalibrating = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            text = when {
                isCalibrating && animatedProgress < 0.3f -> "📡 Initializing gyro offsets..."
                isCalibrating && animatedProgress < 0.7f -> "⚖️ Averaging accelerometer drift..."
                isCalibrating -> "⚡ Magnetometer alignment..."
                animatedProgress >= 1f -> "✅ Sensors perfectly aligned!"
                else -> "⚠️ Sensors drifted. Place flat & calibrate."
            },
            style = MaterialTheme.typography.bodySmall,
            color = if (animatedProgress >= 1f) Color(0xFF4CAF50) else Color.White.copy(alpha = 0.8f),
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp
        )

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = 55f

                // Draw target grid/bullseye
                drawCircle(
                    color = Color.White.copy(alpha = 0.05f),
                    radius = radius * 2.2f,
                    center = center
                )
                drawCircle(
                    color = Color.White.copy(alpha = 0.1f),
                    radius = radius * 1.5f,
                    center = center,
                    style = Stroke(width = 1.5f, pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f))
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.15f),
                    radius = radius,
                    center = center,
                    style = Stroke(width = 2f)
                )
                drawCircle(
                    color = Color(0xFF00E5FF).copy(alpha = 0.25f),
                    radius = 8f,
                    center = center
                )

                // Draw crosshairs
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(center.x - radius * 2.5f, center.y),
                    end = Offset(center.x + radius * 2.5f, center.y),
                    strokeWidth = 2f
                )
                drawLine(
                    color = Color.White.copy(alpha = 0.08f),
                    start = Offset(center.x, center.y - radius * 2.5f),
                    end = Offset(center.x, center.y + radius * 2.5f),
                    strokeWidth = 2f
                )

                // Radar Sweep Animation
                if (isCalibrating) {
                    rotate(degrees = radarAngle, pivot = center) {
                        drawArc(
                            color = Color(0xFF00E5FF).copy(alpha = 0.22f),
                            startAngle = 0f,
                            sweepAngle = 45f,
                            useCenter = true,
                            topLeft = Offset(center.x - radius * 1.8f, center.y - radius * 1.8f),
                            size = Size(radius * 3.6f, radius * 3.6f)
                        )
                    }

                    // Circular Progress Outer Ring
                    drawArc(
                        color = Color(0xFF00E5FF),
                        startAngle = -90f,
                        sweepAngle = animatedProgress * 360f,
                        useCenter = false,
                        topLeft = Offset(center.x - radius * 1.5f, center.y - radius * 1.5f),
                        size = Size(radius * 3f, radius * 3f),
                        style = Stroke(width = 3.5f, cap = StrokeCap.Round)
                    )
                }

                // Draw sensor offset dot
                val dotCenter = Offset(center.x + devicePos.x * 2.5f, center.y + devicePos.y * 2.5f)

                // Glow effect around the offset sensor dot
                drawCircle(
                    color = if (animatedProgress >= 1f && !isCalibrating) Color(0xFF4CAF50).copy(alpha = 0.35f) else Color(0xFFFF5722).copy(alpha = 0.3f),
                    radius = 16f,
                    center = dotCenter
                )
                drawCircle(
                    color = if (animatedProgress >= 1f && !isCalibrating) Color(0xFF4CAF50) else Color(0xFFFF5722),
                    radius = 6f,
                    center = dotCenter
                )

                // If calibration is complete, draw a large outer success ring pulsing
                if (animatedProgress >= 1f && !isCalibrating) {
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.2f),
                        radius = radius * 2f,
                        center = center,
                        style = Stroke(width = 2f)
                    )
                }
            }
        }

        // Calibration Trigger Button
        Button(
            onClick = {
                isCalibrating = true
                calibrationProgress = 1f
            },
            enabled = !isCalibrating,
            colors = ButtonDefaults.buttonColors(
                containerColor = if (animatedProgress >= 1f) Color(0xFF1B5E20) else Color(0xFF00E5FF).copy(alpha = 0.2f),
                contentColor = if (animatedProgress >= 1f) Color(0xFF81C784) else Color(0xFF00E5FF)
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier
                .height(32.dp)
                .padding(bottom = 2.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
            border = BorderStroke(1.dp, if (animatedProgress >= 1f) Color(0xFF4CAF50) else Color(0xFF00E5FF).copy(alpha = 0.6f))
        ) {
            Text(
                text = if (isCalibrating) "Calibrating..." else if (animatedProgress >= 1f) "Recalibrate Sensors" else "Calibrate",
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun WirelessPairingWalkthrough() {
    var isWifiMode by remember { mutableStateOf(true) } // true: WiFi, false: Bluetooth
    var isPairing by remember { mutableStateOf(false) }
    var isConnected by remember { mutableStateOf(false) }

    val infiniteTransition = rememberInfiniteTransition(label = "PairingWaves")
    val waveOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "WaveOffset"
    )

    LaunchedEffect(isPairing) {
        if (isPairing) {
            kotlinx.coroutines.delay(2500)
            isPairing = false
            isConnected = true
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            modifier = Modifier
                .background(Color.White.copy(alpha = 0.05f), RoundedCornerShape(8.dp))
                .padding(2.dp)
                .height(28.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (isWifiMode) Color(0xFF00E5FF).copy(alpha = 0.2f) else Color.Transparent)
                    .clickable {
                        isWifiMode = true
                        isConnected = false
                        isPairing = false
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("WiFi Connection", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (isWifiMode) Color(0xFF00E5FF) else Color.White.copy(alpha = 0.6f))
            }
            Box(
                modifier = Modifier
                    .clip(RoundedCornerShape(6.dp))
                    .background(if (!isWifiMode) Color(0xFF0288D1).copy(alpha = 0.2f) else Color.Transparent)
                    .clickable {
                        isWifiMode = false
                        isConnected = false
                        isPairing = false
                    }
                    .padding(horizontal = 12.dp, vertical = 4.dp)
            ) {
                Text("Bluetooth Mode", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (!isWifiMode) Color(0xFF0288D1) else Color.White.copy(alpha = 0.6f))
            }
        }

        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val center = Offset(size.width / 2f, size.height / 2f)

                // Draw Phone on Left
                val phoneW = 40f
                val phoneH = 70f
                val phoneL = center.x - 110f
                val phoneT = center.y - 35f
                val phoneCenter = Offset(phoneL + phoneW / 2, phoneT + phoneH / 2)

                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(phoneL, phoneT),
                    size = Size(phoneW, phoneH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 3f)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(phoneL + 1.5f, phoneT + 1.5f),
                    size = Size(phoneW - 3f, phoneH - 3f),
                    cornerRadius = CornerRadius(4.5f, 4.5f)
                )

                // Draw Computer on Right
                val compW = 85f
                val compH = 55f
                val compL = center.x + 40f
                val compT = center.y - 32f
                val compCenter = Offset(compL + compW / 2, compT + compH / 2)

                drawRoundRect(
                    color = Color(0xFF475569),
                    topLeft = Offset(compL, compT),
                    size = Size(compW, compH),
                    cornerRadius = CornerRadius(6f, 6f),
                    style = Stroke(width = 3f)
                )
                drawRoundRect(
                    color = Color(0xFF1E293B),
                    topLeft = Offset(compL + 1.5f, compT + 1.5f),
                    size = Size(compW - 3f, compH - 3f),
                    cornerRadius = CornerRadius(4.5f, 4.5f)
                )
                // stand
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(compCenter.x, compT + compH),
                    end = Offset(compCenter.x, compT + compH + 12f),
                    strokeWidth = 6f
                )
                drawLine(
                    color = Color(0xFF475569),
                    start = Offset(compCenter.x - 20f, compT + compH + 12f),
                    end = Offset(compCenter.x + 20f, compT + compH + 12f),
                    strokeWidth = 4f
                )

                // Draw signal waves or connection line
                val startX = phoneCenter.x + phoneW / 2 + 5f
                val endX = compCenter.x - compW / 2 - 5f
                val distance = endX - startX

                if (isConnected) {
                    drawLine(
                        color = Color(0xFF4CAF50),
                        start = Offset(startX, center.y),
                        end = Offset(endX, center.y),
                        strokeWidth = 4f,
                        cap = StrokeCap.Round
                    )
                    drawCircle(
                        color = Color(0xFF4CAF50).copy(alpha = 0.4f),
                        radius = 12f,
                        center = Offset(startX + distance * waveOffset, center.y)
                    )
                    drawCircle(
                        color = Color.White,
                        radius = 4f,
                        center = Offset(startX + distance * waveOffset, center.y)
                    )
                } else if (isPairing) {
                    repeat(3) { i ->
                        val progress = ((waveOffset + i / 3f) % 1.0f)
                        val x = startX + progress * distance
                        val alpha = if (progress < 0.2f) progress / 0.2f else if (progress > 0.8f) (1f - progress) / 0.2f else 1f

                        drawCircle(
                            color = if (isWifiMode) Color(0xFF00E5FF).copy(alpha = alpha * 0.4f) else Color(0xFF0288D1).copy(alpha = alpha * 0.4f),
                            radius = 6f + progress * 10f,
                            center = Offset(x, center.y)
                        )
                    }
                } else {
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(startX, center.y),
                        end = Offset(endX, center.y),
                        strokeWidth = 2.5f,
                        cap = StrokeCap.Round,
                        pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                    )
                }
            }
        }

        Button(
            onClick = {
                if (isConnected) {
                    isConnected = false
                } else {
                    isPairing = true
                }
            },
            colors = ButtonDefaults.buttonColors(
                containerColor = when {
                    isConnected -> Color(0xFF1B5E20)
                    isPairing -> Color(0xFF374151)
                    isWifiMode -> Color(0xFF00E5FF).copy(alpha = 0.2f)
                    else -> Color(0xFF0288D1).copy(alpha = 0.2f)
                },
                contentColor = when {
                    isConnected -> Color(0xFF81C784)
                    isPairing -> Color.LightGray
                    isWifiMode -> Color(0xFF00E5FF)
                    else -> Color(0xFF0288D1)
                }
            ),
            shape = RoundedCornerShape(10.dp),
            modifier = Modifier.height(32.dp).padding(bottom = 2.dp),
            contentPadding = PaddingValues(horizontal = 14.dp, vertical = 2.dp),
            border = BorderStroke(1.dp, when {
                isConnected -> Color(0xFF4CAF50)
                isPairing -> Color.Transparent
                isWifiMode -> Color(0xFF00E5FF).copy(alpha = 0.6f)
                else -> Color(0xFF0288D1).copy(alpha = 0.6f)
            })
        ) {
            Text(
                text = when {
                    isConnected -> "Connected! Disconnect?"
                    isPairing -> "Searching server..."
                    isWifiMode -> "Scan & Connect via WiFi"
                    else -> "Pair via Bluetooth"
                },
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
fun PremiumSearchBar(
    value: String,
    onValueChange: (String) -> Unit,
    onClear: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "SearchGlow")
    val borderAlpha by infiniteTransition.animateFloat(
        initialValue = 0.08f,
        targetValue = 0.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "BorderAlpha"
    )

    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        placeholder = { Text("Search help articles...", color = Color.White.copy(alpha = 0.4f), fontSize = 14.sp) },
        leadingIcon = { Icon(Icons.Default.Search, contentDescription = "Search", tint = Color(0xFF00E5FF)) },
        trailingIcon = {
            if (value.isNotEmpty()) {
                IconButton(onClick = onClear) {
                    Icon(Icons.Default.Close, contentDescription = "Clear", tint = Color.White.copy(alpha = 0.6f))
                }
            }
        },
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        singleLine = true,
        shape = RoundedCornerShape(24.dp),
        colors = OutlinedTextFieldDefaults.colors(
            focusedContainerColor = Color(0xFF13161F).copy(alpha = 0.55f),
            unfocusedContainerColor = Color(0xFF13161F).copy(alpha = 0.45f),
            focusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.4f),
            unfocusedBorderColor = Color.White.copy(alpha = borderAlpha),
            focusedTextColor = Color.White,
            unfocusedTextColor = Color.White
        )
    )
}

@Composable
fun PremiumCategoryChip(
    category: HelpCategory,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    val alphaAnim by animateFloatAsState(
        targetValue = if (isSelected) 0.25f else 0.05f,
        label = "ChipAlpha"
    )
    val glowColor = getCategoryColor(category)
    val borderAlpha by animateFloatAsState(
        targetValue = if (isSelected) 0.6f else 0.1f,
        label = "ChipBorderAlpha"
    )

    Surface(
        modifier = Modifier
            .clickable { onClick() }
            .animateContentSize(),
        shape = RoundedCornerShape(14.dp),
        color = Color(0xFF13161F).copy(alpha = 0.4f + alphaAnim),
        border = BorderStroke(
            1.dp,
            Brush.linearGradient(
                colors = listOf(
                    glowColor.copy(alpha = borderAlpha),
                    glowColor.copy(alpha = borderAlpha * 0.2f)
                )
            )
        )
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = glowColor,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = category.displayName,
                fontSize = 12.sp,
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                color = if (isSelected) Color.White else Color.White.copy(alpha = 0.65f)
            )
        }
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
    val categoryColor = getCategoryColor(section.category)

    GlassmorphicCard(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        cornerRadius = 20.dp,
        backgroundAlpha = if (isExpanded) 0.6f else 0.4f,
        borderAlpha = if (isExpanded) 0.2f else 0.1f
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onToggle() },
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.padding(bottom = 6.dp)
                    ) {
                        Text(
                            text = section.title,
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                        Surface(
                            shape = CircleShape,
                            color = categoryColor.copy(alpha = 0.15f),
                            border = BorderStroke(1.dp, categoryColor.copy(alpha = 0.4f))
                        ) {
                            Text(
                                text = section.category.displayName,
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp),
                                color = categoryColor
                            )
                        }
                    }
                    Text(
                        text = section.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = Color.White.copy(alpha = 0.7f),
                        maxLines = if (isExpanded) Int.MAX_VALUE else 2,
                        overflow = TextOverflow.Ellipsis
                    )
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    IconButton(
                        onClick = onToggleFavorite,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (isFavorite) Icons.Filled.Star else Icons.Outlined.Star,
                            contentDescription = if (isFavorite) "Remove from favorites" else "Add to favorites",
                            tint = if (isFavorite) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.4f),
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Icon(
                        imageVector = if (isExpanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (isExpanded) "Collapse" else "Expand",
                        tint = Color.White.copy(alpha = 0.5f),
                        modifier = Modifier.size(24.dp)
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
                        .padding(top = 16.dp)
                ) {
                    if (section.steps.isNotEmpty()) {
                        Text(
                            text = "📋 Steps to follow",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF00E5FF),
                            modifier = Modifier.padding(bottom = 10.dp)
                        )
                        section.steps.forEach { step ->
                            if (step.isBlank()) {
                                Spacer(modifier = Modifier.height(6.dp))
                            } else {
                                Row(
                                    modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
                                    verticalAlignment = Alignment.Top
                                ) {
                                    val stepNumberRegex = "^\\s*(\\d+)\\.\\s+(.*)".toRegex()
                                    val matchResult = stepNumberRegex.find(step)

                                    if (matchResult != null) {
                                        val (num, rest) = matchResult.destructured
                                        Box(
                                            modifier = Modifier
                                                .padding(top = 2.dp, end = 8.dp)
                                                .size(18.dp)
                                                .background(Color(0xFF00E5FF).copy(alpha = 0.2f), CircleShape)
                                                .border(1.dp, Color(0xFF00E5FF).copy(alpha = 0.4f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Text(text = num, fontSize = 9.sp, fontWeight = FontWeight.Bold, color = Color(0xFF00E5FF))
                                        }
                                        Text(
                                            text = rest,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    } else {
                                        Icon(
                                            imageVector = Icons.Default.ArrowRight,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier
                                                .size(16.dp)
                                                .padding(top = 2.dp, end = 4.dp)
                                        )
                                        Text(
                                            text = step.trimStart(' ', '•', '-'),
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.9f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    if (section.tips.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF0F172A).copy(alpha = 0.45f), RoundedCornerShape(14.dp))
                                .border(1.dp, Color.White.copy(alpha = 0.06f), RoundedCornerShape(14.dp))
                                .padding(12.dp)
                        ) {
                            Column {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(bottom = 6.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.TipsAndUpdates,
                                        contentDescription = "Tips",
                                        modifier = Modifier.size(16.dp),
                                        tint = Color(0xFFFFD54F)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Pro Tips",
                                        style = MaterialTheme.typography.titleSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = Color(0xFFFFD54F)
                                    )
                                }
                                section.tips.forEach { tip ->
                                    Row(
                                        modifier = Modifier.padding(start = 4.dp, bottom = 4.dp),
                                        verticalAlignment = Alignment.Top
                                    ) {
                                        Text("• ", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.8f))
                                        Text(
                                            text = tip,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = Color.White.copy(alpha = 0.8f),
                                            modifier = Modifier.weight(1f)
                                        )
                                    }
                                }
                            }
                        }
                    }

                    @OptIn(ExperimentalLayoutApi::class)
                    if (section.relatedTopics.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Text(
                            text = "Related Topics",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = Color.White.copy(alpha = 0.6f),
                            modifier = Modifier.padding(bottom = 6.dp)
                        )
                        FlowRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            section.relatedTopics.forEach { topic ->
                                Surface(
                                    modifier = Modifier.clickable { onRelatedTopicClick(topic) },
                                    shape = RoundedCornerShape(8.dp),
                                    color = Color.White.copy(alpha = 0.05f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                                ) {
                                    Row(
                                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.Link,
                                            contentDescription = null,
                                            tint = Color(0xFF00E5FF),
                                            modifier = Modifier.size(12.dp)
                                        )
                                        Text(topic, fontSize = 11.sp, color = Color.White.copy(alpha = 0.8f))
                                    }
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))
                    HorizontalDivider(color = Color.White.copy(alpha = 0.08f))
                    Spacer(modifier = Modifier.height(10.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text("Was this helpful?", fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            Surface(
                                modifier = Modifier.clickable {
                                    android.widget.Toast.makeText(context, "Thanks for the feedback!", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ThumbUp, contentDescription = "Yes", modifier = Modifier.size(14.dp), tint = Color(0xFF4CAF50))
                                    Text("Yes", fontSize = 11.sp, color = Color.White)
                                }
                            }
                            Surface(
                                modifier = Modifier.clickable {
                                    android.widget.Toast.makeText(context, "We'll improve this article.", android.widget.Toast.LENGTH_SHORT).show()
                                },
                                shape = RoundedCornerShape(8.dp),
                                color = Color.White.copy(alpha = 0.05f),
                                border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                                ) {
                                    Icon(Icons.Default.ThumbDown, contentDescription = "No", modifier = Modifier.size(14.dp), tint = Color(0xFFF44336))
                                    Text("No", fontSize = 11.sp, color = Color.White)
                                }
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
    GlassmorphicCard(
        modifier = Modifier.fillMaxWidth(),
        cornerRadius = 20.dp,
        backgroundAlpha = 0.4f,
        borderAlpha = 0.1f
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.Help,
                contentDescription = "No results",
                modifier = Modifier.size(56.dp),
                tint = Color.White.copy(alpha = 0.4f)
            )
            Spacer(modifier = Modifier.height(12.dp))
            Text(
                text = "No help articles found",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = "Try adjusting your search or filters",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White.copy(alpha = 0.6f),
                textAlign = TextAlign.Center
            )
            Spacer(modifier = Modifier.height(20.dp))
            Button(
                onClick = onClearFilters,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF00E5FF).copy(alpha = 0.2f),
                    contentColor = Color(0xFF00E5FF)
                ),
                shape = RoundedCornerShape(10.dp),
                border = BorderStroke(1.dp, Color(0xFF00E5FF).copy(alpha = 0.6f))
            ) {
                Text("Clear All Filters", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
        containerColor = Color(0xFF13161F).copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        title = { Text("Contact Support", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text(
                    text = "Choose how you'd like to get support:",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.padding(bottom = 16.dp)
                )
                ContactOption(
                    icon = Icons.Default.Email,
                    title = "Email Support",
                    description = "support@airmouse.io",
                    onClick = onEmail
                )
                Spacer(modifier = Modifier.height(10.dp))
                ContactOption(
                    icon = Icons.AutoMirrored.Filled.Chat,
                    title = "Discord Community",
                    description = "Join our Discord server",
                    onClick = onDiscord
                )
                Spacer(modifier = Modifier.height(10.dp))
                ContactOption(
                    icon = Icons.Default.Code,
                    title = "GitHub Issues",
                    description = "Report bugs on GitHub",
                    onClick = onGithub
                )
                Spacer(modifier = Modifier.height(10.dp))
                ContactOption(
                    icon = Icons.Default.Language,
                    title = "Website",
                    description = "www.airmouse.io",
                    onClick = onWebsite
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00E5FF))
            ) {
                Text("Close", fontWeight = FontWeight.Bold)
            }
        }
    )
}

@Composable
fun ContactOption(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(12.dp),
        color = Color.White.copy(alpha = 0.05f),
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f))
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = title, modifier = Modifier.size(24.dp), tint = Color(0xFF00E5FF))
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(title, fontWeight = FontWeight.Medium, color = Color.White)
                Text(description, fontSize = 12.sp, color = Color.White.copy(alpha = 0.6f))
            }
        }
    }
}

@Composable
fun FeedbackDialog(
    onDismiss: () -> Unit,
    onSubmit: (rating: Int, message: String) -> Unit
) {
    var rating by remember { mutableStateOf(0) }
    var feedbackText by remember { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = Color(0xFF13161F).copy(alpha = 0.95f),
        shape = RoundedCornerShape(24.dp),
        title = { Text("Send Feedback", color = Color.White, fontWeight = FontWeight.Bold) },
        text = {
            Column {
                Text("How would you rate your experience?", style = MaterialTheme.typography.bodyMedium, color = Color.White.copy(alpha = 0.7f))
                Spacer(modifier = Modifier.height(12.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center
                ) {
                    repeat(5) { index ->
                        IconButton(onClick = { rating = index + 1 }) {
                            Icon(
                                imageVector = if (index < rating) Icons.Filled.Star else Icons.Outlined.Star,
                                contentDescription = "Rating ${index + 1}",
                                tint = if (index < rating) Color(0xFFFFD54F) else Color.White.copy(alpha = 0.3f),
                                modifier = Modifier.size(32.dp)
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(16.dp))
                OutlinedTextField(
                    value = feedbackText,
                    onValueChange = { feedbackText = it },
                    placeholder = { Text("Tell us how we can improve...", color = Color.White.copy(alpha = 0.4f)) },
                    modifier = Modifier.fillMaxWidth(),
                    minLines = 3,
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White.copy(alpha = 0.03f),
                        unfocusedContainerColor = Color.Transparent,
                        focusedBorderColor = Color(0xFF00E5FF).copy(alpha = 0.5f),
                        unfocusedBorderColor = Color.White.copy(alpha = 0.12f),
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White
                    )
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    onSubmit(rating, feedbackText)
                    onDismiss()
                },
                colors = ButtonDefaults.textButtonColors(contentColor = Color(0xFF00E5FF))
            ) {
                Text("Submit", fontWeight = FontWeight.Bold)
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                colors = ButtonDefaults.textButtonColors(contentColor = Color.White.copy(alpha = 0.6f))
            ) {
                Text("Cancel")
            }
        }
    )
}

@Composable
fun getCategoryColor(category: HelpCategory): Color {
    return when (category) {
        HelpCategory.GETTING_STARTED -> Color(0xFF00E5FF)
        HelpCategory.CONNECTION -> Color(0xFF4CAF50)
        HelpCategory.MOUSE_CONTROL -> Color(0xFF2196F3)
        HelpCategory.GESTURES -> Color(0xFFFF9800)
        HelpCategory.CALIBRATION -> Color(0xFFD500F9)
        HelpCategory.TROUBLESHOOTING -> Color(0xFFF44336)
        HelpCategory.SERVER_SHORTCUTS -> Color(0xFF8D6E63)
        HelpCategory.BLUETOOTH -> Color(0xFF29B6F6)
        HelpCategory.BACKGROUND_MODE -> Color(0xFF26A69A)
        HelpCategory.SETTINGS -> Color(0xFF78909C)
        HelpCategory.ADVANCED -> Color(0xFF42A5F5)
        HelpCategory.ACCESSIBILITY -> Color(0xFFEC407A)
        HelpCategory.FAQ -> Color(0xFF90A4AE)
        HelpCategory.GAMING -> Color(0xFF7E57C2)
        HelpCategory.SCREEN_MIRRORING -> Color(0xFF26A69A)
        HelpCategory.FILE_TRANSFER -> Color(0xFFFF7043)
        HelpCategory.VOICE_COMMANDS -> Color(0xFFEC407A)
        HelpCategory.THEMES -> Color(0xFFAB47BC)
        HelpCategory.NOTIFICATIONS -> Color(0xFFFFCA28)
        HelpCategory.BATTERY -> Color(0xFF66BB6A)
        HelpCategory.PROFILES -> Color(0xFF5C6BC0)
        HelpCategory.SERVER_SETUP -> Color(0xFF78909C)
        else -> Color(0xFF00E5FF)
    }
}

@Composable
fun HelpHeader() {
    val infiniteTransition = rememberInfiniteTransition(label = "HeaderIcon")
    val scale by infiniteTransition.animateFloat(
        initialValue = 0.92f,
        targetValue = 1.08f,
        animationSpec = infiniteRepeatable(
            animation = tween(2500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ), label = "HeaderScale"
    )

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.Help,
            contentDescription = "Help",
            modifier = Modifier
                .size(56.dp)
                .scale(scale),
            tint = Color(0xFF00E5FF)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "How can we help you?",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
fun PopularTopicsRow(onTopicClick: (String) -> Unit) {
    val topics = listOf("WiFi Connection", "Calibration", "Gaming Mode", "Shortcuts", "Bluetooth")
    Column(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)) {
        Text(
            text = "Popular Topics",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.Bold,
            color = Color.White.copy(alpha = 0.7f),
            modifier = Modifier.padding(bottom = 8.dp)
        )
        LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            items(topics) { topic ->
                SuggestionChip(
                    onClick = { onTopicClick(topic) },
                    label = { Text(topic, color = Color.White.copy(alpha = 0.85f), fontSize = 11.sp, fontWeight = FontWeight.SemiBold) },
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = Color.White.copy(alpha = 0.05f),
                        labelColor = Color.White
                    ),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
                    shape = RoundedCornerShape(12.dp)
                )
            }
        }
    }
}
