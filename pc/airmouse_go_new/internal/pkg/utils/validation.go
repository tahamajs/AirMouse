package utils

import (
	"net"
	"regexp"
)

var ipRegex = regexp.MustCompile(`^((25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)\.){3}(25[0-5]|2[0-4][0-9]|[01]?[0-9][0-9]?)$`)

func IsValidIPv4(ip string) bool {
	return ipRegex.MatchString(ip)
}

func IsValidPort(port int) bool {
	return port > 0 && port < 65536
}