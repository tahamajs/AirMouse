package com.airmouse.presentation.ui.gesture

import androidx.compose.ui.graphics.Color
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GestureTrainingStats

data class GestureStudioUiState(
    // Recording state
    val gestureName: String = "",
    val isRecording: Boolean = false,
    val status: String = "Ready",
    val statusColor: Color = Color(0xFF4CAF50),
    val progress: Int = 0,
    val recordingTime: Int = 0,
    val recordingQuality: RecordingQuality = RecordingQuality.UNKNOWN,
    
    // Gesture library
    val savedGestures: List<CustomGestureTemplate> = emptyList(),
    val selectedGesture: CustomGestureTemplate? = null,
    val trainingStats: GestureTrainingStats? = null,
    
    // Training state
    val isTraining: Boolean = false,
    val trainingProgress: Int = 0,
    val trainingCurrentGesture: String = "",
    val trainingTotalGestures: Int = 0,
    
    // UI dialogs
    val showDeleteDialog: Boolean = false,
    val showTrainDialog: Boolean = false,
    val showExportDialog: Boolean = false,
    val showImportDialog: Boolean = false,
    val showDetailsDialog: Boolean = false,
    val showPlaybackDialog: Boolean = false,
    
    // Messages
    val errorMessage: String? = null,
    val successMessage: String? = null,
    
    // Gesture recognition
    val lastRecognizedGesture: String? = null,
    val lastRecognitionConfidence: Float = 0f,
    val lastRecognitionTime: Long? = null,
    
    // Visualization
    val showWaveform: Boolean = true,
    val selectedSensor: SensorType = SensorType.GYROSCOPE,
    val playbackSpeed: Float = 1f,
    
    // Export/Import
    val exportPath: String? = null,
    val importProgress: Int = 0,
    val isImporting: Boolean = false
)

enum class RecordingQuality(val displayName: String, val color: Color) {
    EXCELLENT("Excellent", Color(0xFF4CAF50)),
    GOOD("Good", Color(0xFF8BC34A)),
    FAIR("Fair", Color(0xFFFFC107)),
    POOR("Poor", Color(0xFFF44336)),
    UNKNOWN("Unknown", Color(0xFF9E9E9E))
}

enum class SensorType(val displayName: String, val icon: String) {
    GYROSCOPE("Gyroscope", "⚡"),
    ACCELEROMETER("Accelerometer", "📊"),
    MAGNETOMETER("Magnetometer", "🧭"),
    ALL("All Sensors", "📱")
}

data class GestureWaveformData(
    val samples: List<Float>,
    val maxValue: Float,
    val minValue: Float,
    val samplingRate: Int = 50
)