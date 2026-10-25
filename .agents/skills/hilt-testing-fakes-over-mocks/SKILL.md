---
name: hilt-testing-fakes-over-mocks
description: Implements in-memory fake repositories and @TestInstallIn module replacements for deterministic testing in AirMouse.
version: 1.0.0
author: airmouse-skills
tags: [android, hilt, testing]
related_skills: [hilt-module-organization, android-unit-test-turbine-flow]
applies_to: android
---

# Hilt Testing with Fakes and @TestInstallIn

## When to use
- Testing ViewModels without spinning up physical network sockets or hardware sensors.
- Replacing fragile Mockito mock chains with stateful in-memory implementations.
- Executing instrumented tests in CI hermetically.

## Prerequisites
- hilt-android-testing in android/app/build.gradle
- Fake implementation implementing the target domain interface
- Robolectric or AndroidJUnit runner configured

## Step-by-step instructions
1. **Construct In-Memory Fake**
   Implement the domain interface with an in-memory mutable state container (e.g. MutableStateFlow or HashMap).
2. **Declare Test Replacement Module**
   Annotate a test module with @TestInstallIn specifying the production module it replaces.
3. **Inject Fake into Test Runner**
   Inject the fake into the test class and manipulate its state to assert ViewModel reactions.

## Code examples

### Implementation Example (`android/app/src/test/java/com/airmouse/di/TestNetworkModule.kt`)
```kotlin
// File: android/app/src/test/java/com/airmouse/di/TestNetworkModule.kt
package com.airmouse.di

import com.airmouse.network.ConnectionManager
import com.airmouse.network.FakeConnectionManager
import dagger.Binds
import dagger.Module
import dagger.hilt.components.SingletonComponent
import dagger.hilt.testing.TestInstallIn
import javax.inject.Singleton

@Module
@TestInstallIn(
    components = [SingletonComponent::class],
    replaces = [NetworkModule::class]
)
abstract class TestNetworkModule {
    @Binds
    @Singleton
    abstract fun bindConnectionManager(fake: FakeConnectionManager): ConnectionManager
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest
```
Expected output:
```text
PASSED: Tests executed with FakeConnectionManager without network sockets
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Using @UninstallModules without defining a replacement module, causing missing binding compilation errors.
  **Recommended / Correct:**
  Pair @UninstallModules with @BindValue in the test class or use @TestInstallIn.
- **Anti-pattern / Wrong:**
  Allowing test state to leak across test executions in singleton in-memory fakes.
  **Recommended / Correct:**
  Provide an explicit clear() or reset() method on in-memory fakes invoked in @Before or @After.
- **Anti-pattern / Wrong:**
  Using Mockito mocks on Kotlin final classes without enabling mock-maker-inline.
  **Recommended / Correct:**
  Prefer lightweight handwritten in-memory fakes; they are faster and do not break on refactorings.

## Cross-references
- Related skills: `hilt-module-organization`, `android-unit-test-turbine-flow`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
