package com.airmouse.data.datasource.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.airmouse.data.datasource.local.dao.*
import com.airmouse.data.datasource.local.entity.*

@Database(
    entities = [
        CalibrationEntity::class,
        SettingsEntity::class,
        StatisticsEntity::class,
        GestureTemplateEntity::class,
        ProfileEntity::class,
        TrainingSampleEntity::class
    ],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun calibrationDao(): CalibrationDao
    abstract fun settingsDao(): SettingsDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun gestureDao(): GestureDao
    abstract fun profileDao(): ProfileDao
    abstract fun trainingSampleDao(): TrainingSampleDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "airmouse_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
