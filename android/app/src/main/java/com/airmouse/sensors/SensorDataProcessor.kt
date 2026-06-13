package com.airmouse.sensors

import kotlin.math.*

/**
 * Process raw sensor data for filtering, smoothing, and feature extraction
 */
object SensorDataProcessor {

    /**
     * Low-pass filter for smoothing sensor data
     */
    class LowPassFilter(private val alpha: Float = 0.2f) {
        private var filteredX = 0f
        private var filteredY = 0f
        private var filteredZ = 0f
        private var initialized = false

        fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            if (!initialized) {
                filteredX = x
                filteredY = y
                filteredZ = z
                initialized = true
                return Triple(x, y, z)
            }

            filteredX = alpha * x + (1 - alpha) * filteredX
            filteredY = alpha * y + (1 - alpha) * filteredY
            filteredZ = alpha * z + (1 - alpha) * filteredZ

            return Triple(filteredX, filteredY, filteredZ)
        }

        fun reset() {
            initialized = false
            filteredX = 0f
            filteredY = 0f
            filteredZ = 0f
        }
    }

    /**
     * High-pass filter for removing DC bias
     */
    class HighPassFilter(private val alpha: Float = 0.1f) {
        private var previousX = 0f
        private var previousY = 0f
        private var previousZ = 0f
        private var initialized = false

        fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            if (!initialized) {
                previousX = x
                previousY = y
                previousZ = z
                initialized = true
                return Triple(0f, 0f, 0f)
            }

            val filteredX = alpha * (previousX + x) - previousX
            val filteredY = alpha * (previousY + y) - previousY
            val filteredZ = alpha * (previousZ + z) - previousZ

            previousX = x
            previousY = y
            previousZ = z

            return Triple(filteredX, filteredY, filteredZ)
        }

        fun reset() {
            initialized = false
            previousX = 0f
            previousY = 0f
            previousZ = 0f
        }
    }

    /**
     * Moving average filter
     */
    class MovingAverageFilter(private val windowSize: Int = 10) {
        private val bufferX = mutableListOf<Float>()
        private val bufferY = mutableListOf<Float>()
        private val bufferZ = mutableListOf<Float>()

        fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            bufferX.add(x)
            bufferY.add(y)
            bufferZ.add(z)

            while (bufferX.size > windowSize) {
                bufferX.removeAt(0)
                bufferY.removeAt(0)
                bufferZ.removeAt(0)
            }

            val avgX = bufferX.average().toFloat()
            val avgY = bufferY.average().toFloat()
            val avgZ = bufferZ.average().toFloat()

            return Triple(avgX, avgY, avgZ)
        }

        fun reset() {
            bufferX.clear()
            bufferY.clear()
            bufferZ.clear()
        }
    }

    /**
     * Complementary filter for sensor fusion
     */
    class ComplementaryFilter(private val alpha: Float = 0.98f) {
        private var filteredX = 0f
        private var filteredY = 0f
        private var filteredZ = 0f
        private var initialized = false

        fun filter(gyroX: Float, gyroY: Float, gyroZ: Float, accelX: Float, accelY: Float, accelZ: Float, dt: Float): Triple<Float, Float, Float> {
            // Integrate gyroscope
            val gyroAngleX = if (initialized) filteredX + gyroX * dt else 0f
            val gyroAngleY = if (initialized) filteredY + gyroY * dt else 0f
            val gyroAngleZ = if (initialized) filteredZ + gyroZ * dt else 0f

            // Calculate angle from accelerometer
            val accelAngleX = atan2(accelY, accelZ)
            val accelAngleY = atan2(-accelX, sqrt(accelY * accelY + accelZ * accelZ))

            // Complementary filter
            val resultX = alpha * gyroAngleX + (1 - alpha) * accelAngleX
            val resultY = alpha * gyroAngleY + (1 - alpha) * accelAngleY
            val resultZ = gyroAngleZ // Yaw from gyro only

            filteredX = resultX
            filteredY = resultY
            filteredZ = resultZ
            initialized = true

            return Triple(resultX, resultY, resultZ)
        }

        fun reset() {
            initialized = false
            filteredX = 0f
            filteredY = 0f
            filteredZ = 0f
        }
    }

    /**
     * Kalman filter for sensor data
     */
    class KalmanFilter(private val processNoise: Float = 0.01f, private val measurementNoise: Float = 0.1f) {
        private var estimate = 0f
        private var error = 1f

        fun filter(measurement: Float): Float {
            // Prediction
            val prediction = estimate
            val predictionError = error + processNoise

            // Update
            val kalmanGain = predictionError / (predictionError + measurementNoise)
            estimate = prediction + kalmanGain * (measurement - prediction)
            error = (1 - kalmanGain) * predictionError

            return estimate
        }

        fun reset() {
            estimate = 0f
            error = 1f
        }
    }

    /**
     * Butterworth filter for low-pass filtering
     */
    class ButterworthFilter(private val cutoffFreq: Float, private val sampleRate: Float) {
        private var a1: Float = 0f
        private var a2: Float = 0f
        private var b0: Float = 0f
        private var b1: Float = 0f
        private var b2: Float = 0f

        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f
        private var initialized = false

        init {
            calculateCoefficients()
        }

        private fun calculateCoefficients() {
            val c = kotlin.math.tan(Math.PI * cutoffFreq / sampleRate).toFloat()
            val c2 = c * c
            val d = 1f + sqrt(2f) * c + c2

            b0 = c2 / d
            b1 = 2f * b0
            b2 = b0
            a1 = (2f * (c2 - 1f)) / d
            a2 = (1f - sqrt(2f) * c + c2) / d
        }

        fun filter(input: Float): Float {
            if (!initialized) {
                x1 = input
                x2 = input
                y1 = input
                y2 = input
                initialized = true
                return input
            }

            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

            x2 = x1
            x1 = input
            y2 = y1
            y1 = output

            return output
        }

        fun reset() {
            initialized = false
            x1 = 0f
            x2 = 0f
            y1 = 0f
            y2 = 0f
        }
    }

    /**
     * Calculate magnitude of vector
     */
    fun magnitude(x: Float, y: Float, z: Float): Float {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * Calculate angle between two vectors
     */
    fun angleBetween(v1x: Float, v1y: Float, v1z: Float, v2x: Float, v2y: Float, v2z: Float): Float {
        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val mag1 = magnitude(v1x, v1y, v1z)
        val mag2 = magnitude(v2x, v2y, v2z)

        if (mag1 == 0f || mag2 == 0f) return 0f

        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return acos(cosTheta)
    }

    /**
     * Calculate derivative (rate of change)
     */
    fun derivative(current: Float, previous: Float, dt: Float): Float {
        if (dt <= 0f) return 0f
        return (current - previous) / dt
    }

    /**
     * Calculate integral
     */
    fun integral(value: Float, dt: Float, cumulative: Float): Float {
        return cumulative + value * dt
    }
}package com.airmouse.sensors

import kotlin.math.*

/**
 * Process raw sensor data for filtering, smoothing, and feature extraction
 */
object SensorDataProcessor {

    /**
     * Low-pass filter for smoothing sensor data
     */
    class LowPassFilter(private val alpha: Float = 0.2f) {
        private var filteredX = 0f
        private var filteredY = 0f
        private var filteredZ = 0f
        private var initialized = false

        fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            if (!initialized) {
                filteredX = x
                filteredY = y
                filteredZ = z
                initialized = true
                return Triple(x, y, z)
            }

            filteredX = alpha * x + (1 - alpha) * filteredX
            filteredY = alpha * y + (1 - alpha) * filteredY
            filteredZ = alpha * z + (1 - alpha) * filteredZ

            return Triple(filteredX, filteredY, filteredZ)
        }

        fun reset() {
            initialized = false
            filteredX = 0f
            filteredY = 0f
            filteredZ = 0f
        }
    }

    /**
     * High-pass filter for removing DC bias
     */
    class HighPassFilter(private val alpha: Float = 0.1f) {
        private var previousX = 0f
        private var previousY = 0f
        private var previousZ = 0f
        private var initialized = false

        fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            if (!initialized) {
                previousX = x
                previousY = y
                previousZ = z
                initialized = true
                return Triple(0f, 0f, 0f)
            }

            val filteredX = alpha * (previousX + x) - previousX
            val filteredY = alpha * (previousY + y) - previousY
            val filteredZ = alpha * (previousZ + z) - previousZ

            previousX = x
            previousY = y
            previousZ = z

            return Triple(filteredX, filteredY, filteredZ)
        }

        fun reset() {
            initialized = false
            previousX = 0f
            previousY = 0f
            previousZ = 0f
        }
    }

    /**
     * Moving average filter
     */
    class MovingAverageFilter(private val windowSize: Int = 10) {
        private val bufferX = mutableListOf<Float>()
        private val bufferY = mutableListOf<Float>()
        private val bufferZ = mutableListOf<Float>()

        fun filter(x: Float, y: Float, z: Float): Triple<Float, Float, Float> {
            bufferX.add(x)
            bufferY.add(y)
            bufferZ.add(z)

            while (bufferX.size > windowSize) {
                bufferX.removeAt(0)
                bufferY.removeAt(0)
                bufferZ.removeAt(0)
            }

            val avgX = bufferX.average().toFloat()
            val avgY = bufferY.average().toFloat()
            val avgZ = bufferZ.average().toFloat()

            return Triple(avgX, avgY, avgZ)
        }

        fun reset() {
            bufferX.clear()
            bufferY.clear()
            bufferZ.clear()
        }
    }

    /**
     * Complementary filter for sensor fusion
     */
    class ComplementaryFilter(private val alpha: Float = 0.98f) {
        private var filteredX = 0f
        private var filteredY = 0f
        private var filteredZ = 0f
        private var initialized = false

        fun filter(gyroX: Float, gyroY: Float, gyroZ: Float, accelX: Float, accelY: Float, accelZ: Float, dt: Float): Triple<Float, Float, Float> {
            // Integrate gyroscope
            val gyroAngleX = if (initialized) filteredX + gyroX * dt else 0f
            val gyroAngleY = if (initialized) filteredY + gyroY * dt else 0f
            val gyroAngleZ = if (initialized) filteredZ + gyroZ * dt else 0f

            // Calculate angle from accelerometer
            val accelAngleX = atan2(accelY, accelZ)
            val accelAngleY = atan2(-accelX, sqrt(accelY * accelY + accelZ * accelZ))

            // Complementary filter
            val resultX = alpha * gyroAngleX + (1 - alpha) * accelAngleX
            val resultY = alpha * gyroAngleY + (1 - alpha) * accelAngleY
            val resultZ = gyroAngleZ // Yaw from gyro only

            filteredX = resultX
            filteredY = resultY
            filteredZ = resultZ
            initialized = true

            return Triple(resultX, resultY, resultZ)
        }

        fun reset() {
            initialized = false
            filteredX = 0f
            filteredY = 0f
            filteredZ = 0f
        }
    }

    /**
     * Kalman filter for sensor data
     */
    class KalmanFilter(private val processNoise: Float = 0.01f, private val measurementNoise: Float = 0.1f) {
        private var estimate = 0f
        private var error = 1f

        fun filter(measurement: Float): Float {
            // Prediction
            val prediction = estimate
            val predictionError = error + processNoise

            // Update
            val kalmanGain = predictionError / (predictionError + measurementNoise)
            estimate = prediction + kalmanGain * (measurement - prediction)
            error = (1 - kalmanGain) * predictionError

            return estimate
        }

        fun reset() {
            estimate = 0f
            error = 1f
        }
    }

    /**
     * Butterworth filter for low-pass filtering
     */
    class ButterworthFilter(private val cutoffFreq: Float, private val sampleRate: Float) {
        private var a1: Float = 0f
        private var a2: Float = 0f
        private var b0: Float = 0f
        private var b1: Float = 0f
        private var b2: Float = 0f

        private var x1 = 0f
        private var x2 = 0f
        private var y1 = 0f
        private var y2 = 0f
        private var initialized = false

        init {
            calculateCoefficients()
        }

        private fun calculateCoefficients() {
            val c = kotlin.math.tan(Math.PI * cutoffFreq / sampleRate).toFloat()
            val c2 = c * c
            val d = 1f + sqrt(2f) * c + c2

            b0 = c2 / d
            b1 = 2f * b0
            b2 = b0
            a1 = (2f * (c2 - 1f)) / d
            a2 = (1f - sqrt(2f) * c + c2) / d
        }

        fun filter(input: Float): Float {
            if (!initialized) {
                x1 = input
                x2 = input
                y1 = input
                y2 = input
                initialized = true
                return input
            }

            val output = b0 * input + b1 * x1 + b2 * x2 - a1 * y1 - a2 * y2

            x2 = x1
            x1 = input
            y2 = y1
            y1 = output

            return output
        }

        fun reset() {
            initialized = false
            x1 = 0f
            x2 = 0f
            y1 = 0f
            y2 = 0f
        }
    }

    /**
     * Calculate magnitude of vector
     */
    fun magnitude(x: Float, y: Float, z: Float): Float {
        return sqrt(x * x + y * y + z * z)
    }

    /**
     * Calculate angle between two vectors
     */
    fun angleBetween(v1x: Float, v1y: Float, v1z: Float, v2x: Float, v2y: Float, v2z: Float): Float {
        val dot = v1x * v2x + v1y * v2y + v1z * v2z
        val mag1 = magnitude(v1x, v1y, v1z)
        val mag2 = magnitude(v2x, v2y, v2z)

        if (mag1 == 0f || mag2 == 0f) return 0f

        val cosTheta = (dot / (mag1 * mag2)).coerceIn(-1f, 1f)
        return acos(cosTheta)
    }

    /**
     * Calculate derivative (rate of change)
     */
    fun derivative(current: Float, previous: Float, dt: Float): Float {
        if (dt <= 0f) return 0f
        return (current - previous) / dt
    }

    /**
     * Calculate integral
     */
    fun integral(value: Float, dt: Float, cumulative: Float): Float {
        return cumulative + value * dt
    }
}