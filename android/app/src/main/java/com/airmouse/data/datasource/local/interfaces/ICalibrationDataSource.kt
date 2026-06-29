package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus

/**
 * Data source interface for calibration data persistence.
 *
 * Provides methods to save and retrieve all calibration-related data:
 * - Gyroscope bias and variance
 * - Accelerometer offset and scale
 * - Magnetometer offset and scale (hard-iron calibration)
 * - Calibration status, progress, and quality
 *
 * All methods are suspend functions for coroutine support.
 */
interface ICalibrationDataSource {

    // ============================================================
    // Complete Calibration Data
    // ============================================================

    /**
     * Save the complete calibration data object.
     */
    suspend fun saveCalibrationData(data: CalibrationData)

    /**
     * Get the complete calibration data object.
     * Returns null if no calibration data exists.
     */
    suspend fun getCalibrationData(): CalibrationData

    /**
     * Clear all calibration data.
     */
    suspend fun clearCalibrationData()

    // ============================================================
    // Gyroscope Calibration
    // ============================================================

    /**
     * Save gyroscope bias (offset) values.
     */
    suspend fun saveGyroBias(x: Float, y: Float, z: Float)

    /**
     * Get gyroscope bias values.
     * Returns (0f, 0f, 0f) if not set.
     */
    suspend fun getGyroBias(): Triple<Float, Float, Float>

    /**
     * Save gyroscope variance values (for quality assessment).
     */
    suspend fun saveGyroVariance(x: Float, y: Float, z: Float)

    /**
     * Get gyroscope variance values.
     * Returns (0f, 0f, 0f) if not set.
     */
    suspend fun getGyroVariance(): Triple<Float, Float, Float>

    /**
     * Save the number of samples used for gyroscope calibration.
     */
    suspend fun saveGyroSampleCount(count: Int)

    /**
     * Get the number of samples used for gyroscope calibration.
     * Returns 0 if not set.
     */
    suspend fun getGyroSampleCount(): Int

    // ============================================================
    // Accelerometer Calibration (6-position calibration)
    // ============================================================

    /**
     * Save accelerometer offset values.
     */
    suspend fun saveAccelOffset(x: Float, y: Float, z: Float)

    /**
     * Get accelerometer offset values.
     * Returns (0f, 0f, 0f) if not set.
     */
    suspend fun getAccelOffset(): Triple<Float, Float, Float>

    /**
     * Save accelerometer scale values.
     */
    suspend fun saveAccelScale(x: Float, y: Float, z: Float)

    /**
     * Get accelerometer scale values.
     * Returns (1f, 1f, 1f) if not set (no scaling).
     */
    suspend fun getAccelScale(): Triple<Float, Float, Float>

    /**
     * Save accelerometer measurement for a specific position (0-5).
     * Position 0: +X, 1: -X, 2: +Y, 3: -Y, 4: +Z, 5: -Z
     */
    suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>)

    /**
     * Get accelerometer measurement for a specific position.
     * Returns null if position not set.
     */
    suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>?

    /**
     * Get all saved accelerometer positions.
     * Returns map of position -> Triple(x, y, z).
     */
    suspend fun getAllAccelPositions(): Map<Int, Triple<Float, Float, Float>>

    /**
     * Save the number of completed accelerometer positions.
     */
    suspend fun saveAccelPositionsCompleted(count: Int)

    /**
     * Get the number of completed accelerometer positions.
     * Returns 0 if not set.
     */
    suspend fun getAccelPositionsCompleted(): Int

    // ============================================================
    // Magnetometer Calibration (Hard-iron)
    // ============================================================

    /**
     * Save magnetometer offset values.
     */
    suspend fun saveMagOffset(x: Float, y: Float, z: Float)

    /**
     * Get magnetometer offset values.
     * Returns (0f, 0f, 0f) if not set.
     */
    suspend fun getMagOffset(): Triple<Float, Float, Float>

    /**
     * Save magnetometer scale values.
     */
    suspend fun saveMagScale(x: Float, y: Float, z: Float)

    /**
     * Get magnetometer scale values.
     * Returns (1f, 1f, 1f) if not set.
     */
    suspend fun getMagScale(): Triple<Float, Float, Float>

    /**
     * Save the number of samples used for magnetometer calibration.
     */
    suspend fun saveMagSampleCount(count: Int)

    /**
     * Get the number of samples used for magnetometer calibration.
     * Returns 0 if not set.
     */
    suspend fun getMagSampleCount(): Int

    // ============================================================
    // Calibration Status & Metadata
    // ============================================================

    /**
     * Set the overall calibration status.
     */
    suspend fun setCalibrationStatus(status: CalibrationStatus)

    /**
     * Get the overall calibration status.
     * Returns CalibrationStatus.NOT_STARTED if not set.
     */
    suspend fun getCalibrationStatus(): CalibrationStatus

    /**
     * Set the calibration quality assessment.
     */
    suspend fun setCalibrationQuality(quality: CalibrationQuality)

    /**
     * Get the calibration quality assessment.
     * Returns CalibrationQuality.UNKNOWN if not set.
     */
    suspend fun getCalibrationQuality(): CalibrationQuality

    /**
     * Set the calibration progress percentage (0-100).
     */
    suspend fun setCalibrationProgress(progress: Int)

    /**
     * Get the calibration progress percentage.
     * Returns 0 if not set.
     */
    suspend fun getCalibrationProgress(): Int

    /**
     * Set the current calibration step (for multi-step processes).
     */
    suspend fun setCurrentStep(step: Int)

    /**
     * Get the current calibration step.
     * Returns 0 if not set.
     */
    suspend fun getCurrentStep(): Int

    /**
     * Mark calibration as complete.
     */
    suspend fun setCalibrationComplete(complete: Boolean)

    /**
     * Check if calibration is complete.
     * Returns false if not set.
     */
    suspend fun isCalibrationComplete(): Boolean

    /**
     * Set the timestamp when calibration was completed.
     */
    suspend fun setCalibrationTimestamp(timestamp: Long)

    /**
     * Get the timestamp when calibration was completed.
     * Returns 0 if not set.
     */
    suspend fun getCalibrationTimestamp(): Long

    // ============================================================
    // Utility Methods
    // ============================================================

    /**
     * Check if any calibration data exists.
     */
    suspend fun hasCalibrationData(): Boolean

    /**
     * Reset all calibration data (gyro, accel, mag, and status).
     */
    suspend fun resetAll()

    /**
     * Reset only gyroscope calibration data.
     */
    suspend fun resetGyro()

    /**
     * Reset only accelerometer calibration data.
     */
    suspend fun resetAccel()

    /**
     * Reset only magnetometer calibration data.
     */
    suspend fun resetMag()

    /**
     * Get a summary of calibration data as a map.
     * Useful for debugging and displaying to the user.
     */
    suspend fun getCalibrationSummary(): Map<String, Any>
}
