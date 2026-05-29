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

class GyroCalibrationViewModel(application: Application) : AndroidViewModel(application), SensorEventListener {

    private val sensorManager = application.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    private val gyroSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
    private val accelSensor: Sensor? = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

    private val _progress = MutableStateFlow(0)
    val progress: StateFlow<Int> = _progress

    private val _status = MutableStateFlow("Ready")
    val status: StateFlow<String> = _status

    private val _timerText = MutableStateFlow("00:45")
    val timerText: StateFlow<String> = _timerText

    private var collecting = false
    private val samples = mutableListOf<FloatArray>()
    private var sampleCount = 0
    private val targetSamples = 150

    fun startCollection() {
        if (gyroSensor == null || accelSensor == null) {
            _status.value = "Sensors unavailable"
            return
        }
        collecting = true
        samples.clear()
        sampleCount = 0
        _status.value = "Collecting — keep device still"
        _progress.value = 0
        viewModelScope.launch {
            sensorManager.registerListener(this@GyroCalibrationViewModel, gyroSensor, SensorManager.SENSOR_DELAY_FASTEST)
            sensorManager.registerListener(this@GyroCalibrationViewModel, accelSensor, SensorManager.SENSOR_DELAY_FASTEST)
            var secondsLeft = 45
            while (collecting && secondsLeft >= 0) {
                val mins = secondsLeft / 60
                val secs = secondsLeft % 60
                _timerText.value = String.format("%02d:%02d", mins, secs)
                delay(1000)
                secondsLeft--
            }
            if (collecting) {
                // time up — evaluate
                evaluateData()
            }
        }
    }

    fun stopCollection() {
        collecting = false
        sensorManager.unregisterListener(this)
        _status.value = "Stopped"
    }

    private fun evaluateData() {
        sensorManager.unregisterListener(this)
        collecting = false
        if (samples.size < 10) {
            _status.value = "Not enough samples"
            _progress.value = 0
            return
        }
        var bx = 0f; var by = 0f; var bz = 0f
        for (s in samples) { bx += s[0]; by += s[1]; bz += s[2] }
        val n = samples.size.toFloat()
        val biasx = bx / n; val biasy = by / n; val biasz = bz / n
        // Save bias (use CalibrationManager)
        CalibrationManager(getApplication()).saveGyroBias(floatArrayOf(biasx, biasy, biasz))
        _status.value = "Gyro calibrated"
        _progress.value = 100
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!collecting) return
        when (event.sensor.type) {
            Sensor.TYPE_GYROSCOPE -> {
                if (sampleCount < targetSamples) {
                    samples.add(event.values.clone())
                    sampleCount++
                    _progress.value = (sampleCount * 100) / targetSamples
                }
                if (sampleCount >= targetSamples) {
                    evaluateData()
                }
            }
            Sensor.TYPE_ACCELEROMETER -> {
                // could check stationarity here — simplified for now
            }
        }
    }

    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit

    override fun onCleared() {
        super.onCleared()
        sensorManager.unregisterListener(this)
    }
}
