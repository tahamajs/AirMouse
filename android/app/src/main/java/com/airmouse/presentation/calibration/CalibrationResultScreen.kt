package com.airmouse.presentation.ui.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.presentation.navigation.NavigationActions

@Composable
fun CalibrationResultScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onContinue: () -> Unit = {},
    onRecalibrate: () -> Unit = {},
    onShare: (() -> Unit)? = null,
    onExport: (() -> Unit)? = null
) {
    val calibrationData by viewModel.calibrationData.collectAsStateWithLifecycle()
    val calibrationStatus by viewModel.calibrationStatus.collectAsStateWithLifecycle()

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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            ResultCard(
                calibrationData = calibrationData,
                calibrationStatus = calibrationStatus
            )

            Button(
                onClick = {
                    onContinue()
                    navigationActions.navigateToHome()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Continue")
            }

            Button(
                onClick = {
                    onRecalibrate()
                    navigationActions.navigateToCalibration()
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Recalibrate")
            }

            if (onShare != null) {
                Button(onClick = onShare, modifier = Modifier.fillMaxWidth()) {
                    Text("Share")
                }
            }

            if (onExport != null) {
                Button(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text("Export")
                }
            }
        }
    }
}

@Composable
private fun ResultCard(
    calibrationData: CalibrationData?,
    calibrationStatus: CalibrationStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text("Status", fontWeight = FontWeight.Bold)
            Text(calibrationStatus.name)
            Text("Data: ${if (calibrationData?.isCalibrated == true) "Saved" else "Missing"}")
        }
    }
}
