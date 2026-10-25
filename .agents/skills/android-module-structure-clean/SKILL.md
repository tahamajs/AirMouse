---
name: android-module-structure-clean
description: Enforces Clean Architecture modularization (:core:ui, :core:network, :feature:touchpad) across the AirMouse Android codebase.
version: 1.0.0
author: airmouse-skills
tags: [android, gradle, architecture]
related_skills: [hilt-module-organization, android-build-logic-convention-plugins]
applies_to: android
---

# Clean Modular Architecture for AirMouse Android

## When to use
- Refactoring monolithic :app module into cohesive feature and core modules.
- Preventing circular dependencies between features.
- Speeding up incremental Gradle build and test cycles.

## Prerequisites
- settings.gradle with include ':core:network', ':core:ui', ':core:sensors'
- Modular package boundaries
- Gradle Version Catalog setup

## Step-by-step instructions
1. **Register Modules in settings.gradle**
   Declare subprojects for core models, hardware sensors, networking, and individual UI features.
2. **Establish Dependency Inversion Rules**
   Ensure feature modules depend on core contracts and never directly on sibling features.
3. **Wire App Shell Aggregation**
   Let the top-level :app module aggregate features and wire navigation routes together.

## Code examples

### Implementation Example (`settings.gradle`)
```groovy
// File: settings.gradle
include ':app'
include ':core:network'
include ':core:sensors'
include ':core:ui'
include ':feature:touchpad'
include ':feature:calibration'

// File: feature/touchpad/build.gradle
plugins {
    alias(libs.plugins.android.library)
    alias(libs.plugins.kotlin.android)
    alias(libs.plugins.hilt)
}

dependencies {
    implementation(project(':core:network'))
    implementation(project(':core:sensors'))
    implementation(project(':core:ui'))
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew projects
```
Expected output:
```text
Project ':core:network', ':core:sensors', ':feature:touchpad' listed
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Allowing direct cross-feature dependencies (:feature:calibration depending on :feature:touchpad).
  **Recommended / Correct:**
  Feature modules must coordinate exclusively through the top-level :app navigator or shared core modules.
- **Anti-pattern / Wrong:**
  Duplicating common Gradle build scripts across 10 different submodules.
  **Recommended / Correct:**
  Use convention plugins or version catalogs to standardize plugin and compiler configurations.
- **Anti-pattern / Wrong:**
  Leaking Android framework classes into domain entities in :core:model.
  **Recommended / Correct:**
  Keep domain models purely Kotlin-based to enable fast JVM unit testing.

## Cross-references
- Related skills: `hilt-module-organization`, `android-build-logic-convention-plugins`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
