//go:build windows

package sysaction

import (
    "syscall"
    "time"
    "unsafe"
)

var (
    user32               = syscall.NewLazyDLL("user32.dll")
    kernel32             = syscall.NewLazyDLL("kernel32.dll")
    powrprof             = syscall.NewLazyDLL("powrprof.dll")
    
    procKeybdEvent       = user32.NewProc("keybd_event")
    procMouseEvent       = user32.NewProc("mouse_event")
    procSetCursorPos     = user32.NewProc("SetCursorPos")
    procGetCursorPos     = user32.NewProc("GetCursorPos")
    procLockWorkStation  = user32.NewProc("LockWorkStation")
    procExitWindowsEx    = user32.NewProc("ExitWindowsEx")
    procSetThreadExecutionState = kernel32.NewProc("SetThreadExecutionState")
    procSetSuspendState  = powrprof.NewProc("SetSuspendState")
)

const (
    KEYEVENTF_KEYDOWN = 0x0000
    KEYEVENTF_KEYUP   = 0x0002
    KEYEVENTF_EXTENDEDKEY = 0x0001
    
    MOUSEEVENTF_LEFTDOWN   = 0x0002
    MOUSEEVENTF_LEFTUP     = 0x0004
    MOUSEEVENTF_RIGHTDOWN  = 0x0008
    MOUSEEVENTF_RIGHTUP    = 0x0010
    MOUSEEVENTF_MIDDLEDOWN = 0x0020
    MOUSEEVENTF_MIDDLEUP   = 0x0040
    MOUSEEVENTF_WHEEL      = 0x0800
    MOUSEEVENTF_HWHEEL     = 0x1000
    
    EWX_SHUTDOWN      = 0x00000001
    EWX_REBOOT        = 0x00000002
    EWX_FORCE         = 0x00000004
    EWX_LOGOFF        = 0x00000000
    
    ES_CONTINUOUS     = 0x80000000
    ES_SYSTEM_REQUIRED = 0x00000001
    ES_DISPLAY_REQUIRED = 0x00000002
)

// Key codes for Windows
var keyMap = map[string]byte{
    // Media Keys
    "PlayPause": 0xB3,
    "Next":      0xB0,
    "Previous":  0xB1,
    "Stop":      0xB2,
    
    // Volume Keys
    "VolumeUp":   0xAF,
    "VolumeDown": 0xAE,
    "Mute":       0xAD,
    
    // Browser Keys
    "browser_back":     0xA6,
    "browser_forward":  0xA7,
    "browser_refresh":  0xA8,
    "browser_home":     0xAC,
    
    // Navigation
    "page_up":    0x21,
    "page_down":  0x22,
    "home":       0x24,
    "end":        0x23,
    "delete":     0x2E,
    "backspace":  0x08,
    "enter":      0x0D,
    "escape":     0x1B,
    "tab":        0x09,
    
    // Function Keys
    "f1":  0x70,
    "f2":  0x71,
    "f3":  0x72,
    "f4":  0x73,
    "f5":  0x74,
    "f6":  0x75,
    "f7":  0x76,
    "f8":  0x77,
    "f9":  0x78,
    "f10": 0x79,
    "f11": 0x7A,
    "f12": 0x7B,
    
    // Modifier keys
    "ctrl":  0x11,
    "alt":   0x12,
    "shift": 0x10,
    "win":   0x5B,
}

func mediaKey(key string) {
    if vk, ok := keyMap[key]; ok {
        keybdEvent(vk, KEYEVENTF_KEYDOWN)
        time.Sleep(10 * time.Millisecond)
        keybdEvent(vk, KEYEVENTF_KEYUP)
    }
}

func volumeUp() {
    for i := 0; i < 5; i++ {
        mediaKey("VolumeUp")
        time.Sleep(5 * time.Millisecond)
    }
}

func volumeDown() {
    for i := 0; i < 5; i++ {
        mediaKey("VolumeDown")
        time.Sleep(5 * time.Millisecond)
    }
}

func volumeMute() {
    mediaKey("Mute")
}

func keyTap(key string) {
    if vk, ok := keyMap[key]; ok {
        keybdEvent(vk, KEYEVENTF_KEYDOWN)
        time.Sleep(10 * time.Millisecond)
        keybdEvent(vk, KEYEVENTF_KEYUP)
    }
}

func keyCombination(key, modifier string) {
    var modKey byte
    switch modifier {
    case "ctrl":
        modKey = keyMap["ctrl"]
    case "alt":
        modKey = keyMap["alt"]
    case "shift":
        modKey = keyMap["shift"]
    case "win":
        modKey = keyMap["win"]
    }
    
    if modKey != 0 {
        keybdEvent(modKey, KEYEVENTF_KEYDOWN)
    }
    
    if vk, ok := keyMap[key]; ok {
        keybdEvent(vk, KEYEVENTF_KEYDOWN)
        time.Sleep(10 * time.Millisecond)
        keybdEvent(vk, KEYEVENTF_KEYUP)
    }
    
    if modKey != 0 {
        time.Sleep(10 * time.Millisecond)
        keybdEvent(modKey, KEYEVENTF_KEYUP)
    }
}

func keybdEvent(vk byte, flags uint32) {
    procKeybdEvent.Call(uintptr(vk), 0, uintptr(flags), 0)
}

func mouseClick(btn string) {
    var downFlag, upFlag uint32
    
    switch btn {
    case "left":
        downFlag = MOUSEEVENTF_LEFTDOWN
        upFlag = MOUSEEVENTF_LEFTUP
    case "right":
        downFlag = MOUSEEVENTF_RIGHTDOWN
        upFlag = MOUSEEVENTF_RIGHTUP
    case "middle":
        downFlag = MOUSEEVENTF_MIDDLEDOWN
        upFlag = MOUSEEVENTF_MIDDLEUP
    default:
        return
    }
    
    procMouseEvent.Call(uintptr(downFlag), 0, 0, 0, 0)
    time.Sleep(10 * time.Millisecond)
    procMouseEvent.Call(uintptr(upFlag), 0, 0, 0, 0)
}

func mouseDoubleClick() {
    mouseClick("left")
    time.Sleep(50 * time.Millisecond)
    mouseClick("left")
}

func mouseScroll(delta int) {
    procMouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}

func lockScreen() {
    procLockWorkStation.Call()
}

func showDesktop() {
    keyCombination("win", "win")
    keyTap("d")
}

func taskView() {
    keyCombination("win", "win")
    keyTap("tab")
}

func switchWindow() {
    keyCombination("tab", "alt")
}

func minimizeWindow() {
    keyCombination("win", "win")
    keyTap("down")
}

func maximizeWindow() {
    keyCombination("win", "win")
    keyTap("up")
}

func closeWindow() {
    keyCombination("f4", "alt")
}

func altTab() {
    keyCombination("tab", "alt")
}

func zoomIn() {
    keyCombination("plus", "ctrl")
}

func zoomOut() {
    keyCombination("minus", "ctrl")
}

func zoomReset() {
    keyCombination("0", "ctrl")
}

func sleepSystem() {
    procSetSuspendState.Call(0, 0, 0)
}

func shutdownSystem() {
    procExitWindowsEx.Call(EWX_SHUTDOWN|EWX_FORCE, 0)
}

func restartSystem() {
    procExitWindowsEx.Call(EWX_REBOOT|EWX_FORCE, 0)
}

func logoutSystem() {
    procExitWindowsEx.Call(EWX_LOGOFF, 0)
}