// Package syscmd executes system commands (media, lock, zoom, etc.) in a cross‑platform way.
package syscmd

import (
	"runtime"
)

// ExecuteSystemCommand runs a system command by name.
// It dispatches to the platform‑specific implementation.
func ExecuteSystemCommand(command string) error {
	switch runtime.GOOS {
	case "darwin":
		return execDarwin(command)
	case "linux":
		return execLinux(command)
	case "windows":
		return execWindows(command)
	default:
		return nil
	}
}

package syscmd

import "runtime"

func ExecuteSystemCommand(command string) error {
	switch runtime.GOOS {
	case "darwin":
		return execDarwin(command)
	case "linux":
		return execLinux(command)
	case "windows":
		return execWindows(command)
	default:
		return nil
	}
}