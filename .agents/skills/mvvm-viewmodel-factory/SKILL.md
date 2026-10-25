---
name: mvvm-viewmodel-factory
description: Configures Hilt-assisted injection with SavedStateHandle for ViewModel state restoration across process death.
version: 1.0.0
author: airmouse-skills
tags: [android, android, viewmodel]
related_skills: [kotlin-sealed-classes-ui-state, hilt-module-organization]
applies_to: android
---

# SavedStateHandle and Hilt ViewModel Injection

## When to use
- Preserving user inputs or selected connection profiles across Android OS process death.
- Retrieving type-safe navigation arguments inside ViewModel constructors.
- Testing ViewModels with pre-populated SavedStateHandle instances.

## Prerequisites
- androidx.lifecycle:lifecycle-viewmodel-compose dependency
- androidx.lifecycle:lifecycle-viewmodel-savedstate dependency
- Navigation Compose argument routing

## Step-by-step instructions
1. **Inject SavedStateHandle**
   Declare SavedStateHandle as a constructor parameter in the @HiltViewModel class.
2. **Bind Persistent StateFlow**
   Use savedStateHandle.getStateFlow(key, initialValue) to automatically synchronize state changes.
3. **Extract Navigation Arguments**
   Read route parameters passed from NavHost directly from the handle without bundle unpacking.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/presentation/ui/calibration/CalibrationViewModel.kt`)
```kotlin
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
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.calibration.CalibrationViewModelTest
```
Expected output:
```text
PASSED: state successfully restored from synthetic SavedStateHandle
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Storing heavy complex objects or open file streams in SavedStateHandle.
  **Recommended / Correct:**
  Only store primitives, Strings, identifiers, or lightweight Parcelable objects under 50KB.
- **Anti-pattern / Wrong:**
  Accessing arguments without null validation or default fallbacks.
  **Recommended / Correct:**
  Use checkNotNull(savedStateHandle['key']) or provide explicit safe default values.
- **Anti-pattern / Wrong:**
  Creating manual ViewModelProvider.Factory implementations when using Hilt.
  **Recommended / Correct:**
  Use standard @HiltViewModel annotations and let Hilt generate the factory automatically.

## Cross-references
- Related skills: `kotlin-sealed-classes-ui-state`, `hilt-module-organization`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
