package com.airmouse.data.datasource.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface CalibrationDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibration(calibration: CalibrationEntity)

    @Query("SELECT * FROM calibration WHERE id = 'default'")
    suspend fun getCalibration(): CalibrationEntity?

    @Query("DELETE FROM calibration")
    suspend fun deleteAll()

    @Query("SELECT EXISTS(SELECT 1 FROM calibration WHERE id = 'default')")
    suspend fun exists(): Boolean
}
