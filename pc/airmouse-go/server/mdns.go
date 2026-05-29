package server

import (
	"fmt"
	"os"

	"github.com/hashicorp/mdns"
)

func StartMDNS(name string, ip string, port int, logFn func(string)) {
	host, _ := os.Hostname()
	info := []string{"Air Mouse Server"}
	service, err := mdns.NewMDNSService(host, "_airmouse._tcp", "", host+"."+name+".local.", port, nil, info)
	if err != nil {
		logFn("⚠️ mDNS failed: " + err.Error())
		return
	}
	_, err = mdns.NewServer(&mdns.Config{Zone: service})
	if err != nil {
		logFn("⚠️ mDNS server start failed: " + err.Error())
		return
	}
	logFn(fmt.Sprintf("🌐 mDNS advertised as %s.local:%d", name, port))
	// Runs forever.
}