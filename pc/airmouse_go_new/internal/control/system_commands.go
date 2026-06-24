//go:build !darwin

package control

import "airmouse-go/internal/utils"

// ExecuteSystemCommand runs a desktop-level command on the host.
// Platform-specific implementations live behind build tags.
func ExecuteSystemCommand(command string) error {
	utils.LogDebug("System command not supported on this platform: %s", command)
	return nil
}
