---
name: room-database-for-connection-log
description: Implements Room SQLite database with DAOs for persisting connection session history, latency metrics, and calibration profiles.
version: 1.0.0
author: airmouse-skills
tags: [android, room, database]
related_skills: [kotlin-datastore-preferences, hilt-module-organization]
applies_to: android
---

# Room SQLite Persistence for Session and Calibration Logs

## When to use
- Recording historical connection sessions, average RTT, and packet loss statistics.
- Storing multiple device calibration matrices for different desks or environments.
- Executing relational queries over past performance diagnostics.

## Prerequisites
- androidx.room dependencies in android/app/build.gradle
- KSP plugin configured for Room annotation processing
- Hilt module for Database providing

## Step-by-step instructions
1. **Create Room Entity**
   Define table schemas with @Entity and establish primary keys and indices for high-frequency queries.
2. **Implement Data Access Object (DAO)**
   Write DAO interfaces with Flow<List<T>> for reactive UI observation and suspend methods for writes.
3. **Configure Room Database Singleton**
   Subclass RoomDatabase and provide singleton instantiation via Hilt DatabaseModule.

## Code examples

### Implementation Example (`android/app/src/main/java/com/airmouse/data/local/ConnectionLogDao.kt`)
```kotlin
// File: android/app/src/main/java/com/airmouse/data/local/ConnectionLogDao.kt
package com.airmouse.data.local

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Entity(tableName = "connection_logs")
data class ConnectionLogEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val hostIp: String,
    val timestamp: Long,
    val averagePingMs: Long,
    val packetLossPct: Float,
    val sessionDurationSec: Long
)

@Dao
interface ConnectionLogDao {
    @Query("SELECT * FROM connection_logs ORDER BY timestamp DESC LIMIT 50")
    fun observeRecentSessions(): Flow<List<ConnectionLogEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(log: ConnectionLogEntity): Long
}
```

## Verification
Execute the following verification gate to ensure compliance:
```bash
./gradlew testDebugUnitTest --tests com.airmouse.data.local.ConnectionLogDaoTest
```
Expected output:
```text
PASSED: In-memory Room database successfully inserts and observes logs
```

## Common pitfalls
- **Anti-pattern / Wrong:**
  Executing Room DAO queries synchronously on the main thread, freezing the UI.
  **Recommended / Correct:**
  Always return Flow<T> or suspend functions from DAO methods to run off the main thread.
- **Anti-pattern / Wrong:**
  Altering an @Entity schema without incrementing database version, triggering crash at launch.
  **Recommended / Correct:**
  Increment database version and supply an explicit Migration or test migration strategy.
- **Anti-pattern / Wrong:**
  Storing unbounded live sensor coordinates in SQLite tables.
  **Recommended / Correct:**
  Store only session-level aggregate metrics in SQLite; stream live data in memory.

## Cross-references
- Related skills: `kotlin-datastore-preferences`, `hilt-module-organization`
- Protocol V2 specification: `docs/PROTOCOL_V2.md#1-wire-envelope`
- Master audit verification ID: `[ARCH-03](file:///Users/tahamajs/Documents/uni/CPS/AirMouse/AUDIT_REPORT_PRODUCTION.md)`
