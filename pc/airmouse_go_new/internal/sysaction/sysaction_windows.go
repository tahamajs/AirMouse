//go:build windows

package sysaction

import (
	"syscall"
	"time"
	"unsafe"
)

var (
	user32     = syscall.NewLazyDLL("user32.dll")
	kernel32   = syscall.NewLazyDLL("kernel32.dll")
	powrprof   = syscall.NewLazyDLL("powrprof.dll")

	procKeybdEvent = user32.NewProc("keybd_event")
	procMouseEvent = user32.NewProc("mouse_event")
	procLockWorkStation = user32.NewProc("LockWorkStation")
	procExitWindowsEx = user32.NewProc("ExitWindowsEx")
	procSetSuspendState = powrprof.NewProc("SetSuspendState")
)

const (
	KEYEVENTF_KEYDOWN = 0x0000
	KEYEVENTF_KEYUP   = 0x0002

	MOUSEEVENTF_LEFTDOWN   = 0x0002
	MOUSEEVENTF_LEFTUP     = 0x0004
	MOUSEEVENTF_RIGHTDOWN  = 0x0008
	MOUSEEVENTF_RIGHTUP    = 0x0010
	MOUSEEVENTF_MIDDLEDOWN = 0x0020
	MOUSEEVENTF_MIDDLEUP   = 0x0040
	MOUSEEVENTF_WHEEL      = 0x0800

	EWX_SHUTDOWN = 0x00000001
	EWX_REBOOT   = 0x00000002
	EWX_FORCE    = 0x00000004
	EWX_LOGOFF   = 0x00000000
)

// Virtual key codes (simplified)
var winKeyMap = map[string]byte{
	"PlayPause": 0xB3,
	"Next":      0xB0,
	"Previous":  0xB1,
	"Stop":      0xB2,
	"VolumeUp":  0xAF,
	"VolumeDown": 0xAE,
	"Mute":      0xAD,
	"browser_back": 0xA6,
	"browser_forward": 0xA7,
	"browser_refresh": 0xA8,
	"browser_home": 0xAC,
	"page_up":    0x21,
	"page_down":  0x22,
	"home":       0x24,
	"end":        0x23,
	"delete":     0x2E,
	"backspace":  0x08,
	"enter":      0x0D,
	"escape":     0x1B,
	"tab":        0x09,
	"f1":         0x70,
	"f2":         0x71,
	"f3":         0x72,
	"f4":         0x73,
	"f5":         0x74,
	"f6":         0x75,
	"f7":         0x76,
	"f8":         0x77,
	"f9":         0x78,
	"f10":        0x79,
	"f11":        0x7A,
	"f12":        0x7B,
	"ctrl":       0x11,
	"alt":        0x12,
	"shift":      0x10,
	"win":        0x5B,
}

func init() {
	mediaKey = func(key string) {
		if vk, ok := winKeyMap[key]; ok {
			keybdEvent(vk, KEYEVENTF_KEYDOWN)
			time.Sleep(10 * time.Millisecond)
			keybdEvent(vk, KEYEVENTF_KEYUP)
		}
	}
	volumeUp = func() {
		for i := 0; i < 5; i++ {
			mediaKey("VolumeUp")
			time.Sleep(5 * time.Millisecond)
		}
	}
	volumeDown = func() {
		for i := 0; i < 5; i++ {
			mediaKey("VolumeDown")
			time.Sleep(5 * time.Millisecond)
		}
	}
	volumeMute = func() {
		mediaKey("Mute")
	}
	mouseClick = func(btn string) {
		var down, up uint32
		switch btn {
		case "left":
			down, up = MOUSEEVENTF_LEFTDOWN, MOUSEEVENTF_LEFTUP
		case "right":
			down, up = MOUSEEVENTF_RIGHTDOWN, MOUSEEVENTF_RIGHTUP
		case "middle":
			down, up = MOUSEEVENTF_MIDDLEDOWN, MOUSEEVENTF_MIDDLEUP
		default:
			return
		}
		procMouseEvent.Call(uintptr(down), 0, 0, 0, 0)
		time.Sleep(10 * time.Millisecond)
		procMouseEvent.Call(uintptr(up), 0, 0, 0, 0)
	}
	mouseDoubleClick = func() {
		mouseClick("left")
		time.Sleep(50 * time.Millisecond)
		mouseClick("left")
	}
	mouseScroll = func(delta int) {
		procMouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
	}
	lockScreen = func() {
		procLockWorkStation.Call()
	}
	showDesktop = func() {
		keyCombination("win", "win")
		time.Sleep(10 * time.Millisecond)
		keyTap("d")
	}
	taskView = func() {
		keyCombination("win", "win")
		time.Sleep(10 * time.Millisecond)
		keyTap("tab")
	}
	switchWindow = func() {
		keyCombination("tab", "alt")
	}
	keyCombination = func(key, modifier string) {
		var modKey byte
		switch modifier {
		case "ctrl":
			modKey = winKeyMap["ctrl"]
		case "alt":
			modKey = winKeyMap["alt"]
		case "shift":
			modKey = winKeyMap["shift"]
		case "win":
			modKey = winKeyMap["win"]
		}
		if modKey != 0 {
			keybdEvent(modKey, KEYEVENTF_KEYDOWN)
		}
		if vk, ok := winKeyMap[key]; ok {
			keybdEvent(vk, KEYEVENTF_KEYDOWN)
			time.Sleep(10 * time.Millisecond)
			keybdEvent(vk, KEYEVENTF_KEYUP)
		}
		if modKey != 0 {
			time.Sleep(10 * time.Millisecond)
			keybdEvent(modKey, KEYEVENTF_KEYUP)
		}
	}
	keyTap = func(key string) {
		if vk, ok := winKeyMap[key]; ok {
			keybdEvent(vk, KEYEVENTF_KEYDOWN)
			time.Sleep(10 * time.Millisecond)
			keybdEvent(vk, KEYEVENTF_KEYUP)
		}
	}
	zoomIn = func() {
		keyCombination("plus", "ctrl")
	}
	zoomOut = func() {
		keyCombination("minus", "ctrl")
	}
	zoomReset = func() {
		keyCombination("0", "ctrl")
	}
	minimizeWindow = func() {
		keyCombination("win", "win")
		time.Sleep(10 * time.Millisecond)
		keyTap("down")
	}
	maximizeWindow = func() {
		keyCombination("win", "win")
		time.Sleep(10 * time.Millisecond)
		keyTap("up")
	}
	closeWindow = func() {
		keyCombination("f4", "alt")
	}
	altTab = func() {
		keyCombination("tab", "alt")
	}
	sleepSystem = func() {
		procSetSuspendState.Call(0, 0, 0)
	}
	shutdownSystem = func() {
		procExitWindowsEx.Call(EWX_SHUTDOWN|EWX_FORCE, 0)
	}
	restartSystem = func() {
		procExitWindowsEx.Call(EWX_REBOOT|EWX_FORCE, 0)
	}
	logoutSystem = func() {
		procExitWindowsEx.Call(EWX_LOGOFF, 0)
	}
}

func keybdEvent(vk byte, flags uint32) {
	procKeybdEvent.Call(uintptr(vk), 0, uintptr(flags), 0)
}