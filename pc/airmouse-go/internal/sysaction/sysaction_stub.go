package sysaction

import "log"

// Default stub for system actions when robotgo isn't enabled.
func KeyTap(action string) {
    log.Printf("[sysaction stub] KeyTap: %s", action)
}
