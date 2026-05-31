//go:build !robotgo

package sysaction

import (
	"log"
	"os/exec"
	"runtime"
)

func lockScreenFallback() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "windows":
		cmd = exec.Command("rundll32.exe", "user32.dll,LockWorkStation")
	case "darwin":
		cmd = exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
	case "linux":
		cmd = exec.Command("loginctl", "lock-session")
	default:
		log.Println("Lock screen not supported on this OS")
		return
	}
	if err := cmd.Run(); err != nil {
		log.Printf("Lock screen failed: %v", err)
	}
}