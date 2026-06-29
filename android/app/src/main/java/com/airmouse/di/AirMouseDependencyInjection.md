# 📘 Air Mouse Dependency Injection (DI) – Complete Documentation

## 📁 Package Overview

The `com.airmouse.di` package contains all **Dagger Hilt modules** that provide dependencies for the entire application. These modules follow a **modular, layered architecture** that mirrors the Clean Architecture layers (Data, Domain, Presentation, Infrastructure).

```
com.airmouse.di/
├── AppModule.kt                 # Core dependencies (Context, Preferences, etc.)
├── NetworkModule.kt             # Network dependencies (OkHttp, ConnectionManager)
├── DatabaseModule.kt            # Room database & DAOs
├── SensorModule.kt              # Sensor dependencies (SensorManager, CalibrationHelper)
├── ServiceModule.kt             # Service dependencies (SensorService, PresentationMode)
├── RepositoryModule.kt          # Repository bindings (Data Layer)
├── UseCaseModule.kt             # Use case bindings (Domain Layer)
├── FeatureModule.kt             # Feature dependencies (Domain Layer)
├── ViewModelModule.kt           # ViewModel bindings (Presentation Layer)
├── CalibrationModule.kt         # Calibration-specific bindings
├── CoroutineModule.kt           # Coroutine dispatchers
└── GestureRepositoryModule.kt   # Gesture repository bindings
```

---

## 🏗️ DI Architecture Overview

### Module Dependency Graph

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                             │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    ViewModelModule.kt                          │   │
│  │  (Binds ViewModels for Hilt injection)                        │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                           DOMAIN LAYER                                 │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    UseCaseModule.kt                            │   │
│  │  (Provides Use Cases)                                         │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    FeatureModule.kt                            │   │
│  │  (Provides Feature classes)                                   │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                            DATA LAYER                                  │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    RepositoryModule.kt                         │   │
│  │  (Binds Repository implementations to interfaces)             │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    CalibrationModule.kt                        │   │
│  │  (Calibration-specific bindings)                              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    GestureRepositoryModule.kt                  │   │
│  │  (Gesture repository bindings)                                │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└────────────────────────────────┬────────────────────────────────────┘
                                 │
                                 ▼
┌─────────────────────────────────────────────────────────────────────────┐
│                         INFRASTRUCTURE LAYER                          │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    AppModule.kt                                │   │
│  │  (Context, Preferences, BluetoothAdapter, UsbManager, etc.)  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    NetworkModule.kt                           │   │
│  │  (OkHttpClient, ConnectionManager, UdpDiscovery)              │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    DatabaseModule.kt                          │   │
│  │  (AppDatabase, all DAOs)                                      │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    SensorModule.kt                            │   │
│  │  (SensorManager, CalibrationHelper, EnhancedGestureDetector)  │   │
│  └─────────────────────────────────────────────────────────────────┘   │
│  ┌─────────────────────────────────────────────────────────────────┐   │
│  │                    ServiceModule.kt                           │   │
│  │  (SensorService, PresentationModeService)                     │   │
│  └─────────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 📦 1. AppModule.kt

### Purpose
Provides **core application dependencies** that are used across all layers.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `PreferencesManager` | SharedPreferences wrapper for all app settings |
| `PreferencesHelper` | Static preference helper |
| `PermissionHelper` | Permission checking and requesting |
| `VibrateUtils` | Haptic feedback utilities |
| `Vibrator` | Android system vibrator service |
| `AudioUtils` | Sound playback utilities |
| `BluetoothUtils` | Bluetooth operations |
| `CalibrationHelper` | Sensor calibration logic |
| `GestureDetector` | Basic gesture detection |
| `EnhancedGestureDetector` | Advanced gesture detection |
| `PresentationModeService` | Presentation control service |
| `BatterySaver` | Battery optimization |
| `BluetoothAdapter` | Android Bluetooth adapter |
| `WebSocketManager` | WebSocket communication (legacy) |
| `UsbManager` | Android USB manager |
| `ApplicationContext` | Application context |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object AppModule {

    @Provides
    @Singleton
    fun providePreferencesManager(@ApplicationContext context: Context): PreferencesManager {
        return PreferencesManager(context)
    }

    @Provides
    @Singleton
    fun providePreferencesHelper(@ApplicationContext context: Context): PreferencesHelper {
        PreferencesHelper.init(context)
        return PreferencesHelper
    }

    @Provides
    @Singleton
    fun providePermissionHelper(): PermissionHelper {
        return PermissionHelper
    }

    @Provides
    @Singleton
    fun provideVibrateUtils(@ApplicationContext context: Context): VibrateUtils {
        return VibrateUtils(context)
    }

    @Provides
    @Singleton
    fun provideVibrator(@ApplicationContext context: Context): Vibrator {
        return context.getSystemService(Vibrator::class.java)
    }

    @Provides
    @Singleton
    fun provideAudioUtils(@ApplicationContext context: Context): AudioUtils {
        return AudioUtils(context)
    }

    @Provides
    @Singleton
    fun provideBluetoothUtils(@ApplicationContext context: Context): BluetoothUtils {
        return BluetoothUtils(context)
    }

    @Provides
    @Singleton
    fun provideCalibrationHelper(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): CalibrationHelper {
        return CalibrationHelper(context, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideGestureDetector(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager
    ): GestureDetector {
        return GestureDetector(context, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideEnhancedGestureDetector(
        @ApplicationContext context: Context,
        preferencesManager: PreferencesManager,
        vibrator: Vibrator
    ): EnhancedGestureDetector {
        return EnhancedGestureDetector(context, preferencesManager, vibrator)
    }

    @Provides
    @Singleton
    fun providePresentationModeService(
        @ApplicationContext context: Context,
        connectionManager: ConnectionManager,
        preferencesManager: PreferencesManager
    ): PresentationModeService {
        return PresentationModeService(context, connectionManager, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideBatterySaver(): BatterySaver {
        return BatterySaver()
    }

    @Provides
    @Singleton
    fun provideBluetoothAdapter(@ApplicationContext context: Context): BluetoothAdapter {
        val bluetoothManager = context.getSystemService(BluetoothManager::class.java)
        return bluetoothManager.adapter
    }

    @Provides
    @Singleton
    fun provideWebSocketManager(): WebSocketManager {
        return WebSocketManager
    }

    @Provides
    @Singleton
    fun provideUsbManager(@ApplicationContext context: Context): UsbManager {
        return context.getSystemService(UsbManager::class.java)
    }

    @Provides
    @Singleton
    fun provideApplicationContext(@ApplicationContext context: Context): Context {
        return context
    }
}
```

---

## 🌐 2. NetworkModule.kt

### Purpose
Provides **network communication dependencies** for connecting to the PC server.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `OkHttpClient` | HTTP/WebSocket client with timeouts and interceptors |
| `ConnectionManager` | Core network manager (WebSocket/TCP/UDP) |
| `UdpDiscovery` | UDP server discovery |
| `NetworkStateHelper` | Network connectivity helper |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object NetworkModule {

    @Provides
    @Singleton
    fun provideOkHttpClient(): OkHttpClient {
        val logging = HttpLoggingInterceptor().apply {
            level = HttpLoggingInterceptor.Level.BODY
        }
        return OkHttpClient.Builder()
            .connectTimeout(30, TimeUnit.SECONDS)
            .writeTimeout(30, TimeUnit.SECONDS)
            .readTimeout(30, TimeUnit.SECONDS)
            .pingInterval(30, TimeUnit.SECONDS)
            .retryOnConnectionFailure(true)
            .addInterceptor(logging)
            .build()
    }

    @Provides
    @Singleton
    fun provideConnectionManager(
        @ApplicationContext context: Context,
        prefs: PreferencesManager
    ): ConnectionManager {
        return ConnectionManager(context, prefs)
    }

    @Provides
    @Singleton
    fun provideUdpDiscovery(): UdpDiscovery {
        return UdpDiscovery()
    }

    @Provides
    @Singleton
    fun provideNetworkStateHelper(
        @ApplicationContext context: Context
    ): NetworkStateHelper {
        return NetworkStateHelper(context)
    }
}
```

---

## 🗄️ 3. DatabaseModule.kt

### Purpose
Provides **Room database and Data Access Objects (DAOs)** for local persistence.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `AppDatabase` | Room database instance |
| `CalibrationDao` | Calibration data access |
| `SettingsDao` | Settings data access |
| `StatisticsDao` | Statistics data access |
| `GestureDao` | Gesture data access |
| `ProfileDao` | Profile data access |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideAppDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "airmouse_database"
        )
            .fallbackToDestructiveMigration()
            .enableMultiInstanceInvalidation()
            .build()
    }

    @Provides
    @Singleton
    fun provideCalibrationDao(database: AppDatabase): CalibrationDao {
        return database.calibrationDao()
    }

    @Provides
    @Singleton
    fun provideSettingsDao(database: AppDatabase): SettingsDao {
        return database.settingsDao()
    }

    @Provides
    @Singleton
    fun provideStatisticsDao(database: AppDatabase): StatisticsDao {
        return database.statisticsDao()
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
```

---

## 📡 4. SensorModule.kt

### Purpose
Provides **sensor-related dependencies** for motion tracking and gesture detection.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `SensorManager` | Android sensor framework |
| `CalibrationHelper` | Sensor calibration |
| `EnhancedGestureDetector` | Advanced gesture detection |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object SensorModule {

    @Provides
    @Singleton
    fun provideSensorManager(
        @ApplicationContext context: Context
    ): SensorManager {
        return context.getSystemService(Context.SENSOR_SERVICE) as SensorManager
    }
}
```

---

## 🔧 5. ServiceModule.kt

### Purpose
Provides **background services** for sensor processing, presentation mode, and other long-running tasks.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `SensorService` | Sensor data collection and processing |
| `PresentationModeService` | Presentation control |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object ServiceModule {

    @Provides
    @Singleton
    fun provideSensorService(
        @ApplicationContext context: Context,
        calibrationHelper: CalibrationHelper,
        gestureDetector: EnhancedGestureDetector,
        preferencesManager: PreferencesManager
    ): SensorService {
        return SensorService(context, calibrationHelper, gestureDetector, preferencesManager)
    }
}
```

---

## 📂 6. RepositoryModule.kt

### Purpose
**Binds repository interfaces to their implementations** for dependency injection.

### Key Bindings

| Interface | Implementation |
|-----------|----------------|
| `ICalibrationRepository` | `CalibrationRepositoryImpl` |
| `IConnectionRepository` | `ConnectionRepositoryImpl` |
| `IMouseRepository` | `MouseRepositoryImpl` |
| `ISettingsRepository` | `SettingsRepositoryImpl` |
| `ISensorRepository` | `SensorRepositoryImpl` |
| `IProximityRepository` | `ProximityRepositoryImpl` |
| `IVoiceCommandRepository` | `VoiceCommandRepositoryImpl` |
| `IProfileRepository` | `ProfileRepositoryImpl` |
| `IStatisticsRepository` | `StatisticsRepositoryImpl` |
| `IUpdateRepository` | `UpdateRepositoryImpl` |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindCalibrationRepository(
        impl: CalibrationRepositoryImpl
    ): ICalibrationRepository

    @Binds
    @Singleton
    abstract fun bindConnectionRepository(
        impl: ConnectionRepositoryImpl
    ): IConnectionRepository

    @Binds
    @Singleton
    abstract fun bindMouseRepository(
        impl: MouseRepositoryImpl
    ): IMouseRepository

    @Binds
    @Singleton
    abstract fun bindSettingsRepository(
        impl: SettingsRepositoryImpl
    ): ISettingsRepository

    @Binds
    @Singleton
    abstract fun bindSensorRepository(
        impl: SensorRepositoryImpl
    ): ISensorRepository

    @Binds
    @Singleton
    abstract fun bindProximityRepository(
        impl: ProximityRepositoryImpl
    ): IProximityRepository

    @Binds
    @Singleton
    abstract fun bindVoiceCommandRepository(
        impl: VoiceCommandRepositoryImpl
    ): IVoiceCommandRepository

    @Binds
    @Singleton
    abstract fun bindProfileRepository(
        impl: ProfileRepositoryImpl
    ): IProfileRepository

    @Binds
    @Singleton
    abstract fun bindStatisticsRepository(
        impl: StatisticsRepositoryImpl
    ): IStatisticsRepository

    @Binds
    @Singleton
    abstract fun bindUpdateRepository(
        impl: UpdateRepositoryImpl
    ): IUpdateRepository
}

@Module
@InstallIn(SingletonComponent::class)
object RepositoryProvidersModule {

    @Provides
    @Singleton
    fun provideConnectionRepositoryImpl(
        connectionManager: ConnectionManager,
        udpDiscovery: UdpDiscovery,
        preferencesManager: PreferencesManager
    ): ConnectionRepositoryImpl {
        return ConnectionRepositoryImpl(connectionManager, udpDiscovery, preferencesManager)
    }

    @Provides
    @Singleton
    fun provideMouseRepositoryImpl(
        connectionManager: ConnectionManager,
        preferencesManager: PreferencesManager
    ): MouseRepositoryImpl {
        return MouseRepositoryImpl(connectionManager, preferencesManager)
    }

    // ... all other repository implementations
}
```

---

## ⚙️ 7. UseCaseModule.kt

### Purpose
**Provides use cases** for the domain layer. Each use case encapsulates a single business rule.

### Key Use Cases

| Use Case | Purpose |
|----------|---------|
| `ConnectToServerUseCase` | Connect to PC server |
| `SendMovementUseCase` | Send cursor movement |
| `CalibrationUseCase` | Sensor calibration orchestration |
| `DetectGestureUseCase` | Gesture detection |
| `DiscoverServersUseCase` | UDP server discovery |
| `GetConnectionStatusUseCase` | Get connection status |
| `TestConnectionUseCase` | Test server connection |
| `GetStatisticsUseCase` | Get usage statistics |
| `RecordStatisticsUseCase` | Record usage statistics |
| `ManageProfileUseCase` | Profile management |
| `ManageGestureTemplatesUseCase` | Gesture template management |
| `HandleVoiceCommandUseCase` | Voice command processing |
| `GetProximityStateUseCase` | Get proximity state |
| `UpdateProximityConfigUseCase` | Update proximity configuration |
| `CheckForUpdatesUseCase` | Check for app updates |
| `GetGestureStatisticsUseCase` | Get gesture statistics |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object UseCaseModule {

    @Provides
    @Singleton
    fun provideConnectToServerUseCase(
        connectionRepo: IConnectionRepository
    ): ConnectToServerUseCase {
        return ConnectToServerUseCase(connectionRepo)
    }

    @Provides
    @Singleton
    fun provideSendMovementUseCase(
        mouseRepository: IMouseRepository
    ): SendMovementUseCase {
        return SendMovementUseCase(mouseRepository)
    }

    @Provides
    @Singleton
    fun provideCalibrationUseCase(
        calibrationRepo: ICalibrationRepository
    ): CalibrationUseCase {
        return CalibrationUseCase(calibrationRepo)
    }

    // ... all other use cases
}
```

---

## 🎯 8. FeatureModule.kt

### Purpose
**Provides feature orchestrators** that combine multiple use cases into cohesive features.

### Key Features

| Feature | Purpose |
|---------|---------|
| `ConnectionFeature` | Connection management (connect, disconnect, discover) |
| `MouseControlFeature` | Mouse control (movement, clicks, scrolls) |
| `CalibrationFeature` | Sensor calibration |
| `GestureRecognitionFeature` | Gesture detection and training |
| `ProximityFeature` | Proximity detection |
| `StatisticsFeature` | Usage statistics |
| `VoiceFeature` | Voice commands |
| `ProfileFeature` | User profiles |
| `UpdateFeature` | App updates |
| `SensorFeature` | Sensor data |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object FeatureModule {

    @Provides
    @Singleton
    fun provideConnectionFeature(
        connectToServerUseCase: ConnectToServerUseCase,
        discoverServersUseCase: DiscoverServersUseCase,
        getConnectionStatusUseCase: GetConnectionStatusUseCase,
        testConnectionUseCase: TestConnectionUseCase
    ): ConnectionFeature {
        return ConnectionFeature(
            connectToServerUseCase,
            discoverServersUseCase,
            getConnectionStatusUseCase,
            testConnectionUseCase
        )
    }

    @Provides
    @Singleton
    fun provideMouseControlFeature(
        sendMovementUseCase: SendMovementUseCase,
        mouseRepo: IMouseRepository
    ): MouseControlFeature {
        return MouseControlFeature(sendMovementUseCase, mouseRepo)
    }

    // ... all other features
}
```

---

## 🖥️ 9. ViewModelModule.kt

### Purpose
**Binds ViewModels** for Hilt injection into Activities and Fragments.

### Key ViewModels

| ViewModel | Purpose |
|-----------|---------|
| `HomeViewModel` | Home screen |
| `SettingsViewModel` | Settings screen |
| `CalibrationViewModel` | Calibration screen |
| `GestureStudioViewModel` | Gesture studio |
| `StatisticsViewModel` | Statistics screen |
| `HelpViewModel` | Help screen |
| `AboutViewModel` | About screen |
| `ProfilesViewModel` | Profiles screen |
| `ThemesViewModel` | Themes screen |
| `VoiceCommandsViewModel` | Voice commands screen |
| `EdgeGesturesViewModel` | Edge gestures screen |
| `ProximityViewModel` | Proximity screen |
| `AccessibilityViewModel` | Accessibility screen |
| `BatteryViewModel` | Battery screen |
| `NetworkDiscoveryViewModel` | Network discovery |
| `ServerLogsViewModel` | Server logs |
| `OnboardingViewModel` | Onboarding |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class ViewModelModule {

    @Binds
    @IntoMap
    @ViewModelKey(HomeViewModel::class)
    abstract fun bindHomeViewModel(viewModel: HomeViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(SettingsViewModel::class)
    abstract fun bindSettingsViewModel(viewModel: SettingsViewModel): ViewModel

    @Binds
    @IntoMap
    @ViewModelKey(CalibrationViewModel::class)
    abstract fun bindCalibrationViewModel(viewModel: CalibrationViewModel): ViewModel

    // ... all other ViewModels
}
```

---

## 🛠️ 10. CalibrationModule.kt

### Purpose
Provides **calibration-specific dependencies** for sensor calibration.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `ICalibrationDataSource` | Calibration data source |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
object CalibrationModule {

    @Provides
    @Singleton
    fun provideCalibrationDataSource(
        prefs: PreferencesManager
    ): ICalibrationDataSource {
        return CalibrationDataSourceImpl(prefs)
    }
}
```

---

## 🔄 11. CoroutineModule.kt

### Purpose
Provides **coroutine dispatchers** for structured concurrency.

### Key Dispatchers

| Dispatcher | Purpose |
|------------|---------|
| `@DefaultDispatcher` | CPU-intensive work (e.g., sorting, parsing) |
| `@IoDispatcher` | I/O operations (e.g., network, database) |
| `@MainDispatcher` | UI updates |

### Module Structure

```kotlin
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class DefaultDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class IoDispatcher

@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class MainDispatcher

@Module
@InstallIn(SingletonComponent::class)
object CoroutineModule {

    @DefaultDispatcher
    @Provides
    fun provideDefaultDispatcher(): CoroutineDispatcher = Dispatchers.Default

    @IoDispatcher
    @Provides
    fun provideIoDispatcher(): CoroutineDispatcher = Dispatchers.IO

    @MainDispatcher
    @Provides
    fun provideMainDispatcher(): CoroutineDispatcher = Dispatchers.Main
}
```

---

## 🖐️ 12. GestureRepositoryModule.kt

### Purpose
Provides **gesture-specific repository bindings** for gesture detection and management.

### Key Dependencies

| Dependency | Purpose |
|------------|---------|
| `IGestureRepository` | Gesture repository interface |
| `GestureRepositoryImpl` | Gesture repository implementation |
| `IGestureDataSource` | Gesture data source |

### Module Structure

```kotlin
@Module
@InstallIn(SingletonComponent::class)
abstract class GestureRepositoryModule {

    @Binds
    @Singleton
    abstract fun bindGestureRepository(
        impl: GestureRepositoryImpl
    ): IGestureRepository
}

@Module
@InstallIn(SingletonComponent::class)
object GestureRepositoryProvidersModule {

    @Provides
    @Singleton
    fun provideGestureDataSource(
        prefs: PreferencesManager
    ): IGestureDataSource {
        return GestureDataSourceImpl(prefs)
    }

    @Provides
    @Singleton
    fun provideGestureRepositoryImpl(
        context: Context,
        prefs: PreferencesManager,
        gestureDetector: EnhancedGestureDetector,
        dataSource: IGestureDataSource
    ): GestureRepositoryImpl {
        return GestureRepositoryImpl(context, prefs, gestureDetector, dataSource)
    }
}
```

---

## 📊 13. DI Flow Diagram

### Application Startup

```
Application.onCreate()
        ↓
Hilt generates Dagger components
        ↓
@Singleton components are created
        ↓
Dependencies are provided:
├── AppModule → Context, Preferences, Helpers
├── NetworkModule → OkHttp, ConnectionManager, UdpDiscovery
├── DatabaseModule → AppDatabase, DAOs
├── SensorModule → SensorManager, CalibrationHelper
├── ServiceModule → SensorService, PresentationModeService
├── RepositoryModule → Repository implementations
├── UseCaseModule → Use Cases
├── FeatureModule → Feature orchestrators
├── ViewModelModule → ViewModels
├── CoroutineModule → Dispatchers
└── CalibrationModule → Calibration data source
        ↓
Activity is created
        ↓
@AndroidEntryPoint injects ViewModels
        ↓
UI renders with all dependencies
```

### Dependency Injection Example

```kotlin
// In a ViewModel
@HiltViewModel
class HomeViewModel @Inject constructor(
    private val connectionManager: ConnectionManager,       // From NetworkModule
    private val prefs: PreferencesManager,                 // From AppModule
    private val sensorService: SensorService,              // From ServiceModule
    private val connectToServerUseCase: ConnectToServerUseCase, // From UseCaseModule
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher // From CoroutineModule
) : ViewModel() {
    // ViewModel logic
}

// In an Activity
@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var prefs: PreferencesManager

    @Inject
    lateinit var sensorService: SensorService
}
```

---

## 📋 14. Module Summary

| Module | Scope | Purpose |
|--------|-------|---------|
| `AppModule` | `@Singleton` | Core dependencies (Context, Preferences, Helpers) |
| `NetworkModule` | `@Singleton` | Network dependencies (OkHttp, ConnectionManager) |
| `DatabaseModule` | `@Singleton` | Room database and DAOs |
| `SensorModule` | `@Singleton` | Sensor dependencies (SensorManager) |
| `ServiceModule` | `@Singleton` | Services (SensorService, PresentationMode) |
| `RepositoryModule` | `@Singleton` | Repository implementations |
| `UseCaseModule` | `@Singleton` | Use cases |
| `FeatureModule` | `@Singleton` | Feature orchestrators |
| `ViewModelModule` | `SingletonComponent` | ViewModels |
| `CalibrationModule` | `@Singleton` | Calibration dependencies |
| `CoroutineModule` | `SingletonComponent` | Coroutine dispatchers |
| `GestureRepositoryModule` | `@Singleton` | Gesture repository |

---

## ✅ Best Practices Applied

| Practice | Implementation |
|----------|----------------|
| **Singleton Scope** | All repositories, data sources, and services are `@Singleton` |
| **Constructor Injection** | All dependencies are injected via constructor |
| **Field Injection** | Used only in Activities/Fragments with `@Inject lateinit var` |
| **Qualifiers** | `@IoDispatcher`, `@DefaultDispatcher`, `@MainDispatcher` |
| **ViewModels** | `@HiltViewModel` with `@Inject constructor` |
| **Modules** | Organized by layer (Presentation, Domain, Data, Infrastructure) |
| **Interface Binding** | `@Binds` for repository interfaces |
| **Concrete Provision** | `@Provides` for dependencies that need custom creation |

---

**This DI layer provides a clean, maintainable, and testable dependency injection setup for the entire Air Mouse application, following Dagger Hilt best practices.**