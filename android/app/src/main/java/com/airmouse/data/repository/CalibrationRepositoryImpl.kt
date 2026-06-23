
package com.airmouse.data.repository

import com.airmouse.data.datasource.local.ICalibrationDataSource
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.SensorCalibrationData
import com.airmouse.domain.repository.ICalibrationRepository
import com.airmouse.sensors.CalibrationHelper
import com.airmouse.utils.PreferencesManager
import com.airmouse.utils.PreferencesKeys
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationRepositoryImpl @Inject constructor(
    private val calibrationHelper: CalibrationHelper,
    private val dataSource: ICalibrationDataSource,
    private val prefs: PreferencesManager
) : ICalibrationRepository {

    private val _calibrationStatus = MutableStateFlow(getCurrentStatus())
    private val _calibrationProgress = MutableStateFlow(prefs.getInt(PreferencesKeys.KEY_CALIBRATION_PROGRESS, 0))
    private val _calibrationQuality = MutableStateFlow(getCurrentQuality())

    override suspend fun getCalibrationStatus(): CalibrationStatus = getCurrentStatus()

    override fun observeCalibrationStatus(): Flow<CalibrationStatus> = _calibrationStatus.asStateFlow()

    override suspend fun getCalibrationProgress(): Int = _calibrationProgress.value

    override fun observeCalibrationProgress(): Flow<Int> = _calibrationProgress.asStateFlow()

    override fun observeCalibrationQuality(): Flow<CalibrationQuality> = _calibrationQuality.asStateFlow()

    override suspend fun calibrateGyroscope(onProgress: (Int) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, true)
        val result = calibrationHelper.calibrateGyroscope()
        if (result) {
            prefs.putBoolean(PreferencesKeys.KEY_GYRO_CALIBRATED, true)
            _calibrationStatus.value = CalibrationStatus.GYRO_COMPLETE
            _calibrationQuality.value = getCurrentQuality()
            _calibrationProgress.value = 33
            onProgress(33)
        }
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, false)
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
        dataSource.saveGyroBias(bias.offsetX, bias.offsetY, bias.offsetZ)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun calibrateMagnetometer(onProgress: (Int) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, true)
        val result = calibrationHelper.calibrateMagnetometer()
        if (result) {
            prefs.putBoolean(PreferencesKeys.KEY_MAG_CALIBRATED, true)
            _calibrationStatus.value = CalibrationStatus.MAG_COMPLETE
            _calibrationProgress.value = 66
            onProgress(66)
        }
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, false)
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
        dataSource.saveMagOffset(data.offsetX, data.offsetY, data.offsetZ)
        dataSource.saveMagScale(data.scaleX, data.scaleY, data.scaleZ)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun calibrateAccelerometer(onInstruction: (String) -> Unit): Boolean {
        _calibrationStatus.value = CalibrationStatus.IN_PROGRESS
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, true)
        onInstruction("Place device flat facing up")
        val result = calibrationHelper.calibrateAccelerometer()
        if (result) {
            prefs.putBoolean(PreferencesKeys.KEY_ACCEL_CALIBRATED, true)
            _calibrationStatus.value = CalibrationStatus.ACCEL_COMPLETE
            _calibrationProgress.value = 100
        }
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, false)
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
        dataSource.saveAccelOffset(data.offsetX, data.offsetY, data.offsetZ)
        dataSource.saveAccelScale(data.scaleX, data.scaleY, data.scaleZ)
        calibrationHelper.saveCalibrationData()
    }

    override suspend fun getCalibrationData(): CalibrationData {
        return dataSource.getCalibrationData()
    }

    override suspend fun saveCalibrationData(data: CalibrationData) {
        dataSource.saveCalibrationData(data)
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
        dataSource.resetAll()
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_COMPLETE, false)
        prefs.putBoolean(PreferencesKeys.KEY_GYRO_CALIBRATED, false)
        prefs.putBoolean(PreferencesKeys.KEY_MAG_CALIBRATED, false)
        prefs.putBoolean(PreferencesKeys.KEY_ACCEL_CALIBRATED, false)
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, false)
        prefs.putBoolean(PreferencesKeys.KEY_CALIBRATION_APPLIED, false)
    }

    override suspend fun updateCalibrationStatus(status: CalibrationStatus) {
        prefs.putString(PreferencesKeys.KEY_CALIBRATION_STATUS, status.name)
        _calibrationStatus.value = status
    }

    override suspend fun updateCalibrationQuality(quality: CalibrationQuality) {
        prefs.putString(PreferencesKeys.KEY_CALIBRATION_QUALITY, quality.name)
        _calibrationQuality.value = quality
    }

    override suspend fun updateCalibrationProgress(progress: Int) {
        val clamped = progress.coerceIn(0, 100)
        prefs.putInt(PreferencesKeys.KEY_CALIBRATION_PROGRESS, clamped)
        _calibrationProgress.value = clamped
    }

    private fun getCurrentStatus(): CalibrationStatus = when {
        prefs.getBoolean(PreferencesKeys.KEY_CALIBRATION_IN_PROGRESS, false) -> CalibrationStatus.IN_PROGRESS
        prefs.getBoolean(PreferencesKeys.KEY_CALIBRATION_COMPLETE, false) -> CalibrationStatus.COMPLETED
        prefs.getBoolean(PreferencesKeys.KEY_GYRO_CALIBRATED, false) -> CalibrationStatus.GYRO_COMPLETE
        prefs.getBoolean(PreferencesKeys.KEY_MAG_CALIBRATED, false) -> CalibrationStatus.MAG_COMPLETE
        prefs.getBoolean(PreferencesKeys.KEY_ACCEL_CALIBRATED, false) -> CalibrationStatus.ACCEL_COMPLETE
        else -> CalibrationStatus.NOT_STARTED
    }

    private fun getCurrentQuality(): CalibrationQuality {
        return try {
            CalibrationQuality.valueOf(prefs.getString(PreferencesKeys.KEY_CALIBRATION_QUALITY, "UNKNOWN"))
        } catch (e: Exception) {
            CalibrationQuality.UNKNOWN
        }
    }
}
