//go:build darwin

package proximity

import (
	"log"
	"os/exec"
)

func (m *Manager) lockScreen() {
	cmd := exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
	if err := cmd.Run(); err != nil {
		log.Printf("Proximity: macOS lock failed: %v", err)
	} else {
		log.Println("Proximity: macOS screen locked")
	}
}

func (m *Manager) unlockScreen() {
	// macOS does not support automatic unlock
	log.Println("Proximity: Unlock not supported on macOS")
}