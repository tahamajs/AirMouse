// app/src/main/java/com/airmouse/data/datasource/local/AppDatabase.kt
package com.airmouse.data.datasource.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context

@Database(
    entities = [
        CalibrationEntity::class,
        GestureTemplateEntity::class,
        TrainingSampleEntity::class,
        ProfileEntity::class,
        StatisticsEntity::class,
        DailyStatsEntity::class,
        GestureStatsEntity::class
    ],
    version = 4,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {

    abstract fun calibrationDao(): CalibrationDao
    abstract fun gestureDao(): GestureDao
    abstract fun trainingSampleDao(): TrainingSampleDao
    abstract fun profileDao(): ProfileDao
    abstract fun statisticsDao(): StatisticsDao
    abstract fun dailyStatsDao(): DailyStatsDao
    abstract fun gestureStatsDao(): GestureStatsDao

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