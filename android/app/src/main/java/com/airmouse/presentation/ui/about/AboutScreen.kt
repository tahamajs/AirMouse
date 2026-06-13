package com.airmouse.presentation.ui.about

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.R
import com.airmouse.presentation.navigation.NavigationActions

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AboutScreen(
    navigationActions: NavigationActions,
    viewModel: AboutViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val version = uiState.version

    Scaffold(
        topBar = { TopAppBar(title = { Text("About") }, navigationIcon = { BackButton(navigationActions) }) }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier.fillMaxSize().padding(paddingValues).padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(painter = painterResource(R.drawable.ic_air_mouse), contentDescription = null, modifier = Modifier.size(80.dp))
                        Spacer(modifier = Modifier.height(8.dp))
                        Text("Air Mouse Pro", style = MaterialTheme.typography.headlineSmall)
                        Text("Version $version", style = MaterialTheme.typography.bodyMedium)
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("University of Tehran – Embedded Systems Lab", style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Developers", style = MaterialTheme.typography.titleMedium)
                        Text("• Arian Firoozi")
                        Text("• Arsalan Talaee")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Instructors", style = MaterialTheme.typography.titleMedium)
                        Text("• Dr. Mohsen Shokri")
                        Text("• Dr. Mehdi Kargahi")
                    }
                }
            }
            item {
                Card(modifier = Modifier.fillMaxWidth()) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text("Open Source Licenses", style = MaterialTheme.typography.titleMedium)
                        Text("• TensorFlow Lite\n• OkHttp\n• MPAndroidChart")
                    }
                }
            }
        }
    }
}

@Composable
private fun BackButton(navigationActions: NavigationActions) {
    IconButton(onClick = { navigationActions.navigateBack() }) {
        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
    }
}
