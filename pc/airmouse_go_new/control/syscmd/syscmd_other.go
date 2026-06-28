//go:build !darwin && !linux && !windows

package syscmd

func executePlatformCommand(command string) error {
	return nil
}
