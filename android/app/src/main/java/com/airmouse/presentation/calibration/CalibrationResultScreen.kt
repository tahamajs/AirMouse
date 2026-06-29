package com.airmouse.presentation.ui.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
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
 * Displays the results of the calibration process.
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Calibration Result") },
                colors = TopAppBarDefaults.topAppBarColors()
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Result Card with detailed info
            ResultCard(
                calibrationData = calibrationData,
                calibrationStatus = calibrationStatus,
                calibrationQuality = calibrationQuality,
                progress = progress
            )

            // Sync status messages
            if (uiState.statusMessage.contains("synchronized", ignoreCase = true)) {
                Text(
                    text = uiState.statusMessage,
                    color = MaterialTheme.colorScheme.primary,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            }
            uiState.errorMessage?.let { error ->
                Text(
                    text = error,
                    color = MaterialTheme.colorScheme.error,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(8.dp)
                )
            }

            // Sync to PC Button (only active if server is connected and data is calibrated)
            if (calibrationData?.isCalibrated == true) {
                Button(
                    onClick = { viewModel.syncToServer() },
                    modifier = Modifier.fillMaxWidth(),
                    enabled = uiState.isServerConnected
                ) {
                    Text(if (uiState.isServerConnected) "Sync to PC Server" else "Sync to PC (Offline)")
                }
            }

            // Continue Button
            Button(
                onClick = {
                    onContinue()
                    navigationActions.navigateToHome()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }

            // Recalibrate Button
            Button(
                onClick = {
                    onRecalibrate()
                    navigationActions.navigateToCalibration()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recalibrate")
            }

            // Share Button (optional)
            if (onShare != null) {
                Button(
                    onClick = onShare,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Share Results")
                }
            }

            // Export Button (optional)
            if (onExport != null) {
                Button(
                    onClick = onExport,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text("Export Data")
                }
            }
        }
    }
}

/**
 * Result Card displaying calibration details.
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
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Calibration Results",
                fontWeight = FontWeight.Bold,
                style = MaterialTheme.typography.titleMedium
            )

            // Status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Status:", fontWeight = FontWeight.Medium)
                Text(
                    calibrationStatus.name,
                    color = when (calibrationStatus) {
                        CalibrationStatus.COMPLETED -> MaterialTheme.colorScheme.primary
                        CalibrationStatus.IN_PROGRESS -> MaterialTheme.colorScheme.tertiary
                        CalibrationStatus.FAILED -> MaterialTheme.colorScheme.error
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Quality
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Quality:", fontWeight = FontWeight.Medium)
                Text(
                    calibrationQuality.name,
                    color = when (calibrationQuality) {
                        CalibrationQuality.EXCELLENT -> MaterialTheme.colorScheme.primary
                        CalibrationQuality.GOOD -> MaterialTheme.colorScheme.tertiary
                        CalibrationQuality.FAIR -> MaterialTheme.colorScheme.tertiary
                        else -> MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
            }

            // Progress
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Progress:", fontWeight = FontWeight.Medium)
                Text("$progress%")
            }

            // Data saved status
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text("Data:", fontWeight = FontWeight.Medium)
                Text(
                    if (calibrationData?.isCalibrated == true) "✅ Saved" else "❌ Missing",
                    color = if (calibrationData?.isCalibrated == true) {
                        MaterialTheme.colorScheme.primary
                    } else {
                        MaterialTheme.colorScheme.error
                    }
                )
            }

            // Show detailed data if available
            if (calibrationData != null && calibrationData.isCalibrated) {
                Text(
                    text = "Gyro Bias: ${formatOffset(calibrationData.gyroBias)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Accel Offset: ${formatOffset(calibrationData.accelOffset)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Mag Offset: ${formatOffset(calibrationData.magOffset)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Accel Scale: ${formatScale(calibrationData.accelOffset)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Text(
                    text = "Mag Scale: ${formatScale(calibrationData.magOffset)}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
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