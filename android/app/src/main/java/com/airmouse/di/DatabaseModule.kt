// app/src/main/java/com/airmouse/di/DatabaseModule.kt
package com.airmouse.di

import android.content.Context
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.airmouse.data.datasource.local.*
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    // ==================== MIGRATIONS ====================

    private val MIGRATION_1_2 = object : Migration(1, 2) {
        override fun migrate(database: SupportSQLiteDatabase) {
            database.execSQL("ALTER TABLE custom_gestures ADD COLUMN detectionCount INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE custom_gestures ADD COLUMN lastDetected INTEGER NOT NULL DEFAULT 0")
            database.execSQL("ALTER TABLE custom_gestures ADD COLUMN confidenceScore REAL NOT NULL DEFAULT 0.0")
        }
    }

    private val MIGRATION_2_3 = object : Migration(2, 3) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Create profiles table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS profiles (
                    id TEXT PRIMARY KEY,
                    name TEXT NOT NULL,
                    email TEXT NOT NULL DEFAULT '',
                    avatar_uri TEXT,
                    sensitivity REAL NOT NULL DEFAULT 1.0,
                    clickThreshold REAL NOT NULL DEFAULT 5.0,
                    doubleClickInterval INTEGER NOT NULL DEFAULT 400,
                    scrollThreshold REAL NOT NULL DEFAULT 8.0,
                    rightClickTilt REAL NOT NULL DEFAULT 45.0,
                    hapticEnabled INTEGER NOT NULL DEFAULT 1,
                    theme TEXT NOT NULL DEFAULT 'dark',
                    aiSmoothing INTEGER NOT NULL DEFAULT 0,
                    predictiveMovement INTEGER NOT NULL DEFAULT 1,
                    invertX INTEGER NOT NULL DEFAULT 0,
                    invertY INTEGER NOT NULL DEFAULT 0,
                    accelerationEnabled INTEGER NOT NULL DEFAULT 1,
                    smoothingEnabled INTEGER NOT NULL DEFAULT 1,
                    edgeGesturesEnabled INTEGER NOT NULL DEFAULT 0,
                    voiceCommandsEnabled INTEGER NOT NULL DEFAULT 0,
                    isDefault INTEGER NOT NULL DEFAULT 0,
                    isFavorite INTEGER NOT NULL DEFAULT 0,
                    tags TEXT,
                    iconRes INTEGER NOT NULL DEFAULT 0,
                    createdAt INTEGER NOT NULL,
                    lastUsed INTEGER NOT NULL
                )
            """)

            // Create statistics table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS statistics (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    session_id TEXT NOT NULL,
                    total_clicks INTEGER DEFAULT 0,
                    total_double_clicks INTEGER DEFAULT 0,
                    total_right_clicks INTEGER DEFAULT 0,
                    total_scrolls INTEGER DEFAULT 0,
                    total_movements INTEGER DEFAULT 0,
                    total_distance REAL DEFAULT 0,
                    average_speed REAL DEFAULT 0,
                    max_speed REAL DEFAULT 0,
                    start_time INTEGER NOT NULL,
                    end_time INTEGER DEFAULT 0,
                    is_active INTEGER DEFAULT 1
                )
            """)

            // Create daily stats table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS daily_stats (
                    date TEXT PRIMARY KEY,
                    clicks INTEGER DEFAULT 0,
                    double_clicks INTEGER DEFAULT 0,
                    right_clicks INTEGER DEFAULT 0,
                    scrolls INTEGER DEFAULT 0,
                    movements INTEGER DEFAULT 0,
                    distance REAL DEFAULT 0,
                    gestures INTEGER DEFAULT 0
                )
            """)

            // Create gesture stats table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS gesture_stats (
                    gestureName TEXT PRIMARY KEY,
                    detection_count INTEGER DEFAULT 0,
                    confidence_percentage REAL DEFAULT 0,
                    last_detected INTEGER DEFAULT 0,
                    is_custom INTEGER DEFAULT 0,
                    category TEXT DEFAULT 'general',
                    is_favorite INTEGER DEFAULT 0,
                    avg_execution_time_ms REAL DEFAULT 0,
                    total_confidence_sum REAL DEFAULT 0
                )
            """)

            // Create training samples table
            database.execSQL("""
                CREATE TABLE IF NOT EXISTS training_samples (
                    id INTEGER PRIMARY KEY AUTOINCREMENT,
                    gesture_name TEXT NOT NULL,
                    sample_data TEXT NOT NULL,
                    timestamp INTEGER NOT NULL,
                    confidence REAL DEFAULT 0
                )
            """)
        }
    }

    private val MIGRATION_3_4 = object : Migration(3, 4) {
        override fun migrate(database: SupportSQLiteDatabase) {
            // Add new columns to gesture_templates
            database.execSQL("ALTER TABLE gesture_templates ADD COLUMN description TEXT")
            database.execSQL("ALTER TABLE gesture_templates ADD COLUMN icon_res INTEGER DEFAULT 0")
            database.execSQL("ALTER TABLE gesture_templates ADD COLUMN version INTEGER DEFAULT 1")
            database.execSQL("ALTER TABLE gesture_templates ADD COLUMN is_system INTEGER DEFAULT 0")
            database.execSQL("ALTER TABLE gesture_templates ADD COLUMN training_samples_count INTEGER DEFAULT 0")
            database.execSQL("ALTER TABLE gesture_templates ADD COLUMN is_favorite INTEGER DEFAULT 0")

            // Add indices for performance
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_gesture_templates_name ON gesture_templates(name)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_gesture_templates_type ON gesture_templates(type)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_gesture_stats_category ON gesture_stats(category)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_statistics_session ON statistics(session_id)")
            database.execSQL("CREATE INDEX IF NOT EXISTS idx_daily_stats_date ON daily_stats(date)")
        }
    }

    // ==================== DATABASE PROVIDER ====================

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "airmouse_database"
        )
            .addMigrations(MIGRATION_1_2, MIGRATION_2_3, MIGRATION_3_4)
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .build()
    }

    // ==================== DAO PROVIDERS ====================

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

    @Provides
    @Singleton
    fun provideStatisticsDao(database: AppDatabase): StatisticsDao {
        return database.statisticsDao()
    }

    @Provides
    @Singleton
    fun provideDailyStatsDao(database: AppDatabase): DailyStatsDao {
        return database.dailyStatsDao()
    }

    @Provides
    @Singleton
    fun provideGestureStatsDao(database: AppDatabase): GestureStatsDao {
        return database.gestureStatsDao()
    }

    @Provides
    @Singleton
    fun provideTrainingSampleDao(database: AppDatabase): TrainingSampleDao {
        return database.trainingSampleDao()
    }
}