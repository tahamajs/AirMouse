// app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationResultScreen.kt
package com.airmouse.presentation.ui.calibration

import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.BuildConfig
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationResultScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
    onRecalibrate: () -> Unit = {},
    onShare: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
    val calibrationDataState by viewModel.calibrationData.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()

    val calibrationData by produceState<CalibrationData?>(initialValue = calibrationDataState, calibrationDataState) {
        value = calibrationDataState ?: viewModel.loadCalibrationData()
    }

    // Animation states
    var animationTriggered by remember { mutableStateOf(false) }
    var confettiActive by remember { mutableStateOf(false) }

    val scale by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "success_pop_animation"
    )

    val fadeIn by animateFloatAsState(
        targetValue = if (animationTriggered) 1f else 0f,
        animationSpec = tween(durationMillis = 500, delayMillis = 300),
        label = "fade_in_animation"
    )

    LaunchedEffect(Unit) {
        animationTriggered = true
        delay(500)
        confettiActive = true
        delay(3000)
        confettiActive = false
    }

    val data = calibrationData
    val quality = data?.quality ?: CalibrationQuality.UNKNOWN
    val qualityConfig = CalibrationQualityConfig.fromQuality(quality)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                )
            )
    ) {
        if (confettiActive) {
            CalibrationResultConfettiEffect()
        }

        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Calibration Results",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
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
                        if (data != null && data.isCalibrated) {
                            IconButton(onClick = { viewModel.syncToServer() }) {
                                Icon(
                                    Icons.Default.Sync,
                                    contentDescription = "Sync to Server",
                                    tint = Color.White
                                )
                            }
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = Color.Transparent
                    )
                )
            }
        ) { paddingValues ->
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Success header
                item {
                    SuccessHeader(
                        qualityConfig = qualityConfig,
                        scale = scale,
                        fadeIn = fadeIn,
                        animationTriggered = animationTriggered,
                        calibrationData = calibrationData
                    )
                }

                // Quality metrics card
                if (data != null) {
                    item {
                        QualityMetricsCard(
                            qualityConfig = qualityConfig,
                            calibrationData = data
                        )
                    }

                    // Sensor calibration details
                    item {
                        SensorCalibrationDetailsCard(calibrationData = data)
                    }

                    // Calibration summary
                    item {
                        CalibrationSummaryCard(
                            calibrationData = data,
                            timestamp = data.timestamp
                        )
                    }
                }

                // Action buttons
                item {
                    ActionButtonsRow(
                        onContinue = onContinue,
                        onRecalibrate = {
                            onRecalibrate()
                            navigationActions.navigateToCalibration()
                        },
                        onShare = onShare,
                        onExport = onExport,
                        calibrationData = data
                    )
                }

                // Footer
                item {
                    Text(
                        text = "Air Mouse v3.0.0",
                        fontSize = 12.sp,
                        color = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.padding(vertical = 16.dp)
                    )
                }
            }
        }
    }
}

// ==========================================
// SUCCESS HEADER
// ==========================================

@Composable
fun SuccessHeader(
    qualityConfig: CalibrationQualityConfig,
    scale: Float,
    fadeIn: Float,
    animationTriggered: Boolean,
    calibrationData: CalibrationData?
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .graphicsLayer { alpha = fadeIn },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Success animation circle
        Box(
            modifier = Modifier
                .size(120.dp)
                .scale(scale)
                .clip(CircleShape)
                .background(
                    Brush.radialGradient(
                        colors = listOf(
                            qualityConfig.color.copy(alpha = 0.3f),
                            qualityConfig.color.copy(alpha = 0.1f)
                        )
                    )
                ),
            contentAlignment = Alignment.Center
        ) {
            Box(
                modifier = Modifier
                    .size(90.dp)
                    .clip(CircleShape)
                    .background(Color.White),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = qualityConfig.emoji,
                    fontSize = 48.sp,
                    modifier = Modifier.scale(if (animationTriggered) 1f else 0.5f)
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Calibration Complete! 🎉",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = qualityConfig.title,
            fontSize = 18.sp,
            fontWeight = FontWeight.Medium,
            color = qualityConfig.color,
            textAlign = TextAlign.Center
        )

        if (calibrationData != null && calibrationData.isCalibrated) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "✓ Successfully calibrated and saved",
                fontSize = 14.sp,
                color = Color(0xFF10B981),
                textAlign = TextAlign.Center
            )
        }
    }
}

// ==========================================
// QUALITY METRICS CARD
// ==========================================

@Composable
fun QualityMetricsCard(
    qualityConfig: CalibrationQualityConfig,
    calibrationData: CalibrationData
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            qualityConfig.color.copy(alpha = 0.3f)
        )
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "📊 Calibration Quality",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(bottom = 12.dp)
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                QualityMetricItem(
                    label = "Quality",
                    value = calibrationData.quality.name,
                    color = qualityConfig.color
                )
                QualityMetricItem(
                    label = "Score",
                    value = getQualityScore(calibrationData.quality).toString() + "%",
                    color = qualityConfig.color
                )
                QualityMetricItem(
                    label = "Version",
                    value = "v${BuildConfig.VERSION_NAME}",
                    color = Color.White.copy(alpha = 0.6f)
                )
            }
        }
    }
}

// ==========================================
// SENSOR CALIBRATION DETAILS CARD
// ==========================================

@Composable
fun SensorCalibrationDetailsCard(calibrationData: CalibrationData) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                "📐 Sensor Calibration Details",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            // Gyroscope
            SensorDetailRow(
                label = "Gyroscope",
                values = listOf(
                    calibrationData.gyroBias.offsetX,
                    calibrationData.gyroBias.offsetY,
                    calibrationData.gyroBias.offsetZ
                ),
                color = Color(0xFFEF4444),
                labelPrefix = "Bias"
            )

            // Accelerometer
            SensorDetailRow(
                label = "Accelerometer",
                values = listOf(
                    calibrationData.accelOffset.offsetX,
                    calibrationData.accelOffset.offsetY,
                    calibrationData.accelOffset.offsetZ
                ),
                color = Color(0xFF3B82F6),
                labelPrefix = "Offset"
            )

            // Magnetometer
            SensorDetailRow(
                label = "Magnetometer",
                values = listOf(
                    calibrationData.magOffset.offsetX,
                    calibrationData.magOffset.offsetY,
                    calibrationData.magOffset.offsetZ
                ),
                color = Color(0xFF10B981),
                labelPrefix = "Offset"
            )
        }
    }
}

@Composable
fun SensorDetailRow(
    label: String,
    values: List<Float>,
    color: Color,
    labelPrefix: String = ""
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            label,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White.copy(alpha = 0.8f)
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            values.forEachIndexed { index, value ->
                Text(
                    "${if (index == 0) "X: " else if (index == 1) "Y: " else "Z: "}${"%.4f".format(value)}",
                    fontSize = 11.sp,
                    color = color,
                    fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                )
            }
        }
    }
}

// ==========================================
// CALIBRATION SUMMARY CARD
// ==========================================

@Composable
fun CalibrationSummaryCard(
    calibrationData: CalibrationData,
    timestamp: Long
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.05f)
        ),
        border = androidx.compose.foundation.BorderStroke(
            1.dp,
            Color.White.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                "ℹ️ Calibration Summary",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )

            SummaryRow(
                label = "Status",
                value = if (calibrationData.isCalibrated) "✅ Complete" else "❌ Incomplete"
            )
            SummaryRow(
                label = "Quality",
                value = calibrationData.quality.name
            )
            SummaryRow(
                label = "Date",
                value = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
                    .format(Date(timestamp))
            )
            SummaryRow(
                label = "Version",
                value = "v${BuildConfig.VERSION_NAME}"
            )
            SummaryRow(
                label = "Device",
                value = Build.MODEL
            )
        }
    }
}

@Composable
fun SummaryRow(label: String, value: String) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 13.sp,
            color = Color.White.copy(alpha = 0.6f)
        )
        Text(
            value,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = Color.White
        )
    }
}

// ==========================================
// ACTION BUTTONS ROW
// ==========================================

@Composable
fun ActionButtonsRow(
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit,
    onShare: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null,
    calibrationData: CalibrationData?
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Button(
            onClick = onContinue,
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF6366F1)
            )
        ) {
            Icon(Icons.Default.CheckCircle, contentDescription = null, modifier = Modifier.size(20.dp))
            Spacer(modifier = Modifier.width(8.dp))
            Text("Continue to Air Mouse", fontSize = 16.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            OutlinedButton(
                onClick = onRecalibrate,
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.2f)
                )
            ) {
                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Recalibrate", fontSize = 14.sp)
            }

            if (onShare != null) {
                OutlinedButton(
                    onClick = onShare,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(12.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color.White.copy(alpha = 0.7f)
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        Color.White.copy(alpha = 0.2f)
                    )
                ) {
                    Icon(Icons.Default.Share, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Share", fontSize = 14.sp)
                }
            }
        }

        if (onExport != null && calibrationData != null && calibrationData.isCalibrated) {
            OutlinedButton(
                onClick = onExport,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = Color.White.copy(alpha = 0.7f)
                ),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.2f)
                )
            ) {
                Icon(Icons.Default.Download, contentDescription = null, modifier = Modifier.size(18.dp))
                Spacer(modifier = Modifier.width(4.dp))
                Text("Export Calibration Data", fontSize = 14.sp)
            }
        }
    }
}

// ==========================================
// QUALITY METRIC ITEM
// ==========================================

@Composable
fun QualityMetricItem(label: String, value: String, color: Color) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = color
        )
        Text(
            label,
            fontSize = 12.sp,
            color = Color.White.copy(alpha = 0.5f)
        )
    }
}

// ==========================================
// CONFETTI EFFECT
// ==========================================

@Composable
fun CalibrationResultConfettiEffect() {
    val colors = listOf(
        Color(0xFF6366F1),
        Color(0xFF10B981),
        Color(0xFFF59E0B),
        Color(0xFFEF4444),
        Color(0xFF3B82F6),
        Color(0xFFEC4899)
    )

    val infiniteTransition = rememberInfiniteTransition(label = "confetti")
    val progress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(durationMillis = 3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "confetti_progress"
    )

    Box(modifier = Modifier.fillMaxSize()) {
        for (i in 0..30) {
            val index = (i + progress * 20).toInt() % colors.size
            val x = (i * 37 + progress * 150) % 360
            val y = (i * 53 + progress * 200) % 400
            val size = 6 + (i % 4) * 2

            Box(
                modifier = Modifier
                    .offset(x = x.dp, y = (y - 100).dp)
                    .size(size.dp)
                    .clip(CircleShape)
                    .background(colors[index].copy(alpha = 0.6f))
            )
        }
    }
}

fun getQualityScore(quality: CalibrationQuality): Int {
    return when (quality) {
        CalibrationQuality.EXCELLENT -> 95
        CalibrationQuality.GOOD -> 80
        CalibrationQuality.FAIR -> 60
        CalibrationQuality.POOR -> 40
        CalibrationQuality.UNKNOWN -> 50
    }
}

// ==========================================
// PREVIEW
// ==========================================

@Preview(showBackground = true)
@Composable
fun CalibrationResultScreenPreview() {
    MaterialTheme {
        CalibrationResultScreen(
            navigationActions = PreviewNavigationActions,
            onContinue = {},
            onRecalibrate = {}
        )
    }
}

@Preview(showBackground = true, name = "Dark Mode")
@Composable
fun CalibrationResultScreenDarkPreview() {
    MaterialTheme {
        CalibrationResultScreen(
            navigationActions = PreviewNavigationActions,
            onContinue = {},
            onRecalibrate = {}
        )
    }
}

private val PreviewNavigationActions = object : NavigationActions {
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
