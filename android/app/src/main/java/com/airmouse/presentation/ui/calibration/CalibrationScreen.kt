// CalibrationScreen.kt
package com.airmouse.presentation.ui.calibration

import androidx.compose.animation.*
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.vectorResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.presentation.navigation.NavigationActions
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    // Auto‑start calibration when screen loads
    LaunchedEffect(Unit) {
        delay(500)
        if (!uiState.isCollecting && !uiState.isComplete) {
            viewModel.startCalibration()
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(uiState.stepTitle) },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        bottomBar = {
            if (!uiState.isComplete) {
                BottomAppBar {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(
                            onClick = { viewModel.previousStep() },
                            enabled = uiState.currentStep > 0 && !uiState.isCollecting,
                            modifier = Modifier.weight(1f)
                        ) { Text("Back") }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = {
                                if (uiState.isCollecting) viewModel.recordPosition()
                                else viewModel.startCalibration()
                            },
                            enabled = !uiState.isComplete,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (uiState.isCollecting)
                                    MaterialTheme.colorScheme.secondary
                                else
                                    MaterialTheme.colorScheme.primary
                            )
                        ) {
                            Text(
                                when {
                                    uiState.isCollecting && uiState.currentStep == 1 -> "Record Position"
                                    uiState.isCollecting -> "Collecting..."
                                    else -> "Start Calibration"
                                }
                            )
                        }

                        Spacer(modifier = Modifier.width(8.dp))

                        Button(
                            onClick = { viewModel.abortCalibration() },
                            colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error),
                            modifier = Modifier.weight(1f)
                        ) { Text("Stop") }
                    }
                }
            }
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
            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(24.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        AnimatedInstruction(uiState)

                        Spacer(modifier = Modifier.height(16.dp))

                        Text(
                            text = uiState.stepInstruction,
                            style = MaterialTheme.typography.bodyLarge,
                            fontWeight = FontWeight.Medium,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = uiState.stepDescription,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            textAlign = androidx.compose.ui.text.style.TextAlign.Center
                        )
                    }
                }
            }

            item {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text("Progress", style = MaterialTheme.typography.titleMedium)
                    Spacer(modifier = Modifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = uiState.progress / 100f,
                        modifier = Modifier.fillMaxWidth().height(8.dp),
                        color = MaterialTheme.colorScheme.primary
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("${uiState.progress}% Complete", style = MaterialTheme.typography.bodyMedium)
                    if (uiState.samplesCollected > 0) {
                        Text(
                            text = "Samples: ${uiState.samplesCollected}/${uiState.totalSamplesNeeded}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            item {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(
                        containerColor = when {
                            uiState.errorMessage != null -> MaterialTheme.colorScheme.errorContainer
                            uiState.isComplete -> MaterialTheme.colorScheme.primaryContainer
                            else -> MaterialTheme.colorScheme.secondaryContainer
                        }
                    ),
                    shape = RoundedCornerShape(16.dp)
                ) {
                    Text(
                        text = uiState.errorMessage ?: uiState.statusMessage,
                        modifier = Modifier.padding(16.dp),
                        color = when {
                            uiState.errorMessage != null -> MaterialTheme.colorScheme.onErrorContainer
                            uiState.isComplete -> MaterialTheme.colorScheme.onPrimaryContainer
                            else -> MaterialTheme.colorScheme.onSecondaryContainer
                        },
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }

            if (uiState.currentStep == 1 && uiState.currentPosition < positions.size) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant
                        ),
                        shape = RoundedCornerShape(20.dp)
                    ) {
                        Column(
                            modifier = Modifier.padding(16.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text("Current Position", style = MaterialTheme.typography.titleSmall)
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${uiState.currentPosition + 1}/${uiState.totalPositions}",
                                style = MaterialTheme.typography.displaySmall,
                                fontWeight = FontWeight.Bold
                            )
                            Text(
                                text = positions[uiState.currentPosition],
                                style = MaterialTheme.typography.bodyLarge,
                                textAlign = androidx.compose.ui.text.style.TextAlign.Center
                            )
                        }
                    }
                }
            }

            if (uiState.isComplete) {
                item {
                    Button(
                        onClick = onComplete,
                        modifier = Modifier.fillMaxWidth(),
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary)
                    ) { Text("Done") }
                }
            }

            if (uiState.isComplete || uiState.errorMessage != null) {
                item {
                    OutlinedButton(
                        onClick = { viewModel.resetCalibration() },
                        modifier = Modifier.fillMaxWidth()
                    ) { Text("Start Over") }
                }
            }

            item { Spacer(modifier = Modifier.height(32.dp)) }
        }
    }
}

@Composable
fun AnimatedInstruction(uiState: CalibrationUiState) {
    val infiniteTransition = rememberInfiniteTransition()
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = androidx.compose.animation.core.LinearEasing),
            repeatMode = RepeatMode.Restart
        )
    )
    val bounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 20f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = androidx.compose.animation.core.FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        )
    )

    when (uiState.currentStep) {
        0 -> Icon(
            Icons.Default.RotateRight,
            contentDescription = "Gyroscope",
            modifier = Modifier.size(80.dp).rotate(if (uiState.isCollecting) rotation else 0f),
            tint = if (uiState.isCollecting) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
        1 -> {
            // Use a proper vector drawable; fallback to existing icon
            val icon = try {
                ImageVector.vectorResource(R.drawable.ic_phone_android)
            } catch (e: Exception) {
                Icons.Default.PhoneAndroid
            }
            Icon(
                icon,
                contentDescription = "Accelerometer",
                modifier = Modifier.size(80.dp),
                tint = if (uiState.isCollecting) MaterialTheme.colorScheme.primary
                else MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        2 -> Icon(
            Icons.Default.MyLocation,
            contentDescription = "Magnetometer",
            modifier = Modifier.size(80.dp).offset(y = bounce.dp),
            tint = if (uiState.isCollecting) MaterialTheme.colorScheme.primary
            else MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}