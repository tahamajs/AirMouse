# 📘 Air Mouse Calibration UI Package – Complete Documentation

## 📁 Package Overview

The `com.airmouse.presentation.calibration` package contains **UI components and utilities** specific to the calibration flow. These components provide a rich, interactive, and user-friendly interface for sensor calibration.

```
com.airmouse.presentation.calibration/
├── CalibrationComponents.kt          # Reusable calibration UI components
└── CalibrationGuideDialog.kt         # Step-by-step calibration guide dialog
```

---

## 🎯 1. CalibrationComponents

### Purpose
Provides a comprehensive library of **reusable UI components** for the calibration flow, including status indicators, progress trackers, quality badges, and interactive visualizations.

### Component Categories

| Category | Components |
|----------|------------|
| **Containers** | GlassCard, CalibrationInstructionCard, CalibrationSummaryCard |
| **Progress** | CalibrationProgressIndicator, AnimatedLoadingSpinner, CircularProgressWithLabel |
| **Status** | CalibrationStatusChip, CalibrationQualityIndicator, PulseAnimation |
| **Visualization** | CalibrationSensorVisualizer, SensorAxisCard, GyroscopeVisualizer |
| **Interaction** | CalibrationActionButton, AnimatedSwitch, AnimatedCheckbox |
| **Feedback** | ConfettiEffect, AnimatedCheckmark, SuccessMessageBanner |

---

## 📦 Component Documentation

### 1. GlassCard

A **glassmorphism card** with translucent background and subtle blur effect.

```kotlin
@Composable
fun GlassCard(
    modifier: Modifier = Modifier,
    content: @Composable ColumnScope.() -> Unit
)
```

**Features:**
- Translucent background with alpha
- Smooth gradient overlay
- Rounded corners (16dp)
- Full-width responsive

**Use Case:** Wrapping calibration steps or status information.

---

### 2. CalibrationProgressIndicator

**Step-by-step progress** indicator for the calibration flow.

```kotlin
@Composable
fun CalibrationProgressIndicator(
    progress: Int,
    totalSteps: Int = 4,
    currentStep: Int = 1,
    modifier: Modifier = Modifier
)
```

**Features:**
- Visual step indicators with numbers
- Complete/active/inactive states
- Connector lines between steps
- Step labels

**Visual States:**
- Complete: Green checkmark ✓
- Active: Purple fill with pulse
- Inactive: Dark gray fill

---

### 3. CalibrationStatusChip

**Status chip** displaying the current calibration state.

```kotlin
@Composable
fun CalibrationStatusChip(
    status: CalibrationStatus,
    modifier: Modifier = Modifier
)
```

**Status Colors:**
| Status | Color | Display |
|--------|-------|---------|
| IN_PROGRESS | Amber | "In Progress" |
| COMPLETED | Green | "✓ Complete" |
| FAILED | Red | "❌ Failed" |
| SKIPPED | Gray | "⏭️ Skipped" |
| IDLE | Purple | "Idle" |

---

### 4. CalibrationQualityIndicator

**Quality badge** showing the calibration quality with emoji.

```kotlin
@Composable
fun CalibrationQualityIndicator(
    quality: CalibrationQuality,
    modifier: Modifier = Modifier,
    showEmoji: Boolean = true
)
```

**Quality Mapping:**
| Quality | Color | Emoji | Label |
|---------|-------|-------|-------|
| EXCELLENT | Green (0xFF10B981) | 🌟 | Excellent |
| GOOD | Blue (0xFF3B82F6) | 👍 | Good |
| FAIR | Amber (0xFFF59E0B) | ⚠️ | Fair |
| POOR | Red (0xFFEF4444) | ❌ | Poor |
| UNKNOWN | Gray (0xFF64748B) | ❓ | Unknown |

---

### 5. AnimatedCheckmark

**Animated success checkmark** with bounce effect.

```kotlin
@Composable
fun AnimatedCheckmark(
    isVisible: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 64
)
```

**Animation Features:**
- Spring-based scale animation
- Fade-in effect
- Circular background with pulse
- Configurable size

---

### 6. AnimatedLoadingSpinner

**Animated loading spinner** for calibration in progress.

```kotlin
@Composable
fun AnimatedLoadingSpinner(
    isActive: Boolean,
    modifier: Modifier = Modifier,
    size: Int = 48
)
```

**Features:**
- Continuous rotation when active
- Smooth circular progress
- Semi-transparent background
- Configurable size

---

### 7. CalibrationInstructionCard

**Instruction card** with icon and detailed guidance.

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

**Features:**
- Emoji icon
- Title with accent color
- Instruction text
- Description text
- Border with subtle glow

---

### 8. PulseAnimation

**Pulsing dot animation** for live indicators.

```kotlin
@Composable
fun PulseAnimation(
    isActive: Boolean = true,
    modifier: Modifier = Modifier,
    size: Int = 16,
    color: Color = Color(0xFF6366F1)
)
```

**Animation Features:**
- Scale animation (1.0 → 1.3)
- Alpha animation (1.0 → 0.3)
- Configurable color
- Infinite repeat

---

### 9. CalibrationSensorVisualizer

**3D sensor visualization** showing device orientation.

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

**Features:**
- 3D rotation of a phone representation
- Live roll, pitch, yaw tracking
- Crosshair overlay
- Device label

---

### 10. CalibrationStatsRow

**Sensor statistics** display for gyroscope and accelerometer.

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

**Features:**
- Gyroscope values (red)
- Accelerometer values (blue)
- Formatted with 2 decimal places
- Compact card layout

---

### 11. CalibrationActionButton

**Primary action button** for calibration flow.

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

**Features:**
- Loading state with spinner
- Icon support
- Disabled state
- Rounded corners (16dp)
- Full-width responsive

---

### 12. CalibrationStepsGuide

**Step-by-step guide** with icons and status.

```kotlin
@Composable
fun CalibrationStepsGuide(
    steps: List<Pair<String, String>>,
    currentStep: Int,
    modifier: Modifier = Modifier,
    showIcons: Boolean = true
)
```

**Features:**
- Step numbers with icons
- Active step highlighting
- Completed step checkmarks
- Animated transitions

**Icons:**
| Step | Icon |
|------|------|
| 1 | 📱 |
| 2 | 🔄 |
| 3 | 📐 |
| 4 | ✅ |

---

### 13. CalibrationSummaryCard

**Calibration summary** with key metrics.

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

**Features:**
- Quality indicator
- Status chip
- Key-value pairs
- Border with glow

---

### 14. ConfettiEffect

**Celebration confetti** animation for calibration completion.

```kotlin
@Composable
fun ConfettiEffect()
```

**Features:**
- 20+ colored particles
- Continuous animation
- Random colors
- Infinite repeat

**Colors:** Purple, Green, Amber, Red, Blue, Pink

---

### 15. CalibrationStatusCard

**Detailed calibration status** with sensor data.

```kotlin
@Composable
fun CalibrationStatusCard(
    calibrationData: CalibrationData?,
    modifier: Modifier = Modifier
)
```

**Features:**
- Gyroscope bias display
- Accelerometer offset
- Magnetometer offset
- Quality indicator
- Calibrated status

---

## 🎯 2. CalibrationGuideDialog

### Purpose
A **step-by-step guide dialog** that walks users through each calibration step with animations and clear instructions.

### Features

| Feature | Description |
|---------|-------------|
| **Animated Instructions** | Step-specific animations for each calibration phase |
| **Progress Tracking** | Shows current step progress |
| **Live Sensor Data** | Displays real-time sensor values during sampling |
| **Interactive Controls** | Start, Next, Cancel, Finish buttons |
| **Visual Feedback** | Animated icons, progress bars, and status messages |
| **Glassmorphism Design** | Modern translucent UI with blur |

### Dialog Structure

```kotlin
@Composable
fun CalibrationGuideDialog(
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    initialStep: Int = 0,
    viewModel: CalibrationViewModel = hiltViewModel()
)
```

### Step Animations

#### Step 1: Gyroscope
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

#### Step 2: Magnetometer
```kotlin
@Composable
fun MagnetometerInstructionAnimation(frame: Int) {
    val rotation by animateFloatAsState(
        targetValue = 360f,
        animationSpec = infiniteRepeatable(tween(3000))
    )
    Text("∞", fontSize = 80.sp, color = Color(0xFF6366F1), modifier = Modifier.rotate(rotation))
}
```

#### Step 3: Accelerometer
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

#### Completion
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

## 📊 Component Usage Flow

```
┌─────────────────────────────────────────────────────────────────────────┐
│                    CALIBRATION UI FLOW                                 │
├─────────────────────────────────────────────────────────────────────────┤
│                                                                         │
│  CalibrationScreen.kt                                                  │
│         │                                                               │
│         ▼                                                               │
│  CalibrationGuideDialog.kt (shown when user starts)                   │
│         │                                                               │
│         ├── Step 1: Gyroscope                                          │
│         │   ├── GyroscopeInstructionAnimation                         │
│         │   ├── CalibrationStatusChip (IN_PROGRESS)                  │
│         │   └── CalibrationProgressIndicator                         │
│         │                                                               │
│         ├── Step 2: Magnetometer                                       │
│         │   ├── MagnetometerInstructionAnimation                      │
│         │   ├── CalibrationStatusChip (MAG_COMPLETE)                 │
│         │   └── CalibrationProgressIndicator                         │
│         │                                                               │
│         ├── Step 3: Accelerometer                                      │
│         │   ├── AccelerometerInstructionAnimation                     │
│         │   ├── CalibrationStatusChip (ACCEL_COMPLETE)               │
│         │   └── CalibrationProgressIndicator                         │
│         │                                                               │
│         └── Completion                                                 │
│             ├── CompletedInstructionAnimation                         │
│             ├── ConfettiEffect                                        │
│             ├── CalibrationQualityIndicator                          │
│             └── CalibrationSummaryCard                               │
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
| **Reusability** | Components are isolated and composable |
| **Theming** | All colors adapt to current theme |
| **Responsive** | Components adapt to screen size |
| **Animated** | Smooth transitions and feedback |
| **Accessible** | Semantic content and labels |
| **Consistent** | Unified design language |
| **Interactive** | Real-time sensor visualization |

---

**The Calibration UI package provides a complete, polished, and user-friendly interface for sensor calibration, with rich animations, real-time feedback, and clear guidance for each step.**