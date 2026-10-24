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
 * Displays the results of the calibration process in a unified Material 3 theme matching CalibrationScreen.
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

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFF0F172A), Color(0xFF1E1B4B))))
    ) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = {
                        Text(
                            "Calibration Summary",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    },
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
                        containerColor = Color.Transparent,
                        titleContentColor = Color.White,
                        navigationIconContentColor = Color.White
                    )
                )
            },
            containerColor = Color.Transparent
        ) { paddingValues ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(paddingValues)
                    .verticalScroll(rememberScrollState())
                    .padding(20.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Celebration Icon
                Box(
                    modifier = Modifier
                        .size(100.dp)
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
                            .size(70.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF10B981)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }
                }

                Text(
                    text = "Calibration Verified 🎉",
                    color = Color.White,
                    fontSize = 26.sp,
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
                    border = BorderStroke(1.dp, qualityColor.copy(alpha = 0.3f))
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Text(qualityEmoji, fontSize = 24.sp)
                        Text(
                            "Quality: ${effectiveQuality.name}",
                            color = qualityColor,
                            fontWeight = FontWeight.Bold,
                            fontSize = 18.sp
                        )
                    }
                }

                // Detailed Results Card
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.06f)),
                    shape = RoundedCornerShape(22.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.10f))
                ) {
                    Column(
                        modifier = Modifier.padding(18.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            "📊 Sensor Offsets & Scale",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )

                        // Status Row
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Status", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text(
                                if (calibrationData?.isCalibrated == true || calibrationStatus == CalibrationStatus.COMPLETED) "✅ COMPLETED (100%)" else "⏳ IN PROGRESS",
                                color = if (calibrationData?.isCalibrated == true || calibrationStatus == CalibrationStatus.COMPLETED) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }

                        if (calibrationData != null) {
                            HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                            SensorDetailRow(
                                label = "Gyro Bias",
                                offsets = calibrationData.gyroBias,
                                scale = null,
                                color = Color(0xFF38BDF8)
                            )

                            SensorDetailRow(
                                label = "Accelerometer",
                                offsets = calibrationData.accelOffset,
                                scale = Triple(calibrationData.accelOffset.scaleX, calibrationData.accelOffset.scaleY, calibrationData.accelOffset.scaleZ),
                                color = Color(0xFF22C55E)
                            )

                            SensorDetailRow(
                                label = "Magnetometer",
                                offsets = calibrationData.magOffset,
                                scale = Triple(calibrationData.magOffset.scaleX, calibrationData.magOffset.scaleY, calibrationData.magOffset.scaleZ),
                                color = Color(0xFFF59E0B)
                            )
                        }

                        HorizontalDivider(color = Color.White.copy(alpha = 0.08f))

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text("Storage", color = Color.White.copy(alpha = 0.7f), fontSize = 13.sp)
                            Text(
                                if (calibrationData?.isCalibrated == true) "✅ Saved to device" else "⏳ Incomplete",
                                color = if (calibrationData?.isCalibrated == true) Color(0xFF10B981) else Color(0xFFF59E0B),
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }

                // Sync status feedback
                if (uiState.statusMessage.contains("synchronized", ignoreCase = true)) {
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFF10B981).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFF10B981).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = uiState.statusMessage,
                            color = Color(0xFF10B981),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }
                uiState.errorMessage?.let { error ->
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = Color(0xFFEF4444).copy(alpha = 0.15f),
                        border = BorderStroke(1.dp, Color(0xFFEF4444).copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = error,
                            color = Color(0xFFEF4444),
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(12.dp),
                            textAlign = TextAlign.Center
                        )
                    }
                }

                // Sync to PC Button
                if (calibrationData?.isCalibrated == true) {
                    Button(
                        onClick = { viewModel.syncToServer() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        enabled = uiState.isServerConnected,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(imageVector = Icons.Default.CloudUpload, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text(if (uiState.isServerConnected) "Sync to PC Server" else "Sync to PC (Offline)")
                    }
                }

                // Continue Button
                Button(
                    onClick = {
                        onContinue()
                        navigationActions.navigateToHome()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(58.dp),
                    shape = RoundedCornerShape(18.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF6366F1))
                ) {
                    Icon(imageVector = Icons.Default.ArrowForward, contentDescription = null)
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
                        .height(52.dp),
                    shape = RoundedCornerShape(18.dp),
                    border = BorderStroke(1.dp, Color.White.copy(alpha = 0.2f))
                ) {
                    Icon(imageVector = Icons.Default.Refresh, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                    Spacer(Modifier.width(8.dp))
                    Text("Recalibrate", color = Color.White.copy(alpha = 0.8f))
                }

                // Share / Export
                if (onShare != null) {
                    OutlinedButton(
                        onClick = onShare,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(48.dp),
                        shape = RoundedCornerShape(16.dp),
                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
                    ) {
                        Icon(imageVector = Icons.Default.Share, contentDescription = null, tint = Color.White.copy(alpha = 0.7f))
                        Spacer(Modifier.width(8.dp))
                        Text("Share Results", color = Color.White.copy(alpha = 0.8f))
                    }
                }
            }
        }
    }
}

@Composable
private fun SensorDetailRow(
    label: String,
    offsets: SensorCalibrationData,
    scale: Triple<Float, Float, Float>?,
    color: Color
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = Color.White.copy(alpha = 0.85f), fontSize = 13.sp, fontWeight = FontWeight.SemiBold)
            Text(
                "Offset: X=${"%.2f".format(Locale.US, offsets.offsetX)} Y=${"%.2f".format(Locale.US, offsets.offsetY)} Z=${"%.2f".format(Locale.US, offsets.offsetZ)}",
                color = color,
                fontSize = 11.sp,
                fontFamily = FontFamily.Monospace,
                fontWeight = FontWeight.Medium
            )
        }
        if (scale != null) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                Text(
                    "Scale: X=${"%.2f".format(Locale.US, scale.first)} Y=${"%.2f".format(Locale.US, scale.second)} Z=${"%.2f".format(Locale.US, scale.third)}",
                    color = color.copy(alpha = 0.75f),
                    fontSize = 11.sp,
                    fontFamily = FontFamily.Monospace
                )
            }
        }
    }
}
