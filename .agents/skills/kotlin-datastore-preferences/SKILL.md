---
name: kotlin-datastore-preferences
description: Implements Jetpack Preferences DataStore as the single source of truth for user settings, replacing legacy SharedPreferences.
version: 1.0.0
author: airmouse-skills
tags: [android, android, storage]
related_skills: [room-database-for-connection-log, kotlin-flow-state-and-shared]
applies_to: android
---

# Migrating to Jetpack Preferences DataStore

## When to use
- Storing user preferences like One Euro filter parameters, theme mode, and last-used host IP.
- Eliminating blocking disk I/O on the main thread caused by SharedPreferences.apply().
- Providing a reactive Flow-based settings stream to the UI layer.

## Prerequisites
- androidx.datastore:datastore-preferences in android/app/build.gradle
- Application context injected via Hilt @ApplicationContext
- Coroutines dependency configured

## Step-by-step instructions
1. **Define DataStore Delegate**
   Create a top-level context extension delegate for DataStore with a dedicated preferences file name.
2. **Declare Preference Keys**
   Define strongly-typed keys (stringPreferencesKey, floatPreferencesKey, booleanPreferencesKey).
3. **Expose Reactive Accessors**
   Expose read streams via Flow<T> with catch blocks for IOException and provide suspend edit methods.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/PreferencesManager.kt`)
```kotlin
// File: android/app/src/main/java/com/airmouse/PreferencesManager.kt
package com.airmouse

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore by preferencesDataStore(name = "airmouse_settings")

@Singleton
class PreferencesManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private object Keys {
        val SENSITIVITY = floatPreferencesKey("cursor_sensitivity")
        val ONE_EURO_BETA = floatPreferencesKey("one_euro_beta")
    }

    val sensitivityFlow: Flow<Float> = context.dataStore.data
        .catch { exception ->
            if (exception is IOException) emit(emptyPreferences()) else throw exception
        }
        .map { prefs -> prefs[Keys.SENSITIVITY] ?: 1.0f }

    suspend fun setSensitivity(value: Float) {
        context.dataStore.edit { prefs -> prefs[Keys.SENSITIVITY] = value }
    }
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.PreferencesManagerTest
```
Expected output:
```text
PASSED: preferences update asynchronously and emit over flow
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Calling runBlocking on dataStore.data.first() directly on the main UI thread.
  **Recommended / Correct:**
  Always observe DataStore flows asynchronously inside appropriate coroutine scopes.
- **Anti-pattern / Wrong:**
  Creating multiple DataStore instances with identical file names in different classes.
  **Recommended / Correct:**
  Declare the DataStore delegate as a single top-level private property on Context.
- **Anti-pattern / Wrong:**
  Saving massive relational or tabular data sets into Preferences DataStore.
  **Recommended / Correct:**
  Use Room SQLite for complex relational structures, reserving DataStore strictly for key-value settings.

## Cross-references
- Related skills: `room-database-for-connection-log`, `kotlin-flow-state-and-shared`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-03](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
