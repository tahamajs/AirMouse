package com.airmouse.presentation.ui.files

import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowUpward
import androidx.compose.material.icons.automirrored.filled.Download
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.airmouse.files.FileTransferService
import com.airmouse.presentation.navigation.NavigationActions
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun FileTransferScreen(
    navigationActions: NavigationActions,
    viewModel: FileTransferViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val scope = rememberCoroutineScope()
    val context = LocalContext.current

    // File picker launcher for upload
    val filePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        if (uri != null) {
            val fileName = uri.lastPathSegment ?: "file"
            viewModel.uploadFile(uri, fileName)
        }
    }

    // Dialog state for manual download
    var showDownloadDialog by remember { mutableStateOf(false) }
    var downloadFileName by remember { mutableStateOf("") }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Folder, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("File Transfer", fontWeight = FontWeight.Bold)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = { navigationActions.navigateBack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(onClick = { viewModel.clearHistory() }) {
                        Icon(Icons.Default.Delete, contentDescription = "Clear History")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                )
            )
        },
        floatingActionButton = {
            if (uiState.isTransferActive) {
                FloatingActionButton(
                    onClick = { viewModel.cancelActiveTransfer() },
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                ) {
                    Icon(Icons.Default.Close, contentDescription = "Cancel")
                }
            }
        }
    ) { paddingValues ->
        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Action buttons
            item {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Button(
                        onClick = { filePickerLauncher.launch(arrayOf("*/*")) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.ArrowUpward, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Upload")
                    }
                    Button(
                        onClick = { showDownloadDialog = true },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.AutoMirrored.Filled.Download, contentDescription = null)
                        Spacer(Modifier.width(4.dp))
                        Text("Download")
                    }
                }
            }

            // Active transfer
            if (uiState.currentTransfer != null) {
                item {
                    ActiveTransferCard(
                        transfer = uiState.currentTransfer,
                        onCancel = { viewModel.cancelActiveTransfer() }
                    )
                }
            }

            // Queue count
            if (uiState.queueSize > 0) {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer)
                    ) {
                        Row(
                            modifier = Modifier.padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(Icons.Default.Pending, contentDescription = null)
                            Text("Queue: ${uiState.queueSize} file(s) waiting")
                        }
                    }
                }
            }

            // Completed transfers
            if (uiState.completedTransfers.isNotEmpty()) {
                item {
                    Text(
                        text = "History (${uiState.completedTransfers.size})",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
                items(uiState.completedTransfers) { transfer ->
                    TransferHistoryItem(transfer = transfer)
                }
            } else {
                item {
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
                    ) {
                        Column(
                            modifier = Modifier.padding(24.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(Icons.Default.Folder, contentDescription = null, modifier = Modifier.size(48.dp))
                            Spacer(Modifier.height(8.dp))
                            Text("No transfers yet")
                            Text(
                                "Upload or download files to/from the server",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }
    }

    // Download dialog
    if (showDownloadDialog) {
        AlertDialog(
            onDismissRequest = { showDownloadDialog = false },
            title = { Text("Download File") },
            text = {
                Column {
                    Text("Enter the filename to download from the server:")
                    Spacer(Modifier.height(8.dp))
                    OutlinedTextField(
                        value = downloadFileName,
                        onValueChange = { downloadFileName = it },
                        label = { Text("Filename") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        if (downloadFileName.isNotBlank()) {
                            viewModel.downloadFile(downloadFileName)
                            showDownloadDialog = false
                            downloadFileName = ""
                        }
                    }
                ) {
                    Text("Download")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDownloadDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }
}

// ============================================================
// ViewModel
// ============================================================

@HiltViewModel
class FileTransferViewModel @Inject constructor(
    private val fileTransferService: FileTransferService
) : androidx.lifecycle.ViewModel() {

    private val _uiState = MutableStateFlow(FileTransferUiState())
    val uiState: StateFlow<FileTransferUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            fileTransferService.state.collect { state ->
                _uiState.update {
                    it.copy(
                        currentTransfer = state.currentTransfer,
                        completedTransfers = state.completedTransfers,
                        queueSize = state.queueSize,
                        isTransferActive = state.isActive
                    )
                }
            }
        }
    }

    fun uploadFile(uri: Uri, fileName: String) {
        fileTransferService.queueFileForUpload(uri, fileName)
    }

    fun downloadFile(fileName: String) {
        fileTransferService.downloadFile(fileName, "")
    }

    fun cancelActiveTransfer() {
        val transfer = _uiState.value.currentTransfer
        if (transfer != null) {
            fileTransferService.cancelTransfer(transfer.id)
        }
    }

    fun clearHistory() {
        fileTransferService.clearCompletedTransfers()
    }

    data class FileTransferUiState(
        val currentTransfer: FileTransferService.TransferInfo? = null,
        val completedTransfers: List<FileTransferService.TransferInfo> = emptyList(),
        val queueSize: Int = 0,
        val isTransferActive: Boolean = false
    )
}

// ============================================================
// UI Components
// ============================================================

@Composable
fun ActiveTransferCard(
    transfer: FileTransferService.TransferInfo,
    onCancel: () -> Unit
) {
    val animatedProgress by animateFloatAsState(
        targetValue = transfer.progress / 100f,
        animationSpec = tween(500)
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        if (transfer.direction == FileTransferService.TransferInfo.Direction.UPLOAD)
                            Icons.AutoMirrored.Filled.ArrowUpward
                        else
                            Icons.AutoMirrored.Filled.Download,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary
                    )
                    Spacer(Modifier.width(8.dp))
                    Column {
                        Text(
                            transfer.fileName,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            "${formatSize(transfer.transferred)} / ${formatSize(transfer.fileSize)}",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
                Text(
                    "${transfer.progress.toInt()}%",
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            LinearProgressIndicator(
                progress = { animatedProgress },
                modifier = Modifier.fillMaxWidth().height(8.dp),
                color = MaterialTheme.colorScheme.primary,
                trackColor = MaterialTheme.colorScheme.surface
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    "Status: ${transfer.status.name.lowercase()}",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                TextButton(onClick = onCancel) {
                    Text("Cancel", color = MaterialTheme.colorScheme.error)
                }
            }
        }
    }
}

@Composable
fun TransferHistoryItem(
    transfer: FileTransferService.TransferInfo
) {
    val (icon, iconColor) = when (transfer.status) {
        FileTransferService.TransferInfo.Status.COMPLETED -> Icons.Default.CheckCircle to Color(0xFF4CAF50)
        FileTransferService.TransferInfo.Status.FAILED -> Icons.Default.Error to Color(0xFFF44336)
        FileTransferService.TransferInfo.Status.CANCELLED -> Icons.Default.Cancel to Color(0xFFFF9800)
        else -> Icons.Default.Info to Color(0xFF9E9E9E)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(icon, contentDescription = null, tint = iconColor, modifier = Modifier.size(24.dp))
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        transfer.fileName,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        if (transfer.status == FileTransferService.TransferInfo.Status.COMPLETED)
                            formatSize(transfer.fileSize)
                        else
                            "${transfer.progress.toInt()}%",
                        fontSize = 12.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        "${transfer.direction.name.lowercase()} • ${formatDuration(transfer.startTime, transfer.endTime)}",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        transfer.status.name.lowercase(),
                        fontSize = 11.sp,
                        color = iconColor
                    )
                }
                if (transfer.error != null) {
                    Text(
                        "Error: ${transfer.error}",
                        fontSize = 11.sp,
                        color = Color(0xFFF44336)
                    )
                }
            }
        }
    }
}

// ============================================================
// Formatting Helpers
// ============================================================

private fun formatSize(bytes: Long): String {
    if (bytes < 1024) return "$bytes B"
    val units = arrayOf("KB", "MB", "GB", "TB")
    val exp = (Math.log(bytes.toDouble()) / Math.log(1024.0)).toInt()
    val size = bytes / Math.pow(1024.0, exp.toDouble())
    return String.format("%.1f %s", size, units[exp - 1])
}

private fun formatDuration(start: Long, end: Long?): String {
    if (end == null) return "in progress"
    val duration = end - start
    val seconds = duration / 1000
    return if (seconds < 60) "${seconds}s" else "${seconds / 60}m ${seconds % 60}s"
}