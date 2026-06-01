//go:build darwin

package sysaction

import (
    "os/exec"
)

func keyTap(key string) {
    // For simplicity, use AppleScript for media keys.
    // A full implementation would use CGEventPost.
    script := `tell application "System Events" to key code 100 using command down` // placeholder
    cmd := exec.Command("osascript", "-e", script)
    _ = cmd.Run()
}

func mouseClick(btn string) {
    // Not implemented in stub – use CGO for real implementation.
}

func mouseDoubleClick() {}

func mouseScroll(delta int) {}

func lockScreen() {
    cmd := exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
    _ = cmd.Run()
}