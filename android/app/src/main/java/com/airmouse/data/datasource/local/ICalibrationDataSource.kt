// app/src/main/java/com/airmouse/data/datasource/local/ICalibrationDataSource.kt
package com.airmouse.data.datasource.local

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.SensorCalibrationData

/**
 * Data source interface for calibration data persistence.
 *
 * This interface defines all operations for storing and retrieving
 * calibration data from local storage (Room/Preferences).
 */
interface ICalibrationDataSource {

    // ==========================================
    // Complete Calibration Data
    // ==========================================

    /**
     * Saves the complete calibration data object.
     * @param data The calibration data to save
     */
    suspend fun saveCalibrationData(data: CalibrationData)

    /**
     * Retrieves the complete calibration data object.
     * @return The stored calibration data, or default values if none exist
     */
    suspend fun getCalibrationData(): CalibrationData

    // ==========================================
    // Gyroscope Calibration
    // ==========================================

    /**
     * Saves gyroscope bias (offset) values.
     * @param x Offset for X axis
     * @param y Offset for Y axis
     * @param z Offset for Z axis
     */
    suspend fun saveGyroBias(x: Float, y: Float, z: Float)

    /**
     * Retrieves gyroscope bias (offset) values.
     * @return Triple of (x, y, z) offset values
     */
    suspend fun getGyroBias(): Triple<Float, Float, Float>

    /**
     * Saves gyroscope variance values.
     * @param x Variance for X axis
     * @param y Variance for Y axis
     * @param z Variance for Z axis
     */
    suspend fun saveGyroVariance(x: Float, y: Float, z: Float)

    /**
     * Retrieves gyroscope variance values.
     * @return Triple of (x, y, z) variance values
     */
    suspend fun getGyroVariance(): Triple<Float, Float, Float>

    /**
     * Saves gyroscope sample count.
     * @param count Number of samples collected
     */
    suspend fun saveGyroSampleCount(count: Int)

    /**
     * Retrieves gyroscope sample count.
     * @return Number of samples collected
     */
    suspend fun getGyroSampleCount(): Int

    // ==========================================
    // Accelerometer Calibration
    // ==========================================

    /**
     * Saves accelerometer offset values.
     * @param x Offset for X axis
     * @param y Offset for Y axis
     * @param z Offset for Z axis
     */
    suspend fun saveAccelOffset(x: Float, y: Float, z: Float)

    /**
     * Retrieves accelerometer offset values.
     * @return Triple of (x, y, z) offset values
     */
    suspend fun getAccelOffset(): Triple<Float, Float, Float>

    /**
     * Saves accelerometer scale values.
     * @param x Scale for X axis
     * @param y Scale for Y axis
     * @param z Scale for Z axis
     */
    suspend fun saveAccelScale(x: Float, y: Float, z: Float)

    /**
     * Retrieves accelerometer scale values.
     * @return Triple of (x, y, z) scale values
     */
    suspend fun getAccelScale(): Triple<Float, Float, Float>

    /**
     * Saves accelerometer position data.
     * @param position The position index (0-5)
     * @param values Triple of (x, y, z) values
     */
    suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>)

    /**
     * Retrieves accelerometer position data.
     * @param position The position index
     * @return Triple of (x, y, z) values, or null if not found
     */
    suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>?

    /**
     * Retrieves all accelerometer positions.
     * @return Map of position index to triple values
     */
    suspend fun getAllAccelPositions(): Map<Int, Triple<Float, Float, Float>>

    /**
     * Saves the number of completed accelerometer positions.
     * @param count Number of positions completed
     */
    suspend fun saveAccelPositionsCompleted(count: Int)

    /**
     * Retrieves the number of completed accelerometer positions.
     * @return Number of positions completed
     */
    suspend fun getAccelPositionsCompleted(): Int

    // ==========================================
    // Magnetometer Calibration
    // ==========================================

    /**
     * Saves magnetometer offset values.
     * @param x Offset for X axis
     * @param y Offset for Y axis
     * @param z Offset for Z axis
     */
    suspend fun saveMagOffset(x: Float, y: Float, z: Float)

    /**
     * Retrieves magnetometer offset values.
     * @return Triple of (x, y, z) offset values
     */
    suspend fun getMagOffset(): Triple<Float, Float, Float>

    /**
     * Saves magnetometer scale values.
     * @param x Scale for X axis
     * @param y Scale for Y axis
     * @param z Scale for Z axis
     */
    suspend fun saveMagScale(x: Float, y: Float, z: Float)

    /**
     * Retrieves magnetometer scale values.
     * @return Triple of (x, y, z) scale values
     */
    suspend fun getMagScale(): Triple<Float, Float, Float>

    /**
     * Saves magnetometer sample count.
     * @param count Number of samples collected
     */
    suspend fun saveMagSampleCount(count: Int)

    /**
     * Retrieves magnetometer sample count.
     * @return Number of samples collected
     */
    suspend fun getMagSampleCount(): Int

    // ==========================================
    // Calibration Status & Progress
    // ==========================================

    /**
     * Sets the overall calibration status.
     * @param status The calibration status
     */
    suspend fun setCalibrationStatus(status: CalibrationStatus)

    /**
     * Retrieves the overall calibration status.
     * @return The current calibration status
     */
    suspend fun getCalibrationStatus(): CalibrationStatus

    /**
     * Sets the calibration quality.
     * @param quality The calibration quality
     */
    suspend fun setCalibrationQuality(quality: CalibrationQuality)

    /**
     * Retrieves the calibration quality.
     * @return The current calibration quality
     */
    suspend fun getCalibrationQuality(): CalibrationQuality

    /**
     * Sets the overall calibration progress percentage.
     * @param progress Progress value (0-100)
     */
    suspend fun setCalibrationProgress(progress: Int)

    /**
     * Retrieves the overall calibration progress percentage.
     * @return Progress value (0-100)
     */
    suspend fun getCalibrationProgress(): Int

    /**
     * Sets the current calibration step.
     * @param step Current step (0-3)
     */
    suspend fun setCurrentStep(step: Int)

    /**
     * Retrieves the current calibration step.
     * @return Current step (0-3)
     */
    suspend fun getCurrentStep(): Int

    /**
     * Sets whether calibration is complete.
     * @param complete True if complete
     */
    suspend fun setCalibrationComplete(complete: Boolean)

    /**
     * Checks if calibration is complete.
     * @return True if calibration is complete
     */
    suspend fun isCalibrationComplete(): Boolean

    /**
     * Sets the calibration timestamp.
     * @param timestamp The timestamp in milliseconds
     */
    suspend fun setCalibrationTimestamp(timestamp: Long)

    /**
     * Retrieves the calibration timestamp.
     * @return The timestamp in milliseconds, or 0 if not set
     */
    suspend fun getCalibrationTimestamp(): Long

    // ==========================================
    // Helper Methods
    // ==========================================

    /**
     * Checks if any calibration data exists.
     * @return True if calibration data exists
     */
    suspend fun hasCalibrationData(): Boolean

    /**
     * Resets all calibration data to default values.
     */
    suspend fun resetAll()

    /**
     * Resets only gyroscope calibration data.
     */
    suspend fun resetGyro()

    /**
     * Resets only accelerometer calibration data.
     */
    suspend fun resetAccel()

    /**
     * Resets only magnetometer calibration data.
     */
    suspend fun resetMag()

    /**
     * Gets a summary of the current calibration state.
     * @return Map with calibration summary
     */
    suspend fun getCalibrationSummary(): Map<String, Any>
}