// app/src/main/java/com/airmouse/data/datasource/local/AppDatabase.kt
package com.airmouse.data.datasource.local

import androidx.room.Database
import androidx.room.RoomDatabase
import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.MagCalibration
import com.airmouse.domain.model.Profile

@Database(
    entities = [GyroBias::class, AccelCalibration::class, MagCalibration::class, CustomGestureTemplate::class, Profile::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrationDao
    abstract fun gestureDao(): GestureDao
    abstract fun profileDao(): ProfileDao
}
