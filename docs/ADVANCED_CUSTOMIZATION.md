# Advanced Customization

This document shows how to tweak the Air Mouse for your preferences or add new features.

## 1. Changing Sensitivity

### Android side (cursor speed)
In `MainActivity.kt`, find the delta calculation:
```kotlin
val deltaX = (yaw - lastYaw) * 0.5f
val deltaY = (roll - lastRoll) * 0.5f