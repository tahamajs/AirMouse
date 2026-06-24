// Package sysaction provides a unified interface to execute system actions
// (media keys, volume, mouse clicks, keyboard shortcuts, power controls, etc.)
// across Windows, macOS, and Linux.
//
// It uses robotgo for cross‑platform key/mouse simulation and falls back to
// OS‑specific commands for volume and system power actions.
package sysaction

import (
	"fmt"
	"log"
	"os/exec"
	"runtime"
	"strings"

	"github.com/go-vgo/robotgo"
)

// Action represents a system action.
type Action string

// ============================================================
// Action Constants
// ============================================================

const (
	// Media Controls
	ActionPlayPause Action = "play_pause"
	ActionNextTrack Action = "next_track"
	ActionPrevTrack Action = "prev_track"
	ActionStop      Action = "stop"

	// Volume Controls
	ActionVolumeUp   Action = "volume_up"
	ActionVolumeDown Action = "volume_down"
	ActionMute       Action = "mute"

	// Mouse Actions
	ActionLeftClick   Action = "left_click"
	ActionRightClick  Action = "right_click"
	ActionMiddleClick Action = "middle_click"
	ActionDoubleClick Action = "double_click"
	ActionScrollUp    Action = "scroll_up"
	ActionScrollDown  Action = "scroll_down"

	// System Controls
	ActionLockScreen   Action = "lock_screen"
	ActionShowDesktop  Action = "show_desktop"
	ActionTaskView     Action = "task_view"
	ActionSwitchWindow Action = "switch_window"

	// Browser Controls
	ActionBrowserBack    Action = "browser_back"
	ActionBrowserForward Action = "browser_forward"
	ActionBrowserRefresh Action = "browser_refresh"
	ActionBrowserHome    Action = "browser_home"

	// Zoom Controls
	ActionZoomIn    Action = "zoom_in"
	ActionZoomOut   Action = "zoom_out"
	ActionZoomReset Action = "zoom_reset"

	// Window Controls
	ActionMinimizeWindow Action = "minimize_window"
	ActionMaximizeWindow Action = "maximize_window"
	ActionCloseWindow    Action = "close_window"
	ActionAltTab         Action = "alt_tab"

	// Text Editing
	ActionCopy      Action = "copy"
	ActionCut       Action = "cut"
	ActionPaste     Action = "paste"
	ActionUndo      Action = "undo"
	ActionRedo      Action = "redo"
	ActionSelectAll Action = "select_all"

	// Navigation
	ActionPageUp    Action = "page_up"
	ActionPageDown  Action = "page_down"
	ActionHome      Action = "home"
	ActionEnd       Action = "end"
	ActionDelete    Action = "delete"
	ActionBackspace Action = "backspace"
	ActionEnter     Action = "enter"
	ActionEscape    Action = "escape"
	ActionTab       Action = "tab"

	// Function Keys
	ActionF1  Action = "f1"
	ActionF2  Action = "f2"
	ActionF3  Action = "f3"
	ActionF4  Action = "f4"
	ActionF5  Action = "f5"
	ActionF6  Action = "f6"
	ActionF7  Action = "f7"
	ActionF8  Action = "f8"
	ActionF9  Action = "f9"
	ActionF10 Action = "f10"
	ActionF11 Action = "f11"
	ActionF12 Action = "f12"

	// Power Controls
	ActionSleep     Action = "sleep"
	ActionShutdown  Action = "shutdown"
	ActionRestart   Action = "restart"
	ActionLogout    Action = "logout"
)

// ============================================================
// Gesture → Action Mapping
// ============================================================

// ActionMap maps gesture names (from Android app) to system actions.
var ActionMap = map[string]Action{
	"ThumbsUp":   ActionPlayPause,
	"ThumbsDown": ActionStop,
	"LeftSwipe":  ActionPrevTrack,
	"RightSwipe": ActionNextTrack,
	"UpSwipe":    ActionVolumeUp,
	"DownSwipe":  ActionVolumeDown,
	"CircleCW":   ActionVolumeUp,
	"CircleCCW":  ActionVolumeDown,
	"Peace":      ActionLockScreen,
	"Fist":       ActionMute,
	"ZoomIn":     ActionZoomIn,
	"ZoomOut":    ActionZoomOut,
	"DoubleTap":  ActionDoubleClick,
	"LongPress":  ActionRightClick,
	"Shake":      ActionShowDesktop,
	"Pinch":      ActionZoomReset,
}

// ============================================================
// Public API
// ============================================================

// Execute performs the given system action.
func Execute(a Action) {
	log.Printf("Executing action: %s", a)

	switch a {
	// Media Controls
	case ActionPlayPause:
		mediaKey("play")
	case ActionNextTrack:
		mediaKey("next")
	case ActionPrevTrack:
		mediaKey("previous")
	case ActionStop:
		mediaKey("stop")

	// Volume Controls
	case ActionVolumeUp:
		volumeChange(1)
	case ActionVolumeDown:
		volumeChange(-1)
	case ActionMute:
		volumeMute()

	// Mouse Actions
	case ActionLeftClick:
		mouseClick("left")
	case ActionRightClick:
		mouseClick("right")
	case ActionMiddleClick:
		mouseClick("middle")
	case ActionDoubleClick:
		mouseDoubleClick()
	case ActionScrollUp:
		mouseScroll(1)
	case ActionScrollDown:
		mouseScroll(-1)

	// System Controls
	case ActionLockScreen:
		lockScreen()
	case ActionShowDesktop:
		keyCombination("command", "f3") // macOS; fallback to "win+d" on others
	case ActionTaskView:
		keyCombination("command", "tab") // macOS; fallback to "alt+tab"
	case ActionSwitchWindow:
		altTab()

	// Browser Controls
	case ActionBrowserBack:
		keyCombination("alt", "left")
	case ActionBrowserForward:
		keyCombination("alt", "right")
	case ActionBrowserRefresh:
		keyCombination("command", "r")
	case ActionBrowserHome:
		keyCombination("alt", "home")

	// Zoom Controls
	case ActionZoomIn:
		keyCombination("command", "=")
	case ActionZoomOut:
		keyCombination("command", "-")
	case ActionZoomReset:
		keyCombination("command", "0")

	// Window Controls
	case ActionMinimizeWindow:
		keyCombination("command", "m")
	case ActionMaximizeWindow:
		keyCombination("command", "f")
	case ActionCloseWindow:
		keyCombination("command", "w")
	case ActionAltTab:
		altTab()

	// Text Editing
	case ActionCopy:
		keyCombination("command", "c")
	case ActionCut:
		keyCombination("command", "x")
	case ActionPaste:
		keyCombination("command", "v")
	case ActionUndo:
		keyCombination("command", "z")
	case ActionRedo:
		keyCombination("command", "shift", "z")
	case ActionSelectAll:
		keyCombination("command", "a")

	// Navigation
	case ActionPageUp:
		robotgo.KeyTap("pageup")
	case ActionPageDown:
		robotgo.KeyTap("pagedown")
	case ActionHome:
		robotgo.KeyTap("home")
	case ActionEnd:
		robotgo.KeyTap("end")
	case ActionDelete:
		robotgo.KeyTap("delete")
	case ActionBackspace:
		robotgo.KeyTap("backspace")
	case ActionEnter:
		robotgo.KeyTap("enter")
	case ActionEscape:
		robotgo.KeyTap("escape")
	case ActionTab:
		robotgo.KeyTap("tab")

	// Function Keys
	case ActionF1:
		robotgo.KeyTap("f1")
	case ActionF2:
		robotgo.KeyTap("f2")
	case ActionF3:
		robotgo.KeyTap("f3")
	case ActionF4:
		robotgo.KeyTap("f4")
	case ActionF5:
		robotgo.KeyTap("f5")
	case ActionF6:
		robotgo.KeyTap("f6")
	case ActionF7:
		robotgo.KeyTap("f7")
	case ActionF8:
		robotgo.KeyTap("f8")
	case ActionF9:
		robotgo.KeyTap("f9")
	case ActionF10:
		robotgo.KeyTap("f10")
	case ActionF11:
		robotgo.KeyTap("f11")
	case ActionF12:
		robotgo.KeyTap("f12")

	// Power Controls
	case ActionSleep:
		sleepSystem()
	case ActionShutdown:
		shutdownSystem()
	case ActionRestart:
		restartSystem()
	case ActionLogout:
		logoutSystem()

	default:
		log.Printf("Unknown action: %s", a)
	}
}

// ExecuteGesture executes an action based on a gesture name and confidence.
func ExecuteGesture(gesture string, confidence float64) {
	if confidence < 0.7 {
		log.Printf("Gesture confidence too low: %.2f", confidence)
		return
	}
	if action, exists := ActionMap[gesture]; exists {
		Execute(action)
		log.Printf("Gesture %s -> Action %s (confidence: %.2f)", gesture, action, confidence)
	} else {
		log.Printf("No mapping for gesture: %s", gesture)
	}
}

// GetAvailableActions returns a list of all actions.
func GetAvailableActions() []Action {
	return []Action{
		ActionPlayPause, ActionNextTrack, ActionPrevTrack, ActionStop,
		ActionVolumeUp, ActionVolumeDown, ActionMute,
		ActionLeftClick, ActionRightClick, ActionMiddleClick, ActionDoubleClick,
		ActionScrollUp, ActionScrollDown,
		ActionLockScreen, ActionShowDesktop, ActionTaskView,
		ActionBrowserBack, ActionBrowserForward, ActionBrowserRefresh,
		ActionZoomIn, ActionZoomOut, ActionZoomReset,
		ActionMinimizeWindow, ActionMaximizeWindow, ActionCloseWindow,
		ActionCopy, ActionCut, ActionPaste, ActionUndo, ActionRedo, ActionSelectAll,
		ActionPageUp, ActionPageDown, ActionHome, ActionEnd, ActionDelete,
		ActionF1, ActionF2, ActionF3, ActionF4, ActionF5, ActionF6,
		ActionF7, ActionF8, ActionF9, ActionF10, ActionF11, ActionF12,
		ActionSleep, ActionShutdown, ActionRestart, ActionLogout,
	}
}

// GetActionDescription returns a human‑readable description.
func GetActionDescription(a Action) string {
	descriptions := map[Action]string{
		ActionPlayPause:     "Play/Pause Media",
		ActionNextTrack:     "Next Track",
		ActionPrevTrack:     "Previous Track",
		ActionStop:          "Stop Media",
		ActionVolumeUp:      "Volume Up",
		ActionVolumeDown:    "Volume Down",
		ActionMute:          "Mute",
		ActionLeftClick:     "Left Click",
		ActionRightClick:    "Right Click",
		ActionMiddleClick:   "Middle Click",
		ActionDoubleClick:   "Double Click",
		ActionScrollUp:      "Scroll Up",
		ActionScrollDown:    "Scroll Down",
		ActionLockScreen:    "Lock Screen",
		ActionShowDesktop:   "Show Desktop",
		ActionTaskView:      "Task View",
		ActionBrowserBack:   "Browser Back",
		ActionBrowserForward: "Browser Forward",
		ActionBrowserRefresh: "Browser Refresh",
		ActionZoomIn:        "Zoom In",
		ActionZoomOut:       "Zoom Out",
		ActionZoomReset:     "Zoom Reset",
		ActionCopy:          "Copy",
		ActionCut:           "Cut",
		ActionPaste:         "Paste",
		ActionUndo:          "Undo",
		ActionRedo:          "Redo",
		ActionSelectAll:     "Select All",
		ActionSleep:         "Sleep",
		ActionShutdown:      "Shutdown",
		ActionRestart:       "Restart",
		ActionLogout:        "Logout",
	}
	if desc, ok := descriptions[a]; ok {
		return desc
	}
	return string(a)
}

// ============================================================
// Internal Implementation
// ============================================================

// keyCombination presses multiple keys together (e.g., "ctrl+c").
// Accepts a variable number of key names; the last key is the one that is tapped,
// while the preceding keys are held down.
func keyCombination(keys ...string) {
	if len(keys) == 0 {
		return
	}
	// Hold all but the last key
	for i := 0; i < len(keys)-1; i++ {
		robotgo.KeyToggle(keys[i], "down")
	}
	// Tap the last key
	robotgo.KeyTap(keys[len(keys)-1])
	// Release all held keys (reverse order)
	for i := len(keys) - 2; i >= 0; i-- {
		robotgo.KeyToggle(keys[i], "up")
	}
}

// keyTap is a single key press.
func keyTap(key string) {
	robotgo.KeyTap(key)
}

// mouseClick simulates a mouse button click.
func mouseClick(button string) {
	btn := 0
	switch button {
	case "left":
		btn = 0
	case "right":
		btn = 1
	case "middle":
		btn = 2
	default:
		btn = 0
	}
	robotgo.Click(btn)
}

// mouseDoubleClick sends a double‑click.
func mouseDoubleClick() {
	robotgo.Click("left", 2)
}

// mouseScroll sends a scroll event (positive = up, negative = down).
func mouseScroll(delta int) {
	// robotgo.Scroll takes a vertical and horizontal direction.
	// A positive delta means scroll up (towards the user), negative means down.
	// In robotgo, Scroll(0, 1) scrolls up, Scroll(0, -1) scrolls down.
	robotgo.Scroll(0, delta)
}

// mediaKey sends a media key using robotgo's KeyTap with special key names.
// robotgo supports: "play", "next", "previous", "stop".
func mediaKey(key string) {
	robotgo.KeyTap(key)
}

// volumeChange uses platform‑specific commands to change volume.
func volumeChange(delta int) {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("osascript", "-e", fmt.Sprintf("set volume output volume (output volume of (get volume settings) + %d)", delta*5))
	case "linux":
		// Try pactl, fallback to amixer
		if _, err := exec.LookPath("pactl"); err == nil {
			cmd = exec.Command("pactl", "set-sink-volume", "0", fmt.Sprintf("%+d%%", delta*5))
		} else {
			cmd = exec.Command("amixer", "set", "Master", fmt.Sprintf("%+d%%", delta*5))
		}
	case "windows":
		// Use nircmd if available, otherwise fallback to sending volume keys
		if _, err := exec.LookPath("nircmd"); err == nil {
			cmd = exec.Command("nircmd", "changesysvolume", fmt.Sprintf("%d", delta*500))
		} else {
			// Send volume key press
			if delta > 0 {
				keyCombination("command", "volumeup")
			} else {
				keyCombination("command", "volumedown")
			}
			return
		}
	default:
		log.Printf("Volume control not implemented for %s", runtime.GOOS)
		return
	}
	if cmd != nil {
		if err := cmd.Run(); err != nil {
			log.Printf("Volume change failed: %v", err)
		}
	}
}

// volumeMute toggles mute using platform‑specific commands.
func volumeMute() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("osascript", "-e", "set volume output muted true")
	case "linux":
		if _, err := exec.LookPath("pactl"); err == nil {
			cmd = exec.Command("pactl", "set-sink-mute", "0", "toggle")
		} else {
			cmd = exec.Command("amixer", "set", "Master", "toggle")
		}
	case "windows":
		if _, err := exec.LookPath("nircmd"); err == nil {
			cmd = exec.Command("nircmd", "mute", "sysvolume")
		} else {
			keyCombination("command", "volumemute")
			return
		}
	default:
		log.Printf("Mute not implemented for %s", runtime.GOOS)
		return
	}
	if cmd != nil {
		if err := cmd.Run(); err != nil {
			log.Printf("Mute failed: %v", err)
		}
	}
}

// lockScreen locks the screen using platform‑specific commands.
func lockScreen() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		// Use pmset or CGSession
		cmd = exec.Command("/System/Library/CoreServices/Menu Extras/User.menu/Contents/Resources/CGSession", "-suspend")
	case "linux":
		// Try loginctl, gnome‑screensaver, etc.
		if _, err := exec.LookPath("loginctl"); err == nil {
			cmd = exec.Command("loginctl", "lock-session")
		} else if _, err := exec.LookPath("gnome-screensaver-command"); err == nil {
			cmd = exec.Command("gnome-screensaver-command", "-l")
		} else {
			cmd = exec.Command("xdg-screensaver", "lock")
		}
	case "windows":
		cmd = exec.Command("rundll32.exe", "user32.dll,LockWorkStation")
	default:
		log.Printf("Lock screen not implemented for %s", runtime.GOOS)
		return
	}
	if err := cmd.Run(); err != nil {
		log.Printf("Lock screen failed: %v", err)
	}
}

// altTab simulates Alt+Tab to switch windows.
func altTab() {
	// Press and release Alt+Tab
	robotgo.KeyToggle("alt", "down")
	robotgo.KeyTap("tab")
	robotgo.KeyToggle("alt", "up")
}

// sleepSystem puts the system to sleep.
func sleepSystem() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("pmset", "sleepnow")
	case "linux":
		cmd = exec.Command("systemctl", "suspend")
	case "windows":
		cmd = exec.Command("rundll32.exe", "powrprof.dll,SetSuspendState", "0", "1", "0")
	default:
		log.Printf("Sleep not implemented for %s", runtime.GOOS)
		return
	}
	if err := cmd.Run(); err != nil {
		log.Printf("Sleep failed: %v", err)
	}
}

// shutdownSystem shuts down the system.
func shutdownSystem() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("shutdown", "-h", "now")
	case "linux":
		cmd = exec.Command("shutdown", "-h", "now")
	case "windows":
		cmd = exec.Command("shutdown", "/s", "/t", "0")
	default:
		log.Printf("Shutdown not implemented for %s", runtime.GOOS)
		return
	}
	if err := cmd.Run(); err != nil {
		log.Printf("Shutdown failed: %v", err)
	}
}

// restartSystem restarts the system.
func restartSystem() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("shutdown", "-r", "now")
	case "linux":
		cmd = exec.Command("shutdown", "-r", "now")
	case "windows":
		cmd = exec.Command("shutdown", "/r", "/t", "0")
	default:
		log.Printf("Restart not implemented for %s", runtime.GOOS)
		return
	}
	if err := cmd.Run(); err != nil {
		log.Printf("Restart failed: %v", err)
	}
}

// logoutSystem logs out the current user.
func logoutSystem() {
	var cmd *exec.Cmd
	switch runtime.GOOS {
	case "darwin":
		cmd = exec.Command("osascript", "-e", "tell application \"System Events\" to log out")
	case "linux":
		cmd = exec.Command("gnome-session-quit", "--logout", "--no-prompt")
	case "windows":
		cmd = exec.Command("shutdown", "/l")
	default:
		log.Printf("Logout not implemented for %s", runtime.GOOS)
		return
	}
	if err := cmd.Run(); err != nil {
		log.Printf("Logout failed: %v", err)
	}
}

// showDesktop: not a simple key, but we use Cmd+F3 on macOS; fallback to Win+D on Windows.
func showDesktop() {
	switch runtime.GOOS {
	case "darwin":
		keyCombination("command", "f3")
	case "windows":
		keyCombination("command", "d")
	default:
		keyCombination("command", "d")
	}
}