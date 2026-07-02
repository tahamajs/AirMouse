//go:build linux

package syscmd

import (
	"fmt"
	"os/exec"
	"strings"
)

func init() {
	execute = execLinux
}

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
	case "volume_up":
		cmd = exec.Command("xdotool", "key", "XF86AudioRaiseVolume")
	case "volume_down":
		cmd = exec.Command("xdotool", "key", "XF86AudioLowerVolume")
	case "mute":
		cmd = exec.Command("xdotool", "key", "XF86AudioMute")
	case "play_pause":
		cmd = exec.Command("xdotool", "key", "XF86AudioPlay")
	case "next_track":
		cmd = exec.Command("xdotool", "key", "XF86AudioNext")
	case "prev_track":
		cmd = exec.Command("xdotool", "key", "XF86AudioPrev")
	case "window_fullscreen":
		cmd = exec.Command("xdotool", "key", "F11")
	case "steer_left":
		cmd = exec.Command("xdotool", "key", "a")
	case "steer_right":
		cmd = exec.Command("xdotool", "key", "d")
	case "shoot":
		cmd = exec.Command("xdotool", "key", "space")
	case "reload":
		cmd = exec.Command("xdotool", "key", "r")
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
