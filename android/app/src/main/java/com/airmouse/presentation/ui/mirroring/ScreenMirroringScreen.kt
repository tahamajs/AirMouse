package com.airmouse.presentation.ui.mirroring

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.media.projection.MediaProjectionManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ScreenShare
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.airmouse.mirroring.ScreenMirroringService
import com.airmouse.network.ConnectionManager
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ScreenMirroringScreen(
    navigationActions: NavigationActions,
    viewModel: ScreenMirroringViewModel = hiltViewModel()
) {
    val context = LocalContext.current
    val connectionState by viewModel.connectionState.collectAsState()
    val quality by viewModel.quality.collectAsState()
    val isConnected = connectionState == ConnectionManager.ConnectionStatus.CONNECTED

    var isStreaming by remember { mutableStateOf(false) }
    var selectedFps by remember { mutableIntStateOf(15) }
    var selectedQuality by remember { mutableIntStateOf(50) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        val activity = context.findActivity() ?: return@rememberLauncherForActivityResult
        val serverUrl = viewModel.buildServerUrl()
        if (result.resultCode == Activity.RESULT_OK && result.data != null && serverUrl.isNotBlank()) {
            ScreenMirroringService.start(
                context = activity.applicationContext,
                resultCode = result.resultCode,
                data = result.data!!,
                serverUrl = serverUrl,
                quality = selectedQuality,
                frameRate = selectedFps
            )
            isStreaming = true
        }
    }

    // Pulse animation for streaming indicator
    val infiniteTransition = rememberInfiniteTransition(label = "streaming_pulse")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.85f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_scale"
    )
    val pulseAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_alpha"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0A0E1A), Color(0xFF0D1B2A))))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                "Screen Mirroring",
                                fontWeight = FontWeight.Bold,
                                color = Color.White
                            )
                            Text(
                                if (isStreaming) "Streaming active" else if (isConnected) "Ready to stream" else "Not connected",
                                fontSize = 11.sp,
                                color = if (isStreaming) Color(0xFF10B981) else if (isConnected) Color(0xFF38BDF8) else Color(0xFFF59E0B)
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
                    colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
                )
            },
            containerColor = Color.Transparent
        ) { padding ->
            val serverUrl = viewModel.buildServerUrl()

            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 16.dp, vertical = 8.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // ─── Hero status card ───────────────────────────────
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(28.dp))
                        .background(
                            Brush.linearGradient(
                                colors = if (isStreaming)
                                    listOf(Color(0xFF1A4B3A), Color(0xFF0F2D22))
                                else if (isConnected)
                                    listOf(Color(0xFF1A2F4B), Color(0xFF0F1D2D))
                                else
                                    listOf(Color(0xFF2A1F0F), Color(0xFF1A1208))
                            )
                        )
                        .border(
                            1.dp,
                            if (isStreaming) Color(0xFF10B981).copy(alpha = 0.5f)
                            else if (isConnected) Color(0xFF38BDF8).copy(alpha = 0.3f)
                            else Color(0xFFF59E0B).copy(alpha = 0.3f),
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
                            // Animated icon
                            Box(contentAlignment = Alignment.Center) {
                                if (isStreaming) {
                                    Box(
                                        modifier = Modifier
                                            .size(64.dp)
                                            .scale(pulseScale)
                                            .background(Color(0xFF10B981).copy(alpha = pulseAlpha * 0.15f), CircleShape)
                                    )
                                }
                                Box(
                                    modifier = Modifier
                                        .size(52.dp)
                                        .background(
                                            if (isStreaming) Color(0xFF10B981).copy(alpha = 0.2f)
                                            else Color.White.copy(alpha = 0.08f),
                                            CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ScreenShare,
                                        contentDescription = null,
                                        tint = if (isStreaming) Color(0xFF10B981) else Color.White,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                            }

                            // Status badge
                            val badgeColor = when {
                                isStreaming -> Color(0xFF10B981)
                                isConnected -> Color(0xFF38BDF8)
                                else -> Color(0xFFF59E0B)
                            }
                            Surface(
                                color = badgeColor.copy(alpha = 0.18f),
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
                                            .scale(if (isStreaming) pulseScale else 1f)
                                            .background(badgeColor, CircleShape)
                                    )
                                    Text(
                                        text = if (isStreaming) "LIVE" else if (isConnected) "READY" else "OFFLINE",
                                        color = badgeColor,
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                }
                            }
                        }

                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Text(
                                text = if (isStreaming) "Streaming to PC" else "Display Sharing",
                                color = Color.White,
                                fontSize = 22.sp,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = if (isStreaming)
                                    "Your phone screen is being mirrored to the server at ${selectedFps}fps."
                                else
                                    "Stream your phone screen to the PC wirelessly for presenting, drawing, or monitoring.",
                                color = Color.White.copy(alpha = 0.75f),
                                fontSize = 13.sp,
                                lineHeight = 20.sp
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.1f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column {
                                Text("Server", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text(
                                    serverUrl.ifBlank { "Not configured" },
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Text("Quality", color = Color.White.copy(alpha = 0.5f), fontSize = 11.sp)
                                Text(
                                    "${selectedQuality}% · ${selectedFps}fps",
                                    color = Color.White,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.SemiBold
                                )
                            }
                        }
                    }
                }

                // ─── Action buttons ─────────────────────────────────
                AnimatedVisibility(
                    visible = !isStreaming,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = {
                            val activity = context.findActivity() ?: return@Button
                            val projectionManager = activity.getSystemService(Context.MEDIA_PROJECTION_SERVICE) as MediaProjectionManager
                            launcher.launch(projectionManager.createScreenCaptureIntent())
                        },
                        enabled = isConnected && serverUrl.isNotBlank(),
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            contentColor = Color.White,
                            disabledContainerColor = Color.White.copy(alpha = 0.08f),
                            disabledContentColor = Color.White.copy(alpha = 0.35f)
                        )
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ScreenShare, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text(
                            if (!isConnected) "Connect first to start streaming" else "Start Mirroring",
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                AnimatedVisibility(
                    visible = isStreaming,
                    enter = fadeIn() + expandVertically(),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    Button(
                        onClick = {
                            ScreenMirroringService.stop(context)
                            isStreaming = false
                        },
                        modifier = Modifier.fillMaxWidth().height(58.dp),
                        shape = RoundedCornerShape(18.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFFEF4444),
                            contentColor = Color.White
                        )
                    ) {
                        Icon(Icons.Filled.StopCircle, contentDescription = null, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(10.dp))
                        Text("Stop Mirroring", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                    }
                }

                // ─── Quality settings ────────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(16.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.Tune, contentDescription = null, tint = Color(0xFF38BDF8), modifier = Modifier.size(20.dp))
                            Text("Stream Settings", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        // FPS selector
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Text("Frame Rate: ${selectedFps} fps", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                listOf(5, 10, 15, 24, 30).forEach { fps ->
                                    val selected = selectedFps == fps
                                    Surface(
                                        onClick = { selectedFps = fps },
                                        shape = RoundedCornerShape(10.dp),
                                        color = if (selected) Color(0xFF38BDF8).copy(alpha = 0.2f) else Color.White.copy(alpha = 0.06f),
                                        border = if (selected) androidx.compose.foundation.BorderStroke(1.dp, Color(0xFF38BDF8)) else null
                                    ) {
                                        Text(
                                            "$fps",
                                            color = if (selected) Color(0xFF38BDF8) else Color.White.copy(alpha = 0.65f),
                                            fontSize = 13.sp,
                                            fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal,
                                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                                        )
                                    }
                                }
                            }
                        }

                        // Quality slider
                        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Image Quality", color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp)
                                Text("${selectedQuality}%", color = Color(0xFF38BDF8), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                            Slider(
                                value = selectedQuality.toFloat(),
                                onValueChange = { selectedQuality = it.toInt() },
                                valueRange = 10f..100f,
                                steps = 8,
                                colors = SliderDefaults.colors(
                                    thumbColor = Color(0xFF38BDF8),
                                    activeTrackColor = Color(0xFF38BDF8),
                                    inactiveTrackColor = Color.White.copy(alpha = 0.15f)
                                )
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text("Low bandwidth", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                                Text("High quality", color = Color.White.copy(alpha = 0.4f), fontSize = 10.sp)
                            }
                        }
                    }
                }

                // ─── Network quality card ────────────────────────────
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(20.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            Icon(Icons.Filled.SignalWifi4Bar, contentDescription = null, tint = Color(0xFF22C55E), modifier = Modifier.size(20.dp))
                            Text("Network Quality", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp)
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        MirroringStatRow(
                            icon = Icons.Filled.Speed,
                            label = "Latency",
                            value = "${quality.ping} ms",
                            iconTint = Color(0xFF38BDF8),
                            valueColor = if (quality.ping < 50) Color(0xFF22C55E) else if (quality.ping < 100) Color(0xFFF59E0B) else Color(0xFFEF4444)
                        )
                        MirroringStatRow(
                            icon = Icons.Filled.BarChart,
                            label = "Jitter",
                            value = "${quality.jitter} ms",
                            iconTint = Color(0xFF8B5CF6),
                            valueColor = Color.White
                        )
                        MirroringStatRow(
                            icon = Icons.Filled.Warning,
                            label = "Packet Loss",
                            value = "${"%.1f".format(quality.packetLoss * 100)}%",
                            iconTint = Color(0xFFF97316),
                            valueColor = if (quality.packetLoss > 0.05) Color(0xFFEF4444) else Color(0xFF22C55E)
                        )
                        MirroringStatRow(
                            icon = Icons.Filled.NetworkCheck,
                            label = "Signal",
                            value = quality.signalStrength.name,
                            iconTint = Color(0xFF10B981),
                            valueColor = Color.White
                        )
                    }
                }

                // ─── Feature chips ───────────────────────────────────
                Text("Features", color = Color.White.copy(alpha = 0.5f), fontSize = 12.sp, modifier = Modifier.padding(start = 4.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(
                        "📡 WiFi Direct",
                        "🔒 Encrypted",
                        "⚡ Low-latency",
                        "📱 Portrait & Landscape"
                    ).forEach { feature ->
                        Surface(
                            shape = RoundedCornerShape(50.dp),
                            color = Color.White.copy(alpha = 0.07f)
                        ) {
                            Text(
                                feature,
                                color = Color.White.copy(alpha = 0.8f),
                                fontSize = 11.sp,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}

@Composable
private fun MirroringStatRow(
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

private fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
