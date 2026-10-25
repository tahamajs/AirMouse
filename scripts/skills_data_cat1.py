#!/usr/bin/env python3
"""
Wave A Skills Data Definition:
47 Skills across Category 1, Category 6, and Category 9.
"""

from skills_builder import write_and_verify_skill

CAT1_SKILLS = [
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/presentation/viewmodel/ConnectionViewModel.kt",
        "title": "Structured Concurrency with Kotlin Coroutines in AirMouse",
        "when": [
            "Launching background jobs in ViewModels or Services without tying to lifecycle.",
            "Encountering thread leaks or uncaught exceptions cancelling parent scopes.",
            "Refactoring GlobalScope or unmanaged CoroutineScope instances."
        ],
        "prereqs": [
            "android/app/build.gradle with org.jetbrains.kotlinx:kotlinx-coroutines-android",
            "com.airmouse.presentation.viewmodel package structure",
            "Hilt dependency injection configured for CoroutineDispatcher"
        ],
        "steps": [
            {
                "title": "Audit Coroutine Scope Usage",
                "detail": "Search the codebase for GlobalScope, unmanaged CoroutineScope(), or Dispatchers.IO hardcoded instantiations.",
                "command": "grep -rn 'GlobalScope' android/app/src/main/java/"
            },
            {
                "title": "Define Injected Dispatchers",
                "detail": "Create CoroutineScopes bound to SupervisorJob inside ViewModel or Service classes to isolate child task failures.",
                "command": "mkdir -p android/app/src/main/java/com/airmouse/di"
            },
            {
                "title": "Wire Scope Lifecycle Cancellation",
                "detail": "Ensure that any custom CoroutineScope explicitly calls cancel() in onCleared() or onDestroy() to prevent leaked coroutines."
            }
        ],
        "code": """
// BEFORE (Leaky unmanaged scope without lifecycle termination):
// GlobalScope.launch { client.connect() }

// AFTER (Structured concurrency with SupervisorJob and injected dispatcher):
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
                // Must explicitly rethrow cancellation to allow coroutine hierarchy cancellation
                throw e
            } catch (e: Exception) {
                _uiState.update { it.copy(error = e.localizedMessage) }
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        supervisorJob.cancel()
    }
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.viewmodel.ConnectionViewModelTest",
        "verify_exp": "BUILD SUCCESSFUL - 0 failed tests",
        "pitfalls": [
            {
                "wrong": "Catching generic Throwable or Exception and swallowing CancellationException inside coroutines.",
                "right": "Always rethrow CancellationException so parent jobs can orchestrate child lifecycle cancellations properly."
            },
            {
                "wrong": "Using GlobalScope.launch for network dispatching, causing socket operations to survive Activity death.",
                "right": "Use viewModelScope or an injected lifecycle-aware CoroutineScope bound to the Service or Activity."
            },
            {
                "wrong": "Hardcoding Dispatchers.IO or Dispatchers.Default in business logic classes.",
                "right": "Inject CoroutineDispatcher via Hilt annotations (@IoDispatcher, @DefaultDispatcher) to enable deterministic unit tests."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/presentation/ui/main/MainViewModel.kt",
        "title": "StateFlow and SharedFlow State Management in AirMouse",
        "when": [
            "Exposing observable state from ViewModels to Jetpack Compose UI.",
            "Handling transient single-fire events like Navigation or Snackbar triggers.",
            "Preventing unnecessary sensor data processing when the app is in the background."
        ],
        "prereqs": [
            "kotlinx.coroutines.flow in classpath",
            "androidx.lifecycle.compose.collectAsStateWithLifecycle dependency",
            "Jetpack Compose UI module"
        ],
        "steps": [
            {
                "title": "Declare Immutable State Model",
                "detail": "Model screen state as an immutable data class with sensible defaults for offline/disconnected conditions."
            },
            {
                "title": "Configure StateFlow with Backpressure",
                "detail": "Convert cold repository data flows into a hot StateFlow using stateIn() with SharingStarted.WhileSubscribed(5000)."
            },
            {
                "title": "Expose One-Off Event Stream",
                "detail": "Use MutableSharedFlow with extraBufferCapacity=1 and BufferOverflow.DROP_OLDEST for navigation side-effects."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/presentation/ui/main/MainViewModel.kt
@HiltViewModel
class MainViewModel @Inject constructor(
    private val sensorRepository: SensorRepository
) : ViewModel() {
    // Single-event stream for one-off actions
    private val _events = MutableSharedFlow<MainEffect>(
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val events: SharedFlow<MainEffect> = _events.asSharedFlow()

    // Upstream cold stream converted into hot state flow
    val uiState: StateFlow<MainUiState> = sensorRepository.sensorStream
        .map { sample -> MainUiState.Active(sample.gyro, sample.accel) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = MainUiState.Idle
        )

    fun triggerCalibration() {
        viewModelScope.launch {
            _events.emit(MainEffect.NavigateToCalibration)
        }
    }
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.main.MainViewModelTest",
        "verify_exp": "PASSED: uiState emits within 5s window and cancels upstream on unsubscribe",
        "pitfalls": [
            {
                "wrong": "Using MutableLiveData instead of StateFlow in Compose projects.",
                "right": "StateFlow integrates natively with Compose collectAsStateWithLifecycle and enforces strict type safety."
            },
            {
                "wrong": "Using SharingStarted.Eagerly for continuous sensor flows, draining battery when app is backgrounded.",
                "right": "Use SharingStarted.WhileSubscribed(5000) to stop upstream sensor collection 5 seconds after screen disconnects."
            },
            {
                "wrong": "Exposing MutableStateFlow directly to the UI composables allowing accidental external state mutation.",
                "right": "Keep MutableStateFlow strictly private and expose only a read-only StateFlow via asStateFlow()."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/sensors/MotionAnalyzer.kt",
        "title": "High-Frequency Stream Processing with collectLatest",
        "when": [
            "Processing sensor updates arriving at 100-200Hz where intermediate frames can be dropped.",
            "Preventing UI thread queue backpressure when drawing live motion traces.",
            "Cancelling long-running calculations when a newer sensor packet arrives."
        ],
        "prereqs": [
            "android/app/src/main/java/com/airmouse/sensors/SensorFusion.kt",
            "Kotlin Coroutines 1.7+ with Flow conflate support",
            "Hardware IMU streaming enabled"
        ],
        "steps": [
            {
                "title": "Analyze Flow Emission Rates",
                "detail": "Verify if the source flow emits faster than the consumer can process (e.g. 100Hz gyroscope vs 60Hz UI frame)."
            },
            {
                "title": "Apply Flow Conflation",
                "detail": "Insert conflate() or buffer(Channel.CONFLATED) before the terminal operator to drop stale queue items."
            },
            {
                "title": "Switch Collector to collectLatest",
                "detail": "Use collectLatest inside the consuming coroutine to automatically cancel obsolete in-flight jobs."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/sensors/MotionAnalyzer.kt
class MotionAnalyzer @Inject constructor(
    private val sensorFusion: SensorFusion,
    private val networkDispatchChannel: Channel<CursorDelta>
) {
    fun startMotionProcessing(scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            sensorFusion.fusedOrientationFlow
                .conflate() // Keep only the latest emitted sensor orientation
                .collectLatest { orientation ->
                    // If a new orientation packet arrives while computing,
                    // this block is instantly cancelled and restarted with the fresh value
                    val delta = computeComplexSpatialTrajectory(orientation)
                    networkDispatchChannel.send(delta)
                }
        }
    }

    private suspend fun computeComplexSpatialTrajectory(o: Orientation): CursorDelta {
        delay(2) // Simulate small matrix computation
        return CursorDelta(dx = o.yaw * 1.5f, dy = o.pitch * 1.5f)
    }
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.sensors.MotionAnalyzerTest",
        "verify_exp": "PASSED: collectLatest drops stale sensor frames under synthetic 200Hz load",
        "pitfalls": [
            {
                "wrong": "Using collectLatest for discrete transactional events like button clicks or authentication tokens.",
                "right": "Use collect or a buffered Channel for critical control events so none are accidentally aborted."
            },
            {
                "wrong": "Neglecting conflate() on high-speed flows, leading to memory accumulation in unbounded internal buffers.",
                "right": "Pair collectLatest with conflate() or buffer(Channel.CONFLATED) to eliminate memory spikes."
            },
            {
                "wrong": "Performing non-cancellable blocking operations (like synchronized disk write) inside collectLatest.",
                "right": "Ensure worker functions inside collectLatest periodically check ensureActive() or call suspendable APIs."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadContract.kt",
        "title": "Sealed Architecture for UI State, Actions and Effects",
        "when": [
            "Creating or refactoring an AirMouse screen composable and its ViewModel.",
            "Handling complex screen lifecycles (Idle, Connecting, Active, Error).",
            "Eliminating multiple boolean flags in ViewModels (isLoading, isError, isConnected)."
        ],
        "prereqs": [
            "Jetpack Compose runtime in android/app/build.gradle",
            "Kotlin 1.9+ with sealed interface support",
            "Material 3 theme components"
        ],
        "steps": [
            {
                "title": "Define Sealed Contract Interface",
                "detail": "Create a unified contract file holding UiState, UiAction, and UiEffect sealed hierarchies."
            },
            {
                "title": "Add Immutability Annotations",
                "detail": "Annotate all state data classes with @Immutable or @Stable to guide the Compose compiler skipping rules."
            },
            {
                "title": "Implement Centralized Action Handler",
                "detail": "Route all user interactions into a single onAction(action: UiAction) function in the ViewModel."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadContract.kt
package com.airmouse.presentation.ui.touchpad

import androidx.compose.runtime.Immutable

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
}

sealed interface TouchpadEffect {
    data class ShowToast(val message: String) : TouchpadEffect
    data object TriggerHapticSuccess : TouchpadEffect
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.touchpad.TouchpadViewModelTest",
        "verify_exp": "PASSED: exhaustive when checks across all TouchpadUiState branches",
        "pitfalls": [
            {
                "wrong": "Creating mutable var properties inside UiState data classes, breaking Compose state tracking.",
                "right": "All fields in UiState must be val and hold immutable primitives or ImmutableList collections."
            },
            {
                "wrong": "Handling navigation directly inside UiAction callbacks, coupling ViewModels to NavHost.",
                "right": "Emit a UiEffect for navigation side-effects and let the Composable handle the actual NavHost call."
            },
            {
                "wrong": "Using raw, unlocalized strings inside error state objects.",
                "right": "Use String resource IDs (@StringRes Int) or a sealed UI text abstraction for clean internationalization."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/di/NetworkModule.kt",
        "title": "Hilt Module Organization and Scope Segregation",
        "when": [
            "Adding new repositories, sensor managers, or network clients to dependency injection.",
            "Resolving Hilt compilation cycles or missing binding errors.",
            "Refactoring monolithic AppModule into bounded domain modules."
        ],
        "prereqs": [
            "com.google.dagger.hilt.android plugin in android/app/build.gradle",
            "AirMouseApplication annotated with @HiltAndroidApp",
            "Separated domain interfaces and implementation classes"
        ],
        "steps": [
            {
                "title": "Isolate Interface Bindings",
                "detail": "Create abstract class modules using @Binds to link domain interfaces with concrete implementations."
            },
            {
                "title": "Segregate Third-Party Providers",
                "detail": "Put third-party library instantiations (OkHttpClient, Room) into companion objects with @Provides."
            },
            {
                "title": "Assign Scopes Conservatively",
                "detail": "Scope instances strictly (SingletonComponent for app-wide singletons, ViewModelComponent for screen scopes)."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/di/NetworkModule.kt
package com.airmouse.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

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
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }
}
""",
        "verify_cmd": "./gradlew assembleDebug -x lint",
        "verify_exp": "BUILD SUCCESSFUL - Hilt dependency graph validated cleanly",
        "pitfalls": [
            {
                "wrong": "Using @Provides for concrete classes where an abstract @Binds method is sufficient.",
                "right": "Use @Binds on abstract methods for zero-overhead compilation and smaller DEX bytecode."
            },
            {
                "wrong": "Injecting Activity-scoped dependencies into SingletonComponent, causing Activity context leaks.",
                "right": "Always scope singletons strictly to Application context or use un-scoped factory bindings."
            },
            {
                "wrong": "Forgetting the @InstallIn annotation on a @Module class, leading to silent injection failures.",
                "right": "Every Dagger/Hilt module must explicitly declare its target component via @InstallIn."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/test/java/com/airmouse/di/TestNetworkModule.kt",
        "title": "Hilt Testing with Fakes and @TestInstallIn",
        "when": [
            "Testing ViewModels without spinning up physical network sockets or hardware sensors.",
            "Replacing fragile Mockito mock chains with stateful in-memory implementations.",
            "Executing instrumented tests in CI hermetically."
        ],
        "prereqs": [
            "hilt-android-testing in android/app/build.gradle",
            "Fake implementation implementing the target domain interface",
            "Robolectric or AndroidJUnit runner configured"
        ],
        "steps": [
            {
                "title": "Construct In-Memory Fake",
                "detail": "Implement the domain interface with an in-memory mutable state container (e.g. MutableStateFlow or HashMap)."
            },
            {
                "title": "Declare Test Replacement Module",
                "detail": "Annotate a test module with @TestInstallIn specifying the production module it replaces."
            },
            {
                "title": "Inject Fake into Test Runner",
                "detail": "Inject the fake into the test class and manipulate its state to assert ViewModel reactions."
            }
        ],
        "code": """
// File: android/app/src/test/java/com/airmouse/di/TestNetworkModule.kt
package com.airmouse.di

import com.airmouse.network.ConnectionManager
import com.airmouse.network.FakeConnectionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

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
""",
        "verify_cmd": "./gradlew testDebugUnitTest",
        "verify_exp": "PASSED: Tests executed with FakeConnectionManager without network sockets",
        "pitfalls": [
            {
                "wrong": "Using @UninstallModules without defining a replacement module, causing missing binding compilation errors.",
                "right": "Pair @UninstallModules with @BindValue in the test class or use @TestInstallIn."
            },
            {
                "wrong": "Allowing test state to leak across test executions in singleton in-memory fakes.",
                "right": "Provide an explicit clear() or reset() method on in-memory fakes invoked in @Before or @After."
            },
            {
                "wrong": "Using Mockito mocks on Kotlin final classes without enabling mock-maker-inline.",
                "right": "Prefer lightweight handwritten in-memory fakes; they are faster and do not break on refactorings."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt",
        "title": "SavedStateHandle and Hilt ViewModel Injection",
        "when": [
            "Preserving user inputs or selected connection profiles across Android OS process death.",
            "Retrieving type-safe navigation arguments inside ViewModel constructors.",
            "Testing ViewModels with pre-populated SavedStateHandle instances."
        ],
        "prereqs": [
            "androidx.lifecycle:lifecycle-viewmodel-compose dependency",
            "androidx.lifecycle:lifecycle-viewmodel-savedstate dependency",
            "Navigation Compose argument routing"
        ],
        "steps": [
            {
                "title": "Inject SavedStateHandle",
                "detail": "Declare SavedStateHandle as a constructor parameter in the @HiltViewModel class."
            },
            {
                "title": "Bind Persistent StateFlow",
                "detail": "Use savedStateHandle.getStateFlow(key, initialValue) to automatically synchronize state changes."
            },
            {
                "title": "Extract Navigation Arguments",
                "detail": "Read route parameters passed from NavHost directly from the handle without bundle unpacking."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt
package com.airmouse.presentation.ui.calibration

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject

@HiltViewModel
class CalibrationViewModel @Inject constructor(
    private val savedStateHandle: SavedStateHandle
) : ViewModel() {
    // Navigation parameter automatically populated by Navigation Compose
    val profileId: String = checkNotNull(savedStateHandle["profileId"])

    // Reactive state synchronized with process death storage
    val calibrationStep: StateFlow<Int> = savedStateHandle.getStateFlow("step", 0)

    fun advanceStep() {
        savedStateHandle["step"] = calibrationStep.value + 1
    }
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.calibration.CalibrationViewModelTest",
        "verify_exp": "PASSED: state successfully restored from synthetic SavedStateHandle",
        "pitfalls": [
            {
                "wrong": "Storing heavy complex objects or open file streams in SavedStateHandle.",
                "right": "Only store primitives, Strings, identifiers, or lightweight Parcelable objects under 50KB."
            },
            {
                "wrong": "Accessing arguments without null validation or default fallbacks.",
                "right": "Use checkNotNull(savedStateHandle['key']) or provide explicit safe default values."
            },
            {
                "wrong": "Creating manual ViewModelProvider.Factory implementations when using Hilt.",
                "right": "Use standard @HiltViewModel annotations and let Hilt generate the factory automatically."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/PreferencesManager.kt",
        "title": "Migrating to Jetpack Preferences DataStore",
        "when": [
            "Storing user preferences like One Euro filter parameters, theme mode, and last-used host IP.",
            "Eliminating blocking disk I/O on the main thread caused by SharedPreferences.apply().",
            "Providing a reactive Flow-based settings stream to the UI layer."
        ],
        "prereqs": [
            "androidx.datastore:datastore-preferences in android/app/build.gradle",
            "Application context injected via Hilt @ApplicationContext",
            "Coroutines dependency configured"
        ],
        "steps": [
            {
                "title": "Define DataStore Delegate",
                "detail": "Create a top-level context extension delegate for DataStore with a dedicated preferences file name."
            },
            {
                "title": "Declare Preference Keys",
                "detail": "Define strongly-typed keys (stringPreferencesKey, floatPreferencesKey, booleanPreferencesKey)."
            },
            {
                "title": "Expose Reactive Accessors",
                "detail": "Expose read streams via Flow<T> with catch blocks for IOException and provide suspend edit methods."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/PreferencesManager.kt
package com.airmouse

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "airmouse_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SENSITIVITY = floatPreferencesKey("cursor_sensitivity")
        val ONE_EURO_BETA = floatPreferencesKey("one_euro_beta")
    }

    val sensitivityFlow: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[Keys.SENSITIVITY] ?: 1.0f }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.SENSITIVITY] = value }
    }
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.PreferencesManagerTest",
        "verify_exp": "PASSED: preferences update asynchronously and emit over flow",
        "pitfalls": [
            {
                "wrong": "Calling runBlocking on dataStore.data.first() directly on the main UI thread.",
                "right": "Always observe DataStore flows asynchronously inside appropriate coroutine scopes."
            },
            {
                "wrong": "Creating multiple DataStore instances with identical file names in different classes.",
                "right": "Declare the DataStore delegate as a single top-level private property on Context."
            },
            {
                "wrong": "Saving massive relational or tabular data sets into Preferences DataStore.",
                "right": "Use Room SQLite for complex relational structures, reserving DataStore strictly for key-value settings."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/network/ProtocolV2Dto.kt",
        "title": "Strict Protocol V2 Serialization with kotlinx.serialization",
        "when": [
            "Serializing and deserializing JSON wire packets over TCP, TLS, or WebSocket.",
            "Preventing silent fallback to default values on missing required fields.",
            "Rejecting malformed or unexpected JSON keys in network packets."
        ],
        "prereqs": [
            "kotlinx.serialization plugin applied in build.gradle",
            "docs/PROTOCOL_V2.md specification document",
            "Strict JSON encoder configured"
        ],
        "steps": [
            {
                "title": "Define DTO Data Model",
                "detail": "Annotate packet models with @Serializable and define all fields per Protocol V2 specifications."
            },
            {
                "title": "Enforce Required Fields and Invariants",
                "detail": "Implement an init block that asserts v == 2, valid monotonic sequence, and timestamp boundaries."
            },
            {
                "title": "Configure Strict Json Engine",
                "detail": "Build a Json instance with ignoreUnknownKeys = false, encodeDefaults = true, and isLenient = false."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/network/ProtocolV2Dto.kt
package com.airmouse.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

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
        require(v == 2) { "Protocol version must be exactly 2, got: $v" }
        require(type.isNotBlank()) { "Message type cannot be blank" }
        require(seq >= 0) { "Sequence number must be non-negative, got: $seq" }
    }
}

val StrictProtocolJson = Json {
    ignoreUnknownKeys = false
    encodeDefaults = true
    isLenient = false
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.network.ProtocolV2DtoTest",
        "verify_exp": "PASSED: Envelope validation rejects v!=2 and unknown payload fields",
        "pitfalls": [
            {
                "wrong": "Setting ignoreUnknownKeys = true in production protocol parsers, masking schema mismatches.",
                "right": "Keep ignoreUnknownKeys = false to enforce strict Protocol V2 §2.4 contract validation."
            },
            {
                "wrong": "Encoding float coordinates without checking for Float.NaN or Float.POSITIVE_INFINITY.",
                "right": "Check isNaN() and isInfinite() before serializing delta values to prevent JSON corruption."
            },
            {
                "wrong": "Transmitting flat root keys instead of nested payload dictionaries.",
                "right": "All type-specific parameters must be nested under the 'payload' object per Protocol V2."
            }
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
        "lang": "kotlin",
        "file_path": "android/app/src/main/java/com/airmouse/data/local/ConnectionLogDao.kt",
        "title": "Room SQLite Persistence for Session and Calibration Logs",
        "when": [
            "Recording historical connection sessions, average RTT, and packet loss statistics.",
            "Storing multiple device calibration matrices for different desks or environments.",
            "Executing relational queries over past performance diagnostics."
        ],
        "prereqs": [
            "androidx.room dependencies in android/app/build.gradle",
            "KSP plugin configured for Room annotation processing",
            "Hilt module for Database providing"
        ],
        "steps": [
            {
                "title": "Create Room Entity",
                "detail": "Define table schemas with @Entity and establish primary keys and indices for high-frequency queries."
            },
            {
                "title": "Implement Data Access Object (DAO)",
                "detail": "Write DAO interfaces with Flow<List<T>> for reactive UI observation and suspend methods for writes."
            },
            {
                "title": "Configure Room Database Singleton",
                "detail": "Subclass RoomDatabase and provide singleton instantiation via Hilt DatabaseModule."
            }
        ],
        "code": """
// File: android/app/src/main/java/com/airmouse/data/local/ConnectionLogDao.kt
package com.airmouse.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

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
}
""",
        "verify_cmd": "./gradlew testDebugUnitTest --tests com.airmouse.data.local.ConnectionLogDaoTest",
        "verify_exp": "PASSED: In-memory Room database successfully inserts and observes logs",
        "pitfalls": [
            {
                "wrong": "Executing Room DAO queries synchronously on the main thread, freezing the UI.",
                "right": "Always return Flow<T> or suspend functions from DAO methods to run off the main thread."
            },
            {
                "wrong": "Altering an @Entity schema without incrementing database version, triggering crash at launch.",
                "right": "Increment database version and supply an explicit Migration or test migration strategy."
            },
            {
                "wrong": "Storing unbounded live sensor coordinates in SQLite tables.",
                "right": "Store only session-level aggregate metrics in SQLite; stream live data in memory."
            }
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
        "lang": "groovy",
        "file_path": "settings.gradle",
        "title": "Clean Modular Architecture for AirMouse Android",
        "when": [
            "Refactoring monolithic :app module into cohesive feature and core modules.",
            "Preventing circular dependencies between features.",
            "Speeding up incremental Gradle build and test cycles."
        ],
        "prereqs": [
            "settings.gradle with include ':core:network', ':core:ui', ':core:sensors'",
            "Modular package boundaries",
            "Gradle Version Catalog setup"
        ],
        "steps": [
            {
                "title": "Register Modules in settings.gradle",
                "detail": "Declare subprojects for core models, hardware sensors, networking, and individual UI features."
            },
            {
                "title": "Establish Dependency Inversion Rules",
                "detail": "Ensure feature modules depend on core contracts and never directly on sibling features."
            },
            {
                "title": "Wire App Shell Aggregation",
                "detail": "Let the top-level :app module aggregate features and wire navigation routes together."
            }
        ],
        "code": """
// File: settings.gradle
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
}
""",
        "verify_cmd": "./gradlew projects",
        "verify_exp": "Project ':core:network', ':core:sensors', ':feature:touchpad' listed",
        "pitfalls": [
            {
                "wrong": "Allowing direct cross-feature dependencies (:feature:calibration depending on :feature:touchpad).",
                "right": "Feature modules must coordinate exclusively through the top-level :app navigator or shared core modules."
            },
            {
                "wrong": "Duplicating common Gradle build scripts across 10 different submodules.",
                "right": "Use convention plugins or version catalogs to standardize plugin and compiler configurations."
            },
            {
                "wrong": "Leaking Android framework classes into domain entities in :core:model.",
                "right": "Keep domain models purely Kotlin-based to enable fast JVM unit testing."
            }
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
        "lang": "toml",
        "file_path": "gradle/libs.versions.toml",
        "title": "Gradle Version Catalog Centralization",
        "when": [
            "Upgrading libraries like Jetpack Compose, Kotlin, or Hilt across multiple modules.",
            "Eliminating fragmented, conflicting version strings in build.gradle files.",
            "Automating dependency vulnerability checks in CI."
        ],
        "prereqs": [
            "gradle/libs.versions.toml file in project root",
            "Gradle 7.4+",
            "Multi-module Gradle project structure"
        ],
        "steps": [
            {
                "title": "Create Version Catalog File",
                "detail": "Create gradle/libs.versions.toml with [versions], [libraries], [bundles], and [plugins] sections."
            },
            {
                "title": "Map Dependencies to Aliases",
                "detail": "Define canonical group, artifact, and version.ref bindings for all third-party libraries."
            },
            {
                "title": "Refactor Module Build Scripts",
                "detail": "Replace string literals in build.gradle files with type-safe libs.alias references."
            }
        ],
        "code": """
# File: gradle/libs.versions.toml
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
networking = ["okhttp-core", "okhttp-logging"]

[plugins]
android-application = { id = "com.android.application", version = "8.7.1" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
""",
        "verify_cmd": "./gradlew dependencies",
        "verify_exp": "BUILD SUCCESSFUL - All dependencies resolved from catalog",
        "pitfalls": [
            {
                "wrong": "Hardcoding dependency versions inside module build.gradle files instead of referencing the catalog.",
                "right": "Reference all dependencies via libs.bundle.name or libs.library.name."
            },
            {
                "wrong": "Using inconsistent hyphen vs dot naming conventions in catalog alias names.",
                "right": "Use consistent kebab-case or dot notation consistently across the catalog."
            },
            {
                "wrong": "Omitting the [plugins] table and applying plugins with raw version strings in module scripts.",
                "right": "Declare Gradle plugins in the catalog and apply using alias(libs.plugins.name)."
            }
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
        "lang": "kotlin",
        "file_path": "build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt",
        "title": "Gradle Build-Logic Convention Plugins",
        "when": [
            "Copy-pasting compileSdk, kotlinOptions, and Compose configurations across 5+ modules.",
            "Enforcing uniform compiler flags (-Xlint, -Werror) across the Android project.",
            "Maintaining clean, 10-line build.gradle files in feature modules."
        ],
        "prereqs": [
            "build-logic directory configured in settings.gradle via pluginManagement",
            "Kotlin DSL for build-logic",
            "libs.versions.toml version catalog"
        ],
        "steps": [
            {
                "title": "Initialize build-logic Project",
                "detail": "Configure build-logic/settings.gradle.kts and build.gradle.kts with kotlin-dsl plugin."
            },
            {
                "title": "Write Reusable Convention Plugins",
                "detail": "Implement Plugin<Project> classes that apply standard SDK versions, compiler flags, and plugins."
            },
            {
                "title": "Register Plugins in Gradle Block",
                "detail": "Register each convention plugin with a unique identifier in gradlePlugin { plugins { register(...) } }."
            },
            {
                "title": "Apply Conventions in Feature Modules",
                "detail": "Replace boilerplate build scripts with single-line convention plugin IDs."
            }
        ],
        "code": """
// File: build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt
package com.airmouse.buildlogic

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val extension = extensions.getByType<BaseExtension>()
        extension.apply {
            buildFeatures.compose = true
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.14"
            }
        }
    }
}
""",
        "verify_cmd": "./gradlew build-logic:convention:build",
        "verify_exp": "BUILD SUCCESSFUL - Convention plugins compiled cleanly",
        "pitfalls": [
            {
                "wrong": "Hardcoding compileSdk versions in convention plugins instead of reading from catalog.",
                "right": "Extract SDK and dependency coordinates dynamically from libs.versions.toml."
            },
            {
                "wrong": "Creating tightly coupled inheritance trees of custom Gradle plugins.",
                "right": "Compose behavior by applying multiple orthogonal, single-responsibility convention plugins."
            },
            {
                "wrong": "Forgetting to register convention plugins in gradlePlugin block of build.gradle.kts.",
                "right": "Register every convention plugin in gradlePlugin { plugins { register(...) } }."
            }
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
        "lang": "groovy",
        "file_path": "android/app/build.gradle",
        "title": "Migrating Annotation Processors from KAPT to KSP",
        "when": [
            "Observing slow incremental Gradle build times caused by Java stub generation.",
            "Updating Room and Hilt to versions supporting native KSP.",
            "Eliminating kapt block warnings during Gradle builds."
        ],
        "prereqs": [
            "com.google.devtools.ksp plugin in libs.versions.toml matching Kotlin version",
            "Room and Hilt dependencies compatible with KSP",
            "Gradle 8.0+"
        ],
        "steps": [
            {
                "title": "Apply KSP Plugin",
                "detail": "Add id('com.google.devtools.ksp') plugin to project and module build files."
            },
            {
                "title": "Migrate Dependency Configurations",
                "detail": "Replace kapt(...) statements with ksp(...) for Room, Hilt, and Moshi compilers."
            },
            {
                "title": "Remove KAPT Plugin",
                "detail": "Purge apply plugin: 'kotlin-kapt' and delete unused kapt configuration blocks."
            }
        ],
        "code": """
// File: android/app/build.gradle
// BEFORE (Legacy KAPT with Java stub generation):
// apply plugin: 'kotlin-kapt'
// dependencies {
//     kapt "androidx.room:room-compiler:$room_version"
//     kapt "com.google.dagger:hilt-compiler:$hilt_version"
// }

// AFTER (Native high-speed Kotlin Symbol Processing):
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
}
""",
        "verify_cmd": "./gradlew compileDebugKotlin --rerun-tasks",
        "verify_exp": "BUILD SUCCESSFUL - KSP generated files in build/generated/ksp/",
        "pitfalls": [
            {
                "wrong": "Using a KSP version that does not match the active Kotlin version.",
                "right": "Ensure KSP version strictly matches Kotlin (e.g. Kotlin 2.0.21 -> KSP 2.0.21-1.0.25)."
            },
            {
                "wrong": "Leaving kapt dependencies in the same module alongside ksp dependencies.",
                "right": "Completely remove all kapt dependencies so Gradle skips the Java stub generation step."
            },
            {
                "wrong": "Failing to mark KSP generated source folders for IDE code completion.",
                "right": "Android Studio Koala+ automatically indexes build/generated/ksp/debug/kotlin."
            }
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
        "lang": "yaml",
        "file_path": "config/detekt/detekt.yml",
        "title": "Code Quality Enforcement with Detekt and Ktlint",
        "when": [
            "Enforcing uniform Kotlin formatting across all developers and automated agents.",
            "Catching complex code smells (Wildcard imports, LongParameterList, MagicNumber).",
            "Running pre-commit hooks and CI gates before merging PRs."
        ],
        "prereqs": [
            "detekt.yml configuration file in config/detekt/",
            "io.gitlab.arturbosch.detekt Gradle plugin",
            "Git pre-commit hook script"
        ],
        "steps": [
            {
                "title": "Generate Detekt Baseline",
                "detail": "Run ./gradlew detektGenerateConfig to generate the baseline configuration file."
            },
            {
                "title": "Configure AirMouse Rulesets",
                "detail": "Enable Ktlint formatting ruleset and configure cyclomatic complexity and method length limits."
            },
            {
                "title": "Hook into CI Workflow",
                "detail": "Add ./gradlew detekt to GitHub Actions workflow as a mandatory pull-request gate."
            }
        ],
        "code": """
# File: config/detekt/detekt.yml
formatting:
  active: true
  NoWildcardImports:
    active: true
  FinalNewline:
    active: true
  TrailingCommaOnCallSite:
    active: true

complexity:
  LongMethod:
    threshold: 60
  ComplexCondition:
    threshold: 4
  TooManyFunctions:
    threshold: 15

style:
  MagicNumber:
    active: true
    ignoreNumbers: ['-1', '0', '1', '2']
""",
        "verify_cmd": "./gradlew detekt",
        "verify_exp": "BUILD SUCCESSFUL - 0 code smell violations reported",
        "pitfalls": [
            {
                "wrong": "Allowing wildcard imports (import foo.bar.*) in production codebase.",
                "right": "Explicitly list all imports to prevent symbol collisions and namespace ambiguity."
            },
            {
                "wrong": "Suppressing Detekt warnings with @Suppress without explaining the rationale.",
                "right": "Refactor the complex logic or provide a clear comment justifying the suppression."
            },
            {
                "wrong": "Running Detekt without formatting rules enabled, allowing messy spacing to pass CI.",
                "right": "Enable the formatting ruleset to enforce Ktlint-compatible layout automatically."
            }
        ]
    }
]

print("Loaded CAT1_SKILLS:", len(CAT1_SKILLS))
