---
name: android-kotlin-ksp-vs-kapt
description: Migrates annotation processors from legacy KAPT to Kotlin Symbol Processing (KSP) for 2x faster build times.
version: 1.0.0
author: airmouse-skills
tags: [android, kotlin, build]
related_skills: [android-build-logic-convention-plugins, hilt-module-organization]
applies_to: android
---

# Migrating Annotation Processors from KAPT to KSP

## When to use
- Observing slow incremental Gradle build times caused by Java stub generation.
- Updating Room and Hilt to versions supporting native KSP.
- Eliminating kapt block warnings during Gradle builds.

## Prerequisites
- com.google.devtools.ksp plugin in libs.versions.toml matching Kotlin version
- Room and Hilt dependencies compatible with KSP
- Gradle 8.0+

## Step-by-step instructions
1. **Apply KSP Plugin**
   Add id('com.google.devtools.ksp') plugin to project and module build files.
2. **Migrate Dependency Configurations**
   Replace kapt(...) statements with ksp(...) for Room, Hilt, and Moshi compilers.
3. **Remove KAPT Plugin**
   Purge apply plugin: 'kotlin-kapt' and delete unused kapt configuration blocks.

## Code examples

### Implementation Example (`android/app/build.gradle`)
```groovy
// File: android/app/build.gradle
// BEFORE (Legacy KAPT with Java stub generation):
// apply plugin: 'kotlin-kapt'
// dependencies {
//     kapt "androidx.room:room-compiler:$room_version"
//     kapt "com.google.dagger:hilt-compiler:$hilt_version"
// }

// AFTER (Native high-speed Kotlin Symbol Processing):
plugins {
    alias(libs.plugins.ksp)
    alias(libs.plugins.hilt)
}

dependencies {
    ksp(libs.room.compiler)
    ksp(libs.hilt.compiler)
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew compileDebugKotlin --rerun-tasks
```
Expected output:
```text
BUILD SUCCESSFUL - KSP generated files in build/generated/ksp/
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Using a KSP version that does not match the active Kotlin version.
  **Recommended / Correct:**
  Ensure KSP version strictly matches Kotlin (e.g. Kotlin 2.0.21 -> KSP 2.0.21-1.0.25).
- **Anti-pattern / Wrong:**
  Leaving kapt dependencies in the same module alongside ksp dependencies.
  **Recommended / Correct:**
  Completely remove all kapt dependencies so Gradle skips the Java stub generation step.
- **Anti-pattern / Wrong:**
  Failing to mark KSP generated source folders for IDE code completion.
  **Recommended / Correct:**
  Android Studio Koala+ automatically indexes build/generated/ksp/debug/kotlin.

## Cross-references
- Related skills: `android-build-logic-convention-plugins`, `hilt-module-organization`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[DEP-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
