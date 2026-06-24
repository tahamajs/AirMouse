//go:build darwin

package syscmd

import (
	"fmt"
	"os/exec"
	"strings"
)

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
	default:
		return "", nil
	}


	//go:build darwin

package syscmd

import (
	"fmt"
	"os/exec"
	"strings"
)

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
	default:
		return "", nil
	}
}
}