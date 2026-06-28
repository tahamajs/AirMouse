//go:build windows

package proximity

import (
	"syscall"

	"golang.org/x/sys/windows"
)

var (
	user32              = windows.NewLazySystemDLL("user32.dll")
	powrprof            = windows.NewLazySystemDLL("powrprof.dll")
	procLockWorkStation = user32.NewProc("LockWorkStation")
	procSetSuspendState = powrprof.NewProc("SetSuspendState")
)

type WindowsProximity struct {
	powerAvailable bool
}

func NewWindowsProximity() *WindowsProximity {
	return &WindowsProximity{
		powerAvailable: true,
	}
}

func (wp *WindowsProximity) LockWorkstation() error {
	ret, _, _ := procLockWorkStation.Call()
	if ret == 0 {
		return syscall.GetLastError()
	}
	return nil
}

func (wp *WindowsProximity) SetSuspendState(hibernate, forceCritical bool) error {
	hibernateFlag := 0
	if hibernate {
		hibernateFlag = 1
	}
	forceFlag := 0
	if forceCritical {
		forceFlag = 1
	}
	ret, _, _ := procSetSuspendState.Call(
		uintptr(hibernateFlag),
		uintptr(forceFlag),
		0,
	)
	if ret == 0 {
		return syscall.GetLastError()
	}
	return nil
}

// GetSystemPowerStatus returns the system power status using windows.SYSTEM_POWER_STATUS.
func (wp *WindowsProximity) GetSystemPowerStatus() (*windows.SYSTEM_POWER_STATUS, error) {
	var ps windows.SYSTEM_POWER_STATUS
	if err := windows.GetSystemPowerStatus(&ps); err != nil {
		return nil, err
	}
	return &ps, nil
}
