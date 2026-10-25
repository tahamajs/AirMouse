---
name: kotlin-coroutines-structured-concurrency
description: Enforces structured concurrency with SupervisorJob, CoroutineScope lifecycle bindings, and non-leaking dispatchers in AirMouse Android.
version: 1.0.0
author: airmouse-skills
tags: [android, kotlin, coroutines]
related_skills: [kotlin-flow-state-and-shared, mvvm-viewmodel-factory]
applies_to: android
---

# Structured Concurrency with Kotlin Coroutines in AirMouse

## When to use
- Launching background jobs in ViewModels or Services without tying to lifecycle.
- Encountering thread leaks or uncaught exceptions cancelling parent scopes.
- Refactoring GlobalScope or unmanaged CoroutineScope instances.

## Prerequisites
- android/app/build.gradle with org.jetbrains.kotlinx:kotlinx-coroutines-android
- com.airmouse.presentation.viewmodel package structure
- Hilt dependency injection configured for CoroutineDispatcher

## Step-by-step instructions
1. **Audit Coroutine Scope Usage**
   Search the codebase for GlobalScope, unmanaged CoroutineScope(), or Dispatchers.IO hardcoded instantiations.
   ```bash
   grep -rn 'GlobalScope' android/app/src/main/java/
   ```
2. **Define Injected Dispatchers**
   Create CoroutineScopes bound to SupervisorJob inside ViewModel or Service classes to isolate child task failures.
   ```bash
   mkdir -p android/app/src/main/java/com/airmouse/di
   ```
3. **Wire Scope Lifecycle Cancellation**
   Ensure that any custom CoroutineScope explicitly calls cancel() in onCleared() or onDestroy() to prevent leaked coroutines.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/presentation/viewmodel/ConnectionViewModel.kt`)
```kotlin
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
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.presentation.viewmodel.ConnectionViewModelTest
```
Expected output:
```text
BUILD SUCCESSFUL - 0 failed tests
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Catching generic Throwable or Exception and swallowing CancellationException inside coroutines.
  **Recommended / Correct:**
  Always rethrow CancellationException so parent jobs can orchestrate child lifecycle cancellations properly.
- **Anti-pattern / Wrong:**
  Using GlobalScope.launch for network dispatching, causing socket operations to survive Activity death.
  **Recommended / Correct:**
  Use viewModelScope or an injected lifecycle-aware CoroutineScope bound to the Service or Activity.
- **Anti-pattern / Wrong:**
  Hardcoding Dispatchers.IO or Dispatchers.Default in business logic classes.
  **Recommended / Correct:**
  Inject CoroutineDispatcher via Hilt annotations (@IoDispatcher, @DefaultDispatcher) to enable deterministic unit tests.

## Cross-references
- Related skills: `kotlin-flow-state-and-shared`, `mvvm-viewmodel-factory`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
