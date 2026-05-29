package server

import (
	"fmt"
	"net"
)

func StartUDPDiscovery(port int, ipProvider func() string, logFn func(string)) {
	addr := net.UDPAddr{Port: port}
	conn, err := net.ListenUDP("udp4", &addr)
	if err != nil {
		logFn("❌ UDP discovery failed: " + err.Error())
		return
	}
	defer conn.Close()
	logFn(fmt.Sprintf("🔍 UDP discovery on port %d", port))
	buf := make([]byte, 1024)
	for {
		n, clientAddr, err := conn.ReadFromUDP(buf)
		if err != nil {
			continue
		}
		msg := string(buf[:n])
		if msg == "AIRMOUSE_DISCOVER" {
			ip := ipProvider()
			resp := fmt.Sprintf(`{"type":"discovery_response","port":8080,"ip":"%s"}`, ip)
			conn.WriteToUDP([]byte(resp), clientAddr)
			logFn(fmt.Sprintf("📡 Responded to %s with %s", clientAddr.IP, ip))
		}
	}
}