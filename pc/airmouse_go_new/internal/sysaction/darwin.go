//go:build darwin

package sysaction

import (
	"os/exec"
)

func keyTap(key string) {
	// Use AppleScript for media keys
	// Simplified; a full implementation would use CGEventPost
	cmd := exec.Command("osascript", "-e", `tell application "System Events" to key code 100 using command down`)
	_ = cmd.Run()
}

func mouseClick(btn string) {
	// Not implemented in stub – full version uses CGO
}

func mouseDoubleClick() {}

func mouseScroll(delta int) {}

func lockScreen() {
	cmd := exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
	_ = cmd.Run()
}
