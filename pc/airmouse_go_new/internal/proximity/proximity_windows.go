//go:build windows

package proximity

import (
    "syscall"
    "unsafe"
)

var (
    user32               = syscall.NewLazyDLL("user32.dll")
    powrprof             = syscall.NewLazyDLL("powrprof.dll")
    procLockWorkStation  = user32.NewProc("LockWorkStation")
    procSetSuspendState  = powrprof.NewProc("SetSuspendState")
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
    
    ret, _, _ := procSetSuspendState.Call(uintptr(hibernateFlag), uintptr(forceFlag), 0)
    if ret == 0 {
        return syscall.GetLastError()
    }
    return nil
}

func (wp *WindowsProximity) GetSystemPowerStatus() (*syscall.SystemPowerStatus, error) {
    var ps syscall.SystemPowerStatus
    if err := syscall.GetSystemPowerStatus(&ps); err != nil {
        return nil, err
    }
    return &ps, nil
}