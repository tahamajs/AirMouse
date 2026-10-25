//go:build windows

package proximity

import (
	"syscall"
	"unsafe"

	"golang.org/x/sys/windows"
)

var (
	user32                   = windows.NewLazySystemDLL("user32.dll")
	powrprof                 = windows.NewLazySystemDLL("powrprof.dll")
	kernel32                 = windows.NewLazySystemDLL("kernel32.dll")
	procLockWorkStation      = user32.NewProc("LockWorkStation")
	procSetSuspendState      = powrprof.NewProc("SetSuspendState")
	procGetSystemPowerStatus = kernel32.NewProc("GetSystemPowerStatus")
)

// SystemPowerStatus represents the Windows SYSTEM_POWER_STATUS struct.
type SystemPowerStatus struct {
	ACLineStatus        byte
	BatteryFlag         byte
	BatteryLifePercent  byte
	SystemStatusFlag    byte
	BatteryLifeTime     uint32
	BatteryFullLifeTime uint32
}

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

// GetSystemPowerStatus returns the system power status using the Windows API.
func (wp *WindowsProximity) GetSystemPowerStatus() (*SystemPowerStatus, error) {
	var ps SystemPowerStatus
	ret, _, err := procGetSystemPowerStatus.Call(uintptr(unsafe.Pointer(&ps)))
	if ret == 0 {
		return nil, err
	}
	return &ps, nil
}
