//go:build linux

package proximity

import (
	"log"
	"os/exec"
)

func (m *Manager) lockScreen() {
	// Try multiple methods
	methods := []string{
		"loginctl lock-session",
		"gnome-screensaver-command --lock",
		"xscreensaver-command -lock",
		"qdbus org.freedesktop.ScreenSaver /ScreenSaver Lock",
	}
	for _, cmdStr := range methods {
		cmd := exec.Command("sh", "-c", cmdStr)
		if err := cmd.Run(); err == nil {
			log.Printf("Proximity: Linux screen locked via %s", cmdStr)
			return
		}
	}
	log.Println("Proximity: Linux lock failed – no working method")
}

func (m *Manager) unlockScreen() {
	// Automatic unlock may be possible with certain DE settings
	// e.g., GNOME with automatic login disabled
	if m.unlockCmd != "" {
		cmd := exec.Command("sh", "-c", m.unlockCmd)
		if err := cmd.Run(); err != nil {
			log.Printf("Proximity: Linux unlock failed: %v", err)
		} else {
			log.Println("Proximity: Linux screen unlocked")
		}
	} else {
		log.Println("Proximity: Unlock not configured on Linux")
	}
}