package control

import (
    "sync"
    "time"
)

var (
    movementPaused   bool
    movementPausedMu sync.RWMutex
    pauseUntil       time.Time
    pauseMu          sync.RWMutex
)

// SetMovementPaused sets the global movement paused state
func SetMovementPaused(paused bool) {
    movementPausedMu.Lock()
    defer movementPausedMu.Unlock()
    movementPaused = paused
}

// IsMovementPaused returns true if movement is paused
func IsMovementPaused() bool {
    movementPausedMu.RLock()
    defer movementPausedMu.RUnlock()
    
    // Also check time-based pause
    pauseMu.RLock()
    timedPause := time.Now().Before(pauseUntil)
    pauseMu.RUnlock()
    
    return movementPaused || timedPause
}

// PauseForDuration pauses movement for a specific duration
func PauseForDuration(duration time.Duration) {
    pauseMu.Lock()
    defer pauseMu.Unlock()
    pauseUntil = time.Now().Add(duration)
}

// ClearPause clears any pause state
func ClearPause() {
    movementPausedMu.Lock()
    movementPoused = false
    movementPausedMu.Unlock()
    
    pauseMu.Lock()
    pauseUntil = time.Time{}
    pauseMu.Unlock()
}