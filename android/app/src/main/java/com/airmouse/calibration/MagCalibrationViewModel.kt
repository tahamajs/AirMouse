package com.airmouse.calibration

import android.app.Application
import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.airmouse.sensors.CalibrationManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

class MagCalibrationViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {
    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val magSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val _timerText = MutableStateFlow("01:00")
    val timerText: StateFlow<String> = _timerText

    private var collecting = false
    private var completed = false
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 300

    fun startCollection() {
        if (magSensor == null) { _status.value = "Magnetometer not available"; return }
        collecting = true; completed = false; samples.clear(); sampleCount = 0; _progress.value = 0
        _status.value = "Move phone in figure‑8"
        viewModelScope.launch {
            sensorManager.registerListener(this@MagCalibrationViewModel, magSensor, SensorManager.SENSOR_DELAY_FASTEST)
            var secondsLeft = 60
            while (collecting && secondsLeft >= 0) {
                val mins = secondsLeft / 60; val secs = secondsLeft % 60
                _timerText.value = String.format("%02d:%02d", mins, secs)
                delay(1000); secondsLeft--
            }
            if (collecting) finishCalibration()
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
        _timerText.value = "01:00"
    }

    private fun finishCalibration() {
        sensorManager.unregisterListener(this)
        collecting = false
        if (samples.size < 50) { _status.value = "Insufficient data"; _progress.value = 0; return }
        var minX = Float.MAX_VALUE; var maxX = Float.MIN_VALUE
        var minY = Float.MAX_VALUE; var maxY = Float.MIN_VALUE
        var minZ = Float.MAX_VALUE; var maxZ = Float.MIN_VALUE
        for (s in samples) {
            minX = minOf(minX, s[0]); maxX = maxOf(maxX, s[0])
            minY = minOf(minY, s[1]); maxY = maxOf(maxY, s[1])
            minZ = minOf(minZ, s[2]); maxZ = maxOf(maxZ, s[2])
        }
        val offset = floatArrayOf((maxX + minX) / 2f, (maxY + minY) / 2f, (maxZ + minZ) / 2f)
        val scale = floatArrayOf((maxX - minX) / 2f, (maxY - minY) / 2f, (maxZ - minZ) / 2f)
        CalibrationManager(getApplication()).saveMagCalibration(offset, scale)
        completed = true
        _status.value = "Mag calibrated"
        _progress.value = 100
    }

    fun isComplete(): Boolean = completed

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting || event.sensor.type != Sensor.TYPE_MAGNETIC_FIELD) return
        samples.add(event.values.clone())
        sampleCount++
        _progress.value = (sampleCount * 100 / targetSamples).coerceAtMost(100)
        if (sampleCount >= targetSamples) finishCalibration()
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
