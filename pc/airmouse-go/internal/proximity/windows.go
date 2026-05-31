//go:build windows

package proximity

import (
	"log"
	"syscall"
)

var (
	user32          = syscall.NewLazyDLL("user32.dll")
	lockWorkStation = user32.NewProc("LockWorkStation")
)

func (m *Manager) lockScreen() {
	lockWorkStation.Call()
	log.Println("Proximity: Windows screen locked")
}

func (m *Manager) unlockScreen() {
	// Windows does not support programmatic unlock
	log.Println("Proximity: Unlock not supported on Windows")
}