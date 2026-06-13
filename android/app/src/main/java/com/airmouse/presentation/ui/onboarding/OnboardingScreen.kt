package com.airmouse.presentation.ui.onboarding

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class, ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    navigationActions: NavigationActions,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val pagerState = rememberPagerState(pageCount = { uiState.totalPages })
    val coroutineScope = rememberCoroutineScope()

    // Sync pager with ViewModel state
    LaunchedEffect(uiState.currentPage) {
        coroutineScope.launch {
            pagerState.animateScrollToPage(uiState.currentPage)
        }
    }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            // Skip button
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(
                    onClick = {
                        viewModel.skipOnboarding()
                        navigationActions.navigateToHome()
                    }
                ) {
                    Text("Skip")
                }
            }

            // Horizontal Pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { page ->
                AnimatedContent(
                    targetState = page,
                    transitionSpec = {
                        fadeIn(animationSpec = tween(300)) +
                                slideInHorizontally(animationSpec = tween(300)) togetherWith
                                fadeOut(animationSpec = tween(300)) +
                                slideOutHorizontally(animationSpec = tween(300))
                    }
                ) { currentPage ->
                    when (currentPage) {
                        0 -> WelcomePage()
                        1 -> FeaturesPage()
                        2 -> ConnectPage()
                        3 -> PreferencesPage(viewModel, uiState)
                    }
                }
            }

            // Page Indicator and Navigation Buttons
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Page Indicator
                Row(
                    modifier = Modifier.padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    repeat(uiState.totalPages) { page ->
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .clip(CircleShape)
                                .animateContentSize()
                                .then(
                                    if (page == pagerState.currentPage) {
                                        Modifier.size(24.dp, 8.dp)
                                    } else {
                                        Modifier
                                    }
                                )
                                .background(
                                    if (page == pagerState.currentPage)
                                        MaterialTheme.colorScheme.primary
                                    else
                                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.3f)
                                )
                        )
                    }
                }

                // Navigation Buttons
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    if (pagerState.currentPage > 0) {
                        OutlinedButton(
                            onClick = {
                                coroutineScope.launch {
                                    pagerState.animateScrollToPage(pagerState.currentPage - 1)
                                    viewModel.previousPage()
                                }
                            }
                        ) {
                            Text("Back")
                        }
                    } else {
                        Spacer(modifier = Modifier.width(0.dp))
                    }

                    Button(
                        onClick = {
                            coroutineScope.launch {
                                if (pagerState.currentPage == uiState.totalPages - 1) {
                                    viewModel.savePreferences()
                                    navigationActions.navigateToHome()
                                } else {
                                    pagerState.animateScrollToPage(pagerState.currentPage + 1)
                                    viewModel.nextPage()
                                }
                            }
                        }
                    ) {
                        Text(
                            if (pagerState.currentPage == uiState.totalPages - 1) "Get Started"
                            else "Next"
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun WelcomePage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Animated Logo
        Surface(
            modifier = Modifier.size(120.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
            shadowElevation = 8.dp
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_air_mouse),
                    contentDescription = "Air Mouse Logo",
                    modifier = Modifier.size(80.dp),
                    tint = MaterialTheme.colorScheme.primary
                )
            }
        }

        Spacer(modifier = Modifier.height(32.dp))

        Text(
            text = "Welcome to Air Mouse Pro",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center
        )

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Turn your phone into a wireless mouse using motion sensors and AI",
            fontSize = 16.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun FeaturesPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Amazing Features",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        FeatureItem(
            icon = R.drawable.ic_mouse,
            title = "Motion Control",
            description = "Move cursor by rotating your phone"
        )

        FeatureItem(
            icon = R.drawable.ic_gesture,
            title = "Gesture Recognition",
            description = "Quick flips for clicks, scrolls, and more"
        )

        FeatureItem(
            icon = R.drawable.ic_voice,
            title = "Voice Commands",
            description = "Say 'click', 'scroll', 'right click'"
        )

        FeatureItem(
            icon = R.drawable.ic_proximity,
            title = "Proximity Lock",
            description = "Auto-locks when you walk away"
        )
    }
}

@Composable
fun FeatureItem(icon: Int, title: String, description: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Icon(
            painter = painterResource(id = icon),
            contentDescription = title,
            modifier = Modifier.size(40.dp),
            tint = MaterialTheme.colorScheme.primary
        )
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
            Text(
                text = description,
                fontSize = 14.sp,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
fun ConnectPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Connect to Your Computer",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        Surface(
            modifier = Modifier.size(100.dp),
            shape = CircleShape,
            color = MaterialTheme.colorScheme.secondaryContainer
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_wifi),
                    contentDescription = "WiFi",
                    modifier = Modifier.size(60.dp),
                    tint = MaterialTheme.colorScheme.secondary
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Same WiFi Network",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Make sure your phone and computer are connected to the same WiFi network",
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        Spacer(modifier = Modifier.height(24.dp))

        Text(
            text = "Scan QR Code",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Or scan the QR code displayed on your computer screen",
            fontSize = 14.sp,
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
fun PreferencesPage(viewModel: OnboardingViewModel, uiState: OnboardingUiState) {
    var userName by remember { mutableStateOf(uiState.userName) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Customize Your Experience",
            fontSize = 24.sp,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(bottom = 32.dp)
        )

        // User Name Input
        OutlinedTextField(
            value = userName,
            onValueChange = {
                userName = it
                viewModel.updateUserName(it)
            },
            label = { Text("Your Name (Optional)") },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Theme Selection
        Text(
            text = "Choose Theme",
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium,
            modifier = Modifier.align(Alignment.Start)
        )

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            ThemeOption(
                title = "Light",
                isSelected = uiState.selectedTheme == "light",
                onClick = { viewModel.updateTheme("light") }
            )
            ThemeOption(
                title = "Dark",
                isSelected = uiState.selectedTheme == "dark",
                onClick = { viewModel.updateTheme("dark") }
            )
            ThemeOption(
                title = "System",
                isSelected = uiState.selectedTheme == "system",
                onClick = { viewModel.updateTheme("system") }
            )
        }
    }
}

@Composable
fun ThemeOption(title: String, isSelected: Boolean, onClick: () -> Unit) {
    Surface(
        modifier = Modifier
            .weight(1f)
            .clip(MaterialTheme.shapes.medium),
        color = if (isSelected) MaterialTheme.colorScheme.primaryContainer
        else MaterialTheme.colorScheme.surfaceVariant,
        onClick = onClick
    ) {
        Text(
            text = title,
            modifier = Modifier.padding(12.dp),
            textAlign = androidx.compose.ui.text.style.TextAlign.Center,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimaryContainer
            else MaterialTheme.colorScheme.onSurface
        )
    }
}