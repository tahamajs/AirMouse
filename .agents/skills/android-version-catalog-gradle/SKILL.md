---
name: android-version-catalog-gradle
description: Centralizes dependency management and version coordinates using gradle/libs.versions.toml in AirMouse.
version: 1.0.0
author: airmouse-skills
tags: [android, gradle, build]
related_skills: [android-module-structure-clean, android-build-logic-convention-plugins]
applies_to: android
---

# Gradle Version Catalog Centralization

## When to use
- Upgrading libraries like Jetpack Compose, Kotlin, or Hilt across multiple modules.
- Eliminating fragmented, conflicting version strings in build.gradle files.
- Automating dependency vulnerability checks in CI.

## Prerequisites
- gradle/libs.versions.toml file in project root
- Gradle 7.4+
- Multi-module Gradle project structure

## Step-by-step instructions
1. **Create Version Catalog File**
   Create gradle/libs.versions.toml with [versions], [libraries], [bundles], and [plugins] sections.
2. **Map Dependencies to Aliases**
   Define canonical group, artifact, and version.ref bindings for all third-party libraries.
3. **Refactor Module Build Scripts**
   Replace string literals in build.gradle files with type-safe libs.alias references.

## Code examples

### Implementation Example (`gradle/libs.versions.toml`)
```toml
# File: gradle/libs.versions.toml
[versions]
kotlin = "2.0.21"
compose = "1.7.4"
hilt = "2.52"
okhttp = "4.12.0"

[libraries]
kotlin-coroutines-core = { module = "org.jetbrains.kotlinx:kotlinx-coroutines-core", version = "1.9.0" }
okhttp-core = { module = "com.squareup.okhttp3:okhttp", version.ref = "okhttp" }
okhttp-logging = { module = "com.squareup.okhttp3:logging-interceptor", version.ref = "okhttp" }

[bundles]
networking = ["okhttp-core", "okhttp-logging"]

[plugins]
android-application = { id = "com.android.application", version = "8.7.1" }
kotlin-android = { id = "org.jetbrains.kotlin.android", version.ref = "kotlin" }
hilt = { id = "com.google.dagger.hilt.android", version.ref = "hilt" }
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew dependencies
```
Expected output:
```text
BUILD SUCCESSFUL - All dependencies resolved from catalog
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Hardcoding dependency versions inside module build.gradle files instead of referencing the catalog.
  **Recommended / Correct:**
  Reference all dependencies via libs.bundle.name or libs.library.name.
- **Anti-pattern / Wrong:**
  Using inconsistent hyphen vs dot naming conventions in catalog alias names.
  **Recommended / Correct:**
  Use consistent kebab-case or dot notation consistently across the catalog.
- **Anti-pattern / Wrong:**
  Omitting the [plugins] table and applying plugins with raw version strings in module scripts.
  **Recommended / Correct:**
  Declare Gradle plugins in the catalog and apply using alias(libs.plugins.name).

## Cross-references
- Related skills: `android-module-structure-clean`, `android-build-logic-convention-plugins`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[DEP-01](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
