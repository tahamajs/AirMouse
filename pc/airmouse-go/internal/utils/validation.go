package utils

import (
	"regexp"
)

var (
	ipRegex   = regexp.MustCompile(`^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$`)
	nameRegex = regexp.MustCompile(`^[a-zA-Z0-9_\- ]+$`)
)

// IsValidIPv4 checks if a string is a valid IPv4 address.
func IsValidIPv4(ip string) bool {
	return ipRegex.MatchString(ip)
}

// IsValidDeviceName checks if a device name contains only allowed characters.
func IsValidDeviceName(name string) bool {
	return nameRegex.MatchString(name) && len(name) > 0 && len(name) <= 64
}