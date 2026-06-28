//go:build darwin

package syscmd

import (
	"fmt"
	"os/exec"
	"strings"
)

func init() {
	execute = execDarwin
}

func execDarwin(command string) error {
	script, err := appleScriptForCommand(command)
	if err != nil {
		return err
	}
	if script == "" {
		return nil
	}
	cmd := exec.Command("osascript", "-e", script)
	if out, err := cmd.CombinedOutput(); err != nil {
		return fmt.Errorf("osascript failed for %s: %w (%s)", command, err, strings.TrimSpace(string(out)))
	}
	return nil
}

func appleScriptForCommand(command string) (string, error) {
	switch command {
	case "show_desktop":
		return `tell application "System Events" to key code 103 using {command down, option down}`, nil
	case "task_view":
		return `tell application "System Events" to key code 126 using control down`, nil
	case "switch_window":
		return `tell application "System Events" to key code 48 using command down`, nil
	case "window_close":
		return `tell application "System Events" to keystroke "w" using command down`, nil
	case "lock_screen":
		return `tell application "System Events" to key code 12 using {command down, control down}`, nil
	case "zoom_in":
		return `tell application "System Events" to keystroke "+" using command down`, nil
	case "zoom_out":
		return `tell application "System Events" to keystroke "-" using command down`, nil
	case "zoom_reset":
		return `tell application "System Events" to keystroke "0" using command down`, nil
	case "volume_up":
		return `tell application "System Events" to key code 72`, nil
	case "volume_down":
		return `tell application "System Events" to key code 73`, nil
	case "mute":
		return `tell application "System Events" to key code 74`, nil
	case "play_pause":
		return `tell application "System Events" to key code 100`, nil
	case "next_track":
		return `tell application "System Events" to key code 101`, nil
	case "prev_track":
		return `tell application "System Events" to key code 98`, nil
	case "window_maximize":
		return `tell application "System Events" to keystroke "m" using {command down, option down}`, nil
	case "window_minimize":
		return `tell application "System Events" to keystroke "m" using command down`, nil
	case "window_fullscreen":
		return `tell application "System Events" to keystroke "f" using {command down, control down}`, nil
	case "browser_back":
		return `tell application "System Events" to keystroke "[" using command down`, nil
	case "browser_forward":
		return `tell application "System Events" to keystroke "]" using command down`, nil
	case "browser_refresh":
		return `tell application "System Events" to keystroke "r" using command down`, nil
	case "browser_home":
		return `tell application "System Events" to keystroke "h" using {command down, shift down}`, nil
	default:
		return "", nil
	}
}