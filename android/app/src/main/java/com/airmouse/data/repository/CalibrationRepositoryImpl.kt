// app/src/main/java/com/airmouse/data/repository/CalibrationRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.domain.model.*
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepositoryImpl @Inject constructor(
    private val calibrationHelper: CalibrationHelper,
    private val prefs: PreferencesManager
) : ICalibrationRepository {

    private val _calibrationStatus = MutableStateFlow(CalibrationStatus.NOT_STARTED)
    override fun observeCalibrationStatus(): Flow<CalibrationStatus> = _calibrationStatus.asStateFlow()

    private val _calibrationProgress = MutableStateFlow(0)
    override fun observeCalibrationProgress(): Flow<Int> = _calibrationProgress.asStateFlow()

    private val _calibrationQuality = MutableStateFlow(CalibrationQuality.UNKNOWN)
    override fun observeCalibrationQuality(): Flow<CalibrationQuality> = _calibrationQuality.asStateFlow()

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        return if (prefs.isCalibrated()) {
            CalibrationStatus.COMPLETED
        } else if (prefs.getBoolean("calibration_in_progress", false)) {
            CalibrationStatus.IN_PROGRESS
        } else {
            CalibrationStatus.NOT_STARTED
        }
    }

    override suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean("calibration_in_progress", true)

        val result = calibrationHelper.calibrateGyroscope { progress ->
            onProgress(progress)
            _calibrationProgress.value = progress
        }

        if (result) {
            _calibrationStatus.value = CalibrationStatus.GYRO_COMPLETE
            prefs.putBoolean("gyro_calibrated", true)
        }
        prefs.putBoolean("calibration_in_progress", false)
        return result
    }

    override suspend fun getGyroBias(): GyroBias {
        val bias = calibrationHelper.getGyroBias()
        return GyroBias(bias[0], bias[1], bias[2])
    }

    override suspend fun saveGyroBias(bias: GyroBias) {
        prefs.putFloat("gyro_bias_x", bias.x)
        prefs.putFloat("gyro_bias_y", bias.y)
        prefs.putFloat("gyro_bias_z", bias.z)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean("calibration_in_progress", true)

        val result = calibrationHelper.calibrateMagnetometer { progress ->
            onProgress(progress)
            _calibrationProgress.value = progress
        }

        if (result) {
            _calibrationStatus.value = CalibrationStatus.MAG_COMPLETE
            prefs.putBoolean("mag_calibrated", true)
        }
        prefs.putBoolean("calibration_in_progress", false)
        return result
    }

    override suspend fun getMagOffset(): SensorCalibrationData {
        val offset = calibrationHelper.getMagOffset()
        val scale = calibrationHelper.getMagScale()
        return SensorCalibrationData(
            offsetX = offset[0],
            offsetY = offset[1],
            offsetZ = offset[2],
            scaleX = scale[0],
            scaleY = scale[1],
            scaleZ = scale[2]
        )
    }

    override suspend fun saveMagOffset(data: SensorCalibrationData) {
        prefs.putFloat("mag_offset_x", data.offsetX)
        prefs.putFloat("mag_offset_y", data.offsetY)
        prefs.putFloat("mag_offset_z", data.offsetZ)
        prefs.putFloat("mag_scale_x", data.scaleX)
        prefs.putFloat("mag_scale_y", data.scaleY)
        prefs.putFloat("mag_scale_z", data.scaleZ)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean("calibration_in_progress", true)

        // Perform 6-point accelerometer calibration
        val orientations = listOf(
            "Place device flat facing UP",
            "Place device flat facing DOWN",
            "Place device on LEFT side",
            "Place device on RIGHT side",
            "Place device standing UP",
            "Place device standing DOWN"
        )

        var success = true
        for (i in orientations.indices) {
            onInstruction(orientations[i])
            val result = calibrationHelper.calibrateAccelerometer(i) { progress ->
                _calibrationProgress.value = progress
            }
            if (!result) {
                success = false
                break
            }
            _calibrationProgress.value = ((i + 1) * 100 / orientations.size)
        }

        if (success) {
            _calibrationStatus.value = CalibrationStatus.ACCEL_COMPLETE
            prefs.putBoolean("accel_calibrated", true)
        }
        prefs.putBoolean("calibration_in_progress", false)
        return success
    }

    override suspend fun getAccelOffset(): SensorCalibrationData {
        val offset = calibrationHelper.getAccelOffset()
        val scale = calibrationHelper.getAccelScale()
        return SensorCalibrationData(
            offsetX = offset[0],
            offsetY = offset[1],
            offsetZ = offset[2],
            scaleX = scale[0],
            scaleY = scale[1],
            scaleZ = scale[2]
        )
    }

    override suspend fun saveAccelOffset(data: SensorCalibrationData) {
        prefs.putFloat("accel_offset_x", data.offsetX)
        prefs.putFloat("accel_offset_y", data.offsetY)
        prefs.putFloat("accel_offset_z", data.offsetZ)
        prefs.putFloat("accel_scale_x", data.scaleX)
        prefs.putFloat("accel_scale_y", data.scaleY)
        prefs.putFloat("accel_scale_z", data.scaleZ)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun getCalibrationData(): CalibrationData {
        return CalibrationData(
            gyroBias = SensorCalibrationData(
                offsetX = prefs.getFloat("gyro_bias_x", 0f),
                offsetY = prefs.getFloat("gyro_bias_y", 0f),
                offsetZ = prefs.getFloat("gyro_bias_z", 0f)
            ),
            accelOffset = getAccelOffset(),
            magOffset = getMagOffset(),
            isCalibrated = prefs.isCalibrated(),
            quality = getCalibrationQuality()
        )
    }

    override suspend fun saveCalibrationData(data: CalibrationData) {
        saveGyroBias(GyroBias(
            data.gyroBias.offsetX,
            data.gyroBias.offsetY,
            data.gyroBias.offsetZ
        ))
        saveAccelOffset(data.accelOffset)
        saveMagOffset(data.magOffset)
        prefs.putBoolean("calibration_complete", data.isCalibrated)
    }

    override suspend fun getCalibrationQuality(): CalibrationQuality {
        val quality = prefs.getString("calibration_quality", "UNKNOWN")
        return try {
            CalibrationQuality.valueOf(quality)
        } catch (e: IllegalArgumentException) {
            CalibrationQuality.UNKNOWN
        }
    }

    override suspend fun resetCalibration() {
        calibrationHelper.reset()
        _calibrationStatus.value = CalibrationStatus.NOT_STARTED
        _calibrationQuality.value = CalibrationQuality.UNKNOWN
        _calibrationProgress.value = 0
    }

    override suspend fun resetAllCalibration() {
        resetCalibration()
        prefs.putBoolean("calibration_complete", false)
        prefs.putBoolean("gyro_calibrated", false)
        prefs.putBoolean("mag_calibrated", false)
        prefs.putBoolean("accel_calibrated", false)
        prefs.putBoolean("calibration_in_progress", false)
    }
}