# 📘 Air Mouse Calibration UI Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.ui.calibration` package contains the **complete calibration user interface** for the Air Mouse application. This package provides a step-by-step wizard for calibrating gyroscope, magnetometer, and accelerometer sensors with real-time feedback, animations, and quality assessment.

```
com.airmouse.presentation.ui.calibration/
├── CalibrationScreen.kt              # Main calibration screen
├── CalibrationResultScreen.kt        # Calibration results screen
├── CalibrationGuideDialog.kt         # Step-by-step guide dialog
├── CalibrationViewModel.kt           # Calibration ViewModel
├── CalibrationUiState.kt             # Calibration state models
├── CalibrationComponents.kt          # Reusable calibration UI components
├── CalibrationProcessScreen.kt       # Process screen (legacy/stub)
└── CalibrationGuideDialog.kt         # Guide dialog (duplicate/legacy)
```

---

## 🎯 1. CalibrationScreen

### Purpose
The **main calibration screen** that guides users through the entire calibration process. It displays step-by-step instructions, real-time sensor data, progress indicators, and interactive controls.

### Key Features

| Feature | Description |
|---------|-------------|
| **3-Step Process** | Gyroscope → Magnetometer → Accelerometer |
| **Live Sensor Data** | Real-time gyro, accel, mag values display |
| **Progress Tracking** | Step indicators and progress bars |
| **Interactive Controls** | Start, Next, Cancel, Finish buttons |
| **Animations** | Pulsing rings, confetti, countdown animations |
| **Quality Assessment** | Automatic quality scoring (Excellent/Good/Fair/Poor) |
| **Error Handling** | User-friendly error messages and retry options |

### Screen Structure

```kotlin
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CalibrationScreen(
    navigationActions: NavigationActions? = null,
    viewModel: CalibrationViewModel = hiltViewModel(),
    onComplete: () -> Unit = {}
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val isCalibrating by viewModel.isCalibrating.collectAsStateWithLifecycle()
    val calibrationData by viewModel.calibrationData.collectAsStateWithLifecycle()
    val phase = uiState.calibrationPhase

    Scaffold(
        topBar = { /* TopAppBar with title and navigation */ }
    ) { paddingValues ->
        when {
            uiState.isComplete -> CompletionScreen(...)
            phase == CalibrationPhase.INTRO -> CalibrationStepIntroScreen(...)
            phase == CalibrationPhase.COUNTDOWN -> CalibrationStepIntroScreen(...)
            phase == CalibrationPhase.SAMPLING -> CalibrationSamplingScreen(...)
        }
    }
}
```

### Phases

| Phase | Description | UI Elements |
|-------|-------------|-------------|
| **INTRO** | Show step information and instructions | Hero card, step tracker, start button |
| **COUNTDOWN** | Countdown before sampling starts | Pulsing animation, progress indicator |
| **SAMPLING** | Collect sensor data with live progress | Live sensor values, progress bar, cancel button |

---

## 🎯 2. CalibrationResultScreen

### Purpose
Displays the **calibration results** with quality assessment, sensor data summary, and actions to continue or recalibrate.

### Key Features

| Feature | Description |
|---------|-------------|
| **Quality Badge** | Visual quality indicator (Excellent/Good/Fair/Poor) |
| **Sensor Summary** | Gyro bias, accel offset, mag offset values |
| **Status Display** | Calibration status (Complete/Failed/Incomplete) |
| **Action Buttons** | Continue, Recalibrate, Share, Export |

### Result Card

```kotlin
@Composable
private fun ResultCard(
    calibrationData: CalibrationData?,
    calibrationStatus: CalibrationStatus
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Status", fontWeight = FontWeight.Bold)
            Text(calibrationStatus.name)
            Text("Data: ${if (calibrationData?.isCalibrated == true) "Saved" else "Missing"}")
        }
    }
}
```

---

## 🎯 3. CalibrationGuideDialog

### Purpose
An **animated step-by-step guide dialog** that walks users through each calibration step with visual instructions and real-time feedback.

### Key Features

| Feature | Description |
|---------|-------------|
| **Step Animations** | Unique animations for each step |
| **Instruction Display** | Clear step-by-step instructions |
| **Live Progress** | Real-time progress tracking |
| **Interactive Controls** | Start, Next, Cancel, Finish |
| **Visual Feedback** | Animated icons, status chips |

### Step Animations

#### Gyroscope Instruction
```kotlin
@Composable
fun GyroscopeInstructionAnimation(frame: Int) {
    val rotation = when (frame % 3) {
        0 -> 0f
        1 -> 3f
        else -> -3f
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("📱", fontSize = 64.sp, modifier = Modifier.rotate(rotation))
        Text("KEEP STILL", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
```

#### Magnetometer Instruction
```kotlin
@Composable
fun MagnetometerInstructionAnimation(frame: Int) {
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000, easing = LinearEasing))
    )
    Text("∞", fontSize = 80.sp, color = Color(0xFF6366F1), modifier = Modifier.rotate(rotation))
}
```

#### Accelerometer Instruction
```kotlin
@Composable
fun AccelerometerInstructionAnimation(position: Int) {
    val orientations = listOf("⬆️ Top", "⬇️ Bottom", "⬅️ Left", "➡️ Right", "🔄 Front", "↩️ Back")
    val current = orientations.getOrElse(position % 6) { "📱" }
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(current.split(" ")[0], fontSize = 64.sp)
        Text(current.split(" ").getOrElse(1) { "" }, fontSize = 14.sp)
    }
}
```

#### Completion Animation
```kotlin
@Composable
fun CompletedInstructionAnimation() {
    val scale by animateFloatAsState(
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(tween(1000), repeatMode = RepeatMode.Reverse)
    )
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text("🎉", fontSize = 72.sp, modifier = Modifier.scale(scale))
        Text("READY TO GO", fontSize = 12.sp, fontWeight = FontWeight.Bold)
    }
}
```

---

## 🎯 4. CalibrationViewModel

### Purpose
Manages the **calibration state and logic**, coordinating sensor sampling, data persistence, and UI updates.

### Key State Properties

| Property | Type | Description |
|----------|------|-------------|
| `uiState` | `StateFlow<CalibrationUiState>` | Complete UI state |
| `isCalibrating` | `StateFlow<Boolean>` | Whether calibration is in progress |
| `calibrationData` | `StateFlow<CalibrationData?>` | Current calibration data |
| `calibrationStatus` | `StateFlow<CalibrationStatus>` | Current calibration status |
| `calibrationProgress` | `StateFlow<Int>` | Progress percentage (0-100) |

### Key Methods

| Method | Purpose |
|--------|---------|
| `startCalibration()` | Start the full calibration process |
| `beginCurrentStep()` | Begin the current calibration step |
| `startCurrentStep()` | Start sensor sampling for current step |
| `nextStep()` | Advance to the next calibration step |
| `completeCalibration()` | Finalize and save calibration data |
| `resetCalibration()` | Reset all calibration data |
| `skipCalibration()` | Skip calibration (use defaults) |
| `syncToServer()` | Sync calibration to PC server |

### Sampling Constants

```kotlin
companion object {
    private const val GYRO_SAMPLES_NEEDED = 250        // 5 seconds at 50Hz
    private const val MAG_SAMPLES_NEEDED = 500         // 10 seconds at 50Hz
    private const val ACCEL_SAMPLES_PER_POS = 100      // 2 seconds per position
    private const val ACCEL_POSITIONS_NEEDED = 6
    private const val TRANSITION_DELAY = 1000L
    private const val SENSOR_TIMEOUT_EXTRA_MS = 2500L
    private const val GRAVITY_EARTH = 9.80665f
}
```

### Calibration Steps

| Step | Sensor | Samples | Duration | Position |
|------|--------|---------|----------|----------|
| 1 | Gyroscope | 250 | 5 seconds | Stationary |
| 2 | Magnetometer | 500 | 10 seconds | Figure-8 motion |
| 3 | Accelerometer | 100 × 6 | 12 seconds | 6 positions |

### Accelerometer Positions

```kotlin
private val accelPositionsList = listOf(
    "Flat (Screen Up)",
    "Flat (Screen Down)",
    "Left Side Down",
    "Right Side Down",
    "Top Edge Down",
    "Bottom Edge Down"
)
```

### Quality Calculation

```kotlin
private fun calculateCalibrationQuality(): CalibrationQuality {
    val gyroVariance = gyroSamples.variance { it.first } +
        gyroSamples.variance { it.second } +
        gyroSamples.variance { it.third }
    val hasAllSamples = gyroSamples.size >= GYRO_SAMPLES_NEEDED &&
        magSamples.size >= MAG_SAMPLES_NEEDED &&
        accelPositions.size >= ACCEL_POSITIONS_NEEDED
    return when {
        !hasAllSamples -> CalibrationQuality.FAIR
        gyroVariance < 0.001f -> CalibrationQuality.EXCELLENT
        gyroVariance < 0.01f -> CalibrationQuality.GOOD
        else -> CalibrationQuality.FAIR
    }
}
```

---

## 🎯 5. CalibrationUiState

### Purpose
Defines the **complete state model** for the calibration UI.

### Properties

| Property | Type | Description |
|----------|------|-------------|
| `currentStep` | Int | Current step number (0-3) |
| `totalSteps` | Int | Total steps (3) |
| `calibrationPhase` | `CalibrationPhase` | Current phase (INTRO/COUNTDOWN/SAMPLING) |
| `isCalibrating` | Boolean | Whether calibration is in progress |
| `isCollecting` | Boolean | Whether samples are being collected |
| `isComplete` | Boolean | Whether calibration is complete |
| `isSkipped` | Boolean | Whether calibration was skipped |
| `isCalibrationApplied` | Boolean | Whether calibration is applied |
| `progress` | Int | Overall progress (0-100) |
| `stepProgress` | Float | Current step progress (0-1) |
| `samplesCollected` | Int | Samples collected in current step |
| `totalSamplesNeeded` | Int | Total samples needed for current step |
| `statusMessage` | String | Current status message |
| `stepInstruction` | String | Instruction for current step |
| `detailedInstruction` | String | Detailed instruction text |
| `errorMessage` | String? | Error message if any |
| `calibrationQuality` | String | Quality rating |
| `quality` | String | Alias for calibrationQuality |
| `showConfetti` | Boolean | Whether to show confetti animation |
| `currentPosition` | Int | Current accelerometer position (0-5) |
| `totalPositions` | Int | Total positions (6) |
| `completedPositions` | List<Int> | Completed positions |
| `roll` | Float | Current roll angle |
| `pitch` | Float | Current pitch angle |
| `yaw` | Float | Current yaw angle |
| `gyroData` | `Triple<Float,Float,Float>` | Current gyroscope data |
| `accelData` | `Triple<Float,Float,Float>` | Current accelerometer data |
| `magData` | `Triple<Float,Float,Float>` | Current magnetometer data |
| `isServerConnected` | Boolean | Whether server is connected |
| `calibrationData` | `CalibrationData?` | Complete calibration data |

### Enums

```kotlin
enum class CalibrationPhase {
    INTRO,      // Showing step information
    COUNTDOWN,  // Countdown before sampling
    SAMPLING    // Actively collecting samples
}
```

---

## 🎯 6. CalibrationComponents

### Purpose
Provides **reusable UI components** for the calibration flow.

### Component Categories

#### Glass Card
```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```
A **glassmorphism card** with translucent background and blur effect.

#### Calibration Progress Indicator
```kotlin
@Composable
fun CalibrationProgressIndicator(
    progress: Int,
    totalSteps: Int = 4,
    currentStep: Int = 1,
    modifier: Modifier = Modifier
)
```
**Step-by-step progress** indicator with numbered steps and connectors.

#### Calibration Status Chip
```kotlin
@Composable
fun CalibrationStatusChip(
    status: CalibrationStatus,
    modifier: Modifier = Modifier
)
```
**Status chip** displaying current calibration state with color coding.

| Status | Color | Display |
|--------|-------|---------|
| IN_PROGRESS | Amber | "In Progress" |
| COMPLETED | Green | "✓ Complete" |
| FAILED | Red | "❌ Failed" |
| SKIPPED | Gray | "⏭️ Skipped" |
| IDLE | Purple | "Idle" |

#### Calibration Quality Indicator
```kotlin
@Composable
fun CalibrationQualityIndicator(
    quality: CalibrationQuality,
    modifier: Modifier = Modifier,
    showEmoji: Boolean = true
)
```
**Quality badge** with emoji and color coding.

| Quality | Color | Emoji | Label |
|---------|-------|-------|-------|
| EXCELLENT | Green | 🌟 | Excellent |
| GOOD | Blue | 👍 | Good |
| FAIR | Amber | ⚠️ | Fair |
| POOR | Red | ❌ | Poor |
| UNKNOWN | Gray | ❓ | Unknown |

#### Animated Checkmark
```kotlin
@Composable
fun AnimatedCheckmark(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 64
)
```
**Animated success checkmark** with bounce effect.

#### Animated Loading Spinner
```kotlin
@Composable
fun AnimatedLoadingSpinner(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 48
)
```
**Animated loading spinner** for in-progress states.

#### Calibration Instruction Card
```kotlin
@Composable
fun CalibrationInstructionCard(
    title: String,
    instruction: String,
    description: String,
    modifier: Modifier = Modifier,
    icon: String = "📱"
)
```
**Instruction card** with icon and detailed guidance.

#### Pulse Animation
```kotlin
@Composable
fun PulseAnimation(
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    size: Int = 16,
    color: Color = Color(0xFF6366F1)
)
```
**Pulsing dot animation** for live indicators.

#### Calibration Sensor Visualizer
```kotlin
@Composable
fun CalibrationSensorVisualizer(
    roll: Float = 0f,
    pitch: Float = 0f,
    yaw: Float = 0f,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
)
```
**3D sensor visualization** showing device orientation.

#### Calibration Stats Row
```kotlin
@Composable
fun CalibrationStatsRow(
    gyroX: Float,
    gyroY: Float,
    gyroZ: Float,
    accelX: Float,
    accelY: Float,
    accelZ: Float,
    modifier: Modifier = Modifier,
    showLabels: Boolean = true
)
```
**Sensor statistics** display for gyroscope and accelerometer.

#### Calibration Action Button
```kotlin
@Composable
fun CalibrationActionButton(
    text: String,
    onClick: () -> Unit,
    isLoading: Boolean = false,
    enabled: Boolean = true,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null
)
```
**Primary action button** with loading state.

#### Calibration Steps Guide
```kotlin
@Composable
fun CalibrationStepsGuide(
    steps: List<Pair<String, String>>,
    currentStep: Int,
    modifier: Modifier = Modifier,
    showIcons: Boolean = true
)
```
**Step-by-step guide** with icons and status.

#### Calibration Summary Card
```kotlin
@Composable
fun CalibrationSummaryCard(
    title: String = "Calibration Summary",
    items: List<Pair<String, String>>,
    modifier: Modifier = Modifier,
    quality: CalibrationQuality = CalibrationQuality.UNKNOWN,
    status: CalibrationStatus = CalibrationStatus.NOT_STARTED
)
```
**Calibration summary** with key metrics.

#### Confetti Effect
```kotlin
@Composable
fun ConfettiEffect()
```
**Celebration confetti** animation for completion.

#### Calibration Status Card
```kotlin
@Composable
fun CalibrationStatusCard(
    calibrationData: CalibrationData?,
    modifier: Modifier = Modifier
)
```
**Detailed calibration status** with sensor data.

---

## 📊 Calibration Flow Diagram

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CALIBRATION FLOW                                    │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  START → Step 1: Gyroscope                                             │
│         │                                                               │
│         ├── INTRO: Show instruction                                    │
│         │                                                               │
│         ├── COUNTDOWN: 3-2-1 animation                                 │
│         │                                                               │
│         └── SAMPLING: Collect 250 samples (5 seconds)                 │
│               │                                                         │
│               ├── Calculate bias (average)                            │
│               ├── Calculate variance                                   │
│               └── Save to preferences                                 │
│                                                                         │
│  Step 2: Magnetometer                                                  │
│         │                                                               │
│         ├── INTRO: Show instruction                                    │
│         │                                                               │
│         ├── COUNTDOWN: 3-2-1 animation                                 │
│         │                                                               │
│         └── SAMPLING: Collect 500 samples (10 seconds)                │
│               │                                                         │
│               ├── Calculate min/max per axis                          │
│               ├── Calculate offset (min+max)/2                        │
│               ├── Calculate scale                                      │
│               └── Save to preferences                                 │
│                                                                         │
│  Step 3: Accelerometer (6 positions)                                   │
│         │                                                               │
│         ├── INTRO: Show instruction                                    │
│         │                                                               │
│         ├── For each position (0-5):                                   │
│         │   ├── COUNTDOWN: 3-2-1 animation                            │
│         │   ├── SAMPLING: Collect 100 samples (2 seconds)             │
│         │   └── Position complete                                      │
│         │                                                               │
│         └── After all 6 positions:                                     │
│               ├── Calculate offsets per axis                          │
│               ├── Calculate scales per axis                           │
│               └── Save to preferences                                 │
│                                                                         │
│  Complete:                                                              │
│         │                                                               │
│         ├── Calculate overall quality                                  │
│         ├── Save full calibration data                                │
│         └── Show results                                               │
│                                                                         │
└─────────────────────────────────────────────────────────────────────────┘
```

---

## 🎨 Theme Integration

All components use the `LocalThemeColors` CompositionLocal for theming:

```kotlin
val colors = LocalThemeColors.current

// Used for:
Surface(
    color = colors.primary,
    contentColor = colors.onPrimary
)
```

---

## 📋 Component Dependencies

| Component | Dependencies |
|-----------|--------------|
| `GlassCard` | `MaterialTheme`, `LocalThemeColors` |
| `CalibrationStatusChip` | `CalibrationStatus` enum, `LocalThemeColors` |
| `CalibrationQualityIndicator` | `CalibrationQuality` enum |
| `AnimatedCheckmark` | `animateFloatAsState`, `spring` |
| `CalibrationSensorVisualizer` | `graphicsLayer`, `Canvas` |
| `ConfettiEffect` | `rememberInfiniteTransition`, `Canvas` |

---

## ✅ Key Design Principles

| Principle | Implementation |
|-----------|----------------|
| **Step-by-Step Guidance** | Clear phases with instructions |
| **Real-time Feedback** | Live sensor data display |
| **Visual Progress** | Step indicators and progress bars |
| **Quality Assessment** | Automatic quality scoring |
| **Error Handling** | User-friendly error messages |
| **Accessibility** | Clear labels and instructions |
| **Consistency** | Unified design language |
| **Animations** | Smooth transitions and feedback |

---

**The Calibration UI package provides a complete, polished, and user-friendly interface for sensor calibration, with rich animations, real-time feedback, and clear guidance for each step.**