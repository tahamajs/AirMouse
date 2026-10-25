---
name: kotlin-serialization-protocol-dto
description: Generates strict kotlinx.serialization DTOs strictly adhering to Protocol V2 canonical wire specifications.
version: 1.0.0
author: airmouse-skills
tags: [android, kotlin, serialization]
related_skills: [protocol-v2-envelope-validation, protocol-v2-bounds-check]
applies_to: android
protocol_ref: docs/PROTOCOL_V2.md#1-wire-envelope
---

# Strict Protocol V2 Serialization with kotlinx.serialization

## When to use
- Serializing and deserializing JSON wire packets over TCP, TLS, or WebSocket.
- Preventing silent fallback to default values on missing required fields.
- Rejecting malformed or unexpected JSON keys in network packets.

## Prerequisites
- kotlinx.serialization plugin applied in build.gradle
- docs/PROTOCOL_V2.md specification document
- Strict JSON encoder configured

## Step-by-step instructions
1. **Define DTO Data Model**
   Annotate packet models with @Serializable and define all fields per Protocol V2 specifications.
2. **Enforce Required Fields and Invariants**
   Implement an init block that asserts v == 2, valid monotonic sequence, and timestamp boundaries.
3. **Configure Strict Json Engine**
   Build a Json instance with ignoreUnknownKeys = false, encodeDefaults = true, and isLenient = false.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/network/ProtocolV2Dto.kt`)
```kotlin
// File: android/app/src/main/java/com/airmouse/network/ProtocolV2Dto.kt
package com.airmouse.network

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement

@Serializable
data class WireEnvelope(
    val v: Int = 2,
    val type: String,
    val seq: Long,
    val ts: Long,
    val id: String? = null,
    val payload: JsonElement
) {
    init {
        require(v == 2) { "Protocol version must be exactly 2, got: $v" }
        require(type.isNotBlank()) { "Message type cannot be blank" }
        require(seq >= 0) { "Sequence number must be non-negative, got: $seq" }
    }
}

val StrictProtocolJson = Json {
    ignoreUnknownKeys = false
    encodeDefaults = true
    isLenient = false
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.network.ProtocolV2DtoTest
```
Expected output:
```text
PASSED: Envelope validation rejects v!=2 and unknown payload fields
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Setting ignoreUnknownKeys = true in production protocol parsers, masking schema mismatches.
  **Recommended / Correct:**
  Keep ignoreUnknownKeys = false to enforce strict Protocol V2 §2.4 contract validation.
- **Anti-pattern / Wrong:**
  Encoding float coordinates without checking for Float.NaN or Float.POSITIVE_INFINITY.
  **Recommended / Correct:**
  Check isNaN() and isInfinite() before serializing delta values to prevent JSON corruption.
- **Anti-pattern / Wrong:**
  Transmitting flat root keys instead of nested payload dictionaries.
  **Recommended / Correct:**
  All type-specific parameters must be nested under the 'payload' object per Protocol V2.

## Cross-references
- Related skills: `protocol-v2-envelope-validation`, `protocol-v2-bounds-check`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[SEC-04](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
