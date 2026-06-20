// app/src/main/java/com/airmouse/domain/usecase/CalibrationUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.repository.ICalibrationRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class CalibrationUseCase @Inject constructor(
    private val calibrationRepository: ICalibrationRepository
) {

    suspend fun startFullCalibration(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            // Step 1: Gyroscope
            val gyroSuccess = calibrationRepository.calibrateGyroscope { progress ->
                onProgress(progress / 3)
            }
            if (!gyroSuccess) {
                return Result.failure(Exception("Gyroscope calibration failed"))
            }

            // Step 2: Magnetometer
            val magSuccess = calibrationRepository.calibrateMagnetometer { progress ->
                onProgress(33 + (progress / 3))
            }
            if (!magSuccess) {
                return Result.failure(Exception("Magnetometer calibration failed"))
            }

            // Step 3: Accelerometer
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

    suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateGyroscope(onProgress)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateMagnetometer(onProgress)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Result<Boolean> {
        return try {
            val result = calibrationRepository.calibrateAccelerometer(onInstruction)
            Result.success(result)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun getCalibrationStatus(): CalibrationStatus {
        return calibrationRepository.getCalibrationStatus()
    }

    fun observeCalibrationStatus(): Flow<CalibrationStatus> {
        return calibrationRepository.observeCalibrationStatus()
    }

    suspend fun getCalibrationQuality(): CalibrationQuality {
        return calibrationRepository.getCalibrationQuality()
    }

    fun observeCalibrationQuality(): Flow<CalibrationQuality> {
        return calibrationRepository.observeCalibrationQuality()
    }

    suspend fun getCalibrationData(): CalibrationData {
        return calibrationRepository.getCalibrationData()
    }

    suspend fun saveCalibrationData(data: CalibrationData) {
        calibrationRepository.saveCalibrationData(data)
    }

    suspend fun applyCalibration(data: CalibrationData) {
        calibrationRepository.saveCalibrationData(data)
        calibrationRepository.updateCalibrationStatus(CalibrationStatus.COMPLETED)
        calibrationRepository.updateCalibrationQuality(data.quality)
        calibrationRepository.updateCalibrationProgress(100)
    }

    suspend fun resetCalibration(): Result<Unit> {
        return try {
            calibrationRepository.resetCalibration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun isCalibrated(): Boolean {
        return calibrationRepository.getCalibrationStatus() == CalibrationStatus.COMPLETED
    }

    suspend fun resetAllCalibration(): Result<Unit> {
        return try {
            calibrationRepository.resetAllCalibration()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun hasCalibrationData(): Boolean {
        val data = calibrationRepository.getCalibrationData()
        return data.isCalibrated
    }
}
