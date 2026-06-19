# Air Mouse Android App - Calibration System Complete Documentation

## 📋 Table of Contents

1. [Overview](#overview)
2. [Architecture](#architecture)
3. [Package Structure](#package-structure)
4. [Core Components](#core-components)
5. [Data Flow](#data-flow)
6. [UI Screens](#ui-screens)
7. [Calibration Process](#calibration-process)
8. [State Management](#state-management)
9. [Extension Functions](#extension-functions)
10. [Testing](#testing)
11. [Usage Examples](#usage-examples)
12. [Troubleshooting](#troubleshooting)

---

## Overview

The Air Mouse Calibration System is a complete, production-ready solution for sensor calibration in Android applications. It provides a modern, intuitive user interface with real-time feedback, animated transitions, and comprehensive sensor data visualization.

### Key Features

- ✅ **3-Step Calibration Process** - Gyroscope, Magnetometer, Accelerometer
- ✅ **Real-time Sensor Visualization** - Live orientation tracking
- ✅ **Animated UI** - Smooth transitions, confetti effects, particle animations
- ✅ **Quality Assessment** - Automatic quality scoring (Excellent/Good/Fair/Poor)
- ✅ **Comprehensive Error Handling** - User-friendly error messages
- ✅ **Haptic Feedback** - Vibration on calibration steps
- ✅ **Data Persistence** - Saves calibration data to preferences
- ✅ **Modern Compose UI** - Material 3 design with dark theme support

---

## Architecture

### Layer Architecture

```
┌─────────────────────────────────────────────────────────────────────┐
│                         PRESENTATION LAYER                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  CalibrationScreen.kt          - Main calibration UI       │   │
│  │  CalibrationResultScreen.kt    - Results screen            │   │
│  │  CalibrationGuideDialog.kt     - Step-by-step guide        │   │
│  │  CalibrationViewModel.kt       - State management          │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                          DOMAIN LAYER                              │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  CalibrationFeature.kt       - Business logic              │   │
│  │  ICalibrationRepository.kt   - Repository interface        │   │
│  │  CalibrationUseCase.kt       - Use case                    │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                          DATA LAYER                                │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  CalibrationRepositoryImpl.kt  - Repository implementation │   │
│  │  CalibrationDataSourceImpl.kt  - Data source               │   │
│  └─────────────────────────────────────────────────────────────┘   │
├─────────────────────────────────────────────────────────────────────┤
│                      INFRASTRUCTURE LAYER                          │
│  ┌─────────────────────────────────────────────────────────────┐   │
│  │  CalibrationHelper.kt         - Sensor calibration logic   │   │
│  │  MadgwickAHRS.kt              - Sensor fusion algorithm    │   │
│  │  SensorManager                - Android sensor framework   │   │
│  └─────────────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────────────┘
```

### Dependency Direction

```
Presentation → Domain ← Data ← Infrastructure
     ↓           ↓        ↓          ↓
     └───────────┴────────┴──────────┘
              (Dependency Injection)
```

---

## Package Structure

```
app/src/main/java/com/airmouse/
│
├── data/
│   ├── datasource/
│   │   └── local/
│   │       ├── CalibrationDao.kt
│   │       ├── CalibrationEntity.kt
│   │       └── CalibrationDataSourceImpl.kt
│   ├── mapper/
│   │   ├── DomainToEntityMapper.kt
│   │   └── EntityToDomainMapper.kt
│   └── repository/
│       └── CalibrationRepositoryImpl.kt
│
├── domain/
│   ├── model/
│   │   └── CalibrationModels.kt
│   ├── repository/
│   │   └── ICalibrationRepository.kt
│   └── usecase/
│       └── CalibrationUseCase.kt
│
├── features/
│   ├── CalibrationFeature.kt
│   └── SensorFeature.kt
│
├── presentation/
│   └── ui/
│       └── calibration/
│           ├── CalibrationScreen.kt
│           ├── CalibrationResultScreen.kt
│           ├── CalibrationGuideDialog.kt
│           ├── CalibrationViewModel.kt
│           ├── CalibrationData.kt
│           ├── CalibrationComponents.kt
│           ├── CalibrationConstants.kt
│           └── CalibrationExtensions.kt
│
├── sensors/
│   ├── CalibrationHelper.kt
│   └── MadgwickAHRS.kt
│
└── utils/
    └── PreferencesManager.kt
```

---

## Core Components

### 1. **CalibrationViewModel**

The ViewModel manages all calibration state and business logic.

```kotlin
class CalibrationViewModel @Inject constructor(
    private val calibrationFeature: CalibrationFeature,
    private val sensorFeature: SensorFeature
) : ViewModel() {
    private val _uiState = MutableStateFlow(CalibrationUiState())
    val uiState: StateFlow<CalibrationUiState> = _uiState.asStateFlow()
    
    fun startFullCalibration() { ... }
    fun startGyroCalibration() { ... }
    fun startMagCalibration() { ... }
    fun startAccelCalibration() { ... }
    fun resetCalibration() { ... }
    fun skipCalibration() { ... }
    fun selectPosition(index: Int) { ... }
}
```

### 2. **CalibrationUiState**

The UI state data class holds all screen state.

```kotlin
data class CalibrationUiState(
    val status: CalibrationStatus = CalibrationStatus.NOT_STARTED,
    val isComplete: Boolean = false,
    val isCollecting: Boolean = false,
    val progress: Int = 0,
    val samplesCollected: Int = 0,
    val totalSamples: Int = 100,
    val currentStep: Int = 1,
    val currentPosition: Int = 0,
    val totalPositions: Int = 4,
    val message: String = "Keep your device steady",
    val errorMessage: String? = null,
    val quality: String = "UNKNOWN",
    val calibrationData: CalibrationData = CalibrationData(),
    val gyroData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magData: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val roll: Float = 0f,
    val pitch: Float = 0f,
    val yaw: Float = 0f
)
```

### 3. **CalibrationData**

The calibration data model stores sensor biases.

```kotlin
data class CalibrationData(
    val gyroBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val accelBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val magBias: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val gyroVariance: Triple<Float, Float, Float> = Triple(0f, 0f, 0f),
    val sampleCount: Int = 0,
    val timestamp: Long = System.currentTimeMillis()
)
```

### 4. **CalibrationQuality**

Quality assessment enum with extensions.

```kotlin
enum class CalibrationQuality {
    EXCELLENT, GOOD, FAIR, POOR, UNKNOWN
}

// Extension functions
fun CalibrationQuality.toColor(): Color { ... }
fun CalibrationQuality.toEmoji(): String { ... }
fun CalibrationQuality.toDescription(): String { ... }
fun CalibrationQuality.toScore(): Int { ... }
fun CalibrationQuality.isAcceptable(): Boolean { ... }
```

---

## Data Flow

### Calibration Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                         CALIBRATION FLOW                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  User Action → ViewModel → UseCase → Repository → DataSource          │
│       ↓           ↓          ↓          ↓            ↓                 │
│  Start Button   State      Business   Data        Database             │
│                 Update     Logic      Access                           │
│                                                                         │
│  ────────────────────────────────────────────────────────────────────── │
│                                                                         │
│  DataSource → Repository → UseCase → ViewModel → UI                    │
│     ↓           ↓           ↓          ↓          ↓                    │
│  Database    Data        Business   State     Compose                  │
│              Storage     Logic      Update    Screen                   │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### State Management Flow

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   User      │    │  ViewModel  │    │   State     │
│   Action    │───▶│  Event      │───▶│   Update    │
└─────────────┘    └─────────────┘    └─────────────┘
                                              │
                                              ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Compose   │◀───│   State     │◀───│   State     │
│   Screen    │    │   Flow      │    │   Container │
└─────────────┘    └─────────────┘    └─────────────┘
```

---

## UI Screens

### 1. **CalibrationScreen**

The main calibration screen with real-time feedback.

```kotlin
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions,
    viewModel: CalibrationViewModel = CalibrationViewModel(),
    onComplete: () -> Unit = {}
) {
    // UI Components:
    // - Progress indicator
    // - Sensor visualizer (3D phone)
    // - Sensor data cards (gyro, accel, mag)
    // - Position guide
    // - Status messages
    // - Action buttons (Start, Skip)
}
```

### 2. **CalibrationResultScreen**

The results screen with animations and quality metrics.

```kotlin
@Composable
fun CalibrationResultScreen(
    quality: String,
    onContinue: () -> Unit,
    onRecalibrate: () -> Unit,
    onShare: (() -> Unit)? = null,
    onViewDetails: (() -> Unit)? = null,
    calibrationData: CalibrationData? = null,
    stats: Map<String, Any>? = null
) {
    // UI Components:
    // - Animated success icon
    // - Quality metrics (Quality, Score, Status)
    // - Expandable details
    // - Action buttons
    // - Confetti effect
}
```

### 3. **CalibrationGuideDialog**

Step-by-step instruction dialog.

```kotlin
@Composable
fun CalibrationGuideDialog(
    step: Int,
    onDismiss: () -> Unit
) {
    // UI Components:
    // - Animated instruction icons
    // - Step descriptions
    // - Dismiss button
}
```

---

## Calibration Process

### Step 1: Gyroscope Calibration

```
┌─────────────────────────────────────────────────────────────────────────┐
│                      GYROSCOPE CALIBRATION                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User places device on flat surface                                 │
│  2. System collects 500 samples (5 seconds)                            │
│  3. Calculates bias (average of samples)                               │
│  4. Saves gyro bias to preferences                                     │
│  5. Quality assessed based on variance                                 │
│                                                                         │
│  Data Collected: Gyroscope readings (X, Y, Z)                          │
│  Output: Gyroscope bias (offset values)                                │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Step 2: Magnetometer Calibration

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    MAGNETOMETER CALIBRATION                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User moves device in figure-8 pattern                              │
│  2. System collects 300 samples (8 seconds)                            │
│  3. Finds min/max values for each axis                                 │
│  4. Calculates hard-iron offsets                                       │
│  5. Saves magnetometer offsets to preferences                          │
│                                                                         │
│  Data Collected: Magnetometer readings (X, Y, Z)                       │
│  Output: Hard-iron offsets                                             │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Step 3: Accelerometer Calibration

```
┌─────────────────────────────────────────────────────────────────────────┐
│                   ACCELEROMETER CALIBRATION                            │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  1. User places device in 6 orientations                                │
│  2. System collects 50 samples per orientation                         │
│  3. Calculates scale and offset for each axis                          │
│  4. Saves accelerometer calibration to preferences                     │
│                                                                         │
│  Orientations: Flat Up, Flat Down, Left, Right, Top, Bottom            │
│  Output: Scale and offset values                                       │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

### Quality Assessment

```
┌─────────────────────────────────────────────────────────────────────────┐
│                       QUALITY ASSESSMENT                               │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  Gyroscope Variance:                                                   │
│  - < 0.01   → EXCELLENT                                                │
│  - < 0.05   → GOOD                                                     │
│  - < 0.1    → FAIR                                                     │
│  - ≥ 0.1    → POOR                                                     │
│                                                                         │
│  Overall Quality = Min(Individual Qualities)                           │
│                                                                         │
│  Score Mapping:                                                        │
│  - EXCELLENT → 95%                                                     │
│  - GOOD      → 80%                                                     │
│  - FAIR      → 60%                                                     │
│  - POOR      → 30%                                                     │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## State Management

### UI State Flow

```kotlin
// Initial State
CalibrationUiState(
    status = CalibrationStatus.NOT_STARTED,
    progress = 0,
    samplesCollected = 0,
    totalSamples = 100,
    message = "Keep your device steady"
)

// During Calibration
CalibrationUiState(
    status = CalibrationStatus.IN_PROGRESS,
    progress = 45,
    samplesCollected = 45,
    totalSamples = 100,
    message = "Calibrating gyroscope..."
)

// Completed
CalibrationUiState(
    status = CalibrationStatus.COMPLETED,
    progress = 100,
    samplesCollected = 100,
    totalSamples = 100,
    isComplete = true,
    quality = "EXCELLENT",
    message = "Calibration complete!"
)
```

### Event Handling

```kotlin
sealed class CalibrationEvent {
    object StartFullCalibration : CalibrationEvent()
    object StartGyroCalibration : CalibrationEvent()
    object StartMagCalibration : CalibrationEvent()
    object StartAccelCalibration : CalibrationEvent()
    object ResetCalibration : CalibrationEvent()
    object SkipCalibration : CalibrationEvent()
    data class SelectPosition(val index: Int) : CalibrationEvent()
    data class ShowToast(val message: String) : CalibrationEvent()
    data class ShowError(val message: String) : CalibrationEvent()
    object NavigateToHome : CalibrationEvent()
}
```

---

## Extension Functions

### CalibrationQuality Extensions

```kotlin
fun CalibrationQuality.toColor(): Color
fun CalibrationQuality.toEmoji(): String
fun CalibrationQuality.toDescription(): String
fun CalibrationQuality.toDisplayName(): String
fun CalibrationQuality.toScore(): Int
fun CalibrationQuality.isAcceptable(): Boolean
fun CalibrationQuality.getRecommendation(): String
fun CalibrationQuality.isUsable(): Boolean
```

### Float Extensions

```kotlin
fun Float.formatCalibrationValue(): String
fun Float.toPercentage(): String
fun Float.formatDistance(): String
fun Float.formatSpeed(): String
fun Float.formatAngle(): String
fun Float.clamp(min: Float, max: Float): Float
fun Float.isWithinTolerance(target: Float, tolerance: Float): Boolean
fun Float.mapRange(fromLow: Float, fromHigh: Float, toLow: Float, toHigh: Float): Float
```

### Sensor Data Helpers

```kotlin
fun isSensorDataStable(data: List<Float>, threshold: Float): Boolean
fun calculateConfidenceFromStability(variance: Float): Float
fun formatSensorValue(value: Float, type: String): String
fun getCalibrationQualityFromVariance(variance: Float): CalibrationQuality
fun getCalibrationQualityFromScore(score: Int): CalibrationQuality
fun getCalibrationRecommendation(quality: CalibrationQuality): String
```

---

## Testing

### Unit Tests

```kotlin
@RunWith(MockitoJUnitRunner::class)
class CalibrationViewModelTest {
    
    @Mock
    lateinit var calibrationFeature: CalibrationFeature
    
    @Mock
    lateinit var sensorFeature: SensorFeature
    
    lateinit var viewModel: CalibrationViewModel
    
    @Before
    fun setUp() {
        viewModel = CalibrationViewModel(calibrationFeature, sensorFeature)
    }
    
    @Test
    fun `test start full calibration`() {
        viewModel.startFullCalibration()
        assertThat(viewModel.uiState.value.isCollecting).isTrue()
    }
    
    @Test
    fun `test reset calibration`() {
        viewModel.resetCalibration()
        assertThat(viewModel.uiState.value.isComplete).isFalse()
    }
}
```

### UI Tests

```kotlin
@RunWith(AndroidJUnit4::class)
class CalibrationScreenTest {
    
    @get:Rule
    val composeTestRule = createComposeRule()
    
    @Test
    fun testCalibrationScreenDisplays() {
        composeTestRule.setContent {
            CalibrationScreen(
                navigationActions = mock(),
                viewModel = mock()
            )
        }
        composeTestRule.onNodeWithText("Sensor Calibration").assertIsDisplayed()
    }
}
```

---

## Usage Examples

### 1. Start Full Calibration

```kotlin
// In your activity or fragment
@Inject
lateinit var viewModel: CalibrationViewModel

// Start calibration
viewModel.startFullCalibration()

// Observe state
lifecycleScope.launch {
    viewModel.uiState.collect { state ->
        if (state.isComplete) {
            navigateToResultScreen(state.quality)
        }
    }
}
```

### 2. Custom Calibration Steps

```kotlin
// Gyroscope only
viewModel.startGyroCalibration()

// Magnetometer only
viewModel.startMagCalibration()

// Accelerometer only
viewModel.startAccelCalibration()
```

### 3. Using Extensions

```kotlin
val quality = CalibrationQuality.EXCELLENT
val color = quality.toColor() // Green
val emoji = quality.toEmoji() // 🌟
val text = quality.toDisplayName() // "Excellent"

val value = 12.3456f
value.formatCalibrationValue() // "12.35"
value.toPercentage() // "1234.6%"
value.formatDistance() // "12.35 m"
```

---

## Troubleshooting

### Common Issues and Solutions

| Issue | Cause | Solution |
|-------|-------|----------|
| **Calibration fails** | Sensor not available | Check sensor availability |
| **High variance** | Device moving during calibration | Keep device still |
| **No data collected** | Permission denied | Request sensor permissions |
| **Quality poor** | Environmental interference | Recalibrate in clean environment |
| **UI freezing** | Heavy computation on main thread | Use coroutines for sensor processing |

### Debugging Commands

```bash
# Log calibration events
adb logcat | grep "Calibration"

# Check sensor availability
adb shell dumpsys sensorservice

# Reset calibration data
adb shell pm clear com.airmouse
```

---

## Summary

| Component | Purpose | Key Features |
|-----------|---------|--------------|
| **CalibrationViewModel** | State management | Events, StateFlow, Coroutines |
| **CalibrationScreen** | Main UI | Progress, Sensor data, Controls |
| **CalibrationResultScreen** | Results | Animations, Quality metrics, Actions |
| **CalibrationData** | Data model | Sensor biases, Quality |
| **CalibrationExtensions** | Helpers | Formatting, Conversions, Quality assessment |
| **CalibrationComponents** | Reusable UI | Cards, Indicators, Animations |
| **CalibrationConstants** | Configuration | Steps, Thresholds, Colors |

---

## Version History

| Version | Date | Changes |
|---------|------|---------|
| 3.0.0 | 2025-01-15 | Initial release |
| | | Complete calibration UI |
| | | Real-time sensor visualization |
| | | Quality assessment |
| | | Animated transitions |

---

**Built with Jetpack Compose, Kotlin Coroutines, and Material 3**