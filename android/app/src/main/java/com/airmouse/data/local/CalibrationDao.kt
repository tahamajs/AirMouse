// app/src/main/java/com/airmouse/data/local/CalibrationDao.kt
package com.airmouse.data.local

import androidx.room.*
import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration

@Dao
interface CalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertGyroBias(bias: GyroBias)

    @Query("SELECT * FROM gyro_bias LIMIT 1")
    suspend fun getGyroBias(): GyroBias?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAccelCalibration(calibration: AccelCalibration)

    @Query("SELECT * FROM accel_calibration LIMIT 1")
    suspend fun getAccelCalibration(): AccelCalibration?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertMagCalibration(calibration: MagCalibration)

    @Query("SELECT * FROM mag_calibration LIMIT 1")
    suspend fun getMagCalibration(): MagCalibration?

    @Query("DELETE FROM gyro_bias")
    suspend fun clearGyroBias()

    @Query("DELETE FROM accel_calibration")
    suspend fun clearAccelCalibration()

    @Query("DELETE FROM mag_calibration")
    suspend fun clearMagCalibration()

    @Transaction
    suspend fun clearAllCalibration() {
        clearGyroBias()
        clearAccelCalibration()
        clearMagCalibration()
    }
}