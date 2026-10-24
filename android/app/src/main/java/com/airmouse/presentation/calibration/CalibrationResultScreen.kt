package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CloudUpload
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.presentation.navigation.NavigationActions
import java.util.Locale

/**
 * Calibration Result Screen
 *
 * Displays the results of the calibration process in a unified modern purple/green design.
 * Shows status, quality, and detailed calibration data.
 */
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
    val calibrationData by viewModel.calibrationData.collectAsStateWithLifecycle()
    val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()
    val calibrationQuality by viewModel.calibrationQuality.collectAsStateWithLifecycle()
    val progress by viewModel.calibrationProgress.collectAsStateWithLifecycle()

    val checkScale by animateFloatAsState(
        targetValue = 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "checkmark"
    )

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration Result", color = Color.White, fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateToHome() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
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
                .background(
                    Brush.verticalGradient(
                        listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))
                    )
                )
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebration checkmark icon
                Box(
                    modifier = Modifier
                        .size(90.dp)
                        .scale(checkScale)
                        .clip(CircleShape)
                        .background(
                            Brush.radialGradient(
                                listOf(Color(0xFF10B981).copy(alpha = 0.35f), Color(0xFF10B981).copy(alpha = 0.05f))
                            )
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(36.dp)
                        )
                    }
                }

                Text(
                    text = "Calibration Complete! 🎉",
                    color = Color.White,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Black,
                    textAlign = TextAlign.Center
                )

                // Quality Badge
                val effectiveQuality = calibrationData?.quality ?: calibrationQuality
                val qualityColor = when (effectiveQuality) {
                    CalibrationQuality.EXCELLENT -> Color(0xFF10B981)
                    CalibrationQuality.GOOD -> Color(0xFF3B82F6)
                    CalibrationQuality.FAIR -> Color(0xFFF59E0B)
                    CalibrationQuality.POOR -> Color(0xFFEF4444)
                    CalibrationQuality.UNKNOWN -> Color(0xFF64748B)
                }
                val qualityEmoji = when (effectiveQuality) {
                    CalibrationQuality.EXCELLENT -> "🌟"
                    CalibrationQuality.GOOD -> "👍"
                    CalibrationQuality.FAIR -> "⚠️"
                    CalibrationQuality.POOR -> "❌"
                    CalibrationQuality.UNKNOWN -> "❓"
                }

                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = qualityColor.copy(alpha = 0.15f),
                    border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.35f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(qualityEmoji, fontSize = 20.sp)
                        Text(
                            "Quality: ${effectiveQuality.name}",
                            color = qualityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                }

                // Result Card with detailed telemetry
                ResultCard(
                    calibrationData = calibrationData,
                    calibrationStatus = calibrationStatus,
                    calibrationQuality = effectiveQuality,
                    progress = progress
                )

                // Sync status messages
                if (uiState.statusMessage.contains("synchronized", ignoreCase = true)) {
                    Text(
                        text = uiState.statusMessage,
                        color = Color(0xFF10B981),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }
                uiState.errorMessage?.let { error ->
                    Text(
                        text = error,
                        color = Color(0xFFEF4444),
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(4.dp)
                    )
                }

                // Sync to PC Button (only active if server is connected and data is calibrated)
                if (calibrationData?.isCalibrated == true) {
                    Button(
                        onClick = { viewModel.syncToServer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uiState.isServerConnected,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color(0xFF10B981),
                            disabledContainerColor = Color.White.copy(alpha = 0.12f)
                        )
                    ) {
                        Icon(Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (uiState.isServerConnected) "Sync to PC Server" else "Sync to PC (Offline)",
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Continue Button (Primary CTA in Modern Indigo/Purple)
                Button(
                    onClick = {
                        onContinue()
                        navigationActions.navigateToHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(56.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Icon(Icons.Default.ArrowForward, contentDescription = null)
                    Spacer(Modifier.width(10.dp))
                    Text("Continue to App", fontWeight = FontWeight.Bold, fontSize = 16.sp)
                }

                // Recalibrate Button
                OutlinedButton(
                    onClick = {
                        onRecalibrate()
                        navigationActions.navigateToCalibration()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    shape = RoundedCornerShape(16.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("Recalibrate", color = Color.White.copy(alpha = 0.85f))
                }

                // Share Button (optional)
                if (onShare != null) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Icon(Icons.Default.Share, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Results", color = Color.White.copy(alpha = 0.85f))
                    }
                }

                // Export Button (optional)
                if (onExport != null) {
                    OutlinedButton(
                        onClick = onExport,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                    ) {
                        Text("Export Data", color = Color.White.copy(alpha = 0.85f))
                    }
                }
            }
        }
    }
}

/**
 * Result Card displaying calibration details with modern dark styling and glowing indicators.
 */
@Composable
private fun ResultCard(
    calibrationData: CalibrationData?,
    calibrationStatus: CalibrationStatus,
    calibrationQuality: CalibrationQuality,
    progress: Int
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.06f)
        ),
        border = BorderStroke(1.dp, Color(0xFF6366F1).copy(alpha = 0.25f))
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Text(
                text = "📊 Telemetry & Calibration Data",
                fontWeight = FontWeight.Bold,
                color = Color.White,
                fontSize = 16.sp
            )

            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Status:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(
                    calibrationStatus.name,
                    color = when (calibrationStatus) {
                        CalibrationStatus.COMPLETED -> Color(0xFF10B981)
                        CalibrationStatus.IN_PROGRESS -> Color(0xFF38BDF8)
                        CalibrationStatus.FAILED -> Color(0xFFEF4444)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Quality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Quality:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(
                    calibrationQuality.name,
                    color = when (calibrationQuality) {
                        CalibrationQuality.EXCELLENT -> Color(0xFF10B981)
                        CalibrationQuality.GOOD -> Color(0xFF3B82F6)
                        CalibrationQuality.FAIR -> Color(0xFFF59E0B)
                        else -> Color.White.copy(alpha = 0.7f)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Progress:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text("$progress%", color = Color.White, fontWeight = FontWeight.Bold, fontSize = 13.sp)
            }

            // Data saved status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Data Storage:", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                Text(
                    if (calibrationData?.isCalibrated == true) "✅ Saved to device" else "❌ Missing",
                    color = if (calibrationData?.isCalibrated == true) {
                        Color(0xFF10B981)
                    } else {
                        Color(0xFFEF4444)
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }

            HorizontalDivider(color = Color.White.copy(alpha = 0.08f), modifier = Modifier.padding(vertical = 4.dp))

            // Show detailed data if available
            if (calibrationData != null && calibrationData.isCalibrated) {
                TelemetryRow("Gyro Bias", formatOffset(calibrationData.gyroBias), Color(0xFF38BDF8))
                TelemetryRow("Accel Offset", formatOffset(calibrationData.accelOffset), Color(0xFF22C55E))
                TelemetryRow("Mag Offset", formatOffset(calibrationData.magOffset), Color(0xFFF59E0B))
                TelemetryRow("Accel Scale", formatScale(calibrationData.accelOffset), Color(0xFF22C55E))
                TelemetryRow("Mag Scale", formatScale(calibrationData.magOffset), Color(0xFFF59E0B))
            }
        }
    }
}

@Composable
private fun TelemetryRow(label: String, value: String, accentColor: Color) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, color = Color.White.copy(alpha = 0.7f), fontSize = 12.sp)
        Text(
            value,
            color = accentColor,
            fontSize = 11.sp,
            fontFamily = FontFamily.Monospace,
            fontWeight = FontWeight.Medium
        )
    }
}

/**
 * Format a sensor data's offsets as a string.
 */
private fun formatOffset(data: SensorCalibrationData): String {
    return String.format(Locale.US, "(%.2f, %.2f, %.2f)", data.offsetX, data.offsetY, data.offsetZ)
}

/**
 * Format a sensor data's scale factors as a string.
 */
private fun formatScale(data: SensorCalibrationData): String {
    return String.format(Locale.US, "(%.2f, %.2f, %.2f)", data.scaleX, data.scaleY, data.scaleZ)
}
