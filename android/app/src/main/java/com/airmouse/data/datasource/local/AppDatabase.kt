package com.airmouse.data.datasource.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.airmouse.domain.model.GyroBias
import com.airmouse.domain.model.AccelCalibration
import com.airmouse.domain.model.MagCalibration
import com.airmouse.domain.model.CustomGestureTemplate
import com.airmouse.domain.model.Profile

@Database(
    entities = [
        GyroBias::class,
        AccelCalibration::class,
        MagCalibration::class,
        CustomGestureTemplate::class,
        Profile::class
    ],
    version = 3,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun calibrationDao(): CalibrationDao
    abstract fun gestureDao(): GestureDao
    abstract fun profileDao(): ProfileDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airmouse_db"
                ).fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
