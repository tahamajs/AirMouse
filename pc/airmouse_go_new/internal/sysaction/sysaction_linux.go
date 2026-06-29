//go:build linux

package sysaction

import (
	"fmt"
	"os/exec"
)

// Key mapping for xdotool and other Linux tools.
var linuxKeyMap = map[string]string{
	// Media Keys
	"PlayPause": "XF86AudioPlay",
	"Next":      "XF86AudioNext",
	"Previous":  "XF86AudioPrev",
	"Stop":      "XF86AudioStop",

	// Volume Keys
	"VolumeUp":   "XF86AudioRaiseVolume",
	"VolumeDown": "XF86AudioLowerVolume",
	"Mute":       "XF86AudioMute",

	// Browser Keys
	"browser_back":    "Alt+Left",
	"browser_forward": "Alt+Right",
	"browser_refresh": "Ctrl+R",
	"browser_home":    "Alt+Home",

	// Navigation
	"page_up":   "Page_Up",
	"page_down": "Page_Down",
	"home":      "Home",
	"end":       "End",
	"delete":    "Delete",
	"backspace": "BackSpace",
	"enter":     "Return",
	"escape":    "Escape",
	"tab":       "Tab",

	// Function Keys
	"f1":  "F1",
	"f2":  "F2",
	"f3":  "F3",
	"f4":  "F4",
	"f5":  "F5",
	"f6":  "F6",
	"f7":  "F7",
	"f8":  "F8",
	"f9":  "F9",
	"f10": "F10",
	"f11": "F11",
	"f12": "F12",
}

func init() {
	mediaKey = func(key string) {
		if keyCode, ok := linuxKeyMap[key]; ok {
			_ = exec.Command("xdotool", "key", keyCode).Run()
		}
	}
	volumeUp = func() {
		// Try pactl first, fallback to amixer
		if err := exec.Command("pactl", "set-sink-volume", "@DEFAULT_SINK@", "+5%").Run(); err != nil {
			_ = exec.Command("amixer", "set", "Master", "5%+").Run()
		}
	}
	volumeDown = func() {
		if err := exec.Command("pactl", "set-sink-volume", "@DEFAULT_SINK@", "-5%").Run(); err != nil {
			_ = exec.Command("amixer", "set", "Master", "5%-").Run()
		}
	}
	volumeMute = func() {
		if err := exec.Command("pactl", "set-sink-mute", "@DEFAULT_SINK@", "toggle").Run(); err != nil {
			_ = exec.Command("amixer", "set", "Master", "toggle").Run()
		}
	}
	mouseClick = func(btn string) {
		var button int
		switch btn {
		case "left":
			button = 1
		case "right":
			button = 3
		case "middle":
			button = 2
		default:
			return
		}
		_ = exec.Command("xdotool", "click", fmt.Sprintf("%d", button)).Run()
	}
	mouseDoubleClick = func() {
		_ = exec.Command("xdotool", "click", "--repeat", "2", "1").Run()
	}
	mouseScroll = func(delta int) {
		if delta > 0 {
			_ = exec.Command("xdotool", "click", "4").Run() // up
		} else {
			_ = exec.Command("xdotool", "click", "5").Run() // down
		}
	}
	lockScreen = func() {
		// Try various lock commands
		commands := [][]string{
			{"loginctl", "lock-session"},
			{"gnome-screensaver-command", "-l"},
			{"xdg-screensaver", "lock"},
			{"dm-tool", "lock"},
			{"gdmflexiserver", "--lock"},
		}
		for _, cmd := range commands {
			if err := exec.Command(cmd[0], cmd[1:]...).Run(); err == nil {
				return
			}
		}
	}
	showDesktop = func() {
		_ = exec.Command("xdotool", "key", "ctrl+alt+d").Run()
	}
	taskView = func() {
		_ = exec.Command("xdotool", "key", "super+tab").Run()
	}
	switchWindow = func() {
		_ = exec.Command("xdotool", "key", "alt+tab").Run()
	}
	keyCombination = func(key, modifier string) {
		var combo string
		switch key {
		case "copy":
			combo = "ctrl+c"
		case "cut":
			combo = "ctrl+x"
		case "paste":
			combo = "ctrl+v"
		case "undo":
			combo = "ctrl+z"
		case "redo":
			combo = "ctrl+shift+z"
		case "select_all":
			combo = "ctrl+a"
		case "browser_back":
			combo = "alt+Left"
		case "browser_forward":
			combo = "alt+Right"
		case "browser_refresh":
			combo = "ctrl+r"
		case "browser_home":
			combo = "alt+Home"
		default:
			if keyCode, ok := linuxKeyMap[key]; ok {
				combo = keyCode
			}
		}
		if combo != "" {
			_ = exec.Command("xdotool", "key", combo).Run()
		}
	}
	keyTap = func(key string) {
		if keyCode, ok := linuxKeyMap[key]; ok {
			_ = exec.Command("xdotool", "key", keyCode).Run()
		} else {
			_ = exec.Command("xdotool", "key", key).Run()
		}
	}
	typeText = func(txt string) {
		_ = exec.Command("xdotool", "type", txt).Run()
	}
	zoomIn = func() {
		_ = exec.Command("xdotool", "key", "ctrl+plus").Run()
	}
	zoomOut = func() {
		_ = exec.Command("xdotool", "key", "ctrl+minus").Run()
	}
	zoomReset = func() {
		_ = exec.Command("xdotool", "key", "ctrl+0").Run()
	}
	minimizeWindow = func() {
		_ = exec.Command("xdotool", "key", "alt+F9").Run()
	}
	maximizeWindow = func() {
		_ = exec.Command("xdotool", "key", "alt+F10").Run()
	}
	closeWindow = func() {
		_ = exec.Command("xdotool", "key", "alt+F4").Run()
	}
	altTab = func() {
		_ = exec.Command("xdotool", "key", "alt+Tab").Run()
	}
	sleepSystem = func() {
		_ = exec.Command("systemctl", "suspend").Run()
	}
	shutdownSystem = func() {
		_ = exec.Command("shutdown", "-h", "now").Run()
	}
	restartSystem = func() {
		_ = exec.Command("shutdown", "-r", "now").Run()
	}
	logoutSystem = func() {
		_ = exec.Command("gnome-session-quit", "--no-prompt").Run()
	}
}
