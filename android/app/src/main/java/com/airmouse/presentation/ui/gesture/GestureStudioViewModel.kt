package com.airmouse.presentation.ui.gesture

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.repository.IGestureRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.io.File
import java.text.SimpleDateFormat
import java.util.*
import javax.inject.Inject
import kotlin.math.*

@HiltViewModel
class GestureStudioViewModel @Inject constructor(
    private val gestureRepo: IGestureRepository,
    @ApplicationContext private val context: Context
) : ViewModel() {

    private val _uiState = MutableStateFlow(GestureStudioUiState())
    val uiState: StateFlow<GestureStudioUiState> = _uiState.asStateFlow()

    private lateinit var sensorManager: SensorManager
    private var gyroscope: Sensor? = null
    private var accelerometer: Sensor? = null
    private var magnetometer: Sensor? = null
    private var recordingSamples = mutableListOf<FloatArray>()
    private var recordingJob: kotlinx.coroutines.Job? = null
    private var recordingTime = 0
    private val targetSamples = 200 // 4 seconds at 50Hz
    private var isCollecting = false
    private var startTime = 0L
    
    // Waveform data for visualization
    private val _waveformData = MutableStateFlow(GestureWaveformData(emptyList(), 0f, 0f))
    val waveformData: StateFlow<GestureWaveformData> = _waveformData.asStateFlow()

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollecting) {
                val timestamp = System.currentTimeMillis()
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        val sample = floatArrayOf(
                            event.values[0], event.values[1], event.values[2],
                            0f, 0f, 0f, 0f, 0f, 0f, timestamp.toFloat()
                        )
                        recordingSamples.add(sample)
                        updateWaveform(sample, SensorType.GYROSCOPE)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val lastIdx = recordingSamples.lastIndex
                        if (lastIdx >= 0) {
                            recordingSamples[lastIdx][3] = event.values[0]
                            recordingSamples[lastIdx][4] = event.values[1]
                            recordingSamples[lastIdx][5] = event.values[2]
                        } else {
                            recordingSamples.add(floatArrayOf(0f,0f,0f, event.values[0], event.values[1], event.values[2], 0f,0f,0f, timestamp.toFloat()))
                        }
                        updateWaveform(recordingSamples[lastIdx], SensorType.ACCELEROMETER)
                    }
                    Sensor.TYPE_MAGNETIC_FIELD -> {
                        val lastIdx = recordingSamples.lastIndex
                        if (lastIdx >= 0) {
                            recordingSamples[lastIdx][6] = event.values[0]
                            recordingSamples[lastIdx][7] = event.values[1]
                            recordingSamples[lastIdx][8] = event.values[2]
                        }
                    }
                }
                val progress = (recordingSamples.size * 100 / targetSamples).coerceIn(0, 100)
                val quality = calculateRecordingQuality()
                _uiState.update { 
                    it.copy(
                        progress = progress,
                        recordingQuality = quality,
                        recordingTime = ((System.currentTimeMillis() - startTime) / 1000).toInt()
                    )
                }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }
    
    private fun updateWaveform(sample: FloatArray, sensorType: SensorType) {
        val values = when (sensorType) {
            SensorType.GYROSCOPE -> listOf(sample[0], sample[1], sample[2])
            SensorType.ACCELEROMETER -> listOf(sample[3], sample[4], sample[5])
            SensorType.MAGNETOMETER -> listOf(sample[6], sample[7], sample[8])
            SensorType.ALL -> sample.toList()
        }
        val maxVal = values.maxOrNull() ?: 0f
        val minVal = values.minOrNull() ?: 0f
        _waveformData.value = GestureWaveformData(
            samples = values,
            maxValue = maxVal,
            minValue = minVal,
            samplingRate = 50
        )
    }
    
    private fun calculateRecordingQuality(): RecordingQuality {
        if (recordingSamples.size < 10) return RecordingQuality.UNKNOWN
        // Calculate variance of recent samples
        val recentSamples = recordingSamples.takeLast(10)
        val variances = mutableListOf<Float>()
        for (i in 0 until 3) {
            val values = recentSamples.map { it[i] }
            val mean = values.average().toFloat()
            val variance = values.map { (it - mean) * (it - mean) }.average().toFloat()
            variances.add(variance)
        }
        val avgVariance = variances.average()
        return when {
            avgVariance < 0.5 -> RecordingQuality.EXCELLENT
            avgVariance < 1.0 -> RecordingQuality.GOOD
            avgVariance < 2.0 -> RecordingQuality.FAIR
            else -> RecordingQuality.POOR
        }
    }

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        loadGestures()
    }

    private fun loadGestures() {
        viewModelScope.launch {
            gestureRepo.observeGestures().collect { gestures ->
                _uiState.update { it.copy(savedGestures = gestures) }
            }
        }
    }

    fun updateGestureName(name: String) {
        _uiState.update { it.copy(gestureName = name) }
    }

    fun startRecording() {
        if (_uiState.value.gestureName.isBlank()) {
            _uiState.update {
                it.copy(
                    errorMessage = "Enter a gesture name",
                    status = "Error: No name",
                    statusColor = Color(0xFFF44336)
                )
            }
            return
        }
        
        if (gyroscope == null || accelerometer == null) {
            _uiState.update {
                it.copy(
                    errorMessage = "Required sensors not available",
                    status = "Error: Sensors missing",
                    statusColor = Color(0xFFF44336)
                )
            }
            return
        }
        
        recordingSamples.clear()
        isCollecting = true
        startTime = System.currentTimeMillis()
        recordingTime = 0
        
        sensorManager.registerListener(sensorListener, gyroscope, 20000) // 50Hz
        sensorManager.registerListener(sensorListener, accelerometer, 20000)
        magnetometer?.let { sensorManager.registerListener(sensorListener, it, 20000) }
        
        _uiState.update {
            it.copy(
                isRecording = true,
                status = "Recording... Perform the gesture naturally",
                statusColor = Color(0xFFF44336),
                progress = 0,
                recordingTime = 0,
                errorMessage = null,
                recordingQuality = RecordingQuality.UNKNOWN
            )
        }
        
        recordingJob = viewModelScope.launch {
            while (isCollecting && recordingSamples.size < targetSamples) {
                delay(50)
            }
            if (recordingSamples.size >= targetSamples) {
                stopRecording()
            } else if (recordingTime >= 10) {
                stopRecording()
            }
        }
    }

    fun stopRecording() {
        recordingJob?.cancel()
        isCollecting = false
        sensorManager.unregisterListener(sensorListener)

        if (recordingSamples.size < 30) {
            _uiState.update {
                it.copy(
                    isRecording = false,
                    status = "Too few samples (${recordingSamples.size}). Need at least 30 samples.",
                    statusColor = Color(0xFFF44336)
                )
            }
            return
        }

        saveGesture()
    }

    private fun saveGesture() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            // Normalize sampling to fixed length
            val normalized = resampleSamples(recordingSamples, targetSamples)
            
            // Extract features for better recognition
            val features = extractFeatures(normalized)
            
            val gesture = CustomGestureTemplate(
                id = id,
                name = _uiState.value.gestureName,
                samples = normalized,
                features = features,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                isTrained = false,
                sampleCount = normalized.size,
                duration = recordingTime,
                quality = _uiState.value.recordingQuality.name
            )
            
            gestureRepo.saveCustomGesture(gesture)
            _uiState.update {
                it.copy(
                    isRecording = false,
                    gestureName = "",
                    status = "Gesture saved! Training...",
                    statusColor = Color(0xFF4CAF50),
                    progress = 0,
                    recordingTime = 0
                )
            }
            // Train this gesture immediately
            trainGesture(id)
        }
    }
    
    private fun extractFeatures(samples: List<FloatArray>): Map<String, Float> {
        if (samples.isEmpty()) return emptyMap()
        
        val gyroX = samples.map { it[0] }
        val gyroY = samples.map { it[1] }
        val gyroZ = samples.map { it[2] }
        val accelX = samples.map { it[3] }
        val accelY = samples.map { it[4] }
        val accelZ = samples.map { it[5] }
        
        return mapOf(
            "gyro_mean" to (gyroX.average() + gyroY.average() + gyroZ.average()).toFloat() / 3,
            "gyro_std" to (stdDev(gyroX) + stdDev(gyroY) + stdDev(gyroZ)) / 3,
            "gyro_max" to maxOf(gyroX.maxOrNull() ?: 0f, gyroY.maxOrNull() ?: 0f, gyroZ.maxOrNull() ?: 0f),
            "accel_mean" to (accelX.average() + accelY.average() + accelZ.average()).toFloat() / 3,
            "accel_std" to (stdDev(accelX) + stdDev(accelY) + stdDev(accelZ)) / 3,
            "duration" to samples.size.toFloat(),
            "energy" to (gyroX.sumOf { it * it } + gyroY.sumOf { it * it } + gyroZ.sumOf { it * it }).toFloat()
        )
    }
    
    private fun stdDev(values: List<Float>): Float {
        if (values.isEmpty()) return 0f
        val mean = values.average().toFloat()
        val variance = values.map { (it - mean) * (it - mean) }.average()
        return sqrt(variance).toFloat()
    }

    private fun resampleSamples(samples: List<FloatArray>, targetLen: Int): List<FloatArray> {
        if (samples.isEmpty()) return emptyList()
        if (samples.size == targetLen) return samples
        
        val result = mutableListOf<FloatArray>()
        val step = (samples.size - 1).toFloat() / (targetLen - 1)
        
        for (i in 0 until targetLen) {
            val idx = (i * step).toInt().coerceIn(0, samples.lastIndex)
            result.add(samples[idx].copyOf())
        }
        return result
    }

    fun trainGesture(gestureId: String) {
        viewModelScope.launch {
            val totalGestures = _uiState.value.savedGestures.size
            var trainedCount = 0
            
            _uiState.update {
                it.copy(
                    isTraining = true,
                    trainingProgress = 0,
                    trainingCurrentGesture = _uiState.value.savedGestures.find { it.id == gestureId }?.name ?: "",
                    trainingTotalGestures = totalGestures,
                    status = "Training classifier...",
                    statusColor = Color(0xFFFF9800)
                )
            }
            
            // Simulate progress with realistic timing
            for (i in 1..100 step 5) {
                delay(25)
                _uiState.update { it.copy(trainingProgress = i) }
            }
            
            val success = gestureRepo.trainGesture(gestureId)
            trainedCount++
            
            _uiState.update {
                it.copy(
                    isTraining = false,
                    trainingProgress = 0,
                    status = if (success) "Gesture trained successfully!" else "Training failed",
                    statusColor = if (success) Color(0xFF4CAF50) else Color(0xFFF44336)
                )
            }
            loadGestures()
        }
    }

    fun trainAllGestures() {
        viewModelScope.launch {
            val gestures = _uiState.value.savedGestures.filter { !it.isTrained }
            if (gestures.isEmpty()) {
                _uiState.update {
                    it.copy(
                        status = "All gestures are already trained!",
                        statusColor = Color(0xFF4CAF50)
                    )
                }
                return@launch
            }
            
            _uiState.update {
                it.copy(
                    isTraining = true,
                    trainingTotalGestures = gestures.size,
                    status = "Training ${gestures.size} gestures...",
                    statusColor = Color(0xFFFF9800)
                )
            }
            
            var trainedCount = 0
            for (gesture in gestures) {
                _uiState.update { 
                    it.copy(
                        trainingCurrentGesture = gesture.name,
                        trainingProgress = (trainedCount * 100 / gestures.size)
                    )
                }
                
                val success = gestureRepo.trainGesture(gesture.id)
                if (success) trainedCount++
                delay(500)
            }
            
            _uiState.update {
                it.copy(
                    isTraining = false,
                    showTrainDialog = false,
                    status = "Trained $trainedCount/${gestures.size} gestures",
                    statusColor = if (trainedCount == gestures.size) Color(0xFF4CAF50) else Color(0xFFFF9800)
                )
            }
            loadGestures()
        }
    }

    fun deleteGesture(gestureId: String) {
        viewModelScope.launch {
            gestureRepo.deleteCustomGesture(gestureId)
            _uiState.update {
                it.copy(
                    showDeleteDialog = false,
                    status = "Gesture deleted",
                    statusColor = Color(0xFF4CAF50)
                )
            }
        }
    }

    fun exportDataset() {
        viewModelScope.launch {
            try {
                val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
                val fileName = "gesture_export_$timestamp.csv"
                val file = File(context.getExternalFilesDir(null), fileName)
                
                val allGestures = _uiState.value.savedGestures
                file.bufferedWriter().use { writer ->
                    // Write header
                    writer.write("id,name,created_at,duration,sample_count,quality,is_trained")
                    writer.newLine()
                    
                    for (gesture in allGestures) {
                        writer.write("${gesture.id},${gesture.name},${gesture.createdAt},${gesture.duration},${gesture.sampleCount},${gesture.quality},${gesture.isTrained}")
                        writer.newLine()
                    }
                }
                
                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        exportPath = file.absolutePath,
                        status = "Exported to $fileName",
                        statusColor = Color(0xFF4CAF50)
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Export failed: ${e.message}",
                        status = "Export failed",
                        statusColor = Color(0xFFF44336)
                    )
                }
            }
        }
    }

    fun importGestures(filePath: String) {
        viewModelScope.launch {
            try {
                _uiState.update { it.copy(isImporting = true, importProgress = 0) }
                
                val file = File(filePath)
                if (!file.exists()) {
                    throw Exception("File not found")
                }
                
                val lines = file.readLines()
                if (lines.size <= 1) {
                    throw Exception("No data found")
                }
                
                var importedCount = 0
                for ((index, line) in lines.drop(1).withIndex()) {
                    val parts = line.split(",")
                    if (parts.size >= 6) {
                        val gesture = CustomGestureTemplate(
                            id = UUID.randomUUID().toString(),
                            name = parts[1],
                            createdAt = parts[2].toLongOrNull() ?: System.currentTimeMillis(),
                            duration = parts[3].toIntOrNull() ?: 0,
                            sampleCount = parts[4].toIntOrNull() ?: 0,
                            quality = parts[5],
                            isTrained = false,
                            samples = emptyList()
                        )
                        gestureRepo.saveCustomGesture(gesture)
                        importedCount++
                    }
                    _uiState.update { it.copy(importProgress = (index + 1) * 100 / (lines.size - 1)) }
                }
                
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        showImportDialog = false,
                        status = "Imported $importedCount gestures",
                        statusColor = Color(0xFF4CAF50)
                    )
                }
                loadGestures()
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        isImporting = false,
                        errorMessage = "Import failed: ${e.message}",
                        status = "Import failed",
                        statusColor = Color(0xFFF44336)
                    )
                }
            }
        }
    }

    fun recognizeGesture(samples: List<FloatArray>): Pair<String, Float> {
        // Simplified recognition logic
        if (samples.isEmpty()) return Pair("none", 0f)
        
        val features = extractFeatures(samples)
        var bestMatch = "none"
        var bestConfidence = 0f
        
        for (gesture in _uiState.value.savedGestures.filter { it.isTrained }) {
            val gestureFeatures = gesture.features ?: continue
            var similarity = 0f
            var count = 0
            
            for ((key, value) in features) {
                val gestureValue = gestureFeatures[key] ?: continue
                val diff = abs(value - gestureValue)
                val confidence = (1f - (diff / max(value, gestureValue))).coerceIn(0f, 1f)
                similarity += confidence
                count++
            }
            
            val avgConfidence = if (count > 0) similarity / count else 0f
            if (avgConfidence > bestConfidence && avgConfidence > 0.6f) {
                bestConfidence = avgConfidence
                bestMatch = gesture.name
            }
        }
        
        return Pair(bestMatch, bestConfidence)
    }

    fun selectGesture(gesture: CustomGestureTemplate) {
        _uiState.update { it.copy(selectedGesture = gesture) }
    }

    fun showDeleteDialog(show: Boolean) {
        _uiState.update { it.copy(showDeleteDialog = show) }
    }

    fun showTrainDialog(show: Boolean) {
        _uiState.update { it.copy(showTrainDialog = show) }
    }

    fun showExportDialog(show: Boolean) {
        _uiState.update { it.copy(showExportDialog = show) }
    }

    fun showImportDialog(show: Boolean) {
        _uiState.update { it.copy(showImportDialog = show) }
    }

    fun showDetailsDialog(show: Boolean) {
        _uiState.update { it.copy(showDetailsDialog = show) }
    }

    fun showPlaybackDialog(show: Boolean) {
        _uiState.update { it.copy(showPlaybackDialog = show) }
    }

    fun toggleWaveform() {
        _uiState.update { it.copy(showWaveform = !it.showWaveform) }
    }

    fun setSelectedSensor(sensor: SensorType) {
        _uiState.update { it.copy(selectedSensor = sensor) }
    }

    fun setPlaybackSpeed(speed: Float) {
        _uiState.update { it.copy(playbackSpeed = speed.coerceIn(0.5f, 2f)) }
    }

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null, successMessage = null) }
    }

    fun resetRecognition() {
        _uiState.update { 
            it.copy(
                lastRecognizedGesture = null,
                lastRecognitionConfidence = 0f,
                lastRecognitionTime = null
            )
        }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        sensorManager.unregisterListener(sensorListener)
    }
}