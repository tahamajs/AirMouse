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
		// Use `cliclick` if available, else fallback to python3 Quartz.
		if _, err := exec.LookPath("cliclick"); err == nil {
			switch btn {
			case "left":
				_ = exec.Command("cliclick", "c:.").Run()
			case "right":
				_ = exec.Command("cliclick", "rc:.").Run()
			case "middle":
				_ = exec.Command("cliclick", "mc:.").Run()
			}
			return
		}

		var pyCode string
		switch btn {
		case "left":
			pyCode = `from Quartz.CoreGraphics import CGEventCreate, CGEventGetLocation, CGEventCreateMouseEvent, CGEventPost, kCGEventLeftMouseDown, kCGEventLeftMouseUp, kCGMouseButtonLeft, kCGHIDEventTap; pos = CGEventGetLocation(CGEventCreate(None)); CGEventPost(kCGHIDEventTap, CGEventCreateMouseEvent(None, kCGEventLeftMouseDown, pos, kCGMouseButtonLeft)); CGEventPost(kCGHIDEventTap, CGEventCreateMouseEvent(None, kCGEventLeftMouseUp, pos, kCGMouseButtonLeft))`
		case "right":
			pyCode = `from Quartz.CoreGraphics import CGEventCreate, CGEventGetLocation, CGEventCreateMouseEvent, CGEventPost, kCGEventRightMouseDown, kCGEventRightMouseUp, kCGMouseButtonRight, kCGHIDEventTap; pos = CGEventGetLocation(CGEventCreate(None)); CGEventPost(kCGHIDEventTap, CGEventCreateMouseEvent(None, kCGEventRightMouseDown, pos, kCGMouseButtonRight)); CGEventPost(kCGHIDEventTap, CGEventCreateMouseEvent(None, kCGEventRightMouseUp, pos, kCGMouseButtonRight))`
		case "middle":
			pyCode = `from Quartz.CoreGraphics import CGEventCreate, CGEventGetLocation, CGEventCreateMouseEvent, CGEventPost, kCGEventOtherMouseDown, kCGEventOtherMouseUp, kCGMouseButtonCenter, kCGHIDEventTap; pos = CGEventGetLocation(CGEventCreate(None)); CGEventPost(kCGHIDEventTap, CGEventCreateMouseEvent(None, kCGEventOtherMouseDown, pos, kCGMouseButtonCenter)); CGEventPost(kCGHIDEventTap, CGEventCreateMouseEvent(None, kCGEventOtherMouseUp, pos, kCGMouseButtonCenter))`
		}
		if pyCode != "" {
			if exec.Command("python3", "-c", pyCode).Run() == nil {
				return
			}
		}
		fmt.Printf("cliclick and python3 Quartz not available; simulate %s click\n", btn)
	}
	mouseDoubleClick = func() {
		if _, err := exec.LookPath("cliclick"); err == nil {
			_ = exec.Command("cliclick", "dc:.").Run()
			return
		}
		pyCode := `from Quartz.CoreGraphics import CGEventCreate, CGEventGetLocation, CGEventCreateMouseEvent, CGEventPost, kCGEventLeftMouseDown, kCGEventLeftMouseUp, kCGMouseButtonLeft, kCGHIDEventTap, kCGMouseEventClickState; pos = CGEventGetLocation(CGEventCreate(None)); d1 = CGEventCreateMouseEvent(None, kCGEventLeftMouseDown, pos, kCGMouseButtonLeft); u1 = CGEventCreateMouseEvent(None, kCGEventLeftMouseUp, pos, kCGMouseButtonLeft); d2 = CGEventCreateMouseEvent(None, kCGEventLeftMouseDown, pos, kCGMouseButtonLeft); d2.setIntegerValueField(kCGMouseEventClickState, 2); u2 = CGEventCreateMouseEvent(None, kCGEventLeftMouseUp, pos, kCGMouseButtonLeft); u2.setIntegerValueField(kCGMouseEventClickState, 2); CGEventPost(kCGHIDEventTap, d1); CGEventPost(kCGHIDEventTap, u1); CGEventPost(kCGHIDEventTap, d2); CGEventPost(kCGHIDEventTap, u2)`
		_ = exec.Command("python3", "-c", pyCode).Run()
	}
	mouseScroll = func(delta int) {
		// cliclick scroll: https://github.com/BlueM/cliclick
		if _, err := exec.LookPath("cliclick"); err == nil {
			if delta > 0 {
				_ = exec.Command("cliclick", "wu:5").Run()
			} else {
				_ = exec.Command("cliclick", "wd:5").Run()
			}
			return
		}
		amount := 5
		if delta < 0 {
			amount = -5
		}
		pyCode := fmt.Sprintf(`from Quartz.CoreGraphics import CGEventCreateScrollWheelEvent, CGEventPost, kCGHIDEventTap; CGEventPost(kCGHIDEventTap, CGEventCreateScrollWheelEvent(None, 0, 1, %d))`, amount)
		_ = exec.Command("python3", "-c", pyCode).Run()
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
		} else if len(key) == 1 {
			_ = exec.Command("osascript", "-e", fmt.Sprintf(`tell application "System Events" to keystroke "%s"`, key)).Run()
		} else {
			_ = exec.Command("osascript", "-e", fmt.Sprintf(`tell application "System Events" to keystroke "%s"`, key)).Run()
		}
	}
	typeText = func(txt string) {
		// Escape quotes for AppleScript
		escaped := txt
		// Replace backslash first, then quotes
		// Go strings: \\ -> \, \" -> "
		// But AppleScript needs double-escaped backslash/quotes
		// We'll keep it simple: just type it using osascript
		script := fmt.Sprintf(`tell application "System Events" to keystroke "%s"`, escaped)
		_ = exec.Command("osascript", "-e", script).Run()
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
	setSystemMouseSpeed = func(speed float64) {
		// macOS defaults command (speed is typically 0 to 3.0 or 5.0)
		// Let's execute: defaults write -g com.apple.mouse.scaling <speed>
		_ = exec.Command("defaults", "write", "-g", "com.apple.mouse.scaling", fmt.Sprintf("%.2f", speed)).Run()
	}
	keyDown = func(key string) {
		_ = exec.Command("osascript", "-e", fmt.Sprintf(`tell application "System Events" to key down "%s"`, key)).Run()
	}
	keyUp = func(key string) {
		_ = exec.Command("osascript", "-e", fmt.Sprintf(`tell application "System Events" to key up "%s"`, key)).Run()
	}
	mouseHScroll = func(delta int) {
		amount := 5
		if delta < 0 {
			amount = -5
		}
		pyCode := fmt.Sprintf(`from Quartz.CoreGraphics import CGEventCreateScrollWheelEvent, CGEventPost, kCGHIDEventTap; CGEventPost(kCGHIDEventTap, CGEventCreateScrollWheelEvent(None, 0, 2, 0, %d))`, amount)
		_ = exec.Command("python3", "-c", pyCode).Run()
	}
	adjustBrightness = func(direction string, amount float64) {
		if direction == "up" {
			_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 144`).Run()
		} else {
			_ = exec.Command("osascript", "-e", `tell application "System Events" to key code 145`).Run()
		}
	}
	releaseAllKeys = func() {
		_ = exec.Command("osascript", "-e", `tell application "System Events" to key up {shift, control, option, command}`).Run()
	}
}
