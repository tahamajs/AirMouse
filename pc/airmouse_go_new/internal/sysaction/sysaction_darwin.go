//go:build darwin

package sysaction

import (
    "fmt"
    "os/exec"
)

func keyTap(key string) {
    script := fmt.Sprintf(`tell application "System Events" to key code %s`, key)
    cmd := exec.Command("osascript", "-e", script)
    _ = cmd.Run()
}

func mouseClick(btn string) {
    // Placeholder for macOS
    fmt.Printf("Mouse click: %s\n", btn)
}

func mouseDoubleClick() {
    fmt.Println("Double click")
}

func mouseScroll(delta int) {
    fmt.Printf("Scroll: %d\n", delta)
}

func lockScreen() {
    cmd := exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
    _ = cmd.Run()
}
