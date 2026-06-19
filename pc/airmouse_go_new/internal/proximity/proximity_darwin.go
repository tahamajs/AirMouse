//go:build darwin

package proximity

import (
    "os/exec"
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
    
    cmd := exec.Command("caffeinate", "-t", string(rune(durationSec)))
    return cmd.Start()
}

func (dp *DarwinProximity) IsDisplaySleeping() (bool, error) {
    cmd := exec.Command("pmset", "-g", "powerstate")
    _, err := cmd.Output()
    if err != nil {
        return false, err
    }
    
    // Parse output to check display state
    return false, nil
}

func (dp *DarwinProximity) WakeDisplay() error {
    // Simulate mouse movement to wake display
    // This would use CGEventPost in real implementation
    return nil
}
