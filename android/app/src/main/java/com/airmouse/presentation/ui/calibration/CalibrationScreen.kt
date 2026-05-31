package com.airmouse.presentation.ui.calibration

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Calibration Wizard") }) },
        bottomBar = {
            BottomAppBar {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Button(
                        onClick = { viewModel.previousStep() },
                        enabled = uiState.currentStep > 0 && !uiState.isCollecting
                    ) {
                        Text("Back")
                    }
                    Button(
                        onClick = { if (uiState.isCollecting) viewModel.recordPosition() else viewModel.nextStep() },
                        enabled = !uiState.isComplete
                    ) {
                        Text(if (uiState.isCollecting) "Record" else "Next")
                    }
                    Button(
                        onClick = { viewModel.abortCalibration() },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.error)
                    ) {
                        Text("Stop")
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
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            item {
                Text(uiState.stepTitle, style = MaterialTheme.typography.headlineSmall)
                Spacer(modifier = Modifier.height(8.dp))
                Text(uiState.stepDescription)
                Spacer(modifier = Modifier.height(16.dp))
                LinearProgressIndicator(
                    progress = uiState.progress / 100f,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text("${uiState.progress}%")
                Spacer(modifier = Modifier.height(16.dp))
                Text(uiState.statusMessage)
                if (uiState.isComplete) {
                    Spacer(modifier = Modifier.height(16.dp))
                    Button(onClick = onComplete) { Text("Done") }
                }
                uiState.errorMessage?.let {
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(it, color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}