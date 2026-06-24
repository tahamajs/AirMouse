package com.airmouse.presentation.ui.calibration

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.airmouse.presentation.navigation.NavigationActions

@Composable
fun CalibrationProcessScreen(
    navigationActions: NavigationActions
) {
    Scaffold(
        topBar = {
            TopAppBar(title = { Text("Calibration") })
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Calibration is running.", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Text("Use the main calibration screen to start, retry, or review results.")
            Button(onClick = { navigationActions.navigateBack() }, modifier = Modifier.fillMaxWidth()) {
                Text("Back")
            }
        }
    }
}
