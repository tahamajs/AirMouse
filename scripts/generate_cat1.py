#!/usr/bin/env python3
"""
Wave A Generator:
- Category 1: Android Kotlin & Architecture (15 skills)
- Category 6: Go Server Deep Dive (22 skills)
- Category 9: Protocol V2 & Networking (10 skills)
Total: 47 skills
"""

import os

SKILLS_DIR = os.path.abspath(os.path.join(os.path.dirname(__file__), "../.agents/skills"))

wave_a_skills = [
    # Category 1: Android Kotlin & Architecture
    {
        "name": "kotlin-coroutines-structured-concurrency",
        "category": "android",
        "tech": "kotlin",
        "topic": "coroutines",
        "applies_to": "android",
        "desc": "Enforces structured concurrency with SupervisorJob, CoroutineScope lifecycle bindings, and non-leaking dispatchers in AirMouse Android.",
        "related": ["kotlin-flow-state-and-shared", "mvvm-viewmodel-factory"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "Structured Concurrency with Kotlin Coroutines in AirMouse",
        "when": [
            "Launching background jobs in ViewModels or Services without tying to lifecycle.",
            "Encountering thread leaks or uncaught exceptions cancelling parent scopes.",
            "Refactoring GlobalScope or unmanaged CoroutineScope instances."
        ],
        "prereqs": [
            "android/app/build.gradle with org.jetbrains.kotlinx:kotlinx-coroutines-android",
            "com.airmouse.presentation.viewmodel package structure"
        ],
        "steps": [
            "Identify unmanaged CoroutineScope or GlobalScope calls in ViewModel/Service classes.",
            "Replace unmanaged scopes with viewModelScope or a dedicated SupervisorJob scope.",
            "Inject Dispatchers via Hilt (IoDispatcher, DefaultDispatcher) instead of hardcoding Dispatchers.IO."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/presentation/viewmodel/ConnectionViewModel.kt
// BEFORE (Leaky unmanaged scope):
// GlobalScope.launch { client.connect() }

// AFTER (Structured concurrency):
@HiltViewModel
class ConnectionViewModel @Inject constructor(
    private val client: AirMouseNetworkClient,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ViewModel() {
    private val supervisorJob = SupervisorJob()
    private val scope = CoroutineScope(ioDispatcher + supervisorJob)

    fun initiateConnection(targetIp: String) {
        viewModelScope.launch(ioDispatcher) {
            try {
                client.connect(targetIp)
            } catch (e: CancellationException) {
                throw e // Always rethrow cancellation in coroutines
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        supervisorJob.cancel()
    }
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.viewmodel.ConnectionViewModelTest",
        "verify_exp": "BUILD SUCCESSFUL with 0 failed tests",
        "pitfalls": [
            ("Catching generic Throwable without rethrowing CancellationException", "Always catch Exception or explicitly rethrow CancellationException"),
            ("Using GlobalScope.launch for operations expected to finish with screen lifecycle", "Use viewModelScope or lifecycleScope tied to Activity/Fragment"),
            ("Hardcoding Dispatchers.IO in class body", "Inject CoroutineDispatcher via Hilt for test determinism")
        ]
    },
    {
        "name": "kotlin-flow-state-and-shared",
        "category": "android",
        "tech": "kotlin",
        "topic": "flow",
        "applies_to": "android",
        "desc": "Implements StateFlow for UI state caching and SharedFlow for one-off events with WhileSubscribed(5000) backpressure handling.",
        "related": ["kotlin-coroutines-structured-concurrency", "kotlin-flow-collect-latest"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "StateFlow and SharedFlow State Management in AirMouse",
        "when": [
            "Exposing observable state from ViewModels to Jetpack Compose UI.",
            "Handling transient single-fire events like Navigation or Snackbar triggers.",
            "Preventing unnecessary sensor data processing when the app is in the background."
        ],
        "prereqs": [
            "kotlinx.coroutines.flow in classpath",
            "androidx.lifecycle.compose.collectAsStateWithLifecycle"
        ],
        "steps": [
            "Define a sealed or data class UI state as immutable.",
            "Create a private MutableStateFlow and expose it as a read-only StateFlow via asStateFlow().",
            "Use stateIn with SharingStarted.WhileSubscribed(5000) for upstream cold flows to conserve battery."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/presentation/ui/main/MainViewModel.kt
@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository
) : ViewModel() {
    private val _events = MutableSharedFlow<MainEffect>(extraBufferCapacity = 1)
    val events: SharedFlow<MainEffect> = _events.asSharedFlow()

    val uiState: StateFlow<MainUiState> = sensorRepository.sensorStream
        .map { sample -> MainUiState.Active(sample.gyro, sample.accel) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState.Idle
        )
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.main.MainViewModelTest",
        "verify_exp": "PASSED: uiState emits within 5s window and cancels upstream on unsubscribe",
        "pitfalls": [
            ("Using MutableLiveData instead of StateFlow in Compose projects", "StateFlow integrates directly with collectAsStateWithLifecycle"),
            ("Using SharingStarted.Eagerly for continuous sensor flows", "Use WhileSubscribed(5000) to halt sensor polling when screen goes dark"),
            ("Exposing MutableStateFlow directly to UI layer", "Always expose public read-only StateFlow via asStateFlow()")
        ]
    },
    {
        "name": "kotlin-flow-collect-latest",
        "category": "android",
        "tech": "kotlin",
        "topic": "flow",
        "applies_to": "android",
        "desc": "Applies collectLatest for rapid high-frequency sensor streams to cancel obsolete in-flight UI calculations in AirMouse.",
        "related": ["kotlin-flow-state-and-shared", "sensor-fusion-dsp"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "High-Frequency Stream Processing with collectLatest",
        "when": [
            "Processing sensor updates arriving at 100-200Hz where intermediate frames can be dropped.",
            "Preventing UI thread queue backpressure when drawing live motion traces.",
            "Cancelling long-running calculations when a newer sensor packet arrives."
        ],
        "prereqs": [
            "android/app/src/main/java/com/airmouse/sensors/SensorFusion.kt",
            "Kotlin Coroutines 1.7+"
        ],
        "steps": [
            "Identify high-frequency flow collectors that perform transformations or UI updates.",
            "Replace standard collect with collectLatest in consuming coroutines.",
            "Ensure shared resources are protected against cancellation mid-execution."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/sensors/MotionAnalyzer.kt
fun observeHighFrequencyStream(scope: CoroutineScope) {
    scope.launch(Dispatchers.Default) {
        sensorFusion.fusedOrientationFlow
            .conflate()
            .collectLatest { orientation ->
                // Automatically cancels previous processing if a new packet arrives
                val cursorDelta = computeCursorDelta(orientation)
                networkDispatchChannel.send(cursorDelta)
            }
    }
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.sensors.MotionAnalyzerTest",
        "verify_exp": "PASSED: collectLatest drops stale sensor frames under synthetic 200Hz load",
        "pitfalls": [
            ("Using collectLatest for discrete transactional events like button clicks", "Use collect for critical events to avoid accidental cancellation"),
            ("Neglecting conflate() on high-speed flows", "Pair with conflate() or buffer(Channel.CONFLATED) to eliminate memory spikes"),
            ("Performing non-cancellable blocking operations inside collectLatest", "Ensure operations check ensureActive() or yield()")
        ]
    },
    {
        "name": "kotlin-sealed-classes-ui-state",
        "category": "android",
        "tech": "kotlin",
        "topic": "architecture",
        "applies_to": "android",
        "desc": "Standardizes screen states with sealed interfaces for UiState, UiAction, and UiEffect across all AirMouse Compose screens.",
        "related": ["design-system-standardization", "kotlin-flow-state-and-shared"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "Sealed Architecture for UI State, Actions and Effects",
        "when": [
            "Creating or refactoring an AirMouse screen composable and its ViewModel.",
            "Handling complex screen lifecycles (Idle, Connecting, Active, Error).",
            "Eliminating multiple boolean flags in ViewModels (isLoading, isError, isConnected)."
        ],
        "prereqs": [
            "Jetpack Compose runtime in android/app/build.gradle",
            "Kotlin 1.9+ with sealed interface support"
        ],
        "steps": [
            "Create a Contract file defining UiState, UiAction, and UiEffect for the screen.",
            "Annotate data classes in UiState with @Immutable for Compose skipping optimization.",
            "Handle actions inside ViewModel via a single onAction(action: UiAction) dispatcher."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadContract.kt
@Immutable
sealed interface TouchpadUiState {
    data object Disconnected : TouchpadUiState
    data class Connecting(val targetHost: String) : TouchpadUiState
    data class Connected(
        val host: String,
        val pingMs: Long,
        val activeMode: MouseMode
    ) : TouchpadUiState
    data class Error(val message: String) : TouchpadUiState
}

sealed interface TouchpadAction {
    data class OnMove(val dx: Float, val dy: Float) : TouchpadAction
    data class OnClick(val button: MouseButton) : TouchpadAction
    data object OnReconnect : TouchpadAction
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.touchpad.TouchpadViewModelTest",
        "verify_exp": "PASSED: exhaustive when checks across all TouchpadUiState branches",
        "pitfalls": [
            ("Creating mutable properties inside UiState data classes", "All fields in UiState must be val and immutable"),
            ("Handling navigation directly inside UiAction", "Emit a UiEffect for navigation to preserve unidirectional data flow"),
            ("Using raw strings for error states", "Use localized string resource IDs or structured error objects")
        ]
    },
    {
        "name": "hilt-module-organization",
        "category": "android",
        "tech": "hilt",
        "topic": "dependency-injection",
        "applies_to": "android",
        "desc": "Structures Hilt modules with @InstallIn scopes, segregating @Binds interface implementations from @Provides third-party builders.",
        "related": ["hilt-testing-fakes-over-mocks", "android-module-structure-clean"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "Hilt Module Organization and Scope Segregation",
        "when": [
            "Adding new repositories, sensor managers, or network clients to dependency injection.",
            "Resolving Hilt compilation cycles or missing binding errors.",
            "Refactoring monolithic AppModule into bounded domain modules."
        ],
        "prereqs": [
            "com.google.dagger.hilt.android plugin in android/app/build.gradle",
            "AirMouseApplication annotated with @HiltAndroidApp"
        ],
        "steps": [
            "Create separate abstract class modules for @Binds interfaces (NetworkModule, SensorModule).",
            "Create companion object or separate object modules for @Provides instances requiring builders.",
            "Select appropriate component scope: SingletonComponent for global clients, ViewModelComponent for screen helpers."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/di/NetworkModule.kt
@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindConnectionManager(impl: DefaultConnectionManager): ConnectionManager

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }
}""",
        "verify_cmd": "./gradlew assembleDebug -x lint",
        "verify_exp": "BUILD SUCCESSFUL - Hilt dependency graph validated cleanly",
        "pitfalls": [
            ("Using @Provides for concrete classes where @Binds is possible", "Use @Binds on abstract methods for faster compilation and smaller DEX"),
            ("Injecting Activity-scoped dependencies into SingletonComponent", "Always scope singletons strictly to Application lifecycle"),
            ("Forgetting @InstallIn on a @Module class", "Every Hilt module must declare its target component explicitly")
        ]
    },
    {
        "name": "hilt-testing-fakes-over-mocks",
        "category": "android",
        "tech": "hilt",
        "topic": "testing",
        "applies_to": "android",
        "desc": "Implements in-memory fake repositories and @TestInstallIn module replacements for deterministic testing in AirMouse.",
        "related": ["hilt-module-organization", "android-unit-test-turbine-flow"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "Hilt Testing with Fakes and @TestInstallIn",
        "when": [
            "Testing ViewModels without spinning up physical network sockets or hardware sensors.",
            "Replacing fragile Mockito mock chains with stateful in-memory implementations.",
            "Executing instrumented tests in CI hermetically."
        ],
        "prereqs": [
            "hilt-android-testing in android/app/build.gradle",
            "Fake implementation implementing the target domain interface"
        ],
        "steps": [
            "Create a FakeRepository implementing the production repository interface.",
            "Define a test module annotated with @TestInstallIn specifying replaces = [ProductionModule::class].",
            "Inject the fake repository into the test class using @HiltAndroidTest."
        ],
        "code": """// File: android/app/src/test/java/com/airmouse/di/TestNetworkModule.kt
@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
abstract class TestNetworkModule {
    @Binds
    @Singleton
    abstract fun bindConnectionManager(fake: FakeConnectionManager): ConnectionManager
}

// In test:
@HiltAndroidTest
class TouchpadViewModelTest {
    @get:Rule var hiltRule = HiltAndroidRule(this)
    @Inject lateinit var connectionManager: ConnectionManager
}""",
        "verify_cmd": "./gradlew testDebugUnitTest",
        "verify_exp": "PASSED: Tests executed with FakeConnectionManager without network sockets",
        "pitfalls": [
            ("Using @UninstallModules without defining a replacement module", "Pair @UninstallModules with @BindValue or a custom test module"),
            ("Letting test state leak across test runs in singleton fakes", "Provide a clear() or reset() method on in-memory fakes called in @Before"),
            ("Using Mockito on Kotlin final classes without mock-maker-inline", "Prefer lightweight fakes over heavy reflection mocks")
        ]
    },
    {
        "name": "mvvm-viewmodel-factory",
        "category": "android",
        "tech": "android",
        "topic": "viewmodel",
        "applies_to": "android",
        "desc": "Configures Hilt-assisted injection with SavedStateHandle for ViewModel state restoration across process death.",
        "related": ["kotlin-sealed-classes-ui-state", "hilt-module-organization"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "SavedStateHandle and Hilt ViewModel Injection",
        "when": [
            "Preserving user inputs or selected connection profiles across Android OS process death.",
            "Retrieving type-safe navigation arguments inside ViewModel constructors.",
            "Testing ViewModels with pre-populated SavedStateHandle instances."
        ],
        "prereqs": [
            "androidx.lifecycle:lifecycle-viewmodel-compose",
            "androidx.lifecycle:lifecycle-viewmodel-savedstate"
        ],
        "steps": [
            "Inject SavedStateHandle into the @HiltViewModel constructor.",
            "Read navigation parameters or restore persistent screen state using handle.getStateFlow().",
            "Write updated critical state back into SavedStateHandle."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt
@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle,
    private val calibrationHelper: CalibrationHelper
) : ViewModel() {
    private val deviceProfileId: String = checkNotNull(savedStateHandle["profileId"])
    val calibrationStep = savedStateHandle.getStateFlow("step", 0)

    fun advanceStep() {
        savedStateHandle["step"] = calibrationStep.value + 1
    }
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.calibration.CalibrationViewModelTest",
        "verify_exp": "PASSED: state successfully restored from synthetic SavedStateHandle",
        "pitfalls": [
            ("Storing non-Parcelable/non-serializable complex objects in SavedStateHandle", "Only store primitives, Strings, Bundles, or Parcelable objects"),
            ("Using direct dictionary access without checkNotNull or defaults", "Provide fallback defaults or validate argument presence"),
            ("Using custom factories instead of @HiltViewModel when Hilt is available", "Stick to @HiltViewModel with standard SavedStateHandle injection")
        ]
    },
    {
        "name": "kotlin-datastore-preferences",
        "category": "android",
        "tech": "android",
        "topic": "storage",
        "applies_to": "android",
        "desc": "Implements Jetpack Preferences DataStore as the single source of truth for user settings, replacing legacy SharedPreferences.",
        "related": ["room-database-for-connection-log", "kotlin-flow-state-and-shared"],
        "pref": None,
        "audit": "ARCH-03",
        "title": "Migrating to Jetpack Preferences DataStore",
        "when": [
            "Storing user preferences like One Euro filter parameters, theme mode, and last-used host IP.",
            "Eliminating blocking disk I/O on the main thread caused by SharedPreferences.apply().",
            "Providing a reactive Flow-based settings stream to the UI layer."
        ],
        "prereqs": [
            "androidx.datastore:datastore-preferences in android/app/build.gradle",
            "Application context injected via Hilt @ApplicationContext"
        ],
        "steps": [
            "Create a PreferencesManager class backed by dataStore delegate.",
            "Define typed PreferencesKeys for each configuration item.",
            "Expose settings as Flow<T> and provide suspend functions for mutations."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/PreferencesManager.kt
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "airmouse_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SENSITIVITY = floatPreferencesKey("sensitivity")
        val ONE_EURO_BETA = floatPreferencesKey("one_euro_beta")
    }

    val sensitivityFlow: Flow<Float> = context.dataStore.data
        .map { prefs -> prefs[Keys.SENSITIVITY] ?: 1.0f }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.SENSITIVITY] = value }
    }
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.PreferencesManagerTest",
        "verify_exp": "PASSED: preferences update asynchronously and emit over flow",
        "pitfalls": [
            ("Calling runBlocking on dataStore.data.first() on the main thread", "Always collect flows asynchronously in coroutine scopes"),
            ("Creating multiple DataStore instances with the same file name", "Declare by preferencesDataStore as a top-level singleton delegate"),
            ("Saving large relational data sets into Preferences DataStore", "Use Room SQLite for structured tabular data")
        ]
    },
    {
        "name": "kotlin-serialization-protocol-dto",
        "category": "android",
        "tech": "kotlin",
        "topic": "serialization",
        "applies_to": "android",
        "desc": "Generates strict kotlinx.serialization DTOs strictly adhering to Protocol V2 canonical wire specifications.",
        "related": ["protocol-v2-envelope-validation", "protocol-v2-bounds-check"],
        "pref": "docs/PROTOCOL_V2.md#1-wire-envelope",
        "audit": "SEC-04",
        "title": "Strict Protocol V2 Serialization with kotlinx.serialization",
        "when": [
            "Serializing and deserializing JSON wire packets over TCP, TLS, or WebSocket.",
            "Preventing silent fallback to default values on missing required fields.",
            "Rejecting malformed or unexpected JSON keys in network packets."
        ],
        "prereqs": [
            "kotlinx.serialization plugin applied in build.gradle",
            "docs/PROTOCOL_V2.md specification document"
        ],
        "steps": [
            "Configure Json { ignoreUnknownKeys = false; encodeDefaults = false; isLenient = false }.",
            "Model the root Envelope class with v=2, type, seq, ts, id, and polymorphic or JsonElement payload.",
            "Provide explicit validation guards in init blocks or serializer hooks."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/network/ProtocolV2Dto.kt
@Serializable
data class WireEnvelope(
    val v: Int = 2,
    val type: String,
    val seq: Long,
    val ts: Long,
    val id: String? = null,
    val payload: JsonElement
) {
    init {
        require(v == 2) { "Protocol version must be 2, got $v" }
        require(type.isNotBlank()) { "Message type cannot be blank" }
        require(seq >= 0) { "Sequence number must be non-negative" }
    }
}

val StrictProtocolJson = Json {
    ignoreUnknownKeys = false
    encodeDefaults = true
    isLenient = false
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.network.ProtocolV2DtoTest",
        "verify_exp": "PASSED: Envelope validation rejects v!=2 and unknown payload fields",
        "pitfalls": [
            ("Enabling ignoreUnknownKeys = true in production protocol parser", "Strict schema requires rejecting unknown fields per Protocol V2 §2.4"),
            ("Using float types without rejecting NaN and Infinity", "Check for isNaN() and isInfinite() before serializing delta coordinates"),
            ("Sending flat root keys instead of nested payload object", "Flat payloads are deprecated and rejected by V2 servers")
        ]
    },
    {
        "name": "room-database-for-connection-log",
        "category": "android",
        "tech": "room",
        "topic": "database",
        "applies_to": "android",
        "desc": "Implements Room SQLite database with DAOs for persisting connection session history, latency metrics, and calibration profiles.",
        "related": ["kotlin-datastore-preferences", "hilt-module-organization"],
        "pref": None,
        "audit": "ARCH-03",
        "title": "Room SQLite Persistence for Session and Calibration Logs",
        "when": [
            "Recording historical connection sessions, average RTT, and packet loss statistics.",
            "Storing multiple device calibration matrices for different desks or environments.",
            "Executing relational queries over past performance diagnostics."
        ],
        "prereqs": [
            "androidx.room dependencies in android/app/build.gradle",
            "KSP plugin configured for Room annotation processing"
        ],
        "steps": [
            "Define Room @Entity classes with primary keys and indexed foreign keys.",
            "Create @Dao interfaces exposing Flow<List<T>> for live reactive observation.",
            "Implement RoomDatabase abstract class with schema migration strategies."
        ],
        "code": """// File: android/app/src/main/java/com/airmouse/data/local/ConnectionLogDao.kt
@Entity(tableName = "connection_logs")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostIp: String,
    val timestamp: Long,
    val averagePingMs: Long,
    val packetLossPct: Float,
    val sessionDurationSec: Long
)

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentSessions(): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(log: ConnectionLogEntity): Long
}""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.data.local.ConnectionLogDaoTest",
        "verify_exp": "PASSED: In-memory Room database successfully inserts and observes logs",
        "pitfalls": [
            ("Executing Room DAO queries on the main UI thread", "Always use suspend functions or Flow return types for DAO operations"),
            ("Modifying @Entity schema without incrementing version or writing Migration", "Provide fallbackToDestructiveMigration() in development or explicit migrations in production"),
            ("Storing massive unindexed blob logs in SQLite", "Index foreign keys and prune stale telemetry periodically")
        ]
    },
    {
        "name": "android-module-structure-clean",
        "category": "android",
        "tech": "gradle",
        "topic": "architecture",
        "applies_to": "android",
        "desc": "Enforces Clean Architecture modularization (:core:ui, :core:network, :feature:touchpad) across the AirMouse Android codebase.",
        "related": ["hilt-module-organization", "android-build-logic-convention-plugins"],
        "pref": None,
        "audit": "ARCH-01",
        "title": "Clean Modular Architecture for AirMouse Android",
        "when": [
            "Refactoring monolithic :app module into cohesive feature and core modules.",
            "Preventing circular dependencies between features.",
            "Speeding up incremental Gradle build and test cycles."
        ],
        "prereqs": [
            "settings.gradle with include ':core:network', ':core:ui', ':core:sensors'",
            "Modular package boundaries"
        ],
        "steps": [
            "Separate core domain models and protocol contracts into :core:model.",
            "Place hardware and socket implementations into :core:sensors and :core:network.",
            "Expose self-contained screens in :feature:touchpad, :feature:calibration."
        ],
        "code": """// File: settings.gradle
include ':app'
include ':core:network'
include ':core:sensors'
include ':core:ui'
include ':feature:touchpad'
include ':feature:calibration'

// File: feature/touchpad/build.gradle
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
}
dependencies {
    implementation(project(':core:network'))
    implementation(project(':core:sensors'))
    implementation(project(':core:ui'))
}""",
        "verify_cmd": "./gradlew projects",
        "verify_exp": "Project ':core:network', ':core:sensors', ':feature:touchpad' listed",
        "pitfalls": [
            ("Creating feature-to-feature direct dependencies", "Feature modules should only depend on core modules, coordinating via navigation in :app"),
            ("Duplicating Gradle dependency declarations across modules", "Use Gradle Version Catalog (libs.versions.toml) and convention plugins"),
            ("Leaking Android framework imports into pure domain logic", "Keep domain entities free of android.* dependencies")
        ]
    },
    {
        "name": "android-version-catalog-gradle",
        "category": "android",
        "tech": "gradle",
        "topic": "build",
        "applies_to": "android",
        "desc": "Centralizes dependency management and version coordinates using gradle/libs.versions.toml in AirMouse.",
        "related": ["android-module-structure-clean", "android-build-logic-convention-plugins"],
        "pref": None,
        "audit": "DEP-01",
        "title": "Gradle Version Catalog Centralization",
        "when": [
            "Upgrading libraries like Jetpack Compose, Kotlin, or Hilt across multiple modules.",
            "Eliminating fragmented, conflicting version strings in build.gradle files.",
            "Automating dependency vulnerability checks in CI."
        ],
        "prereqs": [
            "gradle/libs.versions.toml file in project root",
            "Gradle 7.4+"
        ],
        "steps": [
            "Define version pins in the [versions] table of libs.versions.toml.",
            "Map dependency coordinates in [libraries] and group bundles in [bundles].",
            "Reference libraries in module build files using libs.library.name syntax."
        ],
        "code": """# File: gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
compose = "1.7.4"
hilt = "2.52"
okhttp = "4.12.0"

[libraries]
kotlin-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.9.0" }
okhttp-core = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

[bundles]
networking = ["okhttp-core", "okhttp-logging"]""",
        "verify_cmd": "./gradlew dependencies",
        "verify_exp": "BUILD SUCCESSFUL - All dependencies resolved from catalog",
        "pitfalls": [
            ("Hardcoding library versions inside subproject build.gradle files", "Always use version.ref = 'name' in catalog"),
            ("Mixing hyphens and underscores inconsistently in alias names", "Use kebab-case or dot-separated aliases consistently"),
            ("Forgetting to define plugins in [plugins] table", "Declare Gradle plugins in the catalog for centralized plugin management")
        ]
    },
    {
        "name": "android-build-logic-convention-plugins",
        "category": "android",
        "tech": "gradle",
        "topic": "build",
        "applies_to": "android",
        "desc": "Creates convention plugins in build-logic to share common Compose, Hilt, and Android compiler options across modules.",
        "related": ["android-version-catalog-gradle", "android-module-structure-clean"],
        "pref": None,
        "audit": "DEP-01",
        "title": "Gradle Build-Logic Convention Plugins",
        "when": [
            "Copy-pasting compileSdk, kotlinOptions, and Compose configurations across 5+ modules.",
            "Enforcing uniform compiler flags (-Xlint, -Werror) across the Android project.",
            "Maintaining clean, 10-line build.gradle files in feature modules."
        ],
        "prereqs": [
            "build-logic directory configured in settings.gradle via pluginManagement",
            "Kotlin DSL for build-logic"
        ],
        "steps": [
            "Create build-logic/convention directory with its own settings.gradle.kts and build.gradle.kts.",
            "Implement custom plugins implementing Plugin<Project>.",
            "Apply custom convention plugins (e.g., airmouse.android.feature) in submodules."
        ],
        "code": """// File: build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt
class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val extension = extensions.getByType<BaseExtension>()
        extension.apply {
            buildFeatures.compose = true
        }
    }
}

// In feature module build.gradle.kts:
plugins {
    id("airmouse.android.library")
    id("airmouse.android.compose")
    id("airmouse.android.hilt")
}""",
        "verify_cmd": "./gradlew build-logic:convention:build",
        "verify_exp": "BUILD SUCCESSFUL - Convention plugins compiled cleanly",
        "pitfalls": [
            ("Hardcoding SDK versions in convention plugins instead of reading catalog", "Read versions dynamically from libs.versions.toml"),
            ("Over-complicating convention logic with cross-plugin inheritance", "Prefer flat composition of single-purpose convention plugins"),
            ("Forgetting to register convention plugins in build.gradle.kts of build-logic", "Use gradlePlugin { plugins { register(...) } } block")
        ]
    },
    {
        "name": "android-kotlin-ksp-vs-kapt",
        "category": "android",
        "tech": "kotlin",
        "topic": "build",
        "applies_to": "android",
        "desc": "Migrates annotation processors from legacy KAPT to Kotlin Symbol Processing (KSP) for 2x faster build times.",
        "related": ["android-build-logic-convention-plugins", "hilt-module-organization"],
        "pref": None,
        "audit": "DEP-01",
        "title": "Migrating Annotation Processors from KAPT to KSP",
        "when": [
            "Observing slow incremental Gradle build times caused by Java stub generation.",
            "Updating Room and Hilt to versions supporting native KSP.",
            "Eliminating kapt block warnings during Gradle builds."
        ],
        "prereqs": [
            "com.google.devtools.ksp plugin in libs.versions.toml matching Kotlin version",
            "Room and Hilt dependencies"
        ],
        "steps": [
            "Apply id('com.google.devtools.ksp') plugin in build.gradle.",
            "Replace kapt('...') with ksp('...') for Room, Hilt, and Moshi/Kotlinx processors.",
            "Remove kotlin-kapt plugin from build.gradle."
        ],
        "code": """// File: android/app/build.gradle
// BEFORE:
// apply plugin: 'kotlin-kapt'
// dependencies { kapt "androidx.room:room-compiler:$room_version" }

// AFTER:
plugins {
    alias(libs.plugins.ksp)
}
dependencies {
    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
}""",
        "verify_cmd": "./gradlew compileDebugKotlin --rerun-tasks",
        "verify_exp": "BUILD SUCCESSFUL - KSP generated files in build/generated/ksp/",
        "pitfalls": [
            ("Mismatched Kotlin and KSP plugin versions", "KSP version must strictly match the Kotlin version (e.g. 2.0.21-1.0.25)"),
            ("Leaving kapt dependencies lingering in the same module", "Completely remove kapt dependencies to avoid redundant stub generation"),
            ("Missing generated sources in IDE index", "Ensure build/generated/ksp/debug/kotlin is recognized by Android Studio")
        ]
    },
    {
        "name": "android-detekt-ktlint-standards",
        "category": "android",
        "tech": "lint",
        "topic": "quality",
        "applies_to": "android",
        "desc": "Enforces static code analysis, formatting rules, and code complexity limits with Detekt and Ktlint in CI.",
        "related": ["ci-github-actions-android", "android-build-logic-convention-plugins"],
        "pref": None,
        "audit": "DEP-02",
        "title": "Code Quality Enforcement with Detekt and Ktlint",
        "when": [
            "Enforcing uniform Kotlin formatting across all developers and automated agents.",
            "Catching complex code smells (Wildcard imports, LongParameterList, MagicNumber).",
            "Running pre-commit hooks and CI gates before merging PRs."
        ],
        "prereqs": [
            "detekt.yml configuration file in config/detekt/",
            "io.gitlab.arturbosch.detekt Gradle plugin"
        ],
        "steps": [
            "Generate baseline detekt config with ./gradlew detektGenerateConfig.",
            "Tune rules: enable formatting ruleset and configure complexity thresholds.",
            "Integrate ./gradlew detekt into local verification workflows."
        ],
        "code": """# File: config/detekt/detekt.yml
formatting:
  active: true
  NoWildcardImports:
    active: true
  FinalNewline:
    active: true

complexity:
  LongMethod:
    threshold: 60
  ComplexCondition:
    threshold: 4
  TooManyFunctions:
    threshold: 15""",
        "verify_cmd": "./gradlew detekt",
        "verify_exp": "BUILD SUCCESSFUL - 0 code smell violations reported",
        "pitfalls": [
            ("Allowing wildcard imports (import foo.bar.*)", "Explicitly list all imports to prevent naming collisions"),
            ("Suppressing Detekt issues with @Suppress without addressing root cause", "Refactor the complex function or document rationale"),
            ("Failing to include Detekt check in GitHub Actions CI", "Always fail CI builds if static analysis fails")
        ]
    }
]

def generate_skill(skill):
    name = skill["name"]
    path = os.path.join(SKILLS_DIR, name)
    os.makedirs(path, exist_ok=True)
    file_path = os.path.join(path, "SKILL.md")
    
    related_str = "[" + ", ".join(skill["related"]) + "]"
    tags_str = f"[{skill['category']}, {skill['tech']}, {skill['topic']}]"
    pref_line = f"protocol_ref: {skill['pref']}\n" if skill.get("pref") else ""
    
    when_bullets = "\n".join([f"- {w}" for w in skill["when"]])
    prereq_bullets = "\n".join([f"- {p}" for p in skill["prereqs"]])
    steps_numbered = "\n".join([f"{i+1}. {s}" for i, s in enumerate(skill["steps"])])
    pitfalls_text = "\n".join([
        f"- **Wrong:** {wrong}\n  **Correct:** {correct}"
        for wrong, correct in skill["pitfalls"]
    ])
    
    content = f"""---
name: {name}
description: {skill['desc']}
version: 1.0.0
author: airmouse-skills
tags: {tags_str}
related_skills: {related_str}
applies_to: {skill['applies_to']}
{pref_line}---

# {skill['title']}

## When to use
{when_bullets}

## Prerequisites
{prereq_bullets}

## Step-by-step instructions
{steps_numbered}

## Code examples
```kotlin
{skill['code']}
```

## Verification
Run the verification command:
```bash
{skill['verify_cmd']}
```
Expected output:
```text
{skill['verify_exp']}
```

## Common pitfalls
{pitfalls_text}

## Cross-references
- Related skills: {', '.join(skill['related'])}
- Protocol V2 specification: {skill.get('pref') or 'docs/PROTOCOL_V2.md'}
- Master audit check: [{skill['audit']}](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)
"""
    with open(file_path, "w", encoding="utf-8") as f:
        f.write(content)
    
    lines = content.strip().splitlines()
    print(f"Generated {name}: {len(lines)} lines")

if __name__ == "__main__":
    for s in wave_a_skills:
        generate_skill(s)
    print(f"Successfully generated {len(wave_a_skills)} skills in Category 1.")
