package com.airmouse.data.datasource.local

import com.airmouse.data.datasource.local.dao.CalibrationDao
import com.airmouse.data.datasource.local.entity.CalibrationEntity
import com.airmouse.domain.model.CalibrationData
import com.airmouse.domain.model.CalibrationQuality
import com.airmouse.domain.model.CalibrationStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class CalibrationDataSourceImpl @Inject constructor(
    private val calibrationDao: CalibrationDao
) : ICalibrationDataSource {

    private val accelPositions = ConcurrentHashMap<Int, Triple<Float, Float, Float>>()
    private val accelPositionsCompleted = AtomicInteger(0)
    private val calibrationProgress = AtomicInteger(0)
    private val currentStep = AtomicInteger(0)
    private val gyroSampleCount = AtomicInteger(0)
    private val magSampleCount = AtomicInteger(0)

    private suspend fun getOrCreate(): CalibrationEntity {
        return calibrationDao.getCalibration() ?: CalibrationEntity()
    }

    override suspend fun saveCalibrationData(data: CalibrationData) {
        withContext(Dispatchers.IO) {
            val entity = CalibrationEntity.fromDomainModel(data)
            calibrationDao.insertCalibration(entity)
        }
    }

    override suspend fun getCalibrationData(): CalibrationData {
        return withContext(Dispatchers.IO) {
            val entity = calibrationDao.getCalibration()
            entity?.toDomainModel() ?: CalibrationData()
        }
    }

    override suspend fun clearCalibrationData() {
        withContext(Dispatchers.IO) {
            resetAll()
        }
    }

    override suspend fun hasCalibrationData(): Boolean {
        return withContext(Dispatchers.IO) {
            calibrationDao.exists()
        }
    }

    override suspend fun resetAll() {
        withContext(Dispatchers.IO) {
            calibrationDao.deleteAll()
            accelPositions.clear()
            accelPositionsCompleted.set(0)
            calibrationProgress.set(0)
            currentStep.set(0)
            gyroSampleCount.set(0)
            magSampleCount.set(0)
        }
    }

    override suspend fun getCalibrationSummary(): Map<String, Any> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            mapOf(
                "gyro_bias" to mapOf("x" to e.gyroBiasX, "y" to e.gyroBiasY, "z" to e.gyroBiasZ),
                "accel_offset" to mapOf("x" to e.accelOffsetX, "y" to e.accelOffsetY, "z" to e.accelOffsetZ),
                "mag_offset" to mapOf("x" to e.magOffsetX, "y" to e.magOffsetY, "z" to e.magOffsetZ),
                "quality" to e.calibrationQuality,
                "is_complete" to e.isCalibrated,
                "timestamp" to e.timestamp
            )
        }
    }

    // ============================================================
    // Gyroscope
    // ============================================================

    override suspend fun saveGyroBias(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(gyroBiasX = x, gyroBiasY = y, gyroBiasZ = z)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getGyroBias(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            Triple(e.gyroBiasX, e.gyroBiasY, e.gyroBiasZ)
        }
    }

    override suspend fun saveGyroVariance(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(gyroVarianceX = x, gyroVarianceY = y, gyroVarianceZ = z)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getGyroVariance(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            Triple(e.gyroVarianceX, e.gyroVarianceY, e.gyroVarianceZ)
        }
    }

    override suspend fun saveGyroSampleCount(count: Int) {
        gyroSampleCount.set(count)
    }

    override suspend fun getGyroSampleCount(): Int {
        return gyroSampleCount.get()
    }

    override suspend fun resetGyro() {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(
                gyroBiasX = 0f, gyroBiasY = 0f, gyroBiasZ = 0f,
                gyroVarianceX = 0f, gyroVarianceY = 0f, gyroVarianceZ = 0f
            )
            calibrationDao.insertCalibration(updated)
            gyroSampleCount.set(0)
        }
    }

    // ============================================================
    // Accelerometer
    // ============================================================

    override suspend fun saveAccelOffset(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(accelOffsetX = x, accelOffsetY = y, accelOffsetZ = z)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getAccelOffset(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            Triple(e.accelOffsetX, e.accelOffsetY, e.accelOffsetZ)
        }
    }

    override suspend fun saveAccelScale(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(accelScaleX = x, accelScaleY = y, accelScaleZ = z)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getAccelScale(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            Triple(e.accelScaleX, e.accelScaleY, e.accelScaleZ)
        }
    }

    override suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>) {
        accelPositions[position] = values
    }

    override suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>? {
        return accelPositions[position]
    }

    override suspend fun getAllAccelPositions(): Map<Int, Triple<Float, Float, Float>> {
        return HashMap(accelPositions)
    }

    override suspend fun saveAccelPositionsCompleted(count: Int) {
        accelPositionsCompleted.set(count)
    }

    override suspend fun getAccelPositionsCompleted(): Int {
        return accelPositionsCompleted.get()
    }

    override suspend fun resetAccel() {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(
                accelOffsetX = 0f, accelOffsetY = 0f, accelOffsetZ = 0f,
                accelScaleX = 1f, accelScaleY = 1f, accelScaleZ = 1f
            )
            calibrationDao.insertCalibration(updated)
            accelPositions.clear()
            accelPositionsCompleted.set(0)
        }
    }

    // ============================================================
    // Magnetometer
    // ============================================================

    override suspend fun saveMagOffset(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(magOffsetX = x, magOffsetY = y, magOffsetZ = z)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getMagOffset(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            Triple(e.magOffsetX, e.magOffsetY, e.magOffsetZ)
        }
    }

    override suspend fun saveMagScale(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(magScaleX = x, magScaleY = y, magScaleZ = z)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getMagScale(): Triple<Float, Float, Float> {
        return withContext(Dispatchers.IO) {
            val e = getOrCreate()
            Triple(e.magScaleX, e.magScaleY, e.magScaleZ)
        }
    }

    override suspend fun saveMagSampleCount(count: Int) {
        magSampleCount.set(count)
    }

    override suspend fun getMagSampleCount(): Int {
        return magSampleCount.get()
    }

    override suspend fun resetMag() {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(
                magOffsetX = 0f, magOffsetY = 0f, magOffsetZ = 0f,
                magScaleX = 1f, magScaleY = 1f, magScaleZ = 1f
            )
            calibrationDao.insertCalibration(updated)
            magSampleCount.set(0)
        }
    }

    // ============================================================
    // Calibration Status & Metadata
    // ============================================================

    override suspend fun setCalibrationStatus(status: CalibrationStatus) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(calibrationQuality = status.name)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getCalibrationStatus(): CalibrationStatus {
        return withContext(Dispatchers.IO) {
            val quality = getOrCreate().calibrationQuality
            try {
                CalibrationStatus.valueOf(quality)
            } catch (_: Exception) {
                CalibrationStatus.NOT_STARTED
            }
        }
    }

    override suspend fun setCalibrationQuality(quality: CalibrationQuality) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(calibrationQuality = quality.name)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getCalibrationQuality(): CalibrationQuality {
        return withContext(Dispatchers.IO) {
            val quality = getOrCreate().calibrationQuality
            try {
                CalibrationQuality.valueOf(quality)
            } catch (_: Exception) {
                CalibrationQuality.UNKNOWN
            }
        }
    }

    override suspend fun setCalibrationProgress(progress: Int) {
        calibrationProgress.set(progress.coerceIn(0, 100))
    }

    override suspend fun getCalibrationProgress(): Int {
        return calibrationProgress.get()
    }

    override suspend fun setCurrentStep(step: Int) {
        currentStep.set(step.coerceIn(0, 3))
    }

    override suspend fun getCurrentStep(): Int {
        return currentStep.get()
    }

    override suspend fun setCalibrationComplete(complete: Boolean) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(isCalibrated = complete)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun isCalibrationComplete(): Boolean {
        return withContext(Dispatchers.IO) {
            getOrCreate().isCalibrated
        }
    }

    override suspend fun setCalibrationTimestamp(timestamp: Long) {
        withContext(Dispatchers.IO) {
            val updated = getOrCreate().copy(timestamp = timestamp)
            calibrationDao.insertCalibration(updated)
        }
    }

    override suspend fun getCalibrationTimestamp(): Long {
        return withContext(Dispatchers.IO) {
            getOrCreate().timestamp
        }
    }
}
