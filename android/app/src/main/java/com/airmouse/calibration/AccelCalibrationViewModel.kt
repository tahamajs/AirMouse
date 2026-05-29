package com.airmouse.calibration

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlin.math.sqrt

class AccelCalibrationViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val _timerText = MutableStateFlow("01:15")
    val timerText: StateFlow<String> = _timerText

    private var collecting = false
    private var completed = false
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 120

    fun startCollection() {
        if (accelSensor == null) {
            _status.value = "No accelerometer"
            return
        }
        collecting = true
        completed = false
        samples.clear(); sampleCount = 0; _progress.value = 0
        _status.value = "Place phone in the requested orientation"
        viewModelScope.launch {
            sensorManager.registerListener(this@AccelCalibrationViewModel, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
            var secondsLeft = 75
            while (collecting && secondsLeft >= 0) {
                val mins = secondsLeft / 60
                val secs = secondsLeft % 60
                _timerText.value = String.format("%02d:%02d", mins, secs)
                delay(1000)
                secondsLeft--
            }
            if (collecting) evaluateData()
        }
    }

    fun stopCollection() {
        collecting = false
        sensorManager.unregisterListener(this)
        _status.value = "Stopped"
    }

    fun reset() {
        stopCollection()
        samples.clear()
        sampleCount = 0
        completed = false
        _progress.value = 0
        _status.value = "Ready"
        _timerText.value = "01:15"
    }

    private fun evaluateData() {
        sensorManager.unregisterListener(this)
        collecting = false
        if (samples.isEmpty()) {
            _status.value = "No samples"
            _progress.value = 0
            return
        }
        // simple average
        var meanX=0f; var meanY=0f; var meanZ=0f
        for (s in samples) { meanX += s[0]; meanY += s[1]; meanZ += s[2] }
        val n = samples.size.toFloat()
        meanX /= n; meanY /= n; meanZ /= n
        // save via CalibrationManager
        CalibrationManager(getApplication()).setAccelCalibrated(true)
        completed = true
        _status.value = "Accel calibrated"
        _progress.value = 100
    }

    fun isComplete(): Boolean = completed

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_ACCELEROMETER) return
        samples.add(event.values.clone())
        sampleCount++
        _progress.value = (sampleCount * 100) / targetSamples
        if (sampleCount >= targetSamples) evaluateData()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
