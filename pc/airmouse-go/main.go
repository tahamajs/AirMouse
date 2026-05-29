package main

import (
	"airmouse-go/config"
	"airmouse-go/control"
	"airmouse-go/server"
	"airmouse-go/ui"
	"fmt"
	"os"
)

func main() {
	cfg, err := config.Load("config.json")
	if err != nil {
		fmt.Fprintf(os.Stderr, "config error: %v\n", err)
		os.Exit(1)
	}

	mouseCtrl := control.NewMouseController(cfg.Sensitivity)

	// Build the GUI first – this creates all widgets
	app := ui.NewApp(cfg, mouseCtrl)

	// Now start background services (they can safely call ui.Log)
	go server.StartUDPDiscovery(cfg.DiscoveryPort, func() string { return cfg.SelectedIP }, ui.Log)
	go server.StartMDNS(cfg.MDNSName, cfg.SelectedIP, cfg.Port, ui.Log)

	// Run the application (blocks until quit)
	app.Run()
}