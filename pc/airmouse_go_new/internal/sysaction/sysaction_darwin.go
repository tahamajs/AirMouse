//go:build darwin

package sysaction

import (
	"fmt"
	"os/exec"
)

func init() {
	mediaKey = func(key string) {
		// Use AppleScript to send media key events via System Events
		var script string
		switch key {
		case "PlayPause":
			script = `tell application "System Events" to key code 111`
		case "Next":
			script = `tell application "System Events" to key code 124 using {command down, option down}`
		case "Previous":
			script = `tell application "System Events" to key code 123 using {command down, option down}`
		case "Stop":
			script = `tell application "System Events" to key code 31 using {command down}`
		default:
			return
		}
		_ = exec.Command("osascript", "-e", script).Run()
	}
	volumeUp = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 73`).Run()
	}
	volumeDown = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 74`).Run()
	}
	volumeMute = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 72`).Run()
	}
	mouseClick = func(btn string) {
		// Use `cliclick` if available, else fallback to print.
		if _, err := exec.LookPath("cliclick"); err == nil {
			switch btn {
			case "left":
				_ = exec.Command("cliclick", "c:.").Run()
			case "right":
				_ = exec.Command("cliclick", "rc:.").Run()
			case "middle":
				_ = exec.Command("cliclick", "mc:.").Run()
			}
		} else {
			fmt.Printf("cliclick not installed; simulate %s click\n", btn)
		}
	}
	mouseDoubleClick = func() {
		if _, err := exec.LookPath("cliclick"); err == nil {
			_ = exec.Command("cliclick", "dc:.").Run()
		}
	}
	mouseScroll = func(delta int) {
		// cliclick scroll: https://github.com/BlueM/cliclick
		if _, err := exec.LookPath("cliclick"); err == nil {
			if delta > 0 {
				_ = exec.Command("cliclick", "wu:5").Run()
			} else {
				_ = exec.Command("cliclick", "wd:5").Run()
			}
		}
	}
	lockScreen = func() {
		_ = exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend").Run()
	}
	showDesktop = func() {
		// Use `cmd+F3` (Show Desktop) or `cmd+alt+d` (Hide/Show Dock)
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 101 using {command down}`).Run()
	}
	taskView = func() {
		// `ctrl+up` (Mission Control) on macOS
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 126 using {control down}`).Run()
	}
	switchWindow = func() {
		// `cmd+tab` (App Switcher)
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 48 using {command down}`).Run()
	}
	keyCombination = func(key, modifier string) {
		var script string
		switch key {
		case "copy":
			script = `tell application "System Events" to key code 8 using {command down}`
		case "cut":
			script = `tell application "System Events" to key code 7 using {command down}`
		case "paste":
			script = `tell application "System Events" to key code 9 using {command down}`
		case "undo":
			script = `tell application "System Events" to key code 6 using {command down}`
		case "redo":
			script = `tell application "System Events" to key code 6 using {command down, shift down}`
		case "select_all":
			script = `tell application "System Events" to key code 0 using {command down}`
		case "browser_back":
			script = `tell application "System Events" to key code 123 using {command down}`
		case "browser_forward":
			script = `tell application "System Events" to key code 124 using {command down}`
		case "browser_refresh":
			script = `tell application "System Events" to key code 15 using {command down}`
		case "browser_home":
			script = `tell application "System Events" to key code 0 using {command down, shift down}`
		default:
			// fallback to keyTap
			keyTap(key)
			return
		}
		_ = exec.Command("osascript", "-e", script).Run()
	}
	keyTap = func(key string) {
		// Map common key names to key codes (simplified)
		keyCodes := map[string]string{
			"page_up":   `key code 116`,
			"page_down": `key code 121`,
			"home":      `key code 115`,
			"end":       `key code 119`,
			"delete":    `key code 117`,
			"backspace": `key code 51`,
			"enter":     `key code 36`,
			"escape":    `key code 53`,
			"tab":       `key code 48`,
			"f1":        `key code 122`,
			"f2":        `key code 120`,
			"f3":        `key code 99`,
			"f4":        `key code 118`,
			"f5":        `key code 96`,
			"f6":        `key code 97`,
			"f7":        `key code 98`,
			"f8":        `key code 100`,
			"f9":        `key code 101`,
			"f10":       `key code 109`,
			"f11":       `key code 103`,
			"f12":       `key code 111`,
		}
		if code, ok := keyCodes[key]; ok {
			_ = exec.Command("osascript", "-e", `tell application "System Events" to `+code).Run()
		}
	}
	zoomIn = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 44 using {command down}`).Run()
	}
	zoomOut = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 45 using {command down}`).Run()
	}
	zoomReset = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 46 using {command down}`).Run()
	}
	minimizeWindow = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 32 using {command down}`).Run()
	}
	maximizeWindow = func() {
		// Use `cmd+ctrl+f` for full‑screen toggle
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 3 using {control down, command down}`).Run()
	}
	closeWindow = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 13 using {command down}`).Run()
	}
	altTab = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 48 using {command down}`).Run()
	}
	sleepSystem = func() {
		_ = exec.Command("pmset", "sleepnow").Run()
	}
	shutdownSystem = func() {
		_ = exec.Command("osascript", "-e", `tell app "System Events" to shut down`).Run()
	}
	restartSystem = func() {
		_ = exec.Command("osascript", "-e", `tell app "System Events" to restart`).Run()
	}
	logoutSystem = func() {
		_ = exec.Command("osascript", "-e", `tell app "System Events" to log out`).Run()
	}
}
