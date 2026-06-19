// app/src/main/java/com/airmouse/presentation/ui/gesture/GestureStudioUiState.kt
package com.airmouse.presentation.ui.gesture

import androidx.compose.ui.graphics.Color
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats
import com.airmouse.domain.model.GestureType

data class GestureStudioUiState(
    // Recording state
    val gestureName: String = "",
    val isRecording: Boolean = false,
    val status: String = "Ready",
    val statusColor: Color = Color(0xFF4CAF50),
    val progress: Int = 0,
    val recordingTime: Int = 0,
    val recordingQuality: RecordingQuality = RecordingQuality.UNKNOWN,
    val samplesCollected: Int = 0,
    val totalSamplesNeeded: Int = 50,

    // Gesture library
    val savedGestures: List<CustomGestureTemplate> = emptyList(),
    val selectedGesture: CustomGestureTemplate? = null,
    val trainingStats: GestureTrainingStats? = null,
    val filteredGestures: List<CustomGestureTemplate> = emptyList(),
    val searchQuery: String = "",
    val filterType: GestureType? = null,
    val sortBy: GestureSort = GestureSort.NAME,

    // Training state
    val isTraining: Boolean = false,
    val trainingProgress: Int = 0,
    val trainingCurrentGesture: String = "",
    val trainingTotalGestures: Int = 0,
    val trainingSamples: Int = 0,
    val trainingStatus: String = "",

    // UI dialogs
    val showDeleteDialog: Boolean = false,
    val showTrainDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showDetailsDialog: Boolean = false,
    val showPlaybackDialog: Boolean = false,
    val showAddGestureDialog: Boolean = false,
    val showEditGestureDialog: Boolean = false,

    // Dialog data
    val deleteGestureId: String? = null,
    val editGesture: CustomGestureTemplate? = null,
    val newGestureName: String = "",
    val newGestureAction: String = "",

    // Messages
    val errorMessage: String? = null,
    val successMessage: String? = null,
    val isLoading: Boolean = false,

    // Gesture recognition
    val lastRecognizedGesture: String? = null,
    val lastRecognitionConfidence: Float = 0f,
    val lastRecognitionTime: Long? = null,
    val recognizedGestureCount: Int = 0,

    // Visualization
    val showWaveform: Boolean = true,
    val selectedSensor: SensorType = SensorType.GYROSCOPE,
    val playbackSpeed: Float = 1f,
    val waveformData: GestureWaveformData = GestureWaveformData(),

    // Export/Import
    val exportPath: String? = null,
    val importProgress: Int = 0,
    val isImporting: Boolean = false,
    val exportFormat: ExportFormat = ExportFormat.JSON,

    // View mode
    val viewMode: ViewMode = ViewMode.GRID,
    val showFavoritesOnly: Boolean = false
)

enum class RecordingQuality(val displayName: String, val color: Color, val description: String) {
    EXCELLENT("Excellent", Color(0xFF4CAF50), "Perfect recording quality"),
    GOOD("Good", Color(0xFF8BC34A), "Good recording quality"),
    FAIR("Fair", Color(0xFFFFC107), "Acceptable recording quality"),
    POOR("Poor", Color(0xFFF44336), "Poor recording quality, consider retrying"),
    UNKNOWN("Unknown", Color(0xFF9E9E9E), "Quality not yet determined")
}

enum class SensorType(val displayName: String, val icon: String) {
    GYROSCOPE("Gyroscope", "⚡"),
    ACCELEROMETER("Accelerometer", "📊"),
    MAGNETOMETER("Magnetometer", "🧭"),
    ALL("All Sensors", "📱")
}

enum class GestureSort(val displayName: String) {
    NAME("Name"),
    USAGE("Most Used"),
    CREATED("Newest"),
    UPDATED("Recently Updated"),
    CONFIDENCE("Highest Confidence")
}

enum class ExportFormat(val displayName: String, val extension: String) {
    JSON("JSON", ".json"),
    CSV("CSV", ".csv"),
    XML("XML", ".xml")
}

enum class ViewMode(val displayName: String) {
    GRID("Grid"),
    LIST("List"),
    COMPACT("Compact")
}

data class GestureWaveformData(
    val samples: List<Float> = emptyList(),
    val maxValue: Float = 0f,
    val minValue: Float = 0f,
    val samplingRate: Int = 50,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun isEmpty(): Boolean = samples.isEmpty()
    fun getAverage(): Float = if (samples.isNotEmpty()) samples.average().toFloat() else 0f
    fun getPeakToPeak(): Float = maxValue - minValue
}