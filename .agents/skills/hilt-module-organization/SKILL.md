---
name: hilt-module-organization
description: Structures Hilt modules with @InstallIn scopes, segregating @Binds interface implementations from @Provides third-party builders.
version: 1.0.0
author: airmouse-skills
tags: [android, hilt, dependency-injection]
related_skills: [hilt-testing-fakes-over-mocks, android-module-structure-clean]
applies_to: android
---

# Hilt Module Organization and Scope Segregation

## When to use
- Adding new repositories, sensor managers, or network clients to dependency injection.
- Resolving Hilt compilation cycles or missing binding errors.
- Refactoring monolithic AppModule into bounded domain modules.

## Prerequisites
- com.google.dagger.hilt.android plugin in android/app/build.gradle
- AirMouseApplication annotated with @HiltAndroidApp
- Separated domain interfaces and implementation classes

## Step-by-step instructions
1. **Isolate Interface Bindings**
   Create abstract class modules using @Binds to link domain interfaces with concrete implementations.
2. **Segregate Third-Party Providers**
   Put third-party library instantiations (OkHttpClient, Room) into companion objects with @Provides.
3. **Assign Scopes Conservatively**
   Scope instances strictly (SingletonComponent for app-wide singletons, ViewModelComponent for screen scopes).

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/di/NetworkModule.kt`)
```kotlin
// File: android/app/src/main/java/com/airmouse/di/NetworkModule.kt
package com.airmouse.di

import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import okhttp3.OkHttpClient
import java.util.concurrent.TimeUnit

@Module
@InstallIn(SingletonComponent::class)
abstract class NetworkModule {
    @Binds
    @Singleton
    abstract fun bindConnectionManager(impl: DefaultConnectionManager): ConnectionManager

    companion object {
        @Provides
        @Singleton
        fun provideOkHttpClient(): OkHttpClient {
            return OkHttpClient.Builder()
                .connectTimeout(5, TimeUnit.SECONDS)
                .readTimeout(10, TimeUnit.SECONDS)
                .build()
        }
    }
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew assembleDebug -x lint
```
Expected output:
```text
BUILD SUCCESSFUL - Hilt dependency graph validated cleanly
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Using @Provides for concrete classes where an abstract @Binds method is sufficient.
  **Recommended / Correct:**
  Use @Binds on abstract methods for zero-overhead compilation and smaller DEX bytecode.
- **Anti-pattern / Wrong:**
  Injecting Activity-scoped dependencies into SingletonComponent, causing Activity context leaks.
  **Recommended / Correct:**
  Always scope singletons strictly to Application context or use un-scoped factory bindings.
- **Anti-pattern / Wrong:**
  Forgetting the @InstallIn annotation on a @Module class, leading to silent injection failures.
  **Recommended / Correct:**
  Every Dagger/Hilt module must explicitly declare its target component via @InstallIn.

## Cross-references
- Related skills: `hilt-testing-fakes-over-mocks`, `android-module-structure-clean`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
