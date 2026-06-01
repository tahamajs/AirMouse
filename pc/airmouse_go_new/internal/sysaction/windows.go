//go:build windows

package sysaction

import (
	"syscall"
)

var (
	user32           = syscall.NewLazyDLL("user32.dll")
	procKeybdEvent   = user32.NewProc("keybd_event")
	procMouseEvent   = user32.NewProc("mouse_event")
	procLockWorkStation = user32.NewProc("LockWorkStation")
)

const (
	KEYEVENTF_KEYDOWN = 0x0000
	KEYEVENTF_KEYUP   = 0x0002
	MOUSEEVENTF_LEFTDOWN   = 0x0002
	MOUSEEVENTF_LEFTUP     = 0x0004
	MOUSEEVENTF_RIGHTDOWN  = 0x0008
	MOUSEEVENTF_RIGHTUP    = 0x0010
	MOUSEEVENTF_WHEEL      = 0x0800
)

var keyMap = map[string]byte{
	"media_play_pause": 0xB3,
	"media_next":       0xB0,
	"media_prev":       0xB1,
	"audio_vol_up":     0xAF,
	"audio_vol_down":   0xAE,
	"audio_mute":       0xAD,
	"media_stop":       0xB2,
	"browser_back":     0xA6,
	"browser_forward":  0xA7,
}

func keyTap(key string) {
	vk, ok := keyMap[key]
	if !ok {
		return
	}
	procKeybdEvent.Call(uintptr(vk), 0, KEYEVENTF_KEYDOWN, 0)
	procKeybdEvent.Call(uintptr(vk), 0, KEYEVENTF_KEYUP, 0)
}

func mouseClick(btn string) {
	if btn == "left" {
		procMouseEvent.Call(MOUSEEVENTF_LEFTDOWN, 0, 0, 0, 0)
		procMouseEvent.Call(MOUSEEVENTF_LEFTUP, 0, 0, 0, 0)
	} else if btn == "right" {
		procMouseEvent.Call(MOUSEEVENTF_RIGHTDOWN, 0, 0, 0, 0)
		procMouseEvent.Call(MOUSEEVENTF_RIGHTUP, 0, 0, 0, 0)
	}
}

func mouseDoubleClick() {
	mouseClick("left")
	mouseClick("left")
}

func mouseScroll(delta int) {
	procMouseEvent.Call(MOUSEEVENTF_WHEEL, 0, 0, uintptr(delta*120), 0)
}

func lockScreen() {
	procLockWorkStation.Call()
}