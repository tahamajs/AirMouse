package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import java.io.File
import java.io.FileOutputStream

class SensorDataLogger(private val context: Context) : SensorEventListener {
    private lateinit var sensorManager: SensorManager
    private var logFile: File? = null
    private var outputStream: FileOutputStream? = null
    private var isLogging = false

    fun startLogging() {
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        val gyroscope = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
        val magnetometer = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
        sensorManager.registerListener(this, accelerometer, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, gyroscope, SensorManager.SENSOR_DELAY_FASTEST)
        sensorManager.registerListener(this, magnetometer, SensorManager.SENSOR_DELAY_FASTEST)

        logFile = File(context.getExternalFilesDir(null), "sensor_log.csv")
        outputStream = FileOutputStream(logFile)
        outputStream?.write("timestamp,type,x,y,z\n".toByteArray())
        isLogging = true
    }

    fun stopLogging() {
        sensorManager.unregisterListener(this)
        outputStream?.close()
        isLogging = false
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isLogging) return
        val line = "${System.currentTimeMillis()},${event.sensor.type},${event.values[0]},${event.values[1]},${event.values[2]}\n"
        outputStream?.write(line.toByteArray())
    }

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
}