# 📘 Air Mouse Local Data Sources – Complete Documentation

## 📁 Package Overview

The `com.airmouse.data.datasource.local` package contains **all local data source implementations** that handle **persistent storage** on the Android device. These data sources abstract away the underlying storage mechanisms (Room Database, SharedPreferences, DataStore) and provide a clean, testable interface for the repository layer.

```
com.airmouse.data.datasource.local/
├── interfaces/
│   ├── ICalibrationDataSource.kt       # Calibration data interface
│   ├── IGestureDataSource.kt           # Gesture data interface
│   ├── IProfileDataSource.kt           # Profile data interface
│   ├── IStatisticsDataSource.kt        # Statistics data interface
│   ├── IPreferencesDataSource.kt       # Preferences interface
│   └── ILocalDataSource.kt             # Unified local data interface
│
├── implementations/
│   ├── CalibrationDataSourceImpl.kt    # Calibration (Preferences)
│   ├── GestureDataSourceImpl.kt        # Gesture (Preferences + JSON)
│   ├── ProfileDataSourceImpl.kt        # Profile (Preferences + JSON)
│   ├── StatisticsDataSourceImpl.kt     # Statistics (Preferences)
│   ├── PreferencesDataSourceImpl.kt    # Preferences (SharedPreferences)
│   └── LocalDataSourceImpl.kt          # Unified (Room + Preferences)
│
├── database/
│   ├── AppDatabase.kt                  # Room Database
│   ├── Converters.kt                   # Room Type Converters
│   ├── entities/
│   │   ├── CalibrationEntity.kt
│   │   ├── SettingsEntity.kt
│   │   ├── StatisticsEntity.kt
│   │   ├── GestureTemplateEntity.kt
│   │   ├── ProfileEntity.kt
│   │   ├── TrainingSampleEntity.kt
│   │   ├── DailyStatsEntity.kt
│   │   └── GestureStatsEntity.kt
│   └── dao/
│       ├── CalibrationDao.kt
│       ├── SettingsDao.kt
│       ├── StatisticsDao.kt
│       ├── GestureDao.kt
│       ├── ProfileDao.kt
│       ├── TrainingSampleDao.kt
│       ├── DailyStatsDao.kt
│       └── GestureStatsDao.kt
│
├── mappers/
│   ├── DomainToEntityMapper.kt
│   └── EntityToDomainMapper.kt
│
└── models/
    ├── CalibrationPrefsData.kt
    ├── GestureData.kt
    ├── SensorData.kt
    ├── Quadruple.kt
    └── GestureTypeCount.kt
```

---

## 🗄️ 1. Room Database (SQLite)

### `AppDatabase.kt` – The Database Definition

```kotlin
@Database(
    entities = [
        CalibrationEntity::class,
        SettingsEntity::class,
        StatisticsEntity::class,
        GestureTemplateEntity::class,
        ProfileEntity::class,
        TrainingSampleEntity::class,
        DailyStatsEntity::class,
        GestureStatsEntity::class
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
```

### Entity Classes

#### `CalibrationEntity.kt`
Stores gyroscope, accelerometer, and magnetometer calibration data.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Primary key (default "default") |
| `gyroBiasX/Y/Z` | Float | Gyroscope bias offsets |
| `gyroVarianceX/Y/Z` | Float | Gyroscope variance values |
| `accelOffsetX/Y/Z` | Float | Accelerometer offsets |
| `accelScaleX/Y/Z` | Float | Accelerometer scale factors |
| `magOffsetX/Y/Z` | Float | Magnetometer offsets |
| `magScaleX/Y/Z` | Float | Magnetometer scale factors |
| `isCalibrated` | Boolean | Whether calibration is complete |
| `calibrationQuality` | String | Quality rating (EXCELLENT/GOOD/FAIR/POOR/UNKNOWN) |
| `timestamp` | Long | Time of calibration |

#### `ProfileEntity.kt`
Stores user profiles with all settings.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Primary key |
| `name` | String | Profile name |
| `sensitivity` | Float | Cursor sensitivity |
| `clickThreshold` | Float | Click detection threshold |
| `doubleClickInterval` | Long | Double-click time window |
| `scrollThreshold` | Float | Scroll detection threshold |
| `rightClickTilt` | Float | Tilt angle for right-click |
| `hapticEnabled` | Boolean | Haptic feedback toggle |
| `theme` | String | UI theme name |
| `aiSmoothing` | Boolean | AI smoothing toggle |
| `predictiveMovement` | Boolean | Predictive movement toggle |
| `invertX/Y` | Boolean | Axis inversion toggles |
| `accelerationEnabled` | Boolean | Acceleration toggle |
| `smoothingEnabled` | Boolean | Smoothing toggle |
| `edgeGesturesEnabled` | Boolean | Edge gestures toggle |
| `voiceCommandsEnabled` | Boolean | Voice commands toggle |
| `isDefault` | Boolean | Default profile flag |
| `isFavorite` | Boolean | Favorite profile flag |

#### `GestureTemplateEntity.kt`
Stores gesture templates for recognition.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Primary key |
| `name` | String | Gesture name |
| `type` | String | Gesture type (SWIPE/CIRCLE/CUSTOM/etc.) |
| `action` | String | Action to execute |
| `confidence` | Float | Confidence threshold |
| `isEnabled` | Boolean | Enabled toggle |
| `isCustom` | Boolean | Custom gesture flag |
| `isFavorite` | Boolean | Favorite gesture flag |
| `detectionCount` | Int | Number of times detected |
| `confidenceThreshold` | Float | Minimum confidence for detection |
| `trainingSamplesCount` | Int | Number of training samples |
| `lastDetected` | Long | Last detection timestamp |
| `createdAt`/`updatedAt` | Long | Timestamps |

#### `TrainingSampleEntity.kt`
Stores training data for machine learning gesture recognition.

| Field | Type | Description |
|-------|------|-------------|
| `id` | Long | Auto-generated primary key |
| `gestureId` | String | Reference to gesture template |
| `gyroX/Y/Z` | Float | Gyroscope sample data |
| `accelX/Y/Z` | Float | Accelerometer sample data |
| `magX/Y/Z` | Float | Magnetometer sample data |
| `label` | String | Gesture label |
| `confidence` | Float | Confidence of this sample |
| `isValid` | Boolean | Whether sample is valid |
| `timestamp` | Long | Sample timestamp |

#### `StatisticsEntity.kt`
Stores aggregated usage statistics.

| Field | Type | Description |
|-------|------|-------------|
| `id` | String | Primary key |
| `sessionId` | String | Session identifier |
| `totalClicks` | Int | Total clicks |
| `totalDoubleClicks` | Int | Total double-clicks |
| `totalRightClicks` | Int | Total right-clicks |
| `totalScrolls` | Int | Total scrolls |
| `totalMovement` | Float | Total cursor distance moved |
| `movementCount` | Long | Number of movements |
| `gestureCount` | Long | Total gestures detected |
| `sessionCount` | Long | Number of sessions |
| `totalSessionTime` | Long | Total time in sessions |
| `startTime`/`endTime` | Long | Session timestamps |
| `isActive` | Boolean | Whether session is active |

#### `DailyStatsEntity.kt`
Stores daily aggregated statistics.

| Field | Type | Description |
|-------|------|-------------|
| `date` | String | Date (YYYY-MM-DD) as primary key |
| `clicks` | Int | Clicks on this day |
| `doubleClicks` | Int | Double-clicks on this day |
| `rightClicks` | Int | Right-clicks on this day |
| `scrolls` | Int | Scrolls on this day |
| `gestures` | Int | Gestures on this day |
| `movements` | Int | Movements on this day |
| `distance` | Float | Total distance moved |
| `sessionTime` | Long | Total session time |
| `activeTime` | Long | Active time |

#### `GestureStatsEntity.kt`
Stores per-gesture statistics.

| Field | Type | Description |
|-------|------|-------------|
| `gesture_name` | String | Gesture name (primary key) |
| `count` | Int | Detection count |
| `avgConfidence` | Float | Average confidence score |
| `lastDetected` | Long | Last detection timestamp |
| `detectionRate` | Float | Detection rate |

---

### Data Access Objects (DAOs)

#### `CalibrationDao.kt`
```kotlin
@Dao
interface CalibrationDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCalibration(calibration: CalibrationEntity)
    
    @Query("SELECT * FROM calibration WHERE id = 'default'")
    suspend fun getCalibration(): CalibrationEntity?
    
    @Query("DELETE FROM calibration")
    suspend fun deleteAll()
}
```

#### `ProfileDao.kt`
```kotlin
@Dao
interface ProfileDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertProfile(profile: ProfileEntity)
    
    @Query("SELECT * FROM profiles WHERE id = :id")
    suspend fun getProfileById(id: String): ProfileEntity?
    
    @Query("SELECT * FROM profiles")
    suspend fun getAllProfiles(): List<ProfileEntity>
    
    @Query("SELECT * FROM profiles")
    fun observeAllProfiles(): Flow<List<ProfileEntity>>
    
    @Query("UPDATE profiles SET is_default = 0")
    suspend fun clearDefaultFlag()
    
    @Query("UPDATE profiles SET is_default = 1 WHERE id = :id")
    suspend fun setDefaultProfile(id: String)
    
    @Query("UPDATE profiles SET is_favorite = :favorite WHERE id = :id")
    suspend fun setFavorite(id: String, favorite: Boolean)
    
    @Query("DELETE FROM profiles WHERE id = :id")
    suspend fun deleteProfile(id: String)
}
```

#### `GestureDao.kt`
```kotlin
@Dao
interface GestureDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTemplate(template: GestureTemplateEntity)
    
    @Query("SELECT * FROM gesture_templates ORDER BY name ASC")
    suspend fun getAllTemplates(): List<GestureTemplateEntity>
    
    @Query("SELECT * FROM gesture_templates ORDER BY name ASC")
    fun observeAllTemplates(): Flow<List<GestureTemplateEntity>>
    
    @Query("SELECT * FROM gesture_templates WHERE id = :id")
    suspend fun getTemplateById(id: String): GestureTemplateEntity?
    
    @Query("SELECT * FROM gesture_templates WHERE is_favorite = 1")
    suspend fun getFavoriteTemplates(): List<GestureTemplateEntity>
    
    @Query("UPDATE gesture_templates SET detection_count = detection_count + 1, last_detected = :timestamp WHERE id = :id")
    suspend fun incrementDetectionCount(id: String, timestamp: Long)
    
    @Query("DELETE FROM gesture_templates WHERE id = :id")
    suspend fun deleteTemplate(id: String)
}
```

---

## 💾 2. Data Source Interfaces

### `ICalibrationDataSource.kt`
```kotlin
interface ICalibrationDataSource {
    // Core operations
    suspend fun saveCalibrationData(data: CalibrationData)
    suspend fun getCalibrationData(): CalibrationData
    suspend fun clearCalibrationData()
    suspend fun hasCalibrationData(): Boolean
    suspend fun resetAll()
    suspend fun getCalibrationSummary(): Map<String, Any>
    
    // Gyroscope
    suspend fun saveGyroBias(x: Float, y: Float, z: Float)
    suspend fun getGyroBias(): Triple<Float, Float, Float>
    suspend fun saveGyroVariance(x: Float, y: Float, z: Float)
    suspend fun getGyroVariance(): Triple<Float, Float, Float>
    suspend fun saveGyroSampleCount(count: Int)
    suspend fun getGyroSampleCount(): Int
    
    // Accelerometer
    suspend fun saveAccelOffset(x: Float, y: Float, z: Float)
    suspend fun getAccelOffset(): Triple<Float, Float, Float>
    suspend fun saveAccelScale(x: Float, y: Float, z: Float)
    suspend fun getAccelScale(): Triple<Float, Float, Float>
    suspend fun saveAccelPosition(position: Int, values: Triple<Float, Float, Float>)
    suspend fun getAccelPosition(position: Int): Triple<Float, Float, Float>?
    suspend fun getAllAccelPositions(): Map<Int, Triple<Float, Float, Float>>
    suspend fun saveAccelPositionsCompleted(count: Int)
    suspend fun getAccelPositionsCompleted(): Int
    
    // Magnetometer
    suspend fun saveMagOffset(x: Float, y: Float, z: Float)
    suspend fun getMagOffset(): Triple<Float, Float, Float>
    suspend fun saveMagScale(x: Float, y: Float, z: Float)
    suspend fun getMagScale(): Triple<Float, Float, Float>
    suspend fun saveMagSampleCount(count: Int)
    suspend fun getMagSampleCount(): Int
    
    // Calibration Status
    suspend fun setCalibrationStatus(status: CalibrationStatus)
    suspend fun getCalibrationStatus(): CalibrationStatus
    suspend fun setCalibrationQuality(quality: CalibrationQuality)
    suspend fun getCalibrationQuality(): CalibrationQuality
    suspend fun setCalibrationProgress(progress: Int)
    suspend fun getCalibrationProgress(): Int
    suspend fun setCurrentStep(step: Int)
    suspend fun getCurrentStep(): Int
    suspend fun setCalibrationComplete(complete: Boolean)
    suspend fun isCalibrationComplete(): Boolean
    suspend fun setCalibrationTimestamp(timestamp: Long)
    suspend fun getCalibrationTimestamp(): Long
    
    // Reset individual sensors
    suspend fun resetGyro()
    suspend fun resetAccel()
    suspend fun resetMag()
}
```

### `IGestureDataSource.kt`
```kotlin
interface IGestureDataSource {
    // Templates
    suspend fun saveGestureTemplate(template: CustomGestureTemplate)
    suspend fun getGestureTemplate(id: String): CustomGestureTemplate?
    suspend fun getAllGestureTemplates(): List<CustomGestureTemplate>
    suspend fun deleteGestureTemplate(id: String)
    suspend fun updateGestureTemplate(template: CustomGestureTemplate)
    
    // Favorites
    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteTemplates(): List<CustomGestureTemplate>
    
    // Training
    suspend fun saveTrainingSamples(gestureName: String, samples: List<FloatArray>)
    suspend fun getTrainingSamples(gestureName: String): List<FloatArray>
    suspend fun clearTrainingData(gestureName: String)
    
    // Statistics
    suspend fun updateGestureStats(stats: GestureTrainingStats)
    suspend fun getGestureStats(): GestureTrainingStats
    suspend fun incrementGestureCount(gestureName: String, confidence: Float)
    suspend fun getGestureCount(): Int
    suspend fun getTotalDetections(): Int
    suspend fun getMostUsedGestures(limit: Int): List<CustomGestureTemplate>
    
    // Configuration
    suspend fun setConfidenceThreshold(threshold: Float)
    suspend fun getConfidenceThreshold(): Float
    suspend fun setCooldownMs(cooldown: Long)
    suspend fun getCooldownMs(): Long
    
    // Reset
    suspend fun resetAllStats()
}
```

### `IProfileDataSource.kt`
```kotlin
interface IProfileDataSource {
    // CRUD
    suspend fun saveProfile(profile: UserProfile)
    suspend fun getProfile(id: String): UserProfile?
    suspend fun getAllProfiles(): List<UserProfile>
    suspend fun deleteProfile(id: String)
    suspend fun updateProfile(profile: UserProfile)
    
    // Default
    suspend fun setDefaultProfile(id: String)
    suspend fun getDefaultProfile(): UserProfile?
    
    // Favorites
    suspend fun toggleFavorite(id: String)
    suspend fun getFavoriteProfiles(): List<UserProfile>
    
    // Settings
    suspend fun saveProfileSettings(profileId: String, settings: ProfileSettings)
    suspend fun getProfileSettings(profileId: String): ProfileSettings?
    
    // Search
    suspend fun searchProfiles(query: String): List<UserProfile>
}
```

### `IStatisticsDataSource.kt`
```kotlin
interface IStatisticsDataSource {
    // Session stats
    suspend fun getStatistics(): StatisticsSummary?
    suspend fun saveStatistics(stats: StatisticsSummary)
    suspend fun getSessionStats(): StatisticsSummary
    suspend fun saveSessionStats(stats: StatisticsSummary)
    
    // Daily stats
    suspend fun saveDailyStats(date: String, stats: DailyStats)
    suspend fun getDailyStats(date: String): DailyStats
    suspend fun getDailyStatsForRange(startDate: String, endDate: String): List<DailyStats>
    
    // Historical stats
    suspend fun saveHistoricalStats(stats: HistoricalStatistics)
    suspend fun getHistoricalStats(): HistoricalStatistics
    
    // Gesture stats
    suspend fun saveGestureStats(stats: List<GestureStatistics>)
    suspend fun getGestureStats(): List<GestureStatistics>
    suspend fun incrementGestureCount(gesture: String)
    
    // Reset
    suspend fun resetSessionStats()
    suspend fun resetAllStats()
}
```

### `IPreferencesDataSource.kt`
```kotlin
interface IPreferencesDataSource {
    // Calibration
    suspend fun setCalibrated(calibrated: Boolean)
    fun isCalibrated(): Flow<Boolean>
    suspend fun isCalibratedOnce(): Boolean
    suspend fun getCalibrationTimestamp(): Long
    suspend fun setCalibrationTimestamp(timestamp: Long)
    
    // Connection
    suspend fun setLastIp(ip: String)
    suspend fun getLastIp(): String
    suspend fun setLastPort(port: Int)
    suspend fun getLastPort(): Int
    suspend fun setLastProtocol(protocol: String)
    suspend fun getLastProtocol(): String
    
    // Settings
    suspend fun setSensitivity(value: Float)
    suspend fun getSensitivity(): Float
    suspend fun setClickThreshold(value: Float)
    suspend fun getClickThreshold(): Float
    suspend fun setDoubleClickInterval(value: Long)
    suspend fun getDoubleClickInterval(): Long
    suspend fun setScrollThreshold(value: Float)
    suspend fun getScrollThreshold(): Float
    suspend fun setRightClickTilt(value: Float)
    suspend fun getRightClickTilt(): Float
    suspend fun setRightClickDuration(value: Long)
    suspend fun getRightClickDuration(): Long
    
    // Haptic & Sound
    suspend fun setHapticEnabled(enabled: Boolean)
    suspend fun isHapticEnabled(): Boolean
    suspend fun setHapticStrength(strength: String)
    suspend fun getHapticStrength(): String
    suspend fun setSoundEnabled(enabled: Boolean)
    suspend fun isSoundEnabled(): Boolean
    
    // Display
    suspend fun setTheme(theme: String)
    suspend fun getTheme(): String
    suspend fun setDynamicColors(enabled: Boolean)
    suspend fun isDynamicColorsEnabled(): Boolean
    suspend fun setFontSize(size: Float)
    suspend fun getFontSize(): Float
    
    // Statistics
    suspend fun incrementClick()
    suspend fun incrementDoubleClick()
    suspend fun incrementRightClick()
    suspend fun incrementScroll()
    suspend fun incrementGesture(gestureName: String)
    suspend fun getClickCount(): Int
    suspend fun getDoubleClickCount(): Int
    suspend fun getRightClickCount(): Int
    suspend fun getScrollCount(): Int
    suspend fun getGestureCount(gestureName: String): Int
    suspend fun getAllGestureCounts(): Map<String, Int>
    
    // Reset
    suspend fun resetAllPreferences()
    suspend fun resetStatistics()
}
```

---

## 📦 3. Implementations

### `CalibrationDataSourceImpl.kt`
Stores all calibration data in `SharedPreferences` using `PreferencesManager`.

**Key Features:**
- Stores gyroscope bias, accelerometer offset/scale, magnetometer offset/scale
- Tracks calibration status, quality, progress, timestamp
- Supports resetting individual sensors or all calibration data
- Uses `PreferencesKeys` for consistent key naming

```kotlin
@Singleton
class CalibrationDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : ICalibrationDataSource {
    
    override suspend fun saveGyroBias(x: Float, y: Float, z: Float) {
        withContext(Dispatchers.IO) {
            prefs.putFloat(PreferencesKeys.KEY_GYRO_BIAS_X, x)
            prefs.putFloat(PreferencesKeys.KEY_GYRO_BIAS_Y, y)
            prefs.putFloat(PreferencesKeys.KEY_GYRO_BIAS_Z, z)
        }
    }
    
    // ... all other methods
}
```

### `GestureDataSourceImpl.kt`
Stores gesture templates, training data, and statistics using `PreferencesManager` with JSON serialization via Gson.

**Key Features:**
- Gesture templates stored as JSON arrays in preferences
- Training samples stored as JSON lists of float arrays
- Gesture statistics (counts, confidence) tracked
- Favorites stored as comma-separated IDs
- Confidence threshold and cooldown settings

```kotlin
@Singleton
class GestureDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IGestureDataSource {
    
    private val gson = Gson()
    private val templatesKey = "gesture_templates"
    
    override suspend fun saveGestureTemplate(template: CustomGestureTemplate) {
        val templates = getAllGestureTemplates().toMutableList()
        val existingIndex = templates.indexOfFirst { it.id == template.id }
        if (existingIndex >= 0) {
            templates[existingIndex] = template
        } else {
            templates.add(template)
        }
        val json = gson.toJson(templates)
        prefs.putString(templatesKey, json)
    }
    
    // ... all other methods
}
```

### `ProfileDataSourceImpl.kt`
Stores user profiles as JSON in `PreferencesManager`.

**Key Features:**
- Multiple profiles stored as JSON array
- Default profile ID stored separately
- Favorite profiles stored as comma-separated IDs
- Profile settings (sensitivity, thresholds, theme, etc.)

```kotlin
@Singleton
class ProfileDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IProfileDataSource {
    
    private val profilesKey = "user_profiles"
    private val defaultProfileKey = "default_profile"
    private val favoriteProfilesKey = "favorite_profiles"
    
    override suspend fun saveProfile(profile: UserProfile) {
        val profiles = getAllProfiles().toMutableList()
        val existingIndex = profiles.indexOfFirst { it.id == profile.id }
        if (existingIndex >= 0) {
            profiles[existingIndex] = profile
        } else {
            profiles.add(profile)
        }
        val json = JSONArray().apply {
            profiles.forEach { put(it.toJson()) }
        }.toString()
        prefs.putString(profilesKey, json)
    }
    
    // ... all other methods
}
```

### `StatisticsDataSourceImpl.kt`
Stores statistics data using `PreferencesManager` with JSON serialization.

**Key Features:**
- Session statistics stored as JSON
- Daily statistics stored with date-based keys
- Historical statistics aggregated
- Gesture statistics per gesture type

```kotlin
@Singleton
class StatisticsDataSourceImpl @Inject constructor(
    private val prefs: PreferencesManager
) : IStatisticsDataSource {
    
    private val sessionKey = "session_stats"
    private val historicalKey = "historical_stats"
    private val gestureStatsKey = "gesture_stats"
    private val dailyPrefix = "daily_"
    
    override suspend fun saveSessionStats(stats: StatisticsSummary) {
        val obj = JSONObject().apply {
            put("totalClicks", stats.totalClicks)
            put("totalDoubleClicks", stats.totalDoubleClicks)
            // ... etc
        }
        prefs.putString(sessionKey, obj.toString())
    }
    
    // ... all other methods
}
```

### `PreferencesDataSourceImpl.kt`
Generic preference operations using `SharedPreferences`.

**Key Features:**
- All CRUD operations for preferences
- `StateFlow` for reactive preference observation
- Type-safe getters/setters for all preference types
- Statistics increments with persistence

```kotlin
@Singleton
class PreferencesDataSourceImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : IPreferencesDataSource {
    
    private val prefs = context.getSharedPreferences("airmouse_prefs", Context.MODE_PRIVATE)
    private val _isCalibrated = MutableStateFlow(prefs.getBoolean("is_calibrated", false))
    
    override fun isCalibrated(): Flow<Boolean> = _isCalibrated.asStateFlow()
    
    override suspend fun setCalibrated(calibrated: Boolean) {
        prefs.edit().putBoolean("is_calibrated", calibrated).apply()
        _isCalibrated.update { calibrated }
    }
    
    // ... all other methods
}
```

### `LocalDataSourceImpl.kt`
Unified local data source that combines Room DAOs and Preferences.

**Key Features:**
- Single interface for all local data operations
- Delegates to appropriate DAOs or preferences
- Handles mapping between domain and entity models

```kotlin
@Singleton
class LocalDataSourceImpl @Inject constructor(
    private val calibrationDao: CalibrationDao,
    private val gestureDao: GestureDao,
    private val trainingSampleDao: TrainingSampleDao,
    private val profileDao: ProfileDao,
    private val statisticsDao: StatisticsDao,
    private val dailyStatsDao: DailyStatsDao,
    private val gestureStatsDao: GestureStatsDao
) : ILocalDataSource {
    
    override suspend fun saveCalibrationData(data: CalibrationData) {
        val entity = DomainToEntityMapper.mapToEntity(data)
        calibrationDao.insertCalibration(entity)
    }
    
    override suspend fun getCalibrationData(): CalibrationData {
        val entity = calibrationDao.getCalibration()
        return entity?.let { EntityToDomainMapper.mapToDomain(it) } ?: CalibrationData()
    }
    
    // ... all other methods
}
```

---

## 🔄 4. Mappers

### `DomainToEntityMapper.kt`
Maps domain models to Room entities.

```kotlin
object DomainToEntityMapper {
    
    fun mapToEntity(data: CalibrationData): CalibrationEntity {
        return CalibrationEntity(
            gyroBiasX = data.gyroBias.offsetX,
            gyroBiasY = data.gyroBias.offsetY,
            gyroBiasZ = data.gyroBias.offsetZ,
            accelOffsetX = data.accelOffset.offsetX,
            accelOffsetY = data.accelOffset.offsetY,
            accelOffsetZ = data.accelOffset.offsetZ,
            accelScaleX = data.accelOffset.scaleX,
            accelScaleY = data.accelOffset.scaleY,
            accelScaleZ = data.accelOffset.scaleZ,
            magOffsetX = data.magOffset.offsetX,
            magOffsetY = data.magOffset.offsetY,
            magOffsetZ = data.magOffset.offsetZ,
            magScaleX = data.magOffset.scaleX,
            magScaleY = data.magOffset.scaleY,
            magScaleZ = data.magOffset.scaleZ,
            isCalibrated = data.isCalibrated,
            calibrationQuality = data.quality.name,
            timestamp = data.timestamp
        )
    }
    
    fun mapToEntity(profile: UserProfile): ProfileEntity {
        return ProfileEntity(
            id = profile.id,
            name = profile.name,
            email = profile.email,
            avatarUri = profile.avatarUri,
            sensitivity = profile.settings.sensitivity,
            clickThreshold = profile.settings.clickThreshold,
            doubleClickInterval = profile.settings.doubleClickInterval,
            scrollThreshold = profile.settings.scrollThreshold,
            rightClickTilt = profile.settings.rightClickTilt,
            hapticEnabled = profile.settings.hapticEnabled,
            theme = profile.settings.theme,
            aiSmoothing = profile.settings.aiSmoothing,
            predictiveMovement = profile.settings.predictiveMovement,
            invertX = profile.settings.invertX,
            invertY = profile.settings.invertY,
            accelerationEnabled = profile.settings.accelerationEnabled,
            smoothingEnabled = profile.settings.smoothingEnabled,
            edgeGesturesEnabled = profile.settings.edgeGesturesEnabled,
            voiceCommandsEnabled = profile.settings.voiceCommandsEnabled,
            isDefault = profile.isDefault,
            isFavorite = profile.isFavorite,
            tags = profile.tags.joinToString(","),
            iconRes = profile.iconRes,
            createdAt = profile.createdAt,
            lastUsed = profile.updatedAt
        )
    }
    
    // ... all other mappers
}
```

### `EntityToDomainMapper.kt`
Maps Room entities back to domain models.

```kotlin
object EntityToDomainMapper {
    
    fun mapToDomain(entity: CalibrationEntity): CalibrationData {
        return CalibrationData(
            gyroBias = SensorCalibrationData(
                offsetX = entity.gyroBiasX,
                offsetY = entity.gyroBiasY,
                offsetZ = entity.gyroBiasZ
            ),
            accelOffset = SensorCalibrationData(
                offsetX = entity.accelOffsetX,
                offsetY = entity.accelOffsetY,
                offsetZ = entity.accelOffsetZ,
                scaleX = entity.accelScaleX,
                scaleY = entity.accelScaleY,
                scaleZ = entity.accelScaleZ
            ),
            magOffset = SensorCalibrationData(
                offsetX = entity.magOffsetX,
                offsetY = entity.magOffsetY,
                offsetZ = entity.magOffsetZ,
                scaleX = entity.magScaleX,
                scaleY = entity.magScaleY,
                scaleZ = entity.magScaleZ
            ),
            isCalibrated = entity.isCalibrated,
            quality = try { CalibrationQuality.valueOf(entity.calibrationQuality) }
                catch (e: Exception) { CalibrationQuality.UNKNOWN },
            timestamp = entity.timestamp
        )
    }
    
    fun mapToDomain(entity: ProfileEntity): UserProfile {
        return UserProfile(
            id = entity.id,
            name = entity.name,
            email = entity.email,
            avatarUri = entity.avatarUri,
            settings = ProfileSettings(
                sensitivity = entity.sensitivity,
                clickThreshold = entity.clickThreshold,
                doubleClickInterval = entity.doubleClickInterval,
                scrollThreshold = entity.scrollThreshold,
                rightClickTilt = entity.rightClickTilt,
                hapticEnabled = entity.hapticEnabled,
                theme = entity.theme,
                aiSmoothing = entity.aiSmoothing,
                predictiveMovement = entity.predictiveMovement,
                invertX = entity.invertX,
                invertY = entity.invertY,
                accelerationEnabled = entity.accelerationEnabled,
                smoothingEnabled = entity.smoothingEnabled,
                edgeGesturesEnabled = entity.edgeGesturesEnabled,
                voiceCommandsEnabled = entity.voiceCommandsEnabled
            ),
            isDefault = entity.isDefault,
            isFavorite = entity.isFavorite,
            tags = entity.tags?.split(",")?.filter { it.isNotEmpty() } ?: emptyList(),
            iconRes = entity.iconRes,
            createdAt = entity.createdAt,
            updatedAt = entity.lastUsed
        )
    }
    
    // ... all other mappers
}
```

---

## 🔧 5. Type Converters

### `Converters.kt`
Room type converters for complex data types.

```kotlin
class Converters {
    
    private val gson = Gson()
    
    @TypeConverter
    fun fromStringList(value: String?): List<String>? {
        if (value == null) return null
        val listType = object : TypeToken<List<String>>() {}.type
        return gson.fromJson(value, listType)
    }
    
    @TypeConverter
    fun fromStringListToString(list: List<String>?): String? {
        if (list == null) return null
        return gson.toJson(list)
    }
    
    @TypeConverter
    fun fromFloatArray(value: String?): FloatArray? {
        if (value == null) return null
        val listType = object : TypeToken<List<Float>>() {}.type
        val list: List<Float> = gson.fromJson(value, listType)
        return list.toFloatArray()
    }
    
    @TypeConverter
    fun fromFloatArrayToString(array: FloatArray?): String? {
        if (array == null) return null
        return gson.toJson(array.toList())
    }
    
    @TypeConverter
    fun fromFloatArrayList(value: String?): List<FloatArray>? {
        if (value == null) return null
        val listType = object : TypeToken<List<List<Float>>>() {}.type
        val list: List<List<Float>> = gson.fromJson(value, listType)
        return list.map { it.toFloatArray() }
    }
    
    @TypeConverter
    fun fromFloatArrayListToString(list: List<FloatArray>?): String? {
        if (list == null) return null
        val outerList = list.map { it.toList() }
        return gson.toJson(outerList)
    }
    
    @TypeConverter
    fun fromGestureType(value: String?): GestureType? {
        if (value == null) return null
        return try { GestureType.valueOf(value) } catch (e: IllegalArgumentException) { GestureType.NONE }
    }
    
    @TypeConverter
    fun fromGestureTypeToString(type: GestureType?): String? {
        return type?.name
    }
    
    // ... all other converters
}
```

---

## 📊 6. Helper Models

### `CalibrationPrefsData.kt`
```kotlin
data class CalibrationPrefsData(
    val gyroBiasX: Float = 0f,
    val gyroBiasY: Float = 0f,
    val gyroBiasZ: Float = 0f,
    val accelOffsetX: Float = 0f,
    val accelOffsetY: Float = 0f,
    val accelOffsetZ: Float = 0f,
    val magOffsetX: Float = 0f,
    val magOffsetY: Float = 0f,
    val magOffsetZ: Float = 0f,
    val isCalibrated: Boolean = false,
    val quality: String = "UNKNOWN",
    val timestamp: Long = System.currentTimeMillis()
)
```

### `GestureData.kt`
```kotlin
data class GestureData(
    val samples: List<FloatArray>,
    val labels: List<String>,
    val timestamp: Long = System.currentTimeMillis()
)
```

### `SensorData.kt`
```kotlin
data class SensorData(
    val gyroX: Float = 0f,
    val gyroY: Float = 0f,
    val gyroZ: Float = 0f,
    val accelX: Float = 0f,
    val accelY: Float = 0f,
    val accelZ: Float = 0f,
    val magX: Float = 0f,
    val magY: Float = 0f,
    val magZ: Float = 0f,
    val timestamp: Long = System.currentTimeMillis()
)
```

### `Quadruple.kt`
```kotlin
data class Quadruple<A, B, C, D>(
    val first: A,
    val second: B,
    val third: C,
    val fourth: D
)
```

### `GestureTypeCount.kt`
```kotlin
data class GestureTypeCount(
    val type: String,
    val count: Int
)
```

---

## 🔗 Data Flow Architecture

### Complete Data Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                          UI LAYER                                      │
│                    (Compose Screens / ViewModels)                     │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         DOMAIN LAYER                                   │
│                    (Use Cases / Repository Interfaces)                 │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                    DATA SOURCE INTERFACES                              │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────────────┐ │
│  │ICalibration │ │IGesture     │ │IProfile     │ │IStatistics       │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └──────────────────┘ │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                   DATA SOURCE IMPLEMENTATIONS                          │
│  ┌──────────────────────────────────────────────────────────────────┐ │
│  │                    LocalDataSourceImpl                           │ │
│  │  (Unified wrapper over Room DAOs + Preferences)                 │ │
│  └──────────────────────────────────────────────────────────────────┘ │
│  ┌─────────────┐ ┌─────────────┐ ┌─────────────┐ ┌──────────────────┐ │
│  │Calibration  │ │Gesture      │ │Profile      │ │Statistics        │ │
│  │DataSource   │ │DataSource   │ │DataSource   │ │DataSource        │ │
│  │Impl         │ │Impl         │ │Impl         │ │Impl              │ │
│  └─────────────┘ └─────────────┘ └─────────────┘ └──────────────────┘ │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                      STORAGE LAYER                                     │
│  ┌───────────────────────────┐     ┌─────────────────────────────────┐ │
│  │       Room Database       │     │     PreferencesManager         │ │
│  │  ┌─────────────────────┐  │     │     (SharedPreferences)        │ │
│  │  │ 8 Entities / 8 DAOs │  │     └─────────────────────────────────┘ │
│  │  └─────────────────────┘  │                                        │
│  └───────────────────────────┘                                        │
└─────────────────────────────────────────────────────────────────────────┘
```

### Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────┐
│                     CALIBRATION DATA FLOW                           │
├─────────────────────────────────────────────────────────────────────┤
│ CalibrationViewModel → CalibrationUseCase → ICalibrationRepository │
│                              ↓                                     │
│                   CalibrationRepositoryImpl                        │
│                              ↓                                     │
│                   ICalibrationDataSource                           │
│                              ↓                                     │
│                   CalibrationDataSourceImpl                        │
│                              ↓                                     │
│                   PreferencesManager (SharedPreferences)          │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      GESTURE DATA FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│ GestureViewModel → ManageGestureTemplatesUseCase → IGestureRepo   │
│                              ↓                                     │
│                   GestureRepositoryImpl                            │
│                              ↓                                     │
│                   IGestureDataSource                               │
│                              ↓                                     │
│                   GestureDataSourceImpl                            │
│                              ↓                                     │
│                   PreferencesManager (JSON serialisation)         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      PROFILE DATA FLOW                              │
├─────────────────────────────────────────────────────────────────────┤
│ ProfilesViewModel → ManageProfileUseCase → IProfileRepository     │
│                              ↓                                     │
│                   ProfileRepositoryImpl                            │
│                              ↓                                     │
│                   IProfileDataSource                               │
│                              ↓                                     │
│                   ProfileDataSourceImpl                            │
│                              ↓                                     │
│                   PreferencesManager (JSON serialisation)         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                    STATISTICS DATA FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│ StatisticsViewModel → GetStatisticsUseCase → IStatisticsRepository│
│                              ↓                                     │
│                   StatisticsRepositoryImpl                         │
│                              ↓                                     │
│                   IStatisticsDataSource                            │
│                              ↓                                     │
│                   StatisticsDataSourceImpl                         │
│                              ↓                                     │
│                   PreferencesManager (JSON serialisation)         │
└─────────────────────────────────────────────────────────────────────┘

┌─────────────────────────────────────────────────────────────────────┐
│                      ROOM DATABASE FLOW                             │
├─────────────────────────────────────────────────────────────────────┤
│ ViewModel → UseCase → Repository → ILocalDataSource               │
│                              ↓                                     │
│                   LocalDataSourceImpl                              │
│                              ↓                                     │
│                   Room Database (AppDatabase)                      │
│                              ↓                                     │
│                   Entity ↔ Domain Mappers                         │
└─────────────────────────────────────────────────────────────────────┘
```

---

## ✅ Summary of Data Sources

| Data Source | Storage Type | Primary Use |
|-------------|--------------|-------------|
| **CalibrationDataSourceImpl** | Preferences (SharedPreferences) | Sensor calibration data |
| **GestureDataSourceImpl** | Preferences (JSON arrays) | Gesture templates, training, stats |
| **ProfileDataSourceImpl** | Preferences (JSON arrays) | User profiles, settings |
| **StatisticsDataSourceImpl** | Preferences (JSON objects) | Session, daily, historical stats |
| **PreferencesDataSourceImpl** | Preferences (SharedPreferences) | Generic preferences |
| **LocalDataSourceImpl** | Room + Preferences | Unified local data access |
| **Room Database** | SQLite (8 tables) | Structured data, relationships |
| **DomainToEntityMapper** | In‑memory | Domain → Entity conversion |
| **EntityToDomainMapper** | In‑memory | Entity → Domain conversion |

---

## 🎯 Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Single Responsibility** | Each data source handles one data domain |
| **Interface Segregation** | Separate interfaces for each data type |
| **Dependency Inversion** | Repositories depend on abstractions |
| **Reactive Programming** | `Flow` and `StateFlow` for reactive data |
| **Separation of Concerns** | Mappers separate domain from persistence |
| **Testability** | All interfaces can be mocked |
| **Performance** | Room with `suspend` functions, coroutines |

---

**These local data sources provide a complete, robust, and maintainable persistence layer for the Air Mouse application, ensuring all user data is safely stored and easily retrievable.**