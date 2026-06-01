package utils

import (
    "net"
)

// GetLocalIP returns the first non‑loopback IPv4 address.
func GetLocalIP() string {
    addrs, err := net.InterfaceAddrs()
    if err != nil {
        return "127.0.0.1"
    }
    for _, addr := range addrs {
        if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
            return ipnet.IP.String()
        }
    }
    return "127.0.0.1"
}

// GetAllLocalIPs returns all non‑loopback IPv4 addresses.
func GetAllLocalIPs() []string {
    var ips []string
    addrs, err := net.InterfaceAddrs()
    if err != nil {
        return []string{"127.0.0.1"}
    }
    for _, addr := range addrs {
        if ipnet, ok := addr.(*net.IPNet); ok && !ipnet.IP.IsLoopback() && ipnet.IP.To4() != nil {
            ips = append(ips, ipnet.IP.String())
        }
    }
    if len(ips) == 0 {
        ips = append(ips, "127.0.0.1")
    }
    return ips
}