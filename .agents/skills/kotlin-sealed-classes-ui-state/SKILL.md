---
name: kotlin-sealed-classes-ui-state
description: Standardizes screen states with sealed interfaces for UiState, UiAction, and UiEffect across all AirMouse Compose screens.
version: 1.0.0
author: airmouse-skills
tags: [android, kotlin, architecture]
related_skills: [design-system-standardization, kotlin-flow-state-and-shared]
applies_to: android
---

# Sealed Architecture for UI State, Actions and Effects

## When to use
- Creating or refactoring an AirMouse screen composable and its ViewModel.
- Handling complex screen lifecycles (Idle, Connecting, Active, Error).
- Eliminating multiple boolean flags in ViewModels (isLoading, isError, isConnected).

## Prerequisites
- Jetpack Compose runtime in android/app/build.gradle
- Kotlin 1.9+ with sealed interface support
- Material 3 theme components

## Step-by-step instructions
1. **Define Sealed Contract Interface**
   Create a unified contract file holding UiState, UiAction, and UiEffect sealed hierarchies.
2. **Add Immutability Annotations**
   Annotate all state data classes with @Immutable or @Stable to guide the Compose compiler skipping rules.
3. **Implement Centralized Action Handler**
   Route all user interactions into a single onAction(action: UiAction) function in the ViewModel.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/presentation/ui/touchpad/TouchpadContract.kt`)
```kotlin
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
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.touchpad.TouchpadViewModelTest
```
Expected output:
```text
PASSED: exhaustive when checks across all TouchpadUiState branches
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Creating mutable var properties inside UiState data classes, breaking Compose state tracking.
  **Recommended / Correct:**
  All fields in UiState must be val and hold immutable primitives or ImmutableList collections.
- **Anti-pattern / Wrong:**
  Handling navigation directly inside UiAction callbacks, coupling ViewModels to NavHost.
  **Recommended / Correct:**
  Emit a UiEffect for navigation side-effects and let the Composable handle the actual NavHost call.
- **Anti-pattern / Wrong:**
  Using raw, unlocalized strings inside error state objects.
  **Recommended / Correct:**
  Use String resource IDs (@StringRes Int) or a sealed UI text abstraction for clean internationalization.

## Cross-references
- Related skills: `design-system-standardization`, `kotlin-flow-state-and-shared`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
