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

func SetMovementPaused(paused bool) {
	movementPausedMu.Lock()
	defer movementPausedMu.Unlock()
	movementPaused = paused
}

func IsMovementPaused() bool {
	movementPausedMu.RLock()
	defer movementPausedMu.RUnlock()
	pauseMu.RLock()
	timedPause := time.Now().Before(pauseUntil)
	pauseMu.RUnlock()
	return movementPaused || timedPause
}

func PauseForDuration(duration time.Duration) {
	pauseMu.Lock()
	defer pauseMu.Unlock()
	pauseUntil = time.Now().Add(duration)
}

func ClearPause() {
	movementPausedMu.Lock()
	movementPaused = false
	movementPausedMu.Unlock()
	pauseMu.Lock()
	pauseUntil = time.Time{}
	pauseMu.Unlock()
}
