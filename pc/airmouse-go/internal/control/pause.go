package control

import "sync"

var (
    movementPaused   bool
    movementPausedMu sync.RWMutex
)

func SetMovementPaused(paused bool) {
    movementPausedMu.Lock()
    defer movementPausedMu.Unlock()
    movementPaused = paused
}

func IsMovementPaused() bool {
    movementPausedMu.RLock()
    defer movementPausedMu.RUnlock()
    return movementPaused
}