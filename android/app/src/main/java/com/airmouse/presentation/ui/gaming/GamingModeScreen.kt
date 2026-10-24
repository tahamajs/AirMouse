package com.airmouse.presentation.ui.gaming

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airmouse.presentation.navigation.NavigationActions
import com.airmouse.presentation.ui.components.PremiumDashboardBackground
import com.airmouse.presentation.ui.components.GlassmorphicCard
import com.airmouse.presentation.ui.touchpad.TouchpadEvent
import com.airmouse.presentation.ui.touchpad.TouchpadMode
import com.airmouse.presentation.ui.touchpad.TouchpadViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GamingModeScreen(
    navigationActions: NavigationActions,
    touchpadViewModel: TouchpadViewModel = hiltViewModel(),
    gamingViewModel: GamingViewModel = hiltViewModel(),
    gameProfilesViewModel: GameProfilesViewModel = hiltViewModel()
) {
    val uiState by touchpadViewModel.uiState.collectAsState()
    val gamingUiState by gamingViewModel.uiState.collectAsState()
    val profilesUiState by gameProfilesViewModel.uiState.collectAsState()

    val isConnected = uiState.isConnected || gamingUiState.isConnected
    val isGamingModeApplied = gamingUiState.isGamingModeApplied || (uiState.sensitivity == 1.5f && !uiState.tapToClick && !uiState.twoFingerScroll)

    PremiumDashboardBackground {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Gaming Mode",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                if (isConnected) "Connected" else "Disconnected",
                                fontSize = 11.sp,
                                color = if (isConnected) Color(0xFF38BDF8) else Color(0xFFF59E0B)
                            )
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = { navigationActions.navigateBack() }) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Back",
                                tint = Color.White
                            )
                        }
                    },
                    actions = {
                        IconButton(
                            onClick = {
                                if (isConnected) {
                                    touchpadViewModel.handleEvent(TouchpadEvent.Disconnect)
                                } else {
                                    touchpadViewModel.handleEvent(TouchpadEvent.Connect)
                                }
                            }
                        ) {
                            Icon(
                                imageVector = Icons.Default.PowerSettingsNew,
                                contentDescription = "Connection Toggle",
                                tint = if (isConnected) Color(0xFFEF4444) else Color(0xFF10B981)
                            )
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Gaming Mode Status Hero Card ───────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (isGamingModeApplied)
                                    listOf(Color(0xFF1A2F4B), Color(0xFF0F1D2D))
                                else
                                    listOf(Color(0xFF232838), Color(0xFF161A26))
                            )
                        )
                        .border(
                            1.dp,
                            if (isGamingModeApplied) Color(0xFF38BDF8).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(28.dp)
                        )
                        .padding(24.dp)
                ) {
                    Column(verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(52.dp)
                                    .background(
                                        if (isGamingModeApplied) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.08f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    imageVector = Icons.Filled.SportsEsports,
                                    contentDescription = null,
                                    tint = if (isGamingModeApplied) Color(0xFF38BDF8) else Color.White,
                                    modifier = Modifier.size(28.dp)
                                )
                            }

                            // Active Profile Badge
                            val gameBadgeColor = if (profilesUiState.currentGame != "None") Color(0xFF10B981) else Color(0xFF9CA3AF)
                            Surface(
                                color = gameBadgeColor.copy(alpha = 0.18f),
                                shape = RoundedCornerShape(50.dp)
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(7.dp)
                                            .background(gameBadgeColor, CircleShape)
                                    )
                                    Text(
                                        text = profilesUiState.currentGame.uppercase(),
                                        color = gameBadgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 11.sp
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = "Ultra Low Latency Preset",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = "Configures UDP protocol, boosts sensor polling rate to maximum, disables default gestures, and optimizes response variables for zero-lag gaming control inputs.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Button(
                            onClick = {
                                gamingViewModel.applyGamingPreset()
                                touchpadViewModel.handleEvent(TouchpadEvent.ApplyPreset(TouchpadMode.GAMING))
                                gameProfilesViewModel.refreshProfiles()
                            },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isGamingModeApplied) Color(0xFF38BDF8) else Color.White,
                                contentColor = if (isGamingModeApplied) Color.White else Color(0xFF0F172A)
                            )
                        ) {
                            Icon(
                                imageVector = if (isGamingModeApplied) Icons.Filled.CheckCircle else Icons.Filled.PlayArrow,
                                contentDescription = null
                            )
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = if (isGamingModeApplied) "Preset Applied" else "Apply Gaming Preset",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                        
                        Button(
                            onClick = { navigationActions.navigateToGameController() },
                            modifier = Modifier.fillMaxWidth().height(50.dp),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color.White.copy(alpha=0.1f),
                                contentColor = Color.White
                            )
                        ) {
                            Icon(Icons.Filled.SportsEsports, contentDescription = null)
                            Spacer(Modifier.width(8.dp))
                            Text(
                                text = "Open Controller Layout",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }
                    }
                }

                // ─── Real-time Engine Settings Card ────────────────
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Text("Engine Configuration", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        GamingConfigRow(
                            icon = Icons.Filled.Speed,
                            label = "Transmission Protocol",
                            value = gamingUiState.transmissionProtocol,
                            iconTint = Color(0xFF8B5CF6),
                            valueColor = if (isGamingModeApplied) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f)
                        )

                        GamingConfigRow(
                            icon = Icons.Filled.Bolt,
                            label = "Polling Rate / Latency Boost",
                            value = gamingUiState.pollingRate,
                            iconTint = Color(0xFFF59E0B),
                            valueColor = if (isGamingModeApplied) Color(0xFF10B981) else Color.White.copy(alpha = 0.7f)
                        )

                        GamingConfigRow(
                            icon = Icons.Filled.TouchApp,
                            label = "Default Gestures",
                            value = gamingUiState.defaultGestures,
                            iconTint = Color(0xFF38BDF8),
                            valueColor = if (isGamingModeApplied) Color(0xFFEF4444) else Color(0xFF10B981)
                        )

                        GamingConfigRow(
                            icon = Icons.Filled.CompareArrows,
                            label = "Cursor Sensitivity",
                            value = "${"%.1f".format(if (gamingUiState.isGamingModeApplied) gamingUiState.sensitivity else uiState.sensitivity)}x",
                            iconTint = Color(0xFFEC4899),
                            valueColor = Color.White
                        )
                    }
                }

                // ─── Game Profiles Summary Card ────────────────
                GlassmorphicCard(
                    modifier = Modifier.fillMaxWidth(),
                    cornerRadius = 20.dp
                ) {
                    Column(
                        verticalArrangement = Arrangement.spacedBy(14.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.List, contentDescription = null, tint = Color(0xFF10B981), modifier = Modifier.size(20.dp))
                            Text("Game Profiles Summary", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text("Available Profiles", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp)
                                Text("${gamingUiState.gameProfiles.size} Profiles Available", color = Color.White, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
                            }
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                IconButton(onClick = { gameProfilesViewModel.refreshProfiles() }) {
                                    Icon(Icons.Filled.Refresh, contentDescription = "Refresh Profiles", tint = Color(0xFF38BDF8))
                                }
                                TextButton(onClick = { navigationActions.navigateToGameProfiles() }) {
                                    Text("Manage", color = Color(0xFF38BDF8), fontWeight = FontWeight.Bold)
                                }
                            }
                        }

                        // LazyColumn showing default profiles: FPS Shooter and MOBA
                        LazyColumn(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(160.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(gamingUiState.gameProfiles) { profile ->
                                Surface(
                                    modifier = Modifier.fillMaxWidth(),
                                    shape = RoundedCornerShape(12.dp),
                                    color = Color.White.copy(alpha = 0.06f),
                                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically,
                                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                                    ) {
                                        Box(
                                            modifier = Modifier
                                                .size(36.dp)
                                                .background(Color(0xFF38BDF8).copy(alpha = 0.18f), CircleShape),
                                            contentAlignment = Alignment.Center
                                        ) {
                                            Icon(
                                                imageVector = Icons.Filled.SportsEsports,
                                                contentDescription = null,
                                                tint = Color(0xFF38BDF8),
                                                modifier = Modifier.size(20.dp)
                                            )
                                        }

                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = profile.name,
                                                color = Color.White,
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 14.sp
                                            )
                                            Text(
                                                text = profile.description,
                                                color = Color.White.copy(alpha = 0.65f),
                                                fontSize = 11.sp,
                                                lineHeight = 15.sp
                                            )
                                        }

                                        Surface(
                                            shape = RoundedCornerShape(50.dp),
                                            color = Color(0xFF10B981).copy(alpha = 0.2f)
                                        ) {
                                            Text(
                                                text = "Default",
                                                color = Color(0xFF10B981),
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Bold,
                                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                            )
                                        }
                                    }
                                }
                            }
                        }

                        if (profilesUiState.summary.isNotBlank()) {
                            Text(
                                text = "Available configurations for: ${profilesUiState.summary}",
                                color = Color.White.copy(alpha = 0.7f),
                                fontSize = 12.sp,
                                lineHeight = 18.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun GamingConfigRow(
    icon: ImageVector,
    label: String,
    value: String,
    iconTint: Color,
    valueColor: Color
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(icon, contentDescription = null, tint = iconTint, modifier = Modifier.size(18.dp))
            Text(label, color = Color.White.copy(alpha = 0.75f), fontSize = 14.sp)
        }
        Text(value, color = valueColor, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
    }
}
