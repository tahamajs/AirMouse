//go:build darwin

package proximity

import (
	"os/exec"
	"strconv"
)

type DarwinProximity struct {
	caffeinateAvailable bool
}

func NewDarwinProximity() *DarwinProximity {
	dp := &DarwinProximity{}
	dp.checkCaffeinate()
	return dp
}

func (dp *DarwinProximity) checkCaffeinate() {
	if _, err := exec.LookPath("caffeinate"); err == nil {
		dp.caffeinateAvailable = true
	}
}

func (dp *DarwinProximity) PreventSleep(durationSec int) error {
	if !dp.caffeinateAvailable {
		return nil
	}
	// Use strconv.Itoa to convert the integer to a string
	cmd := exec.Command("caffeinate", "-t", strconv.Itoa(durationSec))
	return cmd.Start()
}

func (dp *DarwinProximity) IsDisplaySleeping() (bool, error) {
	cmd := exec.Command("pmset", "-g", "powerstate")
	_, err := cmd.Output()
	if err != nil {
		return false, err
	}
	// For now, return false (not sleeping) – implement proper parsing if needed
	return false, nil
}

func (dp *DarwinProximity) WakeDisplay() error {
	// In production, you would use CGEventPost to move the mouse.
	// For now, this is a no‑op.
	return nil
}
