//go:build windows

package syscmd

import (
	"syscall"
	"time"
)

func init() {
	execute = execWindows
}

var (
	user32   = syscall.NewLazyDLL("user32.dll")
	procKey  = user32.NewProc("keybd_event")
	procLock = user32.NewProc("LockWorkStation")
)

const (
	KEYEVENTF_KEYDOWN = 0x0000
	KEYEVENTF_KEYUP   = 0x0002
)

func keybdEvent(vk byte, flags uint32) {
	procKey.Call(uintptr(vk), 0, uintptr(flags), 0)
}

func sendKey(vk byte) {
	keybdEvent(vk, KEYEVENTF_KEYDOWN)
	time.Sleep(10 * time.Millisecond)
	keybdEvent(vk, KEYEVENTF_KEYUP)
}

func execWindows(command string) error {
	switch command {
	case "show_desktop":
		sendKey(0x5B) // Win
		time.Sleep(10 * time.Millisecond)
		sendKey(0x44) // D
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x5B, KEYEVENTF_KEYUP)
	case "task_view":
		sendKey(0x5B) // Win
		time.Sleep(10 * time.Millisecond)
		sendKey(0x09) // Tab
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x5B, KEYEVENTF_KEYUP)
	case "switch_window":
		sendKey(0x12) // Alt
		time.Sleep(10 * time.Millisecond)
		sendKey(0x09) // Tab
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x12, KEYEVENTF_KEYUP)
	case "window_close":
		sendKey(0x12) // Alt
		time.Sleep(10 * time.Millisecond)
		sendKey(0x73) // F4
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x12, KEYEVENTF_KEYUP)
	case "lock_screen":
		procLock.Call()
	case "zoom_in":
		sendKey(0x11) // Ctrl
		time.Sleep(10 * time.Millisecond)
		sendKey(0xBB) // Plus
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x11, KEYEVENTF_KEYUP)
	case "zoom_out":
		sendKey(0x11) // Ctrl
		time.Sleep(10 * time.Millisecond)
		sendKey(0xBD) // Minus
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x11, KEYEVENTF_KEYUP)
	case "zoom_reset":
		sendKey(0x11) // Ctrl
		time.Sleep(10 * time.Millisecond)
		sendKey(0x30) // 0
		time.Sleep(10 * time.Millisecond)
		keybdEvent(0x11, KEYEVENTF_KEYUP)
	default:
		return nil
	}
	return nil
}