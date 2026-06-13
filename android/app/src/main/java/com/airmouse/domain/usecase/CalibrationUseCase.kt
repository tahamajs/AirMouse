// app/src/main/java/com/airmouse/domain/usecase/CalibrationUseCase.kt
package com.airmouse.domain.usecase

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import javax.inject.Inject

class CalibrationUseCase @Inject constructor(
    private val context: Context,
    private val calibrationRepo: ICalibrationRepository,
    private val prefs: PreferencesManager
) {

    /**
     * Calibrate gyroscope by collecting stationary samples
     */
    suspend fun calibrateGyro(onProgress: ((String, Int) -> Unit)? = null): Result<GyroBias> {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val gyro = sensorManager.getDefaultSensor(Sensor.TYPE_GYROSCOPE)
            if (gyro == null) {
                return Result.failure(Exception("Gyroscope not available"))
            }

            val samples = mutableListOf<FloatArray>()
            val targetSamples = 500

            val result = suspendCancellableCoroutine<GyroBias> { cont ->
                var count = 0
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (count < targetSamples) {
                            samples.add(event.values.clone())
                            count++
                            val progress = (count * 100 / targetSamples)
                            if (progress % 10 == 0) {
                                onProgress?.invoke("Collecting gyro data...", progress)
                            }
                        } else {
                            sensorManager.unregisterListener(this)
                            val bias = GyroBias(
                                offsetX = samples.map { it[0] }.average().toFloat(),
                                offsetY = samples.map { it[1] }.average().toFloat(),
                                offsetZ = samples.map { it[2] }.average().toFloat()
                            )
                            if (cont.isActive) cont.resume(bias)
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, gyro, SensorManager.SENSOR_DELAY_FASTEST)
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }

            calibrationRepo.saveGyroBias(result)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate magnetometer using figure-8 pattern
     */
    suspend fun calibrateMagnetometer(
        durationMs: Long = 15000,
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<MagCalibration> {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val mag = sensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD)
            if (mag == null) {
                return Result.failure(Exception("Magnetometer not available"))
            }

            var min = floatArrayOf(Float.MAX_VALUE, Float.MAX_VALUE, Float.MAX_VALUE)
            var max = floatArrayOf(-Float.MAX_VALUE, -Float.MAX_VALUE, -Float.MAX_VALUE)
            val startTime = System.currentTimeMillis()

            val result = suspendCancellableCoroutine<MagCalibration> { cont ->
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        for (i in 0..2) {
                            if (event.values[i] < min[i]) min[i] = event.values[i]
                            if (event.values[i] > max[i]) max[i] = event.values[i]
                        }
                        val elapsed = System.currentTimeMillis() - startTime
                        val progress = (elapsed * 100 / durationMs).toInt().coerceIn(0, 100)
                        if (progress % 10 == 0) {
                            onProgress?.invoke("Move phone in figure-8 pattern", progress)
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, mag, SensorManager.SENSOR_DELAY_FASTEST)

                val handler = android.os.Handler(android.os.Looper.getMainLooper())
                val timeout = Runnable {
                    if (cont.isActive) {
                        sensorManager.unregisterListener(listener)
                        val calibration = MagCalibration(
                            offsetX = (min[0] + max[0]) / 2f,
                            offsetY = (min[1] + max[1]) / 2f,
                            offsetZ = (min[2] + max[2]) / 2f,
                            scaleX = (max[0] - min[0]) / 2f,
                            scaleY = (max[1] - min[1]) / 2f,
                            scaleZ = (max[2] - min[2]) / 2f
                        )
                        cont.resume(calibration)
                    }
                }
                handler.postDelayed(timeout, durationMs)
                cont.invokeOnCancellation {
                    handler.removeCallbacks(timeout)
                    sensorManager.unregisterListener(listener)
                }
            }

            calibrationRepo.saveMagCalibration(result)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * 6-point accelerometer calibration
     */
    suspend fun calibrateAccelerometer6Point(
        onProgress: ((String, Int) -> Unit)? = null
    ): Result<AccelCalibration> {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accel == null) {
                return Result.failure(Exception("Accelerometer not available"))
            }

            val orientations = listOf(
                "Place phone flat (screen up - +Z)",
                "Place phone flat (screen down - -Z)",
                "Hold phone vertical (port down - +X)",
                "Hold phone vertical (port up - -X)",
                "Hold phone on left side - +Y",
                "Hold phone on right side - -Y"
            )

            val measurements = mutableListOf<FloatArray>()

            for ((index, instruction) in orientations.withIndex()) {
                onProgress?.invoke(instruction, (index * 100 / orientations.size))
                delay(3000)

                val samples = mutableListOf<FloatArray>()
                val targetSamples = 100

                val measurement = suspendCancellableCoroutine<FloatArray> { cont ->
                    var count = 0
                    val listener = object : SensorEventListener {
                        override fun onSensorChanged(event: SensorEvent) {
                            if (count < targetSamples) {
                                samples.add(event.values.clone())
                                count++
                            } else {
                                sensorManager.unregisterListener(this)
                                val avg = floatArrayOf(
                                    samples.map { it[0] }.average().toFloat(),
                                    samples.map { it[1] }.average().toFloat(),
                                    samples.map { it[2] }.average().toFloat()
                                )
                                if (cont.isActive) cont.resume(avg)
                            }
                        }
                        override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                    }
                    sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST)
                    cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
                }
                measurements.add(measurement)
            }

            // Calculate offsets and scales
            val ideal = listOf(
                floatArrayOf(9.81f, 0f, 0f), floatArrayOf(-9.81f, 0f, 0f),
                floatArrayOf(0f, 9.81f, 0f), floatArrayOf(0f, -9.81f, 0f),
                floatArrayOf(0f, 0f, 9.81f), floatArrayOf(0f, 0f, -9.81f)
            )

            val offset = FloatArray(3)
            val scale = FloatArray(3) { 1f }

            for (i in 0..2) {
                val posIdeal = ideal[2 * i][i]
                val negIdeal = ideal[2 * i + 1][i]
                val posMeas = measurements[2 * i][i]
                val negMeas = measurements[2 * i + 1][i]
                scale[i] = (posMeas - negMeas) / (posIdeal - negIdeal)
                offset[i] = posMeas - scale[i] * posIdeal
                if (scale[i] == 0f) scale[i] = 1f
            }

            val calibration = AccelCalibration(
                offsetX = offset[0],
                offsetY = offset[1],
                offsetZ = offset[2],
                scaleX = scale[0],
                scaleY = scale[1],
                scaleZ = scale[2]
            )

            calibrationRepo.saveAccelCalibration(calibration)
            onProgress?.invoke("Accelerometer calibrated!", 100)
            Result.success(calibration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Simple accelerometer calibration (single orientation)
     */
    suspend fun calibrateAccelerometerSimple(onProgress: ((String, Int) -> Unit)? = null): Result<AccelCalibration> {
        return try {
            val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
            val accel = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
            if (accel == null) {
                return Result.failure(Exception("Accelerometer not available"))
            }

            val samples = mutableListOf<FloatArray>()
            val targetSamples = 200

            val result = suspendCancellableCoroutine<FloatArray> { cont ->
                var count = 0
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        if (count < targetSamples) {
                            samples.add(event.values.clone())
                            count++
                            val progress = (count * 100 / targetSamples)
                            if (progress % 10 == 0) {
                                onProgress?.invoke("Collecting accelerometer data...", progress)
                            }
                        } else {
                            sensorManager.unregisterListener(this)
                            val avg = floatArrayOf(
                                samples.map { it[0] }.average().toFloat(),
                                samples.map { it[1] }.average().toFloat(),
                                samples.map { it[2] }.average().toFloat()
                            )
                            if (cont.isActive) cont.resume(avg)
                        }
                    }
                    override fun onAccuracyChanged(sensor: Sensor, accuracy: Int) {}
                }
                sensorManager.registerListener(listener, accel, SensorManager.SENSOR_DELAY_FASTEST)
                cont.invokeOnCancellation { sensorManager.unregisterListener(listener) }
            }

            val calibration = AccelCalibration(
                offsetX = result[0],
                offsetY = result[1],
                offsetZ = result[2] - 9.81f,
                scaleX = 1f,
                scaleY = 1f,
                scaleZ = 1f
            )

            calibrationRepo.saveAccelCalibration(calibration)
            Result.success(calibration)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get current calibration status
     */
    suspend fun getCalibrationStatus(): CalibrationStatus {
        val gyroCal = calibrationRepo.getGyroBias()
        val accelCal = calibrationRepo.getAccelCalibration()
        val magCal = calibrationRepo.getMagCalibration()
        val isComplete = calibrationRepo.isCalibrationComplete().first()

        return CalibrationStatus(
            isGyroCalibrated = gyroCal.offsetX != 0f || gyroCal.offsetY != 0f || gyroCal.offsetZ != 0f,
            isAccelCalibrated = accelCal.offsetX != 0f || accelCal.offsetY != 0f || accelCal.offsetZ != 0f,
            isMagCalibrated = magCal.offsetX != 0f || magCal.offsetY != 0f || magCal.offsetZ != 0f,
            isComplete = isComplete,
            lastCalibrationTime = prefs.getLong("calibration_complete_time", 0),
            quality = calculateCalibrationQuality(gyroCal, accelCal, magCal)
        )
    }

    private fun calculateCalibrationQuality(
        gyro: GyroBias,
        accel: AccelCalibration,
        mag: MagCalibration
    ): Float {
        var quality = 100f

        // Gyro quality (lower bias is better)
        val gyroBiasMagnitude = kotlin.math.sqrt(gyro.offsetX * gyro.offsetX +
                gyro.offsetY * gyro.offsetY +
                gyro.offsetZ * gyro.offsetZ)
        if (gyroBiasMagnitude > 0.5f) quality -= 20

        // Accel quality (scale should be close to 1)
        val accelScaleDeviation = kotlin.math.abs(accel.scaleX - 1f) +
                kotlin.math.abs(accel.scaleY - 1f) +
                kotlin.math.abs(accel.scaleZ - 1f)
        if (accelScaleDeviation > 0.2f) quality -= 20

        // Mag quality (scale should be close to 1)
        val magScaleDeviation = kotlin.math.abs(mag.scaleX - 1f) +
                kotlin.math.abs(mag.scaleY - 1f) +
                kotlin.math.abs(mag.scaleZ - 1f)
        if (magScaleDeviation > 0.3f) quality -= 20

        return quality.coerceIn(0f, 100f)
    }

    /**
     * Check if calibration is complete
     */
    suspend fun isCalibrationComplete(): Boolean {
        return calibrationRepo.isCalibrationComplete().first()
    }

    /**
     * Mark calibration as complete
     */
    suspend fun completeCalibration() {
        calibrationRepo.markCalibrationComplete()
    }

    /**
     * Reset all calibration data
     */
    suspend fun resetCalibration() {
        calibrationRepo.resetCalibration()
    }

    private suspend fun delay(ms: Long) {
        kotlinx.coroutines.delay(ms)
    }
}

data class CalibrationStatus(
    val isGyroCalibrated: Boolean,
    val isAccelCalibrated: Boolean,
    val isMagCalibrated: Boolean,
    val isComplete: Boolean,
    val lastCalibrationTime: Long,
    val quality: Float
)