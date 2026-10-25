---
name: android-build-logic-convention-plugins
description: Creates convention plugins in build-logic to share common Compose, Hilt, and Android compiler options across modules.
version: 1.0.0
author: airmouse-skills
tags: [android, gradle, build]
related_skills: [android-version-catalog-gradle, android-module-structure-clean]
applies_to: android
---

# Gradle Build-Logic Convention Plugins

## When to use
- Copy-pasting compileSdk, kotlinOptions, and Compose configurations across 5+ modules.
- Enforcing uniform compiler flags (-Xlint, -Werror) across the Android project.
- Maintaining clean, 10-line build.gradle files in feature modules.

## Prerequisites
- build-logic directory configured in settings.gradle via pluginManagement
- Kotlin DSL for build-logic
- libs.versions.toml version catalog

## Step-by-step instructions
1. **Initialize build-logic Project**
   Configure build-logic/settings.gradle.kts and build.gradle.kts with kotlin-dsl plugin.
2. **Write Reusable Convention Plugins**
   Implement Plugin<Project> classes that apply standard SDK versions, compiler flags, and plugins.
3. **Register Plugins in Gradle Block**
   Register each convention plugin with a unique identifier in gradlePlugin { plugins { register(...) } }.
4. **Apply Conventions in Feature Modules**
   Replace boilerplate build scripts with single-line convention plugin IDs.

## Code examples

### Implementation Example (`build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt`)
```kotlin
// File: build-logic/convention/src/main/kotlin/AndroidComposeConventionPlugin.kt
package com.airmouse.buildlogic

import com.android.build.gradle.BaseExtension
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.kotlin.dsl.getByType

class AndroidComposeConventionPlugin : Plugin<Project> {
    override fun apply(target: Project) = with(target) {
        pluginManager.apply("org.jetbrains.kotlin.plugin.compose")
        val extension = extensions.getByType<BaseExtension>()
        extension.apply {
            buildFeatures.compose = true
            composeOptions {
                kotlinCompilerExtensionVersion = "1.5.14"
            }
        }
    }
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew build-logic:convention:build
```
Expected output:
```text
BUILD SUCCESSFUL - Convention plugins compiled cleanly
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Hardcoding compileSdk versions in convention plugins instead of reading from catalog.
  **Recommended / Correct:**
  Extract SDK and dependency coordinates dynamically from libs.versions.toml.
- **Anti-pattern / Wrong:**
  Creating tightly coupled inheritance trees of custom Gradle plugins.
  **Recommended / Correct:**
  Compose behavior by applying multiple orthogonal, single-responsibility convention plugins.
- **Anti-pattern / Wrong:**
  Forgetting to register convention plugins in gradlePlugin block of build.gradle.kts.
  **Recommended / Correct:**
  Register every convention plugin in gradlePlugin { plugins { register(...) } }.

## Cross-references
- Related skills: `android-version-catalog-gradle`, `android-module-structure-clean`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[DEP-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
