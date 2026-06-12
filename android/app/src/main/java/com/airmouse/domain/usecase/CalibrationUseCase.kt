// app/src/main/java/com/airmouse/domain/usecase/CalibrationUseCase.kt
package com.airmouse.domain.usecase

import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.domain.repository.ISettingsRepository
import kotlinx.coroutines.flow.first
import javax.inject.Inject

class CalibrationUseCase @Inject constructor(
    private val calibrationRepo: ICalibrationRepository,
    private val settingsRepo: ISettingsRepository
) {

    suspend fun calibrateGyro(samples: List<FloatArray>): GyroBias {
        val avgX = samples.map { it[0] }.average().toFloat()
        val avgY = samples.map { it[1] }.average().toFloat()
        val avgZ = samples.map { it[2] }.average().toFloat()
        val bias = GyroBias(x = avgX, y = avgY, z = avgZ)
        calibrationRepo.saveGyroBias(bias)
        return bias
    }

    suspend fun calibrateAccelerometer(measurements: List<FloatArray>): AccelCalibration {
        // expects 6 measurements: +X, -X, +Y, -Y, +Z, -Z
        require(measurements.size == 6)
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
        return calibration
    }

    suspend fun calibrateMagnetometer(min: FloatArray, max: FloatArray): MagCalibration {
        val offset = floatArrayOf((max[0] + min[0]) / 2f, (max[1] + min[1]) / 2f, (max[2] + min[2]) / 2f)
        val scale = floatArrayOf((max[0] - min[0]) / 2f, (max[1] - min[1]) / 2f, (max[2] - min[2]) / 2f)
        val calibration = MagCalibration(
            offsetX = offset[0],
            offsetY = offset[1],
            offsetZ = offset[2],
            scaleX = scale[0],
            scaleY = scale[1],
            scaleZ = scale[2]
        )
        calibrationRepo.saveMagCalibration(calibration)
        calibrationRepo.markCalibrationComplete()
        return calibration
    }

    suspend fun isCalibrationComplete(): Boolean {
        return calibrationRepo.isCalibrationComplete().first()
    }

    suspend fun reset() {
        calibrationRepo.resetCalibration()
    }
}
