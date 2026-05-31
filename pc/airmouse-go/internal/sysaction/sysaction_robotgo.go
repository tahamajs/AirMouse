//go:build robotgo

package sysaction

import "github.com/go-vgo/robotgo"

// robotgo-backed implementation (enabled when built with -tags robotgo)
func KeyTap(action string) {
    robotgo.KeyTap(action)
}
