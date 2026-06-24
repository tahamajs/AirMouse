// Package common provides global pause controls used by mouse and other components.
package common

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

// SetMovementPaused sets the global movement paused state.
func SetMovementPaused(paused bool) {
	movementPausedMu.Lock()
	defer movementPausedMu.Unlock()
	movementPaused = paused
}

// IsMovementPaused returns true if movement is paused (either manually or by timer).
func IsMovementPaused() bool {
	movementPausedMu.RLock()
	defer movementPausedMu.RUnlock()

	pauseMu.RLock()
	timedPause := time.Now().Before(pauseUntil)
	pauseMu.RUnlock()

	return movementPaused || timedPause
}

// PauseForDuration pauses movement for a specific duration.
func PauseForDuration(duration time.Duration) {
	pauseMu.Lock()
	defer pauseMu.Unlock()
	pauseUntil = time.Now().Add(duration)
}

// ClearPause clears any pause state (manual or timed).
func ClearPause() {
	movementPausedMu.Lock()
	movementPaused = false
	movementPausedMu.Unlock()

	pauseMu.Lock()
	pauseUntil = time.Time{}
	pauseMu.Unlock()
}