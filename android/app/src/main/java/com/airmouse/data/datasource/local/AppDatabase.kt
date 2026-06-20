// app/src/main/java/com/airmouse/data/datasource/local/AppDatabase.kt
package com.airmouse.data.datasource.local

import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import android.content.Context
import com.airmouse.data.datasource.local.dao.CalibrationDao
import com.airmouse.data.datasource.local.dao.SettingsDao
import com.airmouse.data.datasource.local.dao.StatisticsDao
import com.airmouse.data.datasource.local.dao.GestureDao
import com.airmouse.data.datasource.local.entity.CalibrationEntity
import com.airmouse.data.datasource.local.entity.SettingsEntity
import com.airmouse.data.datasource.local.entity.StatisticsEntity
import com.airmouse.data.datasource.local.entity.GestureEntity
import com.airmouse.data.datasource.local.Converters // Corrected import

@Database(
    entities = [
        CalibrationEntity::class,
        SettingsEntity::class,
        StatisticsEntity::class,
        GestureEntity::class
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