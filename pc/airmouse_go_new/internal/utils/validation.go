package utils

import (
	"regexp"
)

var (
	ipRegex   = regexp.MustCompile(`^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$`)
	nameRegex = regexp.MustCompile(`^[a-zA-Z0-9_\- ]+$`)
)

func IsValidIPv4(ip string) bool {
	return ipRegex.MatchString(ip)
}

func IsValidDeviceName(name string) bool {
	return nameRegex.MatchString(name) && len(name) > 0 && len(name) <= 64
}

func IsValidPort(port int) bool {
	return port > 0 && port < 65536
}
