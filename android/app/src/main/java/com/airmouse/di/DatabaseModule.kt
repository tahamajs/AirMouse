// app/src/main/java/com/airmouse/di/DatabaseModule.kt
package com.airmouse.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.airmouse.data.datasource.local.AppDatabase
import com.airmouse.data.datasource.local.CalibrationDao
import com.airmouse.data.datasource.local.GestureDao
import com.airmouse.data.datasource.local.ProfileDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE custom_gestures ADD COLUMN detectionCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE custom_gestures ADD COLUMN lastDetected INTEGER NOT NULL DEFAULT 0")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("CREATE TABLE IF NOT EXISTS profiles (" +
                    "id TEXT PRIMARY KEY, " +
                    "name TEXT NOT NULL, " +
                    "sensitivity REAL NOT NULL, " +
                    "clickThreshold REAL NOT NULL, " +
                    "doubleClickInterval INTEGER NOT NULL, " +
                    "scrollThreshold REAL NOT NULL, " +
                    "rightClickTilt REAL NOT NULL, " +
                    "hapticEnabled INTEGER NOT NULL, " +
                    "theme TEXT NOT NULL, " +
                    "aiSmoothing INTEGER NOT NULL, " +
                    "predictiveMovement INTEGER NOT NULL, " +
                    "invertX INTEGER NOT NULL, " +
                    "invertY INTEGER NOT NULL, " +
                    "accelerationEnabled INTEGER NOT NULL, " +
                    "smoothingEnabled INTEGER NOT NULL, " +
                    "createdAt INTEGER NOT NULL, " +
                    "lastUsed INTEGER NOT NULL, " +
                    "isDefault INTEGER NOT NULL, " +
                    "isFavorite INTEGER NOT NULL)")
        }
    }

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "airmouse_db"
        ).addMigrations(MIGRATION_1_2, MIGRATION_2_3)
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    @Singleton
    fun provideCalibrationDao(database: AppDatabase): CalibrationDao {
        return database.calibrationDao()
    }

    @Provides
    @Singleton
    fun provideGestureDao(database: AppDatabase): GestureDao {
        return database.gestureDao()
    }

    @Provides
    @Singleton
    fun provideProfileDao(database: AppDatabase): ProfileDao {
        return database.profileDao()
    }
}