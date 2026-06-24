//go:build linux

package syscmd

import (
	"fmt"
	"os/exec"
	"strings"
)

func execLinux(command string) error {
	var cmd *exec.Cmd
	switch command {
	case "show_desktop":
		cmd = exec.Command("xdotool", "key", "ctrl+alt+d")
	case "task_view":
		cmd = exec.Command("xdotool", "key", "super+tab")
	case "switch_window":
		cmd = exec.Command("xdotool", "key", "alt+tab")
	case "window_close":
		cmd = exec.Command("xdotool", "key", "alt+F4")
	case "lock_screen":
		// Try multiple lock commands
		lockCmds := [][]string{
			{"loginctl", "lock-session"},
			{"gnome-screensaver-command", "-l"},
			{"xdg-screensaver", "lock"},
			{"dm-tool", "lock"},
		}
		for _, lc := range lockCmds {
			if err := exec.Command(lc[0], lc[1:]...).Run(); err == nil {
				return nil
			}
		}
		return fmt.Errorf("no lock command succeeded")
	case "zoom_in":
		cmd = exec.Command("xdotool", "key", "ctrl+plus")
	case "zoom_out":
		cmd = exec.Command("xdotool", "key", "ctrl+minus")
	case "zoom_reset":
		cmd = exec.Command("xdotool", "key", "ctrl+0")
	default:
		return nil
	}
	if cmd != nil {
		if out, err := cmd.CombinedOutput(); err != nil {
			return fmt.Errorf("command %s failed: %w (%s)", command, err, strings.TrimSpace(string(out)))
		}
	}
	return nil
}