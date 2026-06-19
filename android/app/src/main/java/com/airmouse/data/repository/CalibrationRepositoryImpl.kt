package com.airmouse.data.repository

import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepositoryImpl @Inject constructor(
    private val calibrationHelper: CalibrationHelper,
    private val prefs: PreferencesManager
) : ICalibrationRepository {

    private val _calibrationStatus = MutableStateFlow(getCurrentStatus())
    private val _calibrationProgress = MutableStateFlow(prefs.getInt("calibration_progress", 0))
    private val _calibrationQuality = MutableStateFlow(getCurrentQuality())

    override suspend fun getCalibrationStatus(): CalibrationStatus = getCurrentStatus()

    override fun observeCalibrationStatus(): Flow<CalibrationStatus> = _calibrationStatus.asStateFlow()

    override suspend fun getCalibrationProgress(): Int = _calibrationProgress.value

    override fun observeCalibrationProgress(): Flow<Int> = _calibrationProgress.asStateFlow()

    override fun observeCalibrationQuality(): Flow<CalibrationQuality> = _calibrationQuality.asStateFlow()

    override suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean("calibration_in_progress", true)
        val result = calibrationHelper.calibrateGyroscope()
        if (result) {
            prefs.putBoolean("gyro_calibrated", true)
            _calibrationStatus.value = CalibrationStatus.GYRO_COMPLETE
            _calibrationQuality.value = calibrationHelper.getCalibrationStats()["calibration_quality"]
                ?.toString()
                ?.let { runCatching { CalibrationQuality.valueOf(it) }.getOrNull() }
                ?: CalibrationQuality.UNKNOWN
            _calibrationProgress.value = 33
            onProgress(33)
        }
        prefs.putBoolean("calibration_in_progress", false)
        return result
    }

    override suspend fun getGyroBias(): GyroBias {
        val bias = calibrationHelper.getGyroBias()
        return GyroBias(
            offsetX = bias[0],
            offsetY = bias[1],
            offsetZ = bias[2]
        )
    }

    override suspend fun saveGyroBias(bias: GyroBias) {
        prefs.putFloat("gyro_bias_x", bias.offsetX)
        prefs.putFloat("gyro_bias_y", bias.offsetY)
        prefs.putFloat("gyro_bias_z", bias.offsetZ)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean("calibration_in_progress", true)
        val result = calibrationHelper.calibrateMagnetometer()
        if (result) {
            prefs.putBoolean("mag_calibrated", true)
            _calibrationStatus.value = CalibrationStatus.MAG_COMPLETE
            _calibrationProgress.value = 66
            onProgress(66)
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
        onInstruction("Place device flat facing up")
        val result = calibrationHelper.calibrateAccelerometer()
        if (result) {
            prefs.putBoolean("accel_calibrated", true)
            _calibrationStatus.value = CalibrationStatus.ACCEL_COMPLETE
            _calibrationProgress.value = 100
        }
        prefs.putBoolean("calibration_in_progress", false)
        return result
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
            gyroBias = getGyroBias().let {
                SensorCalibrationData(
                    offsetX = it.offsetX,
                    offsetY = it.offsetY,
                    offsetZ = it.offsetZ
                )
            },
            accelOffset = getAccelOffset(),
            magOffset = getMagOffset(),
            isCalibrated = prefs.isCalibrated(),
            quality = getCalibrationQuality()
        )
    }

    override suspend fun saveCalibrationData(data: CalibrationData) {
        saveGyroBias(
            GyroBias(
                offsetX = data.gyroBias.offsetX,
                offsetY = data.gyroBias.offsetY,
                offsetZ = data.gyroBias.offsetZ
            )
        )
        saveAccelOffset(data.accelOffset)
        saveMagOffset(data.magOffset)
        prefs.putBoolean("calibration_complete", data.isCalibrated)
        prefs.putString("calibration_quality", data.quality.name)
    }

    override suspend fun resetCalibration() {
        calibrationHelper.resetCalibration()
        _calibrationStatus.value = CalibrationStatus.NOT_STARTED
        _calibrationProgress.value = 0
        _calibrationQuality.value = CalibrationQuality.UNKNOWN
    }

    override suspend fun getCalibrationQuality(): CalibrationQuality = getCurrentQuality()

    override suspend fun resetAllCalibration() {
        resetCalibration()
        prefs.putBoolean("calibration_complete", false)
        prefs.putBoolean("gyro_calibrated", false)
        prefs.putBoolean("mag_calibrated", false)
        prefs.putBoolean("accel_calibrated", false)
        prefs.putBoolean("calibration_in_progress", false)
    }

    private fun getCurrentStatus(): CalibrationStatus = when {
        prefs.getBoolean("calibration_in_progress", false) -> CalibrationStatus.IN_PROGRESS
        prefs.isCalibrated() -> CalibrationStatus.COMPLETED
        prefs.getBoolean("gyro_calibrated", false) -> CalibrationStatus.GYRO_COMPLETE
        else -> CalibrationStatus.NOT_STARTED
    }

    private fun getCurrentQuality(): CalibrationQuality {
        return runCatching {
            CalibrationQuality.valueOf(prefs.getString("calibration_quality", "UNKNOWN"))
        }.getOrDefault(CalibrationQuality.UNKNOWN)
    }
}
