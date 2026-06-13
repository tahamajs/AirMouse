//go:build linux

package proximity

import (
    "os/exec"
    "strings"
)

// Linux-specific proximity features
type LinuxProximity struct {
    dbusAvailable bool
}

func NewLinuxProximity() *LinuxProximity {
    lp := &LinuxProximity{}
    lp.checkDBus()
    return lp
}

func (lp *LinuxProximity) checkDBus() {
    if _, err := exec.LookPath("dbus-send"); err == nil {
        lp.dbusAvailable = true
    }
}

func (lp *LinuxProximity) LockViaDBus() error {
    if !lp.dbusAvailable {
        return nil
    }
    
    cmd := exec.Command("dbus-send", "--type=method_call", "--dest=org.freedesktop.ScreenSaver",
        "/ScreenSaver", "org.freedesktop.ScreenSaver.Lock")
    return cmd.Run()
}

func (lp *LinuxProximity) GetPowerStatus() (string, error) {
    cmd := exec.Command("upower", "-i", "/org/freedesktop/UPower/devices/battery_BAT0")
    output, err := cmd.Output()
    if err != nil {
        return "", err
    }
    
    lines := strings.Split(string(output), "\n")
    for _, line := range lines {
        if strings.Contains(line, "state:") {
            return strings.TrimSpace(strings.Split(line, ":")[1]), nil
        }
    }
    return "unknown", nil
}