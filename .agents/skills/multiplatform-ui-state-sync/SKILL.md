---
name: multiplatform-ui-state-sync
description: >-
  Architectural patterns for syncing real-time connection state, mode changes, and telemetry
  across screen boundaries, optimistic UI updates, and consistent multiplatform state
  representation between Android, Web, and Desktop hosts.
---

# Multiplatform UI State Synchronization & Coordination

This skill provides patterns for maintaining reactive, real-time consistency across multi-screen mobile apps and desktop hosts, eliminating stale state, race conditions, and sluggish network delays.

## 1. Single Source of Truth for Session State

Session state (active host IP, encryption status, ping, packet loss, battery level) MUST exist in an independent domain repository, shared across all screens via reactive flows:

```kotlin
@Singleton
class SessionRepository @Inject constructor() {
    private val _sessionState = MutableStateFlow(SessionState())
    val sessionState: StateFlow<SessionState> = _sessionState.asStateFlow()

    fun updateMetrics(pingMs: Long, packetLossPct: Float) {
        _sessionState.update { current ->
            current.copy(
                pingMs = pingMs,
                packetLossPct = packetLossPct,
                lastSeenTimestamp = System.currentTimeMillis()
            )
        }
    }
}
```

---

## 2. Optimistic UI Updates with Rollback

When a user toggles a mode (e.g. switching between Trackpad Mode and Gyro Air Mouse Mode, or calibrating sensors), update the UI immediately without waiting for network round-trip confirmation:

```kotlin
fun switchMode(targetMode: MouseMode) {
    val previousMode = _uiState.value.activeMode
    
    // 1. Optimistic instant visual response
    _uiState.update { it.copy(activeMode = targetMode) }
    
    // 2. Dispatch wire command
    viewModelScope.launch {
        try {
            val success = networkClient.sendCommand(ModeCommand(targetMode))
            if (!success) {
                // Rollback if server rejected
                _uiState.update { it.copy(activeMode = previousMode) }
                _effects.send(TouchpadEffect.ShowToast("Server failed to switch mode"))
            }
        } catch (e: Exception) {
            _uiState.update { it.copy(activeMode = previousMode) }
            _effects.send(TouchpadEffect.ShowToast("Connection error: ${e.message}"))
        }
    }
}
```

---

## 3. Screen Interconnection & Contextual HUD

When navigating between sub-screens (e.g., from `MainScreen` to `CalibrationScreen` or `ThemesScreen`):
1. **Never drop the telemetry stream**: Keep the background UDP/WSS connection hot and collecting metrics.
2. **Contextual HUD Banner**: If network latency spikes or a packet drop occurs while the user is inside a sub-screen (like Settings or Calibration), display a non-intrusive HUD banner that alerts the user without booting them out of the current workflow.

```kotlin
@Composable
fun ContextualConnectionBanner(
    connectionState: ConnectionState,
    modifier: Modifier = Modifier
) {
    AnimatedVisibility(
        visible = connectionState.isDegraded || !connectionState.isConnected,
        enter = expandVertically() + fadeIn(),
        exit = shrinkVertically() + fadeOut()
    ) {
        Surface(
            color = MaterialTheme.colorScheme.errorContainer,
            contentColor = MaterialTheme.colorScheme.onErrorContainer,
            modifier = modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier.padding(12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(Icons.Default.Warning, contentDescription = null)
                Text(
                    text = if (!connectionState.isConnected) 
                        "Disconnected from host. Reconnecting..." 
                    else 
                        "High latency detected (${connectionState.pingMs}ms)",
                    style = MaterialTheme.typography.bodySmall
                )
            }
        }
    }
}
```

---

## 4. Multiplatform Parity Standards
1. **Color Coding Parity**: Status indicators MUST share identical thresholds across Android, Web, and Desktop Fyne GUI:
   - Green: Ping $< 20\text{ ms}$, Packet Loss $= 0\%$
   - Yellow: Ping $20-60\text{ ms}$ OR Packet Loss $< 2\%$
   - Red: Ping $> 60\text{ ms}$ OR Packet Loss $\ge 2\%$
2. **Settings Synchronization**: Calibration coefficients, One Euro parameters, and button mapping profiles modified on mobile should sync to the desktop server via `payload.settings_sync` to ensure unified behavior.
