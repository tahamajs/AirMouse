---
name: kotlin-flow-state-and-shared
description: Implements StateFlow for UI state caching and SharedFlow for one-off events with WhileSubscribed(5000) backpressure handling.
version: 1.0.0
author: airmouse-skills
tags: [android, kotlin, flow]
related_skills: [kotlin-coroutines-structured-concurrency, kotlin-flow-collect-latest]
applies_to: android
---

# StateFlow and SharedFlow State Management in AirMouse

## When to use
- Exposing observable state from ViewModels to Jetpack Compose UI.
- Handling transient single-fire events like Navigation or Snackbar triggers.
- Preventing unnecessary sensor data processing when the app is in the background.

## Prerequisites
- kotlinx.coroutines.flow in classpath
- androidx.lifecycle.compose.collectAsStateWithLifecycle dependency
- Jetpack Compose UI module

## Step-by-step instructions
1. **Declare Immutable State Model**
   Model screen state as an immutable data class with sensible defaults for offline/disconnected conditions.
2. **Configure StateFlow with Backpressure**
   Convert cold repository data flows into a hot StateFlow using stateIn() with SharingStarted.WhileSubscribed(5000).
3. **Expose One-Off Event Stream**
   Use MutableSharedFlow with extraBufferCapacity=1 and BufferOverflow.DROP_OLDEST for navigation side-effects.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/presentation/ui/main/MainViewModel.kt`)
```kotlin
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
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.presentation.ui.main.MainViewModelTest
```
Expected output:
```text
PASSED: uiState emits within 5s window and cancels upstream on unsubscribe
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Using MutableLiveData instead of StateFlow in Compose projects.
  **Recommended / Correct:**
  StateFlow integrates natively with Compose collectAsStateWithLifecycle and enforces strict type safety.
- **Anti-pattern / Wrong:**
  Using SharingStarted.Eagerly for continuous sensor flows, draining battery when app is backgrounded.
  **Recommended / Correct:**
  Use SharingStarted.WhileSubscribed(5000) to stop upstream sensor collection 5 seconds after screen disconnects.
- **Anti-pattern / Wrong:**
  Exposing MutableStateFlow directly to the UI composables allowing accidental external state mutation.
  **Recommended / Correct:**
  Keep MutableStateFlow strictly private and expose only a read-only StateFlow via asStateFlow().

## Cross-references
- Related skills: `kotlin-coroutines-structured-concurrency`, `kotlin-flow-collect-latest`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
