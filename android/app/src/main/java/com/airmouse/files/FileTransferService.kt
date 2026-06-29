package com.airmouse.files

import android.content.Context
import android.net.Uri
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.io.File
import java.util.UUID

// ============================================================
// Interfaces
// ============================================================

/**
 * Service for managing file transfers (upload/download).
 */
interface FileTransferService {
    val state: StateFlow<TransferState>

    fun queueFileForUpload(uri: Uri, fileName: String)
    fun downloadFile(fileName: String, destinationPath: String)
    fun transferFile(file: File, destination: String)
    fun cancelTransfer(id: String)
    fun clearCompletedTransfers()

    fun getTransferFolderPath(): String
    fun openTransferFolder(): Boolean
}

// ============================================================
// State Models
// ============================================================

/**
 * Current state of the file transfer system.
 */
data class TransferState(
    val queueSize: Int = 0,
    val currentTransfer: Transfer? = null,
    val completedTransfers: List<Transfer> = emptyList(),
    val failedTransfers: List<Transfer> = emptyList(),
    val isActive: Boolean = false
)

/**
 * A single file transfer.
 */
data class Transfer(
    val id: String = UUID.randomUUID().toString(),
    val fileName: String = "",
    val fileSize: Long = 0L,
    val transferred: Long = 0L,
    val progress: Float = 0f,
    val status: TransferStatus = TransferStatus.PENDING,
    val direction: TransferDirection = TransferDirection.UPLOAD,
    val startTime: Long = System.currentTimeMillis(),
    val endTime: Long? = null,
    val error: String? = null
) {
    /**
     * Check if the transfer is complete.
     */
    fun isComplete(): Boolean {
        return status == TransferStatus.COMPLETED
    }

    /**
     * Check if the transfer failed.
     */
    fun isFailed(): Boolean {
        return status == TransferStatus.FAILED
    }

    /**
     * Get the progress as a percentage (0-100).
     */
    fun getPercentage(): Int {
        return if (fileSize > 0) {
            ((transferred.toFloat() / fileSize) * 100).toInt().coerceIn(0, 100)
        } else {
            (progress * 100).toInt().coerceIn(0, 100)
        }
    }

    /**
     * Create a copy with updated progress.
     */
    fun withProgress(transferred: Long): Transfer {
        val newProgress = if (fileSize > 0) transferred.toFloat() / fileSize else 0f
        return copy(
            transferred = transferred,
            progress = newProgress.coerceIn(0f, 1f)
        )
    }

    /**
     * Mark the transfer as completed.
     */
    fun withComplete(): Transfer {
        return copy(
            status = TransferStatus.COMPLETED,
            progress = 1f,
            transferred = fileSize,
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * Mark the transfer as failed.
     */
    fun withError(error: String): Transfer {
        return copy(
            status = TransferStatus.FAILED,
            error = error,
            endTime = System.currentTimeMillis()
        )
    }

    /**
     * Mark the transfer as cancelled.
     */
    fun withCancelled(): Transfer {
        return copy(
            status = TransferStatus.CANCELLED,
            endTime = System.currentTimeMillis()
        )
    }
}

/**
 * Status of a transfer.
 */
enum class TransferStatus {
    PENDING,
    IN_PROGRESS,
    COMPLETED,
    FAILED,
    CANCELLED
}

/**
 * Direction of a transfer.
 */
enum class TransferDirection {
    UPLOAD,
    DOWNLOAD
}

// ============================================================
// Implementation
// ============================================================

/**
 * Implementation of FileTransferService.
 */
class FileTransferServiceImpl(
    private val context: Context
) : FileTransferService {

    private val _state = MutableStateFlow(TransferState())
    override val state: StateFlow<TransferState> = _state.asStateFlow()

    private val transfers = mutableMapOf<String, Transfer>()
    private var currentTransferId: String? = null

    override fun queueFileForUpload(uri: Uri, fileName: String) {
        val transfer = Transfer(
            fileName = fileName,
            direction = TransferDirection.UPLOAD,
            status = TransferStatus.PENDING
        )
        transfers[transfer.id] = transfer
        updateState()
    }

    override fun downloadFile(fileName: String, destinationPath: String) {
        val transfer = Transfer(
            fileName = fileName,
            direction = TransferDirection.DOWNLOAD,
            status = TransferStatus.PENDING
        )
        transfers[transfer.id] = transfer
        updateState()
    }

    override fun transferFile(file: File, destination: String) {
        val transfer = Transfer(
            fileName = file.name,
            fileSize = file.length(),
            direction = TransferDirection.UPLOAD,
            status = TransferStatus.PENDING
        )
        transfers[transfer.id] = transfer
        updateState()
        // Actual transfer implementation would go here
        startTransfer(transfer.id)
    }

    override fun cancelTransfer(id: String) {
        transfers[id]?.let { transfer ->
            transfers[id] = transfer.withCancelled()
            if (currentTransferId == id) {
                currentTransferId = null
            }
            updateState()
        }
    }

    override fun clearCompletedTransfers() {
        val activeTransfers = transfers.values.filter {
            it.status !in setOf(TransferStatus.COMPLETED, TransferStatus.FAILED, TransferStatus.CANCELLED)
        }.associateBy { it.id }
        transfers.clear()
        transfers.putAll(activeTransfers)
        if (currentTransferId !in transfers) {
            currentTransferId = null
        }
        updateState()
    }

    override fun getTransferFolderPath(): String {
        return context.getExternalFilesDir("transfers")?.absolutePath ?: context.filesDir.absolutePath
    }

    override fun openTransferFolder(): Boolean {
        val folder = getTransferFolderPath()
        val file = File(folder)
        return if (file.exists() && file.isDirectory) {
            // In a real app, you'd use Intent.ACTION_OPEN_DOCUMENT_TREE
            // or open the folder via a file manager intent
            true
        } else {
            file.mkdirs()
            true
        }
    }

    // ============================================================
    // Private Helpers
    // ============================================================

    private fun updateState() {
        val allTransfers = transfers.values.toList()
        val completed = allTransfers.filter { it.status == TransferStatus.COMPLETED }
        val failed = allTransfers.filter { it.status == TransferStatus.FAILED }
        val current = currentTransferId?.let { transfers[it] }
        val pending = allTransfers.count { it.status == TransferStatus.PENDING }
        val inProgress = allTransfers.count { it.status == TransferStatus.IN_PROGRESS }

        _state.update {
            it.copy(
                queueSize = pending + inProgress,
                currentTransfer = current,
                completedTransfers = completed,
                failedTransfers = failed,
                isActive = inProgress > 0 || pending > 0
            )
        }
    }

    private fun startTransfer(id: String) {
        currentTransferId = id
        transfers[id]?.let { transfers[id] = it.copy(status = TransferStatus.IN_PROGRESS) }
        updateState()
        // Simulate progress (in real app, this would be async)
        simulateProgress(id)
    }

    private fun simulateProgress(id: String) {
        // In production, this would be replaced with actual transfer logic
        // For now, it's a placeholder for the implementation
        val transfer = transfers[id] ?: return
        if (transfer.status == TransferStatus.COMPLETED) {
            return
        }

        // Simulate progress (for demo purposes only)
        val newTransferred = (transfer.transferred + (transfer.fileSize / 20)).coerceAtMost(transfer.fileSize)
        transfers[id] = transfer.withProgress(newTransferred)

        if (newTransferred >= transfer.fileSize && transfer.fileSize > 0) {
            transfers[id] = transfer.withComplete()
            currentTransferId = null
        }

        updateState()

        // Schedule next update if not complete
        if (newTransferred < transfer.fileSize || transfer.fileSize == 0L) {
            android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
                simulateProgress(id)
            }, 100)
        }
    }
}
