package com.airmouse.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navigationActions: NavigationActions,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val animationProgress by viewModel.animationProgress.collectAsStateWithLifecycle()
    val onboardingItems = viewModel.getOnboardingItems()
    val pagerState = rememberPagerState(pageCount = { onboardingItems.size })
    val coroutineScope = rememberCoroutineScope()

    // Auto-advance animation for welcome page
    LaunchedEffect(uiState.currentPage, uiState.showWelcomeAnimation) {
        if (uiState.currentPage == 0 && uiState.showWelcomeAnimation) {
            delay(3000)
            if (uiState.currentPage == 0) {
                coroutineScope.launch {
                    pagerState.animateScrollToPage(1)
                    viewModel.nextPage()
                }
            }
        }
    }

    // Sync pager with ViewModel state
    LaunchedEffect(uiState.currentPage) {
        if (pagerState.currentPage != uiState.currentPage) {
            pagerState.scrollToPage(uiState.currentPage)
        }
    }

    Scaffold(
        containerColor = Color.Transparent
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = if (uiState.currentPage < onboardingItems.size) {
                            listOf(
                                onboardingItems[uiState.currentPage].backgroundColor,
                                onboardingItems[uiState.currentPage].backgroundColor.copy(alpha = 0.8f)
                            )
                        } else {
                            listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                        }
                    )
                )
        ) {
            AnimatedBackground(uiState.currentPage)

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                // Top bar with skip button and progress
                TopBar(
                    currentPage = uiState.currentPage,
                    totalPages = onboardingItems.size,
                    accentColor = onboardingItems[uiState.currentPage].accentColor,
                    onSkip = {
                        viewModel.skipOnboarding()
                        navigationActions.navigateToHome()
                    }
                )

                // Horizontal Pager
                HorizontalPager(
                    state = pagerState,
                    modifier = Modifier.weight(1f),
                    key = { it }
                ) { page ->
                    val item = onboardingItems[page]
                    AnimatedContent(
                        targetState = page,
                        transitionSpec = {
                            fadeIn(animationSpec = tween(400, easing = FastOutSlowInEasing)) +
                                    slideInHorizontally { width -> width } togetherWith
                                    fadeOut(animationSpec = tween(300)) +
                                    slideOutHorizontally { width -> -width }
                        },
                        label = "pager_transition"
                    ) { targetPage ->
                        when (targetPage) {
                            0 -> WelcomePage(item, animationProgress)
                            1 -> FeaturesPage(item, viewModel)
                            2 -> ConnectPage(item, viewModel)
                            3 -> VoicePage(item, viewModel)
                            4 -> ProximityPage(item, viewModel)
                            else -> FinalPage(item, viewModel, navigationActions)
                        }
                    }
                }

                // Bottom Navigation
                BottomNavigation(
                    currentPage = uiState.currentPage,
                    totalPages = onboardingItems.size,
                    accentColor = onboardingItems[uiState.currentPage].accentColor,
                    onPrevious = {
                        coroutineScope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage - 1)
                            viewModel.previousPage()
                        }
                    },
                    onNext = {
                        coroutineScope.launch {
                            if (pagerState.currentPage == onboardingItems.size - 1) {
                                viewModel.completeOnboarding()
                                navigationActions.navigateToHome()
                            } else {
                                pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                viewModel.nextPage()
                            }
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun TopBar(
    currentPage: Int,
    totalPages: Int,
    accentColor: Color,
    onSkip: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(
            modifier = Modifier.weight(1f),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            repeat(totalPages) { index ->
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .height(3.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(
                            if (index <= currentPage) accentColor
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Spacer(modifier = Modifier.width(16.dp))

        TextButton(onClick = onSkip) {
            Text("Skip", color = Color.White.copy(alpha = 0.7f))
        }
    }
}

@Composable
fun BottomNavigation(
    currentPage: Int,
    totalPages: Int,
    accentColor: Color,
    onPrevious: () -> Unit,
    onNext: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(
            modifier = Modifier.padding(bottom = 24.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            repeat(totalPages) { page ->
                val isSelected = page == currentPage
                Box(
                    modifier = Modifier
                        .size(if (isSelected) 24.dp else 8.dp, 8.dp)
                        .clip(CircleShape)
                        .animateContentSize()
                        .background(
                            if (isSelected) accentColor
                            else Color.White.copy(alpha = 0.3f)
                        )
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (currentPage > 0) {
                OutlinedButton(
                    onClick = onPrevious,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.1f),
                        contentColor = Color.White
                    )
                ) {
                    Icon(Icons.Default.ArrowBack, contentDescription = "Back", modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Back")
                }
            }

            Button(
                onClick = onNext,
                modifier = Modifier
                    .weight(1f)
                    .height(52.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = accentColor
                )
            ) {
                Text(
                    if (currentPage == totalPages - 1) "Get Started"
                    else "Next",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.SemiBold
                )
                if (currentPage != totalPages - 1) {
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(Icons.Default.ArrowForward, contentDescription = "Next", modifier = Modifier.size(18.dp))
                }
            }
        }
    }
}

@Composable
fun AnimatedBackground(currentPage: Int) {
    val infiniteTransition = rememberInfiniteTransition(label = "bg_transition")
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(20000, easing = LinearEasing)
        ),
        label = "rotation"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in 0..3) {
            Box(
                modifier = Modifier
                    .size((200 + i * 80).dp)
                    .graphicsLayer { rotationZ = rotation * (1f - i * 0.2f) }
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.05f - i * 0.01f),
                                Color.Transparent
                            ),
                            radius = 200f
                        ),
                        shape = CircleShape
                    )
                    .align(Alignment.Center)
            )
        }
    }
}

@Composable
fun WelcomePage(item: OnboardingItem, animationProgress: Float) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier
                .size(140.dp)
                .scale(0.8f + animationProgress * 0.2f),
            shape = CircleShape,
            color = item.accentColor.copy(alpha = 0.2f),
            shadowElevation = 16.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = item.imageRes),
                    contentDescription = item.title,
                    modifier = Modifier.size(80.dp),
                    tint = item.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        AnimatedText(
            text = item.title,
            fontSize = 32.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center,
            delay = 300
        )

        Spacer(modifier = Modifier.height(16.dp))

        AnimatedText(
            text = item.description,
            fontSize = 16.sp,
            color = Color.White.copy(alpha = 0.8f),
            textAlign = TextAlign.Center,
            delay = 600
        )
    }
}

@Composable
fun FeaturesPage(item: OnboardingItem, viewModel: OnboardingViewModel) {
    val features = listOf(
        Triple(R.drawable.ic_mouse, "Motion Control", "Move cursor by rotating your phone"),
        Triple(R.drawable.ic_gesture, "Gesture Recognition", "Quick flips for clicks and scrolls"),
        Triple(R.drawable.ic_voice, "Voice Commands", "Hands-free control"),
        Triple(R.drawable.ic_proximity, "Proximity Lock", "Auto-locks when you walk away")
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        features.forEachIndexed { index, (icon, title, description) ->
            AnimatedFeatureItem(
                icon = icon,
                title = title,
                description = description,
                accentColor = item.accentColor,
                delay = index * 150L
            )
        }
    }
}

@Composable
fun AnimatedFeatureItem(icon: Int, title: String, description: String, accentColor: Color, delay: Long) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(400)) + slideInHorizontally()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Surface(
                modifier = Modifier.size(48.dp),
                shape = RoundedCornerShape(12.dp),
                color = accentColor.copy(alpha = 0.2f)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(
                        painter = painterResource(id = icon),
                        contentDescription = title,
                        modifier = Modifier.size(28.dp),
                        tint = accentColor
                    )
                }
            }
            Column {
                Text(
                    text = title,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp,
                    color = Color.White
                )
                Text(
                    text = description,
                    fontSize = 14.sp,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }
    }
}

@Composable
fun ConnectPage(item: OnboardingItem, viewModel: OnboardingViewModel) {
    var isPulsing by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(1000)
            isPulsing = !isPulsing
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Surface(
            modifier = Modifier
                .size(120.dp)
                .scale(if (isPulsing) 1.05f else 1f),
            shape = CircleShape,
            color = item.accentColor.copy(alpha = 0.2f),
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_wifi),
                    contentDescription = "WiFi",
                    modifier = Modifier.size(60.dp),
                    tint = item.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Same WiFi Network Required",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = "Make sure your phone and computer are connected to the same WiFi network for seamless control",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ConnectionMethod(
                icon = Icons.Default.QrCodeScanner,
                title = "Scan QR Code",
                description = "Quick pairing",
                accentColor = item.accentColor
            )
            ConnectionMethod(
                icon = Icons.Default.Settings,
                title = "Manual Entry",
                description = "Enter IP address",
                accentColor = item.accentColor
            )
            ConnectionMethod(
                icon = Icons.Default.Search,
                title = "Auto Discovery",
                description = "Find servers",
                accentColor = item.accentColor
            )
        }
    }
}

@Composable
fun ConnectionMethod(icon: androidx.compose.ui.graphics.vector.ImageVector, title: String, description: String, accentColor: Color) {
    Column(
        modifier = Modifier
            .weight(1f)
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White.copy(alpha = 0.1f))
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Icon(icon, contentDescription = title, tint = accentColor, modifier = Modifier.size(32.dp))
        Spacer(modifier = Modifier.height(8.dp))
        Text(title, fontSize = 12.sp, fontWeight = FontWeight.Medium, color = Color.White)
        Text(description, fontSize = 10.sp, color = Color.White.copy(alpha = 0.6f))
    }
}

@Composable
fun VoicePage(item: OnboardingItem, viewModel: OnboardingViewModel) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        VoiceWaveAnimation(accentColor = item.accentColor)

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Supported Voice Commands",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )

        Spacer(modifier = Modifier.height(16.dp))

        val commands = listOf("click", "double click", "right click", "scroll up", "scroll down", "next slide", "previous slide", "lock screen")

        commands.chunked(2).forEach { rowCommands ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                rowCommands.forEach { command ->
                    Surface(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(8.dp),
                        color = item.accentColor.copy(alpha = 0.2f)
                    ) {
                        Text(
                            text = command,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                            fontSize = 12.sp,
                            textAlign = TextAlign.Center,
                            color = Color.White
                        )
                    }
                }
                if (rowCommands.size == 1) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
fun VoiceWaveAnimation(accentColor: Color) {
    val infiniteTransition = rememberInfiniteTransition(label = "voice_wave_transition")
    val waveHeights = List(5) { index ->
        infiniteTransition.animateFloat(
            initialValue = 20f,
            targetValue = 60f,
            animationSpec = infiniteRepeatable(
                animation = tween(800, easing = FastOutSlowInEasing),
                repeatMode = RepeatMode.Reverse
            ),
            label = "wave$index"
        )
    }

    Row(
        modifier = Modifier.height(80.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        waveHeights.forEach { height ->
            Box(
                modifier = Modifier
                    .width(8.dp)
                    .height(height.value.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(accentColor)
            )
        }
    }
}

@Composable
fun ProximityPage(item: OnboardingItem, viewModel: OnboardingViewModel) {
    var distance by remember { mutableStateOf(0.5f) }

    LaunchedEffect(Unit) {
        while (true) {
            delay(100)
            distance = (distance + 0.02f) % 1f
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = item.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Box(
            modifier = Modifier.size(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val radius = (size.width / 2f) * (0.5f + distance * 0.3f)
                drawCircle(
                    color = item.accentColor.copy(alpha = 0.3f),
                    radius = radius,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
                drawCircle(
                    color = item.accentColor,
                    radius = 40f,
                    center = Offset(size.width / 2f, size.height / 2f)
                )
            }
            Icon(
                painter = painterResource(id = R.drawable.ic_proximity),
                contentDescription = "Proximity",
                modifier = Modifier.size(60.dp),
                tint = Color.White
            )
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = if (distance > 0.6f) "Device is FAR" else "Device is NEAR",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = if (distance > 0.6f) Color(0xFFEF4444) else Color(0xFF10B981)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Text(
            text = if (distance > 0.6f) "Screen will lock when you walk away" else "Screen will unlock when you return",
            fontSize = 14.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f)
        )
    }
}

@Composable
fun FinalPage(item: OnboardingItem, viewModel: OnboardingViewModel, navigationActions: NavigationActions) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = item.accentColor.copy(alpha = 0.2f)
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_check),
                    contentDescription = "Complete",
                    modifier = Modifier.size(60.dp),
                    tint = item.accentColor
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = item.title,
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = item.description,
            fontSize = 16.sp,
            textAlign = TextAlign.Center,
            color = Color.White.copy(alpha = 0.7f)
        )

        Spacer(modifier = Modifier.height(32.dp))

        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.1f))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text("Your Preferences", fontWeight = FontWeight.Bold, color = Color.White)
                Spacer(modifier = Modifier.height(8.dp))
                PreferenceRow("Theme", uiState.selectedTheme, item.accentColor)
                PreferenceRow("Haptic Feedback", if (uiState.hapticEnabled) "Enabled" else "Disabled", item.accentColor)
                PreferenceRow("Auto Connect", if (uiState.autoConnect) "Enabled" else "Disabled", item.accentColor)
                PreferenceRow("Usage Analytics", if (uiState.allowAnalytics) "Allowed" else "Not Allowed", item.accentColor)
            }
        }
    }
}

@Composable
fun PreferenceRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, fontSize = 14.sp, color = Color.White.copy(alpha = 0.7f))
        Text(value, fontSize = 14.sp, fontWeight = FontWeight.Medium, color = accentColor)
    }
}

@Composable
fun AnimatedText(
    text: String,
    fontSize: TextUnit,
    fontWeight: FontWeight = FontWeight.Normal,
    color: Color,
    textAlign: TextAlign,
    delay: Long
) {
    var isVisible by remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        delay(delay)
        isVisible = true
    }

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(animationSpec = tween(600)) + slideInVertically { boxHeight -> boxHeight / 2 }
    ) {
        Text(
            text = text,
            fontSize = fontSize,
            fontWeight = fontWeight,
            color = color,
            textAlign = textAlign
        )
    }
}