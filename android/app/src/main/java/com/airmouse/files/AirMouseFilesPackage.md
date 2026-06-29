# 📘 Air Mouse Files Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.files` package handles **file transfer operations** between the Android device and the PC server. It provides functionality for uploading files, downloading files, managing transfer queues, and monitoring transfer progress.

```
com.airmouse.files/
├── FileTransferService.kt          # Core file transfer service
└── FileTransferServiceImpl.kt      # Implementation of the transfer service
```

---

## 📦 1. FileTransferService (Interface)

### Purpose
Defines the contract for **file transfer operations** between the Android device and the PC server.

### Interface Definition

```kotlin
interface FileTransferService {
    
    // ============================================================
    // File Transfer Operations
    // ============================================================
    
    /**
     * Transfer a file to the server
     * @param file The file to transfer
     * @param destination The destination path on the server
     */
    suspend fun transferFile(file: File, destination: String)
    
    /**
     * Queue a file for transfer (non-blocking)
     * @param file The file to queue
     * @param destination The destination path on the server
     * @return The ID of the queued transfer
     */
    suspend fun queueTransfer(file: File, destination: String): String
    
    /**
     * Cancel an ongoing transfer
     * @param transferId The ID of the transfer to cancel
     */
    suspend fun cancelTransfer(transferId: String)
    
    /**
     * Get the current transfer status
     * @param transferId The ID of the transfer
     * @return The transfer status, or null if not found
     */
    suspend fun getTransferStatus(transferId: String): Transfer?
    
    /**
     * Get all transfers
     * @return List of all transfers
     */
    suspend fun getAllTransfers(): List<Transfer>
    
    // ============================================================
    // Transfer Management
    // ============================================================
    
    /**
     * Clear completed transfers from the queue
     */
    suspend fun clearCompletedTransfers()
    
    /**
     * Clear all transfers from the queue
     */
    suspend fun clearAllTransfers()
    
    /**
     * Pause all transfers
     */
    suspend fun pauseAllTransfers()
    
    /**
     * Resume all transfers
     */
    suspend fun resumeAllTransfers()
    
    // ============================================================
    // Folder Operations
    // ============================================================
    
    /**
     * Open the transfer folder
     * @return The folder where files are stored
     */
    fun openTransferFolder(): File
    
    /**
     * Get the transfer folder path
     * @return The path to the transfer folder
     */
    fun getTransferFolderPath(): String
    
    // ============================================================
    // Observables (Reactive)
    // ============================================================
    
    /**
     * Observe the transfer state
     * @return Flow of TransferState
     */
    fun observeTransferState(): Flow<TransferState>
    
    /**
     * Observe active transfers
     * @return Flow of active transfers
     */
    fun observeActiveTransfers(): Flow<List<Transfer>>
    
    /**
     * Observe transfer progress for a specific transfer
     * @param transferId The ID of the transfer
     * @return Flow of progress (0.0 - 1.0)
     */
    fun observeTransferProgress(transferId: String): Flow<Float>
}
```

### Data Models

```kotlin
/**
 * Represents a single file transfer
 */
data class Transfer(
    val id: String,                    // Unique identifier
    val fileName: String,              // Name of the file
    val filePath: String,              // Local path of the file
    val destination: String,           // Server destination path
    val totalSize: Long,               // Total file size in bytes
    val transferredBytes: Long,        // Bytes transferred so far
    val progress: Float,               // Progress (0.0 - 1.0)
    val status: TransferStatus,        // Current status
    val createdAt: Long,               // Creation timestamp
    val startedAt: Long?,              // Start timestamp (nullable)
    val completedAt: Long?,            // Completion timestamp (nullable)
    val errorMessage: String?          // Error message if failed
)

/**
 * Transfer status enum
 */
enum class TransferStatus {
    QUEUED,        // Waiting to start
    UPLOADING,     // Currently uploading
    DOWNLOADING,   // Currently downloading
    PAUSED,        // Paused by user
    COMPLETED,     // Successfully completed
    FAILED,        // Failed with error
    CANCELLED      // Cancelled by user
}

/**
 * Transfer state for reactive updates
 */
data class TransferState(
    val isTransferring: Boolean = false,
    val queueSize: Int = 0,
    val activeTransfers: List<Transfer> = emptyList(),
    val completedTransfers: List<Transfer> = emptyList(),
    val totalProgress: Float = 0f,
    val currentTransfer: Transfer? = null,
    val transferSpeed: Long = 0,       // Bytes per second
    val estimatedTimeRemaining: Long = 0  // Milliseconds
)
```

---

## 📂 2. FileTransferServiceImpl (Implementation)

### Purpose
Implements the `FileTransferService` interface, handling the actual file transfer logic, queue management, and progress tracking.

### Key Components

#### 1. State Management

```kotlin
@Singleton
class FileTransferServiceImpl @Inject constructor(
    private val connectionManager: ConnectionManager,
    private val context: Context
) : FileTransferService {
    
    // State flows for reactive updates
    private val _transferState = MutableStateFlow(TransferState())
    override fun observeTransferState(): Flow<TransferState> = _transferState.asStateFlow()
    
    private val _activeTransfers = MutableStateFlow<List<Transfer>>(emptyList())
    override fun observeActiveTransfers(): Flow<List<Transfer>> = _activeTransfers.asStateFlow()
    
    // Transfer tracking
    private val transfers = mutableMapOf<String, Transfer>()
    private val transferProgress = mutableMapOf<String, MutableStateFlow<Float>>()
    private val transferQueue = mutableListOf<String>()
    private var isPaused = false
    private var currentJob: Job? = null
    
    // Speed tracking
    private var lastBytesTransferred = 0L
    private var lastSpeedUpdateTime = 0L
}
```

#### 2. Transfer Queue Management

```kotlin
override suspend fun queueTransfer(file: File, destination: String): String {
    val id = UUID.randomUUID().toString()
    val transfer = Transfer(
        id = id,
        fileName = file.name,
        filePath = file.absolutePath,
        destination = destination,
        totalSize = file.length(),
        transferredBytes = 0,
        progress = 0f,
        status = TransferStatus.QUEUED,
        createdAt = System.currentTimeMillis(),
        startedAt = null,
        completedAt = null,
        errorMessage = null
    )
    
    transfers[id] = transfer
    transferQueue.add(id)
    updateState()
    
    // Start processing queue if not paused and no active transfer
    if (!isPaused && currentJob == null) {
        processQueue()
    }
    
    return id
}

private suspend fun processQueue() {
    if (transferQueue.isEmpty() || isPaused) return
    
    val id = transferQueue.removeAt(0)
    val transfer = transfers[id] ?: return
    
    currentJob = coroutineScope.launch {
        try {
            transferFile(transfer)
        } finally {
            currentJob = null
            processQueue() // Process next in queue
        }
    }
}
```

#### 3. File Transfer Logic

```kotlin
private suspend fun transferFile(transfer: Transfer) {
    val file = File(transfer.filePath)
    if (!file.exists()) {
        updateTransferStatus(transfer.id, TransferStatus.FAILED, "File not found")
        return
    }
    
    updateTransferStatus(transfer.id, TransferStatus.UPLOADING)
    val startTime = System.currentTimeMillis()
    lastBytesTransferred = 0
    lastSpeedUpdateTime = startTime
    
    try {
        val fileContent = file.readBytes()
        val chunkSize = 8192
        var offset = 0
        
        while (offset < fileContent.size) {
            if (isPaused) {
                updateTransferStatus(transfer.id, TransferStatus.PAUSED)
                return
            }
            
            val chunk = fileContent.copyOfRange(offset, min(offset + chunkSize, fileContent.size))
            val message = buildTransferMessage(transfer, chunk, offset, fileContent.size)
            
            val success = connectionManager.send(message)
            if (!success) {
                throw Exception("Failed to send chunk")
            }
            
            offset += chunkSize
            updateTransferProgress(transfer.id, offset.toFloat() / fileContent.size)
            updateSpeed(fileContent.size, offset)
        }
        
        updateTransferStatus(transfer.id, TransferStatus.COMPLETED)
    } catch (e: Exception) {
        updateTransferStatus(transfer.id, TransferStatus.FAILED, e.message)
    }
}
```

#### 4. Progress Tracking

```kotlin
private fun updateTransferProgress(id: String, progress: Float) {
    val transfer = transfers[id] ?: return
    val updated = transfer.copy(
        progress = progress.coerceIn(0f, 1f),
        transferredBytes = (progress * transfer.totalSize).toLong()
    )
    transfers[id] = updated
    
    transferProgress[id]?.value = progress
    updateState()
}

private fun updateSpeed(totalSize: Long, transferred: Long) {
    val now = System.currentTimeMillis()
    val timeDelta = now - lastSpeedUpdateTime
    
    if (timeDelta > 1000) {
        val bytesDelta = transferred - lastBytesTransferred
        val speed = if (timeDelta > 0) (bytesDelta * 1000) / timeDelta else 0
        _transferState.update { it.copy(transferSpeed = speed) }
        
        lastBytesTransferred = transferred
        lastSpeedUpdateTime = now
    }
}
```

#### 5. State Updates

```kotlin
private fun updateState() {
    val allTransfers = transfers.values.toList()
    val active = allTransfers.filter { 
        it.status == TransferStatus.UPLOADING || 
        it.status == TransferStatus.DOWNLOADING 
    }
    val completed = allTransfers.filter { it.status == TransferStatus.COMPLETED }
    val queued = allTransfers.filter { it.status == TransferStatus.QUEUED }
    
    val totalBytes = allTransfers.sumOf { it.totalSize }
    val transferredBytes = allTransfers.sumOf { it.transferredBytes }
    val totalProgress = if (totalBytes > 0) transferredBytes.toFloat() / totalBytes else 0f
    
    _transferState.update {
        it.copy(
            isTransferring = active.isNotEmpty(),
            queueSize = queued.size,
            activeTransfers = active,
            completedTransfers = completed,
            totalProgress = totalProgress,
            currentTransfer = active.firstOrNull()
        )
    }
    
    _activeTransfers.value = active
}
```

#### 6. Transfer Message Building

```kotlin
private fun buildTransferMessage(
    transfer: Transfer,
    chunk: ByteArray,
    offset: Int,
    totalSize: Int
): String {
    val isFirst = offset == 0
    val isLast = offset + chunk.size >= totalSize
    
    return JSONObject().apply {
        put("type", "file_transfer")
        put("action", when {
            isFirst -> "start"
            isLast -> "end"
            else -> "continue"
        })
        put("transfer_id", transfer.id)
        put("file_name", transfer.fileName)
        put("destination", transfer.destination)
        put("total_size", totalSize)
        put("offset", offset)
        put("chunk", Base64.encodeToString(chunk, Base64.NO_WRAP))
        put("is_first", isFirst)
        put("is_last", isLast)
    }.toString()
}
```

#### 7. Folder Operations

```kotlin
override fun openTransferFolder(): File {
    val folder = File(context.getExternalFilesDir(null), "transfers")
    if (!folder.exists()) {
        folder.mkdirs()
    }
    return folder
}

override fun getTransferFolderPath(): String {
    return openTransferFolder().absolutePath
}
```

---

## 🔄 Data Flow

### File Transfer Sequence

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         FILE TRANSFER FLOW                             │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User selects file to transfer                                       │
│         │                                                               │
│         ▼                                                               │
│  2. queueTransfer(file, destination)                                    │
│         │                                                               │
│         ▼                                                               │
│  3. Transfer added to queue (status: QUEUED)                           │
│         │                                                               │
│         ▼                                                               │
│  4. processQueue() picks next transfer                                 │
│         │                                                               │
│         ▼                                                               │
│  5. File read in chunks (8KB each)                                     │
│         │                                                               │
│         ▼                                                               │
│  6. Each chunk sent via ConnectionManager                              │
│         │                                                               │
│         ▼                                                               │
│  7. Progress updated (observable via Flow)                             │
│         │                                                               │
│         ▼                                                               │
│  8. File transfer completes (status: COMPLETED)                        │
│         │                                                               │
│         ▼                                                               │
│  9. Process next file in queue                                          │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Message Protocol

```json
{
  "type": "file_transfer",
  "action": "start|continue|end",
  "transfer_id": "uuid-1234",
  "file_name": "document.pdf",
  "destination": "/documents/",
  "total_size": 1048576,
  "offset": 0,
  "chunk": "base64_encoded_data",
  "is_first": true,
  "is_last": false
}
```

---

## 📋 Public API Summary

| Method | Purpose | Return Type |
|--------|---------|-------------|
| `transferFile(file, destination)` | Transfer a file (blocking) | `suspend fun` |
| `queueTransfer(file, destination)` | Queue a file for transfer | `suspend fun: String` |
| `cancelTransfer(transferId)` | Cancel an ongoing transfer | `suspend fun` |
| `getTransferStatus(transferId)` | Get transfer status | `suspend fun: Transfer?` |
| `getAllTransfers()` | Get all transfers | `suspend fun: List<Transfer>` |
| `clearCompletedTransfers()` | Clear completed transfers | `suspend fun` |
| `clearAllTransfers()` | Clear all transfers | `suspend fun` |
| `pauseAllTransfers()` | Pause all transfers | `suspend fun` |
| `resumeAllTransfers()` | Resume all transfers | `suspend fun` |
| `openTransferFolder()` | Get transfer folder | `fun: File` |
| `getTransferFolderPath()` | Get transfer folder path | `fun: String` |
| `observeTransferState()` | Observe transfer state | `fun: Flow<TransferState>` |
| `observeActiveTransfers()` | Observe active transfers | `fun: Flow<List<Transfer>>` |
| `observeTransferProgress(id)` | Observe specific transfer progress | `fun: Flow<Float>` |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Asynchronous** | All transfers are non-blocking with coroutines |
| **Queue Management** | FIFO queue with pause/resume support |
| **Progress Tracking** | Real-time progress updates via Flow |
| **Chunked Transfer** | Large files split into manageable chunks |
| **Speed Monitoring** | Transfer speed tracking for UX |
| **Error Handling** | Graceful error recovery and reporting |
| **State Management** | Comprehensive transfer state tracking |
| **Reactive Updates** | StateFlow for UI updates |

---

**The File Transfer Service provides a robust, reliable, and user-friendly way to transfer files between the Android device and the PC server, with real-time progress tracking and queue management.**