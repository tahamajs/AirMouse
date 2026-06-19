// app/src/main/java/com/airmouse/domain/usecase/CalibrationUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.repository.ICalibrationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

/**
 * Use case for sensor calibration
 */
class CalibrationUseCase @Inject constructor(
    private val calibrationRepository: ICalibrationRepository
) {

    /**
     * Start full calibration process
     */
    suspend operator fun invoke(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            // Step 1: Calibrate gyroscope
            val gyroSuccess = calibrationRepository.calibrateGyroscope { progress ->
                onProgress(progress / 3)
            }
            if (!gyroSuccess) {
                return Result.failure(Exception("Gyroscope calibration failed"))
            }

            // Step 2: Calibrate magnetometer
            val magSuccess = calibrationRepository.calibrateMagnetometer { progress ->
                onProgress(33 + (progress / 3))
            }
            if (!magSuccess) {
                return Result.failure(Exception("Magnetometer calibration failed"))
            }

            // Step 3: Calibrate accelerometer
            val accelSuccess = calibrationRepository.calibrateAccelerometer { instruction ->
                // Instruction callback
            }
            if (!accelSuccess) {
                return Result.failure(Exception("Accelerometer calibration failed"))
            }

            onProgress(100)
            Result.success(true)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate gyroscope only
     */
    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateGyroscope(onProgress)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate magnetometer only
     */
    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateMagnetometer(onProgress)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Calibrate accelerometer only
     */
    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateAccelerometer(onInstruction)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Get calibration status
     */
    suspend fun getCalibrationStatus(): CalibrationStatus {
        return calibrationRepository.getCalibrationStatus()
    }

    /**
     * Observe calibration status
     */
    fun observeCalibrationStatus(): Flow<CalibrationStatus> {
        return calibrationRepository.observeCalibrationStatus()
    }

    /**
     * Get calibration quality
     */
    suspend fun getCalibrationQuality(): CalibrationQuality {
        return calibrationRepository.getCalibrationQuality()
    }

    /**
     * Observe calibration quality
     */
    fun observeCalibrationQuality(): Flow<CalibrationQuality> {
        return calibrationRepository.observeCalibrationQuality()
    }

    /**
     * Get calibration data
     */
    suspend fun getCalibrationData(): CalibrationData {
        return calibrationRepository.getCalibrationData()
    }

    /**
     * Reset calibration
     */
    suspend fun resetCalibration(): Result<Unit> {
        return try {
            calibrationRepository.resetCalibration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Check if calibrated
     */
    suspend fun isCalibrated(): Boolean {
        return calibrationRepository.getCalibrationStatus() == CalibrationStatus.COMPLETED
    }
}