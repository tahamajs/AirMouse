//go:build darwin

package sysaction

import (
	"log"
	"os/exec"
)

var keyMap = map[string]string{
	"media_play_pause": "play",
	"media_next":       "next",
	"media_prev":       "previous",
	"audio_vol_up":     "volume up",
	"audio_vol_down":   "volume down",
	"audio_mute":       "mute",
	"media_stop":       "stop",
}

func keyTap(key string) {
	script, ok := keyMap[key]
	if !ok {
		log.Printf("Unknown macOS key: %s", key)
		return
	}
	cmd := exec.Command("osascript", "-e", `tell application "System Events" to key code 100 using command down`)
	// For simplicity, use AppleScript for media keys
	// A full implementation would use CGEventPost (Core Graphics)
	log.Printf("Key tap not fully implemented on macOS: %s", key)
}

func mouseClick(btn string) {
	// Use CGEventPost via CGO (simplified)
	log.Printf("Mouse click not fully implemented on macOS: %s", btn)
}

func mouseDoubleClick() {
	mouseClick("left")
	mouseClick("left")
}

func mouseScroll(delta int) {
	// Use CGEventCreateScrollWheelEvent
	log.Printf("Scroll not fully implemented on macOS")
}

func lockScreen() {
	cmd := exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
	cmd.Run()
}