package com.airmouse.presentation.ui.profiles

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.UserProfile
import com.airmouse.domain.model.ProfileSettings
import com.airmouse.domain.repository.IProfileRepository
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import javax.inject.Inject
import java.util.Locale

@HiltViewModel
class ProfilesViewModel @Inject constructor(
    private val profileRepository: IProfileRepository
) : ViewModel() {

    private val _uiState = MutableStateFlow<UserProfile?>(null)
    val uiState: StateFlow<UserProfile?> = _uiState.asStateFlow()

    init {
        loadProfile()
    }

    private fun loadProfile() {
        viewModelScope.launch {
            try {
                val entities = profileRepository.getAllProfiles()
                if (entities.isNotEmpty()) {
                    _uiState.value = entities.first()
                } else {
                    val newProfile = UserProfile(name = "Default Profile")
                    val id = profileRepository.createProfile(newProfile)
                    profileRepository.setDefaultProfile(id)
                    _uiState.value = newProfile.copy(id = id)
                }
            } catch (e: Exception) {
                // Ignore
            }
        }
    }

    fun updateProfile(profile: UserProfile) {
        viewModelScope.launch {
            try {
                profileRepository.updateProfile(profile)
                _uiState.value = profile
            } catch (e: Exception) {
                // Ignore
            }
        }
    }
}

data class ProfilePreset(
    val name: String,
    val description: String,
    val icon: ImageVector,
    val accentColor: Color,
    val settings: ProfileSettings
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfilesScreen(
    navigationActions: NavigationActions? = null,
    viewModel: ProfilesViewModel = hiltViewModel(),
    modifier: Modifier = Modifier,
    onNavigateBack: () -> Unit = { navigationActions?.navigateBack() }
) {
    val profile by viewModel.uiState.collectAsState()

    val presets = remember {
        listOf(
            ProfilePreset(
                name = "Casual Mode",
                description = "Balanced sensitivity and smoothing for daily task navigation.",
                icon = Icons.Default.Speed,
                accentColor = Color(0xFF6366F1),
                settings = ProfileSettings(
                    sensitivity = 1.0f,
                    clickThreshold = 5.0f,
                    scrollThreshold = 8.0f,
                    doubleClickInterval = 400L,
                    hapticEnabled = true,
                    accelerationEnabled = true,
                    smoothingEnabled = true,
                    aiSmoothing = false,
                    predictiveMovement = true
                )
            ),
            ProfilePreset(
                name = "Developer",
                description = "High precision, active AI stabilization for dense UI codebases.",
                icon = Icons.Default.Code,
                accentColor = Color(0xFF10B981),
                settings = ProfileSettings(
                    sensitivity = 1.2f,
                    clickThreshold = 6.0f,
                    scrollThreshold = 6.0f,
                    doubleClickInterval = 350L,
                    hapticEnabled = true,
                    accelerationEnabled = true,
                    smoothingEnabled = true,
                    aiSmoothing = true,
                    predictiveMovement = true
                )
            ),
            ProfilePreset(
                name = "Presenter",
                description = "Optimized for slide navigation. Low speed, no haptics.",
                icon = Icons.Default.Slideshow,
                accentColor = Color(0xFFF59E0B),
                settings = ProfileSettings(
                    sensitivity = 0.8f,
                    clickThreshold = 8.0f,
                    scrollThreshold = 12.0f,
                    doubleClickInterval = 500L,
                    hapticEnabled = false,
                    accelerationEnabled = false,
                    smoothingEnabled = true,
                    aiSmoothing = false,
                    predictiveMovement = true
                )
            ),
            ProfilePreset(
                name = "Gamer",
                description = "Maximum speed, raw input tracking. No delays or smoothing.",
                icon = Icons.Default.SportsEsports,
                accentColor = Color(0xFFEF4444),
                settings = ProfileSettings(
                    sensitivity = 1.8f,
                    clickThreshold = 3.0f,
                    scrollThreshold = 4.0f,
                    doubleClickInterval = 250L,
                    hapticEnabled = true,
                    accelerationEnabled = true,
                    smoothingEnabled = false,
                    aiSmoothing = false,
                    predictiveMovement = false
                )
            )
        )
    }

    PremiumDashboardBackground {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Device Settings",
                            fontWeight = FontWeight.Bold,
                            color = Color.White,
                            fontSize = 20.sp
                        )
                    },
                    navigationIcon = {
                        IconButton(onClick = onNavigateBack) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
            ) {
                if (profile == null) {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        com.airmouse.presentation.ui.components.SweepGradientGlowRing()
                    }
                } else {
                    val p = profile!!
                    Column(
                        modifier = Modifier
                            .fillMaxSize()
                            .verticalScroll(rememberScrollState())
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        // Premium Profile Avatar Header with Animated Neon Ring
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.03f)),
                            shape = RoundedCornerShape(24.dp),
                            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(20.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                // Animated Neon Ring around Avatar
                                val infiniteTransition = rememberInfiniteTransition(label = "AvatarRing")
                                val rotation by infiniteTransition.animateFloat(
                                    initialValue = 0f,
                                    targetValue = 360f,
                                    animationSpec = infiniteRepeatable(
                                        animation = tween(8000, easing = LinearEasing),
                                        repeatMode = RepeatMode.Restart
                                    ),
                                    label = "rotation"
                                )

                                Box(
                                    modifier = Modifier
                                        .size(72.dp)
                                        .clip(CircleShape)
                                        .background(
                                            Brush.sweepGradient(
                                                colors = listOf(
                                                    Color(0xFF6366F1),
                                                    Color(0xFFEC4899),
                                                    Color(0xFF10B981),
                                                    Color(0xFF6366F1)
                                                )
                                            )
                                        )
                                        .padding(3.dp)
                                        .clip(CircleShape)
                                        .background(Color(0xFF0F111A)),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Person,
                                        contentDescription = "Profile Avatar",
                                        modifier = Modifier.size(36.dp),
                                        tint = Color.White
                                    )
                                }

                                Spacer(modifier = Modifier.width(20.dp))

                                Column {
                                    Text(
                                        text = p.name,
                                        color = Color.White,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 20.sp
                                    )
                                    Text(
                                        text = "Active Control Configuration",
                                        color = Color.White.copy(alpha = 0.5f),
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        // Presets Section Header
                        Text(
                            text = "Configuration Presets",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        // 4 Preset Cards in 2x2 Grid Layout
                        Column(
                            verticalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            for (row in 0..1) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                                ) {
                                    for (col in 0..1) {
                                        val index = row * 2 + col
                                        val preset = presets[index]
                                        val isCurrent = p.settings.sensitivity == preset.settings.sensitivity &&
                                                p.settings.scrollThreshold == preset.settings.scrollThreshold &&
                                                p.settings.aiSmoothing == preset.settings.aiSmoothing &&
                                                p.settings.hapticEnabled == preset.settings.hapticEnabled

                                        val borderAlpha by animateFloatAsState(
                                            targetValue = if (isCurrent) 1f else 0f,
                                            animationSpec = tween(400),
                                            label = "BorderAlpha"
                                        )

                                        Card(
                                            modifier = Modifier
                                                .weight(1f)
                                                .clickable {
                                                    viewModel.updateProfile(p.copy(settings = preset.settings))
                                                },
                                            colors = CardDefaults.cardColors(
                                                containerColor = if (isCurrent) preset.accentColor.copy(alpha = 0.08f) else Color.White.copy(alpha = 0.03f)
                                            ),
                                            shape = RoundedCornerShape(16.dp),
                                            border = BorderStroke(
                                                width = 1.5.dp,
                                                color = if (isCurrent) preset.accentColor else Color.White.copy(alpha = 0.08f)
                                            )
                                        ) {
                                            Column(
                                                modifier = Modifier.padding(16.dp),
                                                verticalArrangement = Arrangement.spacedBy(8.dp)
                                            ) {
                                                Icon(
                                                    imageVector = preset.icon,
                                                    contentDescription = preset.name,
                                                    tint = if (isCurrent) preset.accentColor else Color.White.copy(alpha = 0.6f),
                                                    modifier = Modifier.size(28.dp)
                                                )
                                                Text(
                                                    text = preset.name,
                                                    color = Color.White,
                                                    fontWeight = FontWeight.Bold,
                                                    fontSize = 15.sp
                                                )
                                                Text(
                                                    text = preset.description,
                                                    color = Color.White.copy(alpha = 0.45f),
                                                    fontSize = 11.sp,
                                                    lineHeight = 15.sp
                                                )
                                            }
                                        }
                                    }
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(4.dp))

                        // Custom Settings Header
                        Text(
                            text = "Fine-Tuning Parameter Settings",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        // Motion Sensitivity Slider
                        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Motion Sensitivity", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = String.format(Locale.US, "%.1fx", p.settings.sensitivity),
                                        color = Color(0xFF6366F1),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = p.settings.sensitivity,
                                    onValueChange = { 
                                        viewModel.updateProfile(p.copy(settings = p.settings.copy(sensitivity = it)))
                                    },
                                    valueRange = 0.1f..5.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF6366F1),
                                        activeTrackColor = Color(0xFF6366F1),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }

                        // Scrolling Sensitivity Slider
                        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Scrolling Speed", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = String.format(Locale.US, "%.1f lines", p.settings.scrollThreshold),
                                        color = Color(0xFF10B981),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = p.settings.scrollThreshold,
                                    onValueChange = { 
                                        viewModel.updateProfile(p.copy(settings = p.settings.copy(scrollThreshold = it)))
                                    },
                                    valueRange = 1.0f..20.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFF10B981),
                                        activeTrackColor = Color(0xFF10B981),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }

                        // Click Threshold Slider
                        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text("Click Recognition Threshold", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                    Text(
                                        text = String.format(Locale.US, "%.1fG", p.settings.clickThreshold),
                                        color = Color(0xFFF59E0B),
                                        fontSize = 14.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                                Spacer(modifier = Modifier.height(8.dp))
                                Slider(
                                    value = p.settings.clickThreshold,
                                    onValueChange = { 
                                        viewModel.updateProfile(p.copy(settings = p.settings.copy(clickThreshold = it)))
                                    },
                                    valueRange = 1.0f..10.0f,
                                    colors = SliderDefaults.colors(
                                        thumbColor = Color(0xFFF59E0B),
                                        activeTrackColor = Color(0xFFF59E0B),
                                        inactiveTrackColor = Color.White.copy(alpha = 0.1f)
                                    )
                                )
                            }
                        }

                        // Advanced Switch Options
                        GlassmorphicCard(modifier = Modifier.fillMaxWidth()) {
                            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                                Text("Advanced Settings Features", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 14.sp)
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("AI Motion Smoothing", color = Color.White, fontSize = 14.sp)
                                        Text("Filter trembles & smooth pointer trajectory", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = p.settings.aiSmoothing,
                                        onCheckedChange = { viewModel.updateProfile(p.copy(settings = p.settings.copy(aiSmoothing = it))) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1), checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.4f))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Haptic Feedback Click Vibrations", color = Color.White, fontSize = 14.sp)
                                        Text("Active haptic clicks vibration on response triggers", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = p.settings.hapticEnabled,
                                        onCheckedChange = { viewModel.updateProfile(p.copy(settings = p.settings.copy(hapticEnabled = it))) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1), checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.4f))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Predictive Cursor Movement", color = Color.White, fontSize = 14.sp)
                                        Text("Calculate cursor trajectory acceleration to reduce lag", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = p.settings.predictiveMovement,
                                        onCheckedChange = { viewModel.updateProfile(p.copy(settings = p.settings.copy(predictiveMovement = it))) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1), checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.4f))
                                    )
                                }
                                
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Invert Horizontal X Axis", color = Color.White, fontSize = 14.sp)
                                        Text("Reverse left-to-right cursor movement directions", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = p.settings.invertX,
                                        onCheckedChange = { viewModel.updateProfile(p.copy(settings = p.settings.copy(invertX = it))) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1), checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.4f))
                                    )
                                }

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text("Invert Vertical Y Axis", color = Color.White, fontSize = 14.sp)
                                        Text("Reverse up-to-down cursor movement directions", color = Color.White.copy(alpha = 0.45f), fontSize = 11.sp)
                                    }
                                    Switch(
                                        checked = p.settings.invertY,
                                        onCheckedChange = { viewModel.updateProfile(p.copy(settings = p.settings.copy(invertY = it))) },
                                        colors = SwitchDefaults.colors(checkedThumbColor = Color(0xFF6366F1), checkedTrackColor = Color(0xFF6366F1).copy(alpha = 0.4f))
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
