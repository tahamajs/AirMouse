// app/src/main/java/com/airmouse/data/repository/CalibrationRepositoryImpl.kt
package com.airmouse.data.repository

import com.airmouse.data.datasource.local.CalibrationDao
import com.airmouse.data.local.PreferencesManager
import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration
import com.airmouse.domain.repository.ICalibrationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

class CalibrationRepositoryImpl @Inject constructor(
    private val dao: CalibrationDao,
    private val prefs: PreferencesManager
) : ICalibrationRepository {

    override suspend fun saveGyroBias(bias: GyroBias) {
        dao.insertGyroBias(bias)
    }

    override suspend fun getGyroBias(): GyroBias {
        return dao.getGyroBias() ?: GyroBias()
    }

    override suspend fun saveAccelCalibration(calibration: AccelCalibration) {
        dao.insertAccelCalibration(calibration)
    }

    override suspend fun getAccelCalibration(): AccelCalibration {
        return dao.getAccelCalibration() ?: AccelCalibration()
    }

    override suspend fun saveMagCalibration(calibration: MagCalibration) {
        dao.insertMagCalibration(calibration)
    }

    override suspend fun getMagCalibration(): MagCalibration {
        return dao.getMagCalibration() ?: MagCalibration()
    }

    override suspend fun markCalibrationComplete() {
        prefs.setCalibrated(true)
    }

    override fun isCalibrationComplete(): Flow<Boolean> = flow {
        emit(prefs.isCalibrated())
    }

    override suspend fun resetCalibration() {
        dao.clearAllCalibration()
        prefs.setCalibrated(false)
    }
}
