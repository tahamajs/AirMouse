package com.airmouse.files

import android.content.ContentResolver
import android.content.Context
import android.net.Uri
import android.util.Log
import com.airmouse.network.ConnectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.io.*
import java.security.MessageDigest
import java.util.*
import javax.inject.Inject
import javax.inject.Singleton
import org.json.JSONObject

/**
 * Complete file transfer service supporting upload/download with binary WebSocket.
 * Features:
 * - Upload files from phone to PC (via content URI)
 * - Download files from PC to phone
 * - Queue management
 * - Progress reporting
 * - MD5 checksum verification
 * - Resumable transfers (optional)
 */
@Singleton
class FileTransferService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val connectionManager: ConnectionManager
) {

    companion object {
        private const val TAG = "FileTransferService"
        private const val CHUNK_SIZE = 64 * 1024       // 64 KB
        private const val TRANSFER_DIR = "transfers"
        private const val PROTOCOL_VERSION = 1
    }

    data class TransferState(
        val isActive: Boolean = false,
        val currentTransfer: TransferInfo? = null,
        val completedTransfers: List<TransferInfo> = emptyList(),
        val queueSize: Int = 0
    )

    data class TransferInfo(
        val id: String,
        val fileName: String,
        val fileSize: Long,
        val transferred: Long,
        val progress: Float,
        val direction: Direction,
        val status: Status,
        val startTime: Long,
        val endTime: Long? = null,
        val error: String? = null,
        val md5: String? = null
    ) {
        enum class Direction { UPLOAD, DOWNLOAD }
        enum class Status { PENDING, IN_PROGRESS, COMPLETED, FAILED, CANCELLED }
    }

    private val _state = MutableStateFlow(TransferState())
    val state: StateFlow<TransferState> = _state.asStateFlow()

    private val transferQueue = mutableListOf<TransferInfo>()
    private var activeTransfer: TransferInfo? = null
    private var transferJob: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val transfersDir = File(context.filesDir, TRANSFER_DIR)

    // For binary message handling during download
    private var downloadCallback: ((ByteArray) -> Unit)? = null
    private var currentDownloadId: String? = null
    private var currentDownloadStream: FileOutputStream? = null
    private var currentDownloadMd5 = MessageDigest.getInstance("MD5")
    private var currentDownloadExpectedMd5: String? = null
    private var currentDownloadExpectedSize: Long = 0L
    private var currentDownloadTempFile: File? = null
    private var currentDownloadCompletionDeferred: CompletableDeferred<Unit>? = null
    private var transferredBytes = 0L
    private val messageListener: (String) -> Unit = { handleServerMessage(it) }
    private val binaryListener: (ByteArray) -> Unit = { onBinaryMessage(it) }

    init {
        if (!transfersDir.exists()) transfersDir.mkdirs()
        loadTransferHistory()
        setupBinaryMessageListener()
    }

    private fun setupBinaryMessageListener() {
        connectionManager.addMessageListener(messageListener)
        connectionManager.addBinaryMessageListener(binaryListener)
    }

    private fun loadTransferHistory() {
        // Load from PreferencesManager – simplified
    }

    private fun saveTransferHistory() {
        // Save completed transfers (last 50) to SharedPreferences
    }

    // ----------------------------------------------------------------------
    // Public API
    // ----------------------------------------------------------------------

    fun queueFileForUpload(fileUri: Uri, fileName: String) {
        val file = getFileFromUri(fileUri, fileName) ?: return
        val size = file.length()
        val md5 = calculateMd5(file)

        val transfer = TransferInfo(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            fileSize = size,
            transferred = 0,
            progress = 0f,
            direction = TransferInfo.Direction.UPLOAD,
            status = TransferInfo.Status.PENDING,
            startTime = System.currentTimeMillis(),
            md5 = md5
        )

        transferQueue.add(transfer)
        updateQueueState()

        if (activeTransfer == null) startNextTransfer()
        Log.i(TAG, "Queued upload: $fileName (${size} bytes)")
    }

    fun downloadFile(fileName: String, remotePath: String) {
        val transfer = TransferInfo(
            id = UUID.randomUUID().toString(),
            fileName = fileName,
            fileSize = 0,
            transferred = 0,
            progress = 0f,
            direction = TransferInfo.Direction.DOWNLOAD,
            status = TransferInfo.Status.PENDING,
            startTime = System.currentTimeMillis()
        )

        transferQueue.add(transfer)
        updateQueueState()

        if (activeTransfer == null) startNextTransfer()
        Log.i(TAG, "Queued download: $fileName")
    }

    fun cancelTransfer(transferId: String) {
        if (activeTransfer?.id == transferId) {
            transferJob?.cancel()
            updateTransferStatus(transferId, TransferInfo.Status.CANCELLED)
            activeTransfer = null
            startNextTransfer()
        } else {
            transferQueue.removeAll { it.id == transferId }
            updateQueueState()
        }
        Log.i(TAG, "Cancelled transfer: $transferId")
    }

    fun clearCompletedTransfers() {
        _state.value = _state.value.copy(completedTransfers = emptyList())
        saveTransferHistory()
    }

    fun openTransferFolder(): File = transfersDir

    fun getTransferFile(fileName: String): File? {
        val file = File(transfersDir, fileName)
        return if (file.exists()) file else null
    }

    // ----------------------------------------------------------------------
    // Transfer management
    // ----------------------------------------------------------------------

    private fun startNextTransfer() {
        if (transferQueue.isEmpty()) return
        activeTransfer = transferQueue.removeAt(0)
        updateQueueState()

        transferJob = scope.launch {
            try {
                when (activeTransfer?.direction) {
                    TransferInfo.Direction.UPLOAD -> uploadFile(activeTransfer!!)
                    TransferInfo.Direction.DOWNLOAD -> downloadFile(activeTransfer!!)
                    else -> {}
                }
            } catch (e: CancellationException) {
                updateTransferStatus(activeTransfer!!.id, TransferInfo.Status.CANCELLED)
                Log.i(TAG, "Transfer cancelled: ${activeTransfer?.fileName}")
            } catch (e: Exception) {
                updateTransferError(activeTransfer!!.id, e.message ?: "Transfer failed")
            } finally {
                activeTransfer = null
                startNextTransfer()
            }
        }
    }

    private suspend fun uploadFile(transfer: TransferInfo) {
        updateTransferStatus(transfer.id, TransferInfo.Status.IN_PROGRESS)

        val file = getFileForUpload(transfer.fileName) ?: run {
            updateTransferError(transfer.id, "Local file not found")
            return
        }

        val fileSize = file.length()
        updateTransferSize(transfer.id, fileSize)

        // Send metadata
        val meta = JSONObject().apply {
            put("type", "file")
            put("action", "start")
            put("id", transfer.id)
            put("name", transfer.fileName)
            put("size", fileSize)
            put("md5", transfer.md5 ?: "")
            put("version", PROTOCOL_VERSION)
            put("direction", "upload")
        }.toString()
        if (!connectionManager.send(meta)) {
            throw IOException("Failed to send file start packet")
        }

        val inputStream = BufferedInputStream(FileInputStream(file))
        val buffer = ByteArray(CHUNK_SIZE)
        var transferred = 0L

        try {
            while (transferred < fileSize) {
                val bytesRead = inputStream.read(buffer)
                if (bytesRead <= 0) break

                val chunk = buffer.copyOf(bytesRead)
                if (!connectionManager.sendBinary(chunk)) {
                    throw IOException("Failed to send file chunk")
                }

                transferred += bytesRead
                updateTransferProgress(transfer.id, transferred, fileSize)
                delay(5) // small yield to avoid flooding
            }

            inputStream.close()
            if (!connectionManager.send(JSONObject().apply {
                    put("type", "file")
                    put("action", "complete")
                    put("id", transfer.id)
                    put("direction", "upload")
                }.toString())
            ) {
                throw IOException("Failed to send file completion packet")
            }
            updateTransferStatus(transfer.id, TransferInfo.Status.COMPLETED)
            Log.i(TAG, "Upload completed: ${transfer.fileName}")

        } catch (e: Exception) {
            inputStream.close()
            throw e
        }
    }

    private suspend fun downloadFile(transfer: TransferInfo) {
        updateTransferStatus(transfer.id, TransferInfo.Status.IN_PROGRESS)

        val outputFile = File(transfersDir, transfer.fileName)
        if (outputFile.exists()) outputFile.delete()

        currentDownloadId = transfer.id
        currentDownloadTempFile = File(transfersDir, ".download_${transfer.id}_${transfer.fileName}")
        if (currentDownloadTempFile?.exists() == true) currentDownloadTempFile?.delete()
        currentDownloadStream = FileOutputStream(currentDownloadTempFile)
        currentDownloadMd5.reset()
        currentDownloadExpectedMd5 = transfer.md5
        currentDownloadExpectedSize = transfer.fileSize
        transferredBytes = 0L

        val completionDeferred = CompletableDeferred<Unit>()
        currentDownloadCompletionDeferred = completionDeferred

        downloadCallback = { data ->
            if (currentDownloadId == transfer.id) {
                try {
                    currentDownloadStream?.write(data)
                    currentDownloadMd5.update(data)
                    transferredBytes += data.size
                    updateTransferProgress(transfer.id, transferredBytes, currentDownloadExpectedSize.takeIf { it > 0 } ?: transfer.fileSize)
                } catch (e: Exception) {
                    completionDeferred.completeExceptionally(e)
                }
            }
        }

        val request = JSONObject().apply {
            put("type", "file")
            put("action", "download")
            put("id", transfer.id)
            put("name", transfer.fileName)
            put("version", PROTOCOL_VERSION)
        }.toString()
        if (!connectionManager.send(request)) {
            throw IOException("Failed to send file download request")
        }

        try {
            withTimeout(300_000L) { completionDeferred.await() }
            currentDownloadStream?.flush()
            currentDownloadStream?.close()
            val actualMd5 = currentDownloadMd5.digest().joinToString("") { "%02x".format(it) }
            val expectedMd5 = currentDownloadExpectedMd5
            if (!expectedMd5.isNullOrBlank() && actualMd5 != expectedMd5) {
                throw IOException("MD5 mismatch: expected $expectedMd5, got $actualMd5")
            }
            if (currentDownloadExpectedSize > 0 && transferredBytes != currentDownloadExpectedSize) {
                throw IOException("Size mismatch: expected $currentDownloadExpectedSize, got $transferredBytes")
            }
            currentDownloadTempFile?.let { temp ->
                if (outputFile.exists()) outputFile.delete()
                if (!temp.renameTo(outputFile)) {
                    throw IOException("Failed to finalize downloaded file")
                }
            }
            updateTransferSize(transfer.id, transferredBytes)
            updateTransferStatus(transfer.id, TransferInfo.Status.COMPLETED)
            Log.i(TAG, "Download completed: ${transfer.fileName}")
        } catch (e: Exception) {
            currentDownloadStream?.close()
            currentDownloadTempFile?.delete()
            throw e
        } finally {
            downloadCallback = null
            currentDownloadId = null
            currentDownloadStream = null
            currentDownloadTempFile = null
            currentDownloadExpectedMd5 = null
            currentDownloadExpectedSize = 0L
            currentDownloadCompletionDeferred = null
            transferredBytes = 0L
        }
    }

    fun onBinaryMessage(data: ByteArray) {
        downloadCallback?.invoke(data)
    }

    private fun handleServerMessage(message: String) {
        try {
            val json = JSONObject(message)
            if (json.optString("type") != "file") return
            when (json.optString("action")) {
                "start" -> {
                    if (json.optString("id") == currentDownloadId) {
                        currentDownloadExpectedMd5 = json.optString("md5").takeIf { it.isNotBlank() } ?: currentDownloadExpectedMd5
                        if (json.has("size")) currentDownloadExpectedSize = json.optLong("size", currentDownloadExpectedSize)
                    }
                }
                "complete" -> {
                    if (json.optString("id") == currentDownloadId) {
                        currentDownloadStream?.flush()
                        currentDownloadCompletionDeferred?.complete(Unit)
                        if (json.has("md5")) {
                            currentDownloadExpectedMd5 = json.optString("md5")
                        }
                    }
                }
                "error" -> {
                    if (json.optString("id") == currentDownloadId) {
                        val error = json.optString("message", "File transfer failed")
                        currentDownloadStream?.close()
                        currentDownloadTempFile?.delete()
                        throw IOException(error)
                    }
                }
            }
        } catch (_: Exception) {
        }
    }

    // ----------------------------------------------------------------------
    // File utilities
    // ----------------------------------------------------------------------

    private fun getFileFromUri(uri: Uri, fileName: String): File? {
        val contentResolver = context.contentResolver
        return try {
            val inputStream = contentResolver.openInputStream(uri) ?: return null
            val tempFile = File(transfersDir, "temp_${System.currentTimeMillis()}_$fileName")
            tempFile.outputStream().use { output ->
                inputStream.copyTo(output)
            }
            inputStream.close()
            tempFile.renameTo(File(transfersDir, fileName))
            File(transfersDir, fileName)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to copy from URI", e)
            null
        }
    }

    private fun getFileForUpload(fileName: String): File? {
        val file = File(transfersDir, fileName)
        return if (file.exists()) file else null
    }

    private fun calculateMd5(file: File): String? {
        return try {
            val md = MessageDigest.getInstance("MD5")
            val buffer = ByteArray(8192)
            FileInputStream(file).use { fis ->
                var read: Int
                while (fis.read(buffer).also { read = it } != -1) {
                    md.update(buffer, 0, read)
                }
            }
            md.digest().joinToString("") { "%02x".format(it) }
        } catch (e: Exception) {
            Log.e(TAG, "MD5 calc failed", e)
            null
        }
    }

    // ----------------------------------------------------------------------
    // State updaters
    // ----------------------------------------------------------------------

    private fun updateQueueState() {
        _state.value = _state.value.copy(queueSize = transferQueue.size)
        saveTransferHistory()
    }

    private fun updateTransferProgress(transferId: String, transferred: Long, total: Long) {
        val progress = if (total > 0) (transferred.toFloat() / total) * 100f else 0f
        if (activeTransfer?.id == transferId) {
            activeTransfer = activeTransfer?.copy(transferred = transferred, progress = progress)
            _state.value = _state.value.copy(currentTransfer = activeTransfer)
        }
        // Update in completed list if present
        val updatedCompleted = _state.value.completedTransfers.map {
            if (it.id == transferId) it.copy(transferred = transferred, progress = progress) else it
        }
        _state.value = _state.value.copy(completedTransfers = updatedCompleted)
    }

    private fun updateTransferSize(transferId: String, size: Long) {
        if (activeTransfer?.id == transferId) {
            activeTransfer = activeTransfer?.copy(fileSize = size)
            _state.value = _state.value.copy(currentTransfer = activeTransfer)
        }
    }

    private fun updateTransferStatus(transferId: String, status: TransferInfo.Status) {
        val endTime = if (status == TransferInfo.Status.COMPLETED || status == TransferInfo.Status.FAILED) {
            System.currentTimeMillis()
        } else null

        if (activeTransfer?.id == transferId) {
            activeTransfer = activeTransfer?.copy(status = status, endTime = endTime)
            _state.value = _state.value.copy(currentTransfer = activeTransfer)
        }

        if (status == TransferInfo.Status.COMPLETED || status == TransferInfo.Status.FAILED || status == TransferInfo.Status.CANCELLED) {
            activeTransfer?.let { transfer ->
                val updatedTransfers = listOf(transfer) + _state.value.completedTransfers
                _state.value = _state.value.copy(completedTransfers = updatedTransfers.take(50))
            }
            saveTransferHistory()
        }
    }

    private fun updateTransferError(transferId: String, error: String) {
        if (activeTransfer?.id == transferId) {
            activeTransfer = activeTransfer?.copy(status = TransferInfo.Status.FAILED, error = error)
            _state.value = _state.value.copy(currentTransfer = activeTransfer)
        }
        updateTransferStatus(transferId, TransferInfo.Status.FAILED)
    }

    fun cleanup() {
        transferJob?.cancel()
        scope.cancel()
        downloadCallback = null
        currentDownloadStream?.close()
        connectionManager.removeMessageListener(messageListener)
        connectionManager.removeBinaryMessageListener(binaryListener)
    }
}
