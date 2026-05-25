# Air Mouse – `res/` Folder Complete Documentation

This document provides a **complete, in‑depth explanation** of every resource file in the `res/` folder of the Air Mouse Android application. The resources include layouts, drawables, colours, strings, and themes. All IDs are carefully named to match the Kotlin code in `MainActivity`, `CalibrationFragment`, `SettingsFragment`, and `DebugOverlayService`.

---

## 📁 Folder Structure

```
res/
├── drawable/
│   └── green_square.xml          # Shape drawable for the orientation indicator
├── layout/
│   ├── activity_main.xml         # Main screen layout
│   ├── fragment_calibration.xml  # Calibration dialog layout
│   ├── fragment_settings.xml     # Settings dialog layout
│   └── debug_overlay.xml         # Floating overlay layout
├── values/
│   ├── colors.xml                # Colour palette (Material Design 3)
│   ├── strings.xml               # All user‑visible text strings
│   └── themes.xml                # App theme (MaterialComponents.DayNight)
```

---

## 1. `drawable/green_square.xml` – Orientation Indicator

**Purpose:** Provides a simple green square drawable used as the orientation indicator in `MainActivity`. The square rotates according to the phone’s yaw angle (horizontal orientation).

**Content:**
```xml
<shape xmlns:android="http://schemas.android.com/apk/res/android"
    android:shape="rectangle">
    <solid android:color="#00FF00" />
    <size android:width="100dp" android:height="100dp" />
</shape>
```

**Used in:** `activity_main.xml` – the `View` with id `@+id/orientation_view` has `android:background="@drawable/green_square"`.

**Visual flash:** In `MainActivity`, when a click occurs, the background is temporarily changed to red (`R.color.red`) for 100 ms, then back to green.

---

## 2. Layout Files

### 2.1 `activity_main.xml` – Main Screen

**Purpose:** The main user interface of the Air Mouse app. Contains all controls:
- IP address input
- Sensor status indicator
- Calibrate and Start buttons
- Status text
- Sensitivity slider with value display
- Settings and Debug buttons
- Green square orientation indicator

**Key IDs (used in `MainActivity.kt`):**

| ID | Type | Usage |
|----|------|-------|
| `ip_edit_text` | `TextInputEditText` | IP address entry |
| `sensor_status_text` | `TextView` | Shows sensor availability (added in enhanced version) |
| `calibrate_btn` | `MaterialButton` | Starts calibration sequence |
| `start_btn` | `MaterialButton` | Connects to PC and starts sensor streaming |
| `status_text` | `TextView` | Displays connection/activity status |
| `sensitivity_seekbar` | `SeekBar` | Adjusts cursor speed (0.2–2.0) |
| `sensitivity_text` | `TextView` | Shows current sensitivity value |
| `settings_btn` | `MaterialButton` | Opens `SettingsDialog` |
| `debug_toggle_btn` | `MaterialButton` | Shows/hides debug overlay |
| `orientation_view` | `View` | Green square that rotates according to yaw |

**Layout behaviour:** Wrapped in a `ScrollView` to support smaller screens. Uses Material Design 3 components (`TextInputLayout`, `MaterialButton`) with appropriate styles.

---

### 2.2 `fragment_calibration.xml` – Calibration Dialog

**Purpose:** A simple dialog layout used by `CalibrationFragment` to guide the user through gyro, magnetometer, and accelerometer calibration.

**UI elements:**

| ID | Type | Purpose |
|----|------|---------|
| `calibration_status` | `TextView` | Shows current step (e.g., “Step 2/3: Magnetometer – move in figure-8”) |
| `calibration_progress` | `ProgressBar` (horizontal) | Indicates overall calibration progress (0–100) |

**Behaviour:** The fragment updates the status text and progress after each calibration step.

---

### 2.3 `fragment_settings.xml` – Settings Dialog

**Purpose:** A dialog fragment that allows the user to adjust gesture detection thresholds. All changes are saved immediately to `PreferencesDataStore`.

**SeekBars and associated TextViews:**

| ID | Range | Default | Description |
|----|-------|---------|-------------|
| `clickThresholdSeek` + `clickThresholdValue` | 0–100 | 50 → 5.0 rad/s | Click speed threshold (0.1 rad/s per step) |
| `doubleClickSeek` + `doubleClickValue` | 0–80 | 20 → 400 ms | Double‑click interval (200 ms + progress*10) |
| `tiltThresholdSeek` + `tiltThresholdValue` | 0–100 | 100 → 45° | Right‑click tilt angle (0–45°, linear) |

**Note:** The layout does not include a “Save” button – changes are applied in real time via `SeekBar` listeners.

---

### 2.4 `debug_overlay.xml` – Floating Debug Overlay

**Purpose:** A simple `TextView` that is displayed as a system overlay window. It shows real‑time sensor values: roll (degrees), yaw (degrees), gyroY (rad/s), accelY (m/s²).

**Content:**
```xml
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/debugText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="#CC000000"
    android:textColor="#FFFFFF"
    android:padding="8dp"
    android:textSize="12sp"
    android:fontFamily="monospace" />
```

**Usage:** Inflated by `DebugOverlayService` and added to `WindowManager`. The service calls `updateData()` to refresh the text.

---

## 3. Values

### 3.1 `colors.xml` – Colour Palette

**Purpose:** Defines all colours used in the app, following Material Design 3 guidelines.

| Name | Hex | Usage |
|------|-----|-------|
| `purple_200`, `purple_500`, `purple_700` | #FFBB86FC, #FF6200EE, #FF3700B3 | Primary theme colours |
| `teal_200`, `teal_700` | #FF03DAC5, #FF018786 | Secondary theme colours |
| `black`, `white` | #FF000000, #FFFFFFFF | Basic colours |
| `red` | #FFFF0000 | Click flash (temporary background of green square) |
| `green` | #FF00FF00 | Normal green square colour |
| `dark_gray`, `light_gray` | #FF333333, #FFCCCCCC | Additional UI elements (optional) |

**Note:** The `red` and `green` colours are used programmatically in `MainActivity.flashClick()`.

---

### 3.2 `strings.xml` – String Resources

**Purpose:** Centralises all user‑visible text for easy internationalisation (i18n). Currently in English.

| Key | Value | Used in |
|-----|-------|---------|
| `app_name` | Air Mouse Ultimate | `AndroidManifest.xml` |
| `calibrate` | Calibrate Sensors | `calibrate_btn` |
| `start` | Start Air Mouse | `start_btn` |
| `settings` | Settings | `settings_btn` |
| `debug` | Show Debug | `debug_toggle_btn` |
| `status_not_connected` | Not connected | Initial `status_text` |
| `status_active` | Air Mouse Active | After successful start |
| `speed_label` | Cursor Speed | Label above sensitivity slider |
| `ip_hint` | Laptop IP Address | `TextInputLayout` hint |
| `sensor_warning_missing` | Some sensors missing! | `sensor_status_text` when sensors missing |
| `sensor_ok` | All sensors available | `sensor_status_text` when all present |

**Best practice:** Always use `@string/...` in layouts instead of hard‑coded text.

---

### 3.3 `themes.xml` – App Theme

**Purpose:** Defines the visual style of the entire application. Inherits from `Theme.MaterialComponents.DayNight.NoActionBar` (supports day/night mode, no action bar).

**Key attributes:**

| Attribute | Value | Effect |
|-----------|-------|--------|
| `colorPrimary` | `@color/purple_500` | Primary colour (used for buttons, highlights) |
| `colorPrimaryVariant` | `@color/purple_700` | Darker variant for status bar |
| `colorOnPrimary` | `@color/white` | Text colour on primary surfaces |
| `colorSecondary` | `@color/teal_200` | Secondary accent colour |
| `colorSecondaryVariant` | `@color/teal_700` | Darker secondary |
| `colorOnSecondary` | `@color/black` | Text colour on secondary surfaces |
| `android:statusBarColor` | `?attr/colorPrimaryVariant` | Matches status bar with primary variant |
| `android:windowBackground` | `@color/white` | White background for the window |

**Usage:** In `AndroidManifest.xml`, the application or activity should reference this theme:
```xml
<application android:theme="@style/Theme.AirMouse" ...>
```

---

## 🔧 Integration with Code

| Resource ID | Used in Kotlin file(s) |
|-------------|------------------------|
| `R.id.ip_edit_text` | `MainActivity` |
| `R.id.sensor_status_text` | `MainActivity` (optional, for sensor availability) |
| `R.id.calibrate_btn` | `MainActivity` |
| `R.id.start_btn` | `MainActivity` |
| `R.id.status_text` | `MainActivity` |
| `R.id.sensitivity_seekbar` | `MainActivity` |
| `R.id.sensitivity_text` | `MainActivity` |
| `R.id.settings_btn` | `MainActivity` |
| `R.id.debug_toggle_btn` | `MainActivity` |
| `R.id.orientation_view` | `MainActivity` |
| `R.id.calibration_status` | `CalibrationFragment` |
| `R.id.calibration_progress` | `CalibrationFragment` |
| `R.id.clickThresholdSeek`, `R.id.clickThresholdValue` | `SettingsFragment` |
| `R.id.doubleClickSeek`, `R.id.doubleClickValue` | `SettingsFragment` |
| `R.id.tiltThresholdSeek`, `R.id.tiltThresholdValue` | `SettingsFragment` |
| `R.id.debugText` | `DebugOverlayService` |
| `R.color.red`, `R.color.green` | `MainActivity.flashClick()` |
| `R.string.*` | All activities/fragments (via XML or code) |

---

## ✅ Summary

The `res/` folder is **complete and production‑ready**, containing:

- A **green square drawable** as the orientation indicator.
- **Four layouts** covering the main screen, calibration dialog, settings dialog, and debug overlay.
- A **full colour palette** following Material Design 3, including red and green for visual feedback.
- **String resources** for all user‑visible text.
- A **day/night‑compatible theme** with modern Material Components.

Place these files in the correct subdirectories of `app/src/main/res/`. The app will compile and run with a polished, consistent user interface that matches the behaviour defined in the Kotlin code.

---

*This documentation is part of the Air Mouse Ultimate project – University of Tehran, Embedded Systems Exercise.*