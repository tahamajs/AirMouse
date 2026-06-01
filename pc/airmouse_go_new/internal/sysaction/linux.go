//go:build linux

package sysaction

import (
	"os/exec"
)

func keyTap(key string) {
	cmd := exec.Command("xdotool", "key", key)
	_ = cmd.Run()
}

func mouseClick(btn string) {
	cmd := exec.Command("xdotool", "click", btn)
	_ = cmd.Run()
}

func mouseDoubleClick() {
	cmd := exec.Command("xdotool", "click", "--repeat", "2", "1")
	_ = cmd.Run()
}

func mouseScroll(delta int) {
	dir := "up"
	if delta < 0 {
		dir = "down"
	}
	cmd := exec.Command("xdotool", "click", "--repeat", "1", dir)
	_ = cmd.Run()
}

func lockScreen() {
	cmd := exec.Command("loginctl", "lock-session")
	_ = cmd.Run()
}
