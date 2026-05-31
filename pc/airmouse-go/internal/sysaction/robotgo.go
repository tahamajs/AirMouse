//go:build robotgo

package sysaction

import (
	"github.com/go-vgo/robotgo"
)

func keyTap(key string) {
	robotgo.KeyTap(key)
}

func mouseClick(btn string) {
	robotgo.Click(btn)
}

func mouseDoubleClick() {
	robotgo.Click("left", true)
}

func mouseScroll(delta int) {
	robotgo.Scroll(0, delta)
}

func lockScreen() {
	// robotgo doesn't have lock; use fallback to OS command
	lockScreenFallback()
}