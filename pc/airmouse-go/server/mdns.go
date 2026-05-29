package server

import (
	"fmt"
	"net"
	"os"
	"strings"

	"github.com/hashicorp/mdns"
)

func StartMDNS(name string, ip string, port int, logFn func(string)) {
	host, _ := os.Hostname()
	if idx := strings.Index(host, "."); idx > 0 {
		host = host[:idx]
	}

	var addrs []net.IP
	if ip != "" && ip != "127.0.0.1" {
		addrs = []net.IP{net.ParseIP(ip)}
	}

	info := []string{"Air Mouse Server", "version=1.0"}
	service, err := mdns.NewMDNSService(
		name,
		"_airmouse._tcp",
		host+".local.",
		"",
		port,
		addrs,
		info,
	)
	if err != nil {
		logFn("⚠️ mDNS service creation failed: " + err.Error())
		return
	}
	_, err = mdns.NewServer(&mdns.Config{Zone: service})
	if err != nil {
		logFn("⚠️ mDNS server start failed: " + err.Error())
		return
	}
	logFn(fmt.Sprintf("🌐 mDNS service advertised as %s._airmouse._tcp.local (port %d)", name, port))
}