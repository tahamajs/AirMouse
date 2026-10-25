---
name: kotlin-flow-collect-latest
description: Applies collectLatest for rapid high-frequency sensor streams to cancel obsolete in-flight UI calculations in AirMouse.
version: 1.0.0
author: airmouse-skills
tags: [android, kotlin, flow]
related_skills: [kotlin-flow-state-and-shared, sensor-fusion-dsp]
applies_to: android
---

# High-Frequency Stream Processing with collectLatest

## When to use
- Processing sensor updates arriving at 100-200Hz where intermediate frames can be dropped.
- Preventing UI thread queue backpressure when drawing live motion traces.
- Cancelling long-running calculations when a newer sensor packet arrives.

## Prerequisites
- android/app/src/main/java/com/airmouse/sensors/SensorFusion.kt
- Kotlin Coroutines 1.7+ with Flow conflate support
- Hardware IMU streaming enabled

## Step-by-step instructions
1. **Analyze Flow Emission Rates**
   Verify if the source flow emits faster than the consumer can process (e.g. 100Hz gyroscope vs 60Hz UI frame).
2. **Apply Flow Conflation**
   Insert conflate() or buffer(Channel.CONFLATED) before the terminal operator to drop stale queue items.
3. **Switch Collector to collectLatest**
   Use collectLatest inside the consuming coroutine to automatically cancel obsolete in-flight jobs.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/sensors/MotionAnalyzer.kt`)
```kotlin
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
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.sensors.MotionAnalyzerTest
```
Expected output:
```text
PASSED: collectLatest drops stale sensor frames under synthetic 200Hz load
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Using collectLatest for discrete transactional events like button clicks or authentication tokens.
  **Recommended / Correct:**
  Use collect or a buffered Channel for critical control events so none are accidentally aborted.
- **Anti-pattern / Wrong:**
  Neglecting conflate() on high-speed flows, leading to memory accumulation in unbounded internal buffers.
  **Recommended / Correct:**
  Pair collectLatest with conflate() or buffer(Channel.CONFLATED) to eliminate memory spikes.
- **Anti-pattern / Wrong:**
  Performing non-cancellable blocking operations (like synchronized disk write) inside collectLatest.
  **Recommended / Correct:**
  Ensure worker functions inside collectLatest periodically check ensureActive() or call suspendable APIs.

## Cross-references
- Related skills: `kotlin-flow-state-and-shared`, `sensor-fusion-dsp`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
