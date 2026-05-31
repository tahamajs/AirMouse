//go:build linux

package sysaction

import (
	"log"
	"os/exec"
)

var keyMap = map[string]string{
	"media_play_pause": "XF86AudioPlay",
	"media_next":       "XF86AudioNext",
	"media_prev":       "XF86AudioPrev",
	"audio_vol_up":     "XF86AudioRaiseVolume",
	"audio_vol_down":   "XF86AudioLowerVolume",
	"audio_mute":       "XF86AudioMute",
	"media_stop":       "XF86AudioStop",
	"browser_back":     "Alt+Left",
	"browser_forward":  "Alt+Right",
}

func keyTap(key string) {
	xkey, ok := keyMap[key]
	if !ok {
		log.Printf("Unknown Linux key: %s", key)
		return
	}
	// Use xdotool if available, fallback to nothing
	cmd := exec.Command("xdotool", "key", xkey)
	if err := cmd.Run(); err != nil {
		log.Printf("xdotool failed: %v", err)
	}
}

func mouseClick(btn string) {
	cmd := exec.Command("xdotool", "click", btn)
	cmd.Run()
}

func mouseDoubleClick() {
	cmd := exec.Command("xdotool", "click", "--repeat", "2", "1")
	cmd.Run()
}

func mouseScroll(delta int) {
	dir := "up"
	if delta < 0 {
		dir = "down"
	}
	cmd := exec.Command("xdotool", "click", "--repeat", "1", dir)
	cmd.Run()
}

func lockScreen() {
	cmd := exec.Command("loginctl", "lock-session")
	if err := cmd.Run(); err != nil {
		log.Printf("Lock screen failed: %v", err)
	}
}