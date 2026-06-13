// app/src/main/java/com/airmouse/presentation/ui/gesture/GestureStudioViewModel.kt
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
import java.util.*
import javax.inject.Inject
import kotlin.math.sqrt

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
    private var recordingSamples = mutableListOf<FloatArray>()
    private var recordingJob: kotlinx.coroutines.Job? = null
    private var recordingTime = 0
    private val sampleRate = 50 // 50 Hz
    private var isCollecting = false

    private val sensorListener = object : SensorEventListener {
        override fun onSensorChanged(event: SensorEvent) {
            if (isCollecting) {
                when (event.sensor.type) {
                    Sensor.TYPE_GYROSCOPE -> {
                        val sample = floatArrayOf(
                            event.values[0], event.values[1], event.values[2],
                            0f, 0f, 0f
                        )
                        recordingSamples.add(sample)
                    }
                    Sensor.TYPE_ACCELEROMETER -> {
                        val lastIdx = recordingSamples.lastIndex
                        if (lastIdx >= 0) {
                            recordingSamples[lastIdx][3] = event.values[0]
                            recordingSamples[lastIdx][4] = event.values[1]
                            recordingSamples[lastIdx][5] = event.values[2]
                        } else {
                            recordingSamples.add(floatArrayOf(0f,0f,0f, event.values[0], event.values[1], event.values[2]))
                        }
                    }
                }
                _uiState.update { it.copy(progress = recordingSamples.size.coerceAtMost(500) * 100 / 500) }
            }
        }
        override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
    }

    init {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
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
                    statusColor = 0xFFEF4444
                )
            }
            return
        }
        recordingSamples.clear()
        isCollecting = true
        recordingTime = 0
        sensorManager.registerListener(sensorListener, gyroscope, 20000) // 50 Hz
        sensorManager.registerListener(sensorListener, accelerometer, 20000)
        _uiState.update {
            it.copy(
                isRecording = true,
                status = "Recording... Move your phone naturally",
                statusColor = 0xFFEF4444,
                progress = 0,
                recordingTime = 0,
                errorMessage = null
            )
        }
        recordingJob = viewModelScope.launch {
            while (isCollecting && recordingTime < 10) {
                delay(1000)
                recordingTime++
                _uiState.update { it.copy(recordingTime = recordingTime) }
            }
            if (recordingTime >= 10) stopRecording()
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
                    status = "Too few samples (${recordingSamples.size}). Please record longer.",
                    statusColor = 0xFFEF4444
                )
            }
            return
        }

        saveGesture()
    }

    private fun saveGesture() {
        viewModelScope.launch {
            val id = UUID.randomUUID().toString()
            // Normalize sampling to fixed length (e.g., 100 samples) by resampling
            val normalized = resampleSamples(recordingSamples, 100)
            val gesture = CustomGestureTemplate(
                id = id,
                name = _uiState.value.gestureName,
                samples = normalized,
                createdAt = System.currentTimeMillis(),
                modifiedAt = System.currentTimeMillis(),
                isTrained = false
            )
            gestureRepo.saveCustomGesture(gesture)
            _uiState.update {
                it.copy(
                    isRecording = false,
                    gestureName = "",
                    status = "Gesture saved! Training...",
                    statusColor = 0xFF4CAF50,
                    progress = 0,
                    recordingTime = 0
                )
            }
            // Train this gesture immediately
            trainGesture(id)
        }
    }

    private fun resampleSamples(samples: List<FloatArray>, targetLen: Int): List<FloatArray> {
        if (samples.isEmpty()) return emptyList()
        val step = samples.size.toFloat() / targetLen
        val result = mutableListOf<FloatArray>()
        for (i in 0 until targetLen) {
            val idx = (i * step).toInt().coerceIn(0, samples.lastIndex)
            result.add(samples[idx].clone())
        }
        return result
    }

    fun trainGesture(gestureId: String) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isTraining = true,
                    trainingProgress = 0,
                    status = "Training classifier...",
                    statusColor = 0xFFFF9800
                )
            }
            // Simulate progress (actual training is synchronous)
            for (i in 1..100 step 20) {
                delay(50)
                _uiState.update { it.copy(trainingProgress = i) }
            }
            val success = gestureRepo.trainGesture(gestureId)
            _uiState.update {
                it.copy(
                    isTraining = false,
                    trainingProgress = 0,
                    status = if (success) "Gesture trained successfully!" else "Training failed",
                    statusColor = if (success) 0xFF4CAF50 else 0xFFEF4444
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
                    statusColor = 0xFF4CAF50
                )
            }
        }
    }

    fun exportDataset() {
        viewModelScope.launch {
            try {
                val path = gestureRepo.exportGesturesToCSV()
                _uiState.update {
                    it.copy(
                        showExportDialog = false,
                        status = "Exported to $path",
                        statusColor = 0xFF4CAF50
                    )
                }
            } catch (e: Exception) {
                _uiState.update {
                    it.copy(
                        errorMessage = "Export failed: ${e.message}",
                        status = "Export failed"
                    )
                }
            }
        }
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

    fun clearMessages() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    override fun onCleared() {
        super.onCleared()
        recordingJob?.cancel()
        sensorManager.unregisterListener(sensorListener)
    }
}