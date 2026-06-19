# Air Mouse — Modernization Specification

Goal: migrate the Android app to a modern, modular, maintainable architecture using current Android best practices: Jetpack Compose, Kotlin coroutines & Flow, Hilt DI, DataStore for preferences, kotlinx-serialization for network messages, and scoped modules for sensors/network/ui so the PC server and Android client interoperate cleanly.

This document contains: high-level architecture, module layout, suggested Gradle/plugin versions (guidance), concrete dependency lists, networking protocol recommendation, testing & CI suggestions, and a migration checklist.

---

## 1. High-level Architecture

- Multi-module project:
  - `:app` — minimal Android app, Compose UI, navigation, runtime wiring, DI entry points.
  - `:core` — shared utilities, models, serialization declarations (kotlinx-serialization), common exceptions.
  - `:sensors` — sensor handling, flows for sensor streams, calibration logic, Madgwick filter implementation (Kotlin), unit-tested.
  - `:network` — TCP client (coroutine-based), UDP discovery client, ack/retransmit logic, reconnection logic.
  - `:ui` — Compose screens and components (optionally `:ui:compose` submodule), theming, accessibility helpers.
  - `:data` — DataStore wrappers, preferences repository, migrations.

- DI: Hilt to provide singletons and scoped objects (e.g., SensorManager wrappers, Network client, Repositories).
- Concurrency: Kotlin Coroutines and Flow for streams and one-off tasks. Use structured concurrency and a clear set of Dispatchers (IO, Default, Main).
- Serialization: kotlinx-serialization (JSON) for network messages; define stable DTOs in `:core`.
- Testing: unit tests for logic (JVM), instrumented tests for sensors/permission flows, Compose UI tests. Use MockK / Turbine for Flow tests.

## 2. Why these choices

- Compose: modern declarative UI, smaller code surface, easier accessibility & animations.
- Hilt: minimal DI boilerplate and standard for Android ecosystem.
- Coroutines + Flow: natural fit for streaming sensor data and async networking.
- DataStore: coroutine-friendly replacement for SharedPreferences.
- kotlinx-serialization: type-safe message serialization and compatibility across Kotlin platforms.

## 3. Minimum compatibility / plugin guidance (starting point)

- Android Gradle Plugin (AGP): 8.1+ (match your local setup); example: `com.android.application` 8.1.0+.
- Kotlin: 1.9.x (match Kotlin plugin); example: `org.jetbrains.kotlin.jvm`/`kotlin-android` 1.9.22 if available.
- Compose: version compatible with Kotlin 1.9.x — e.g., Compose 1.5.x or later. Use Compose BOM to keep versions in sync.
- Java/Kotlin compatibility: target Java 11.

Note: Always validate AGP ↔ Kotlin ↔ Compose compatibility matrix before pinning exact versions. If you want, I can patch `build.gradle` with exact pins that match your environment.

## 4. Core dependencies (per-module suggestions)

### `:app` (Gradle Kotlin DSL snippet)

- plugins block:
  - `com.android.application`
  - `org.jetbrains.kotlin.android`
  - `dagger.hilt.android.plugin`
  - `kotlinx-serialization` plugin (if needed)

- dependencies:
  - implementation platform("androidx.compose:compose-bom:<bom-version>")
  - implementation "androidx.compose.ui:ui"
  - implementation "androidx.compose.material:material"
  - implementation "androidx.compose.ui:ui-tooling-preview"
  - implementation "androidx.navigation:navigation-compose"
  - implementation "com.google.dagger:hilt-android:2.47" (or latest Hilt)
  - kapt "com.google.dagger:hilt-compiler:2.47"
  - implementation "org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3" (or latest)
  - implementation "androidx.datastore:datastore-preferences:1.1.0"
  - implementation "org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0"
  - implementation project(":network")
  - implementation project(":sensors")
  - testImplementation "junit:junit:4.13.2"
  - androidTestImplementation "androidx.test.ext:junit:1.1.5"
  - androidTestImplementation "androidx.test.espresso:espresso-core:3.5.1"
  - debugImplementation "androidx.compose.ui:ui-tooling"

### `:core`
- implementation "org.jetbrains.kotlinx:kotlinx-serialization-json"
- define DTOs and shared constants. Keep this module pure Kotlin (no Android SDK) where possible to ease JVM tests.

### `:network`
- implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core"
- implementation "org.jetbrains.kotlinx:kotlinx-serialization-json"
- (Optional) implementation "io.ktor:ktor-client-cio" if you prefer Ktor for TCP/UDP abstractions.
- Provide a coroutine-friendly TCP client wrapper and UDP discovery client that returns Flows/SharedFlows for events.

### `:sensors`
- implementation "org.jetbrains.kotlinx:kotlinx-coroutines-core"
- Provide Sensor wrappers using callbackFlow: sensor events -> callbackFlow -> map/filter -> shareIn/StateFlow for UI consumption.
- Implement Madgwick filter in Kotlin; expose `Flow<Orientation>` from sensor pipeline.

### `:data`
- implementation "androidx.datastore:datastore-preferences"
- implementation "androidx.datastore:datastore-core" (if protobuf desired)

## 5. Networking protocol (recommended canonical format)

Use newline-delimited JSON messages with stable DTOs (kotlinx-serialization). Example messages from phone -> PC:

- Move:
  {
    "type": "move",
    "dx": 1.23,
    "dy": -0.44
  }

- Click:
  {
    "type": "click",
    "id": "uuid-or-seq"
  }

Server -> phone for ACK:
  { "type": "ack", "id": "..." }

Discovery protocol: UDP broadcast message `AIRMOUSE_DISCOVER` (ASCII). Server replies with a JSON discovery response containing `type:discovery_response`, `ip`, `port`, `mDNS` optional.

Rationale: newline-delimited JSON is easy to implement in Kotlin coroutines with `BufferedSource` (Okio) or kotlinx-serialization + Ktor/Socket wrappers.

## 6. Sensor pipeline (recommended)

- Use `callbackFlow` for Android `SensorEventListener`.
- Map raw sensor values into timestamped Kotlin data classes in `:sensors`.
- Combine accelerometer+gyro+(magnetometer optional) into a single `Flow<Orientation>` using `combine` and Madgwick algorithm running on `Dispatchers.Default`.
- Expose processed orientation to UI via `StateFlow`.
- Calibration API: expose suspend functions to run calibration steps and emit progress through Flows.

## 7. ACK, retransmit and reliability

- Attach a short `id` (UUID or increasing sequence) to click/scroll packets.
- Maintain a small in-memory map of pending messages with a retry TTL and backoff (e.g., 3 retries, 500ms initial timeout).
- When ACK arrives (matching id), cancel retries.
- Implement this logic inside `:network` using coroutines (Channels/Flows) rather than raw threads.

## 8. DI / Hilt setup

- Provide module bindings for:
  - `@Singleton fun provideDataStore(...)`
  - `@Singleton fun provideNetworkClient(...)`
  - `@Singleton fun provideSensorRepository(...)`
  - `@Singleton fun provideMadgwickFilter(...)`
- Use `@HiltAndroidApp` on `Application` and `@AndroidEntryPoint` for Activities/Fragments/Compose entry points.

## 9. Testing & tooling

- Unit tests: use JUnit + MockK + Turbine (for flows).
- Instrumentation: Robolectric for JVM-level sensors, Android instrumentation for lifecycle and permission flows.
- Linting: add `ktlint` and `detekt` configuration.
- CI: GitHub Actions matrix => runs `./gradlew build` (assembleDebug), run unit tests, run lint; optionally run emulator-based integration later.

## 10. Migration checklist (practical order)

1. Add Kotlin, AGP, and Hilt plugins to `build.gradle` and update top-level Gradle config.
2. Create modules: `:core`, `:network`, `:sensors`, `:data`, `:ui` (or `:app` with `ui` package) and move existing files into logical places.
3. Implement `core` DTOs with `@Serializable` classes for messages.
4. Implement `network` coroutine TCP client and UDP discovery client with tests.
5. Implement `sensors` flows, Madgwick conversion, and calibration APIs.
6. Replace `SharedPreferences` usage with DataStore wrapper and `PreferencesManager` refactor.
7. Replace existing XML UI with Compose screens incrementally (start with `NetworkDiscoveryFragment` -> `NetworkDiscoveryScreen`.
8. Add Hilt modules and integrate DI.
9. Add tests and CI.
10. Polish: theming, accessibility, small UX flows, and final performance testing.

## 11. Example Gradle snippet (top-level `build.gradle`)

```groovy
buildscript {
    repositories { google(); mavenCentral() }
}

plugins {
    id 'com.android.application' version '8.1.0' apply false
    id 'org.jetbrains.kotlin.android' version '1.9.22' apply false
    id 'com.google.dagger.hilt.android' version '2.47' apply false
}
```

And example `app/build.gradle` (Kotlin DSL users please adapt):

```groovy
plugins {
    id 'com.android.application'
    id 'org.jetbrains.kotlin.android'
    id 'com.google.dagger.hilt.android'
}

android {
    namespace 'com.airmouse'
    compileSdk 34
    defaultConfig {
        applicationId 'com.airmouse'
        minSdk 23
        targetSdk 34
        versionCode 1
        versionName '1.0'
    }
    compileOptions { sourceCompatibility JavaVersion.VERSION_11; targetCompatibility JavaVersion.VERSION_11 }
    kotlinOptions { jvmTarget = '11' }
    buildFeatures { compose true }
    composeOptions { kotlinCompilerExtensionVersion '1.5.0' }
}

dependencies {
    implementation platform('androidx.compose:compose-bom:2024.08.00')
    implementation 'androidx.compose.ui:ui'
    implementation 'androidx.compose.material:material'
    implementation 'androidx.navigation:navigation-compose:2.7.0'
    implementation 'com.google.dagger:hilt-android:2.47'
    kapt 'com.google.dagger:hilt-compiler:2.47'
    implementation 'org.jetbrains.kotlinx:kotlinx-coroutines-android:1.7.3'
    implementation 'androidx.datastore:datastore-preferences:1.1.0'
    implementation 'org.jetbrains.kotlinx:kotlinx-serialization-json:1.6.0'
    // project modules
    implementation project(':core')
    implementation project(':network')
    implementation project(':sensors')
}
```

(These numbers are suggestions; verify compatibility with your AGP/Kotlin toolchain.)

## 12. Next steps I can take now

- Patch `settings.gradle` and top-level `build.gradle` to add submodules and plugins.
- Create `:core` module with DTOs (`Message.kt`) and sample serialization tests.
- Scaffold `:network` with coroutine TCP client and UDP discovery client.

If you'd like, I can immediately scaffold the modules and implement the `core` DTOs and the JSON message contracts.

---

Would you like me to scaffold the modules and add the `Message.kt` DTOs now? If yes, choose: `scaffold core` or `scaffold network` or `scaffold sensors` and I'll begin implementing that module and wiring Gradle.