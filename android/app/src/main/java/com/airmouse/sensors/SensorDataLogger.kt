package com.airmouse.sensors

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.os.Build
import timber.log.Timber
import java.io.File
import java.io.FileOutputStream
import java.text.SimpleDateFormat
import java.util.*
import kotlinx.coroutines.*

class SensorDataLogger(private val context: Context) : SensorEventListener {

    enum class SensorType(val typeName: String) {
        ACCELEROMETER("accel"),
        GYROSCOPE("gyro"),
        MAGNETOMETER("mag"),
        ROTATION_VECTOR("rotation_vector"),
        GRAVITY("gravity"),
        LINEAR_ACCELERATION("linear_accel"),
        PRESSURE("pressure"),
        TEMPERATURE("temp"),
        PROXIMITY("prox"),
        LIGHT("light")
    }

    companion object {
        private const val TAG = "SensorDataLogger"
        private const val MAX_FILE_SIZE_BYTES = 10 * 1024 * 1024 
        private const val BUFFER_SIZE = 100 
        private const val LOG_PREFIX = "sensor_log"
        private const val LOG_EXTENSION = ".csv"
    }

    private lateinit var sensorManager: SensorManager
    private var logFile: File? = null
    private var outputStream: FileOutputStream? = null
    private var isLogging = false
    private var sampleCount = 0
    private var buffer = mutableListOf<String>()
    private var activeSensors = mutableSetOf<Int>()
    private var logStartTime = 0L

    var enabledSensors = SensorType.entries.toSet()

    var onProgressUpdate: ((sampleCount: Int, fileSize: Long) -> Unit)? = null
    var onLogComplete: ((File) -> Unit)? = null
    var onError: ((String) -> Unit)? = null

    private val writeJob = SupervisorJob()
    private val ioScope = CoroutineScope(Dispatchers.IO + writeJob)

    fun startLogging() {
        startLogging(enabledSensors)
    }

    fun startLogging(sensors: Set<SensorType>) {
        if (isLogging) {
            onError?.invoke("Already logging")
            return
        }

        enabledSensors = sensors
        sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
        activeSensors.clear()
        buffer.clear()
        sampleCount = 0
        logStartTime = System.currentTimeMillis()

        
        if (enabledSensors.contains(SensorType.ACCELEROMETER)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensors.add(it.type)
                Timber.i("Registered accelerometer")
            }
        }

        if (enabledSensors.contains(SensorType.GYROSCOPE)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensors.add(it.type)
                Timber.i("Registered gyroscope")
            }
        }

        if (enabledSensors.contains(SensorType.MAGNETOMETER)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensors.add(it.type)
                Timber.i("Registered magnetometer")
            }
        }

        if (enabledSensors.contains(SensorType.ROTATION_VECTOR)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_ROTATION_VECTOR)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensors.add(it.type)
                Timber.i("Registered rotation vector")
            }
        }

        if (enabledSensors.contains(SensorType.GRAVITY)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_GRAVITY)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensors.add(it.type)
                Timber.i("Registered gravity sensor")
            }
        }

        if (enabledSensors.contains(SensorType.LINEAR_ACCELERATION)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_FASTEST)
                activeSensors.add(it.type)
                Timber.i("Registered linear acceleration")
            }
        }

        if (enabledSensors.contains(SensorType.PRESSURE)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_PRESSURE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                activeSensors.add(it.type)
                Timber.i("Registered pressure sensor")
            }
        }

        if (enabledSensors.contains(SensorType.TEMPERATURE)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_AMBIENT_TEMPERATURE)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                activeSensors.add(it.type)
                Timber.i("Registered temperature sensor")
            }
        }

        if (enabledSensors.contains(SensorType.PROXIMITY)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_PROXIMITY)?.let {
                
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                activeSensors.add(it.type)
                Timber.i("Registered proximity sensor")
            }
        }

        if (enabledSensors.contains(SensorType.LIGHT)) {
            sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)?.let {
                sensorManager.registerListener(this, it, SensorManager.SENSOR_DELAY_NORMAL)
                activeSensors.add(it.type)
                Timber.i("Registered light sensor")
            }
        }

        if (activeSensors.isEmpty()) {
            onError?.invoke("No sensors available on this device")
            return
        }

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        logFile = File(context.getExternalFilesDir(null), "$LOG_PREFIX$timestamp$LOG_EXTENSION")

        try {
            outputStream = FileOutputStream(logFile)
            writeHeader()
            isLogging = true
            Timber.i("Started logging to ${logFile?.absolutePath}")
            onProgressUpdate?.invoke(0, 0)
        } catch (e: Exception) {
            onError?.invoke("Failed to create log file: ${e.message}")
            Timber.e(e, "Failed to create log file")
        }
    }

    private fun writeHeader() {
        val headers = mutableListOf<String>()
        headers.add("timestamp_ms")
        headers.add("elapsed_ms")

        if (activeSensors.contains(Sensor.TYPE_ACCELEROMETER)) {
            headers.add("accel_x")
            headers.add("accel_y")
            headers.add("accel_z")
        }
        if (activeSensors.contains(Sensor.TYPE_GYROSCOPE)) {
            headers.add("gyro_x")
            headers.add("gyro_y")
            headers.add("gyro_z")
        }
        if (activeSensors.contains(Sensor.TYPE_MAGNETIC_FIELD)) {
            headers.add("mag_x")
            headers.add("mag_y")
            headers.add("mag_z")
        }
        if (activeSensors.contains(Sensor.TYPE_ROTATION_VECTOR)) {
            headers.add("rotvec_x")
            headers.add("rotvec_y")
            headers.add("rotvec_z")
            headers.add("rotvec_w")
        }
        if (activeSensors.contains(Sensor.TYPE_GRAVITY)) {
            headers.add("gravity_x")
            headers.add("gravity_y")
            headers.add("gravity_z")
        }
        if (activeSensors.contains(Sensor.TYPE_LINEAR_ACCELERATION)) {
            headers.add("linaccel_x")
            headers.add("linaccel_y")
            headers.add("linaccel_z")
        }
        if (activeSensors.contains(Sensor.TYPE_PRESSURE)) {
            headers.add("pressure_hpa")
        }
        if (activeSensors.contains(Sensor.TYPE_AMBIENT_TEMPERATURE)) {
            headers.add("temp_celsius")
        }
        if (activeSensors.contains(Sensor.TYPE_PROXIMITY)) {
            headers.add("proximity_cm")
        }
        if (activeSensors.contains(Sensor.TYPE_LIGHT)) {
            headers.add("light_lux")
        }

        val headerLine = headers.joinToString(",") + "\n"
        outputStream?.write(headerLine.toByteArray())
    }

    override fun onSensorChanged(event: SensorEvent) {
        if (!isLogging) return

        val now = System.currentTimeMillis()
        val elapsed = now - logStartTime

        val values = mutableListOf<String>()
        values.add(now.toString())
        values.add(elapsed.toString())

        when (event.sensor.type) {
            Sensor.TYPE_ACCELEROMETER -> {
                values.add(event.values[0].toString())
                values.add(event.values[1].toString())
                values.add(event.values[2].toString())
                addEmptyPlaceholders(values, event.sensor.type)
            }
            Sensor.TYPE_GYROSCOPE -> {
                addEmptyPlaceholders(values, Sensor.TYPE_ACCELEROMETER)
                values.add(event.values[0].toString())
                values.add(event.values[1].toString())
                values.add(event.values[2].toString())
                addEmptyPlaceholders(values, event.sensor.type)
            }
            Sensor.TYPE_MAGNETIC_FIELD -> {
                addEmptyPlaceholders(values, Sensor.TYPE_ACCELEROMETER)
                addEmptyPlaceholders(values, Sensor.TYPE_GYROSCOPE)
                values.add(event.values[0].toString())
                values.add(event.values[1].toString())
                values.add(event.values[2].toString())
                addEmptyPlaceholders(values, event.sensor.type)
            }
            Sensor.TYPE_ROTATION_VECTOR -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
                values.add(event.values[1].toString())
                values.add(event.values[2].toString())
                values.add(if (event.values.size > 3) event.values[3].toString() else "0")
            }
            Sensor.TYPE_GRAVITY -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
                values.add(event.values[1].toString())
                values.add(event.values[2].toString())
            }
            Sensor.TYPE_LINEAR_ACCELERATION -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
                values.add(event.values[1].toString())
                values.add(event.values[2].toString())
            }
            Sensor.TYPE_PRESSURE -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
            }
            Sensor.TYPE_AMBIENT_TEMPERATURE -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
            }
            Sensor.TYPE_PROXIMITY -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
            }
            Sensor.TYPE_LIGHT -> {
                addEmptyPlaceholdersForAll(values)
                values.add(event.values[0].toString())
            }
            else -> return
        }

        buffer.add(values.joinToString(",") + "\n")
        sampleCount++

        if (buffer.size >= BUFFER_SIZE) {
            flushBuffer()
        }

        logFile?.let { file ->
            if (file.length() > MAX_FILE_SIZE_BYTES) {
                rotateLogFile()
            }
        }

        onProgressUpdate?.invoke(sampleCount, logFile?.length() ?: 0)
    }

    private fun addEmptyPlaceholders(values: MutableList<String>, currentSensorType: Int) {
        if (activeSensors.contains(Sensor.TYPE_ACCELEROMETER) && currentSensorType != Sensor.TYPE_ACCELEROMETER) {
            values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_GYROSCOPE) && currentSensorType != Sensor.TYPE_GYROSCOPE) {
            values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_MAGNETIC_FIELD) && currentSensorType != Sensor.TYPE_MAGNETIC_FIELD) {
            values.add(""); values.add(""); values.add("")
        }
    }

    private fun addEmptyPlaceholdersForAll(values: MutableList<String>) {
        if (activeSensors.contains(Sensor.TYPE_ACCELEROMETER)) {
            values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_GYROSCOPE)) {
            values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_MAGNETIC_FIELD)) {
            values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_ROTATION_VECTOR)) {
            values.add(""); values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_GRAVITY)) {
            values.add(""); values.add(""); values.add("")
        }
        if (activeSensors.contains(Sensor.TYPE_LINEAR_ACCELERATION)) {
            values.add(""); values.add(""); values.add("")
        }
    }

    private fun flushBuffer() {
        if (buffer.isEmpty()) return

        ioScope.launch {
            try {
                val data = buffer.joinToString("")
                outputStream?.write(data.toByteArray())
                outputStream?.flush()
                buffer.clear()
            } catch (e: Exception) {
                Timber.e(e, "Failed to write to file")
            }
        }
    }

    private fun rotateLogFile() {
        flushBuffer()

        val timestamp = SimpleDateFormat("yyyyMMdd_HHmmss", Locale.getDefault()).format(Date())
        val newFile = File(context.getExternalFilesDir(null), "$LOG_PREFIX$timestamp$LOG_EXTENSION")

        try {
            outputStream?.close()
            outputStream = FileOutputStream(newFile)
            writeHeader()
            logFile = newFile
            Timber.i("Rotated log file to ${newFile.absolutePath}")
        } catch (e: Exception) {
            Timber.e(e, "Failed to rotate log file")
        }
    }

    fun stopLogging() {
        if (!isLogging) return

        flushBuffer()
        sensorManager.unregisterListener(this)
        outputStream?.close()
        isLogging = false

        logFile?.let { file ->
            Timber.i("Stopped logging. File saved to ${file.absolutePath}")
            Timber.i("Total samples: $sampleCount")
            Timber.i("File size: ${file.length()} bytes")
            onLogComplete?.invoke(file)
        }

        onProgressUpdate?.invoke(sampleCount, logFile?.length() ?: 0)
    }

    fun getLogFile(): File? = logFile
    fun getSampleCount(): Int = sampleCount
    fun isLogging(): Boolean = isLogging
    fun getActiveSensors(): Set<SensorType> = enabledSensors
    fun getLogDuration(): Long = if (isLogging) System.currentTimeMillis() - logStartTime else 0

    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {
        Timber.d("Accuracy changed for ${sensor.name}: $accuracy")
    }

    fun cleanup() {
        writeJob.cancel()
        if (isLogging) {
            stopLogging()
        }
    }
}

class SensorDataLoggerBuilder(private val context: Context) {
    private var sensors: Set<SensorDataLogger.SensorType> = SensorDataLogger.SensorType.entries.toSet()
    private var autoFlushInterval: Long = 1000L
    private var maxFileSizeMB: Int = 10

    fun sensors(vararg sensorTypes: SensorDataLogger.SensorType) = apply {
        this.sensors = sensorTypes.toSet()
    }

    fun autoFlushInterval(intervalMs: Long) = apply {
        this.autoFlushInterval = intervalMs
    }

    fun maxFileSizeMB(sizeMB: Int) = apply {
        this.maxFileSizeMB = sizeMB
    }

    fun build(): SensorDataLogger {
        return SensorDataLogger(context).apply {
            enabledSensors = this@SensorDataLoggerBuilder.sensors
        }
    }
}
